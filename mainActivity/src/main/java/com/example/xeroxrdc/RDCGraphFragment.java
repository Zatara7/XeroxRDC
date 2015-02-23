package com.example.xeroxrdc;

/**
 * RDC Graph Fragment
 * This page shows the image analysis results in graph format.
 * It also allows the user to view two frames side by side.
 * @author Andrew
 * @author Daragh
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import ar.com.daidalos.afiledialog.FileChooserDialog;




import com.xeroxrdc.rof.ROFRider;

public class RDCGraphFragment extends Fragment implements OnClickListener
{
	private static final String TAG = "XeroxRDC::RDCGraphFragment";
	
	// List of valid extensions for images 
	private static final String[] IMAGE_EXTENSIONS = new String[] {"jpg", "jpeg", "png"};

	private View rootView;
	
	private Button imgScan;
	private Button chooseDirectoryButton;
	private Button referenceButton;
	private Button saveGraphButton;
	
	private GraphicalView mChartView;
	private String directory2;

	public static boolean topImage = true;
	public static boolean graphExists = false;
	
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
	
	// private for use between event handlers
	private List<FilenameAndData> filenameAndDataList = new ArrayList<FilenameAndData>();

	private ROFRider rof;

	private TextView chooseDirectoryTextView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Get views
		rootView = inflater.inflate(R.layout.graphpage, container, false);
		imgScan = (Button) rootView.findViewById(R.id.imgscan);
		chooseDirectoryButton = (Button) rootView.findViewById(R.id.chooseDirectoryButton);
		referenceButton = (Button) rootView.findViewById(R.id.referenceButton);
		saveGraphButton = (Button) rootView.findViewById(R.id.saveGraphButton);

		
		// Set callbacks
		imgScan.setOnClickListener(this);
		chooseDirectoryButton.setOnClickListener(this);
		referenceButton.setOnClickListener(this);
		saveGraphButton.setOnClickListener(this);

		// Set up the toggle button to switch between displaying the top/bottom
		// image when clicking the graph
		Switch imageSwitch = (Switch) rootView.findViewById(R.id.imageSwitch);
		imageSwitch
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
				{
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
					{
						topImage = isChecked;
					}
				});
		
		imageSwitch.setChecked(true); // Initialize toggle/switch as the top image

		chooseDirectoryTextView = (TextView) rootView.findViewById(R.id.directoryChosenTextView);

		return rootView;
	}

	/**
	 * Parse a filename and return a String containing its extension
	 * @param filename
	 * @return
	 */
	private static String getExtension(String filename)
	{
		String ext = "";
		
		int i = filename.lastIndexOf('.');
		if (i > 0)
			ext = filename.substring(i+1);
		
		return ext;
	}
	
	/**
	 * Determines if filename is the correct type of file (based on the extension)
	 * Only returns true if the extension is in IMAGE_EXTENSIONS
	 * @param filename
	 * @return
	 */
	private static boolean imageFileFilter(String filename)
	{
		String ext = getExtension(filename);
		
		// Check against valid extensions
		for (int i = 0; i < IMAGE_EXTENSIONS.length; i++)
		{
			// If extension is good, return true now
			if (ext.equalsIgnoreCase(IMAGE_EXTENSIONS[i]))
				return true;
		}
		// The extension isn't on the list, so return false
		return false;
	}
	
	// Source: http://stackoverflow.com/a/5694473/2561421
	/**
	 * 
	 * @param parentDir
	 * @return
	 */
	private List<String> getAndAlphabetizeListFilenames(String parentDir)
	{
		List<String> results = new ArrayList<String>();
		File[] files = new File(parentDir).listFiles();

		for (File file : files)
		{
			if (file.isFile() && imageFileFilter(file.getName()) && !file.getName().equals("GraphImage.png"))
			{
				System.out.println("Filename: " + file.getName());
				// Add the full path of the file
				results.add(file.getAbsolutePath()); 
			}
		}
		Collections.sort(results); // alphabetize by filename
		return results;
	}

	/**
	 * Create FilenameAndData objects, initialize their filenames, and add them to a list
	 * @param filenameList ArrayList of filenames
	 * @param filenameAndDataList ArrayList of FilenameAndData objects
	 */
	private void addFilenamesToFilenameAndDataList(List<String> filenameList, List<FilenameAndData> filenameAndDataList)
	{
		long sizeList = filenameList.size();
		for (int i = 0; i < sizeList; i++)
		{
			// create new object
			FilenameAndData tempFilenameAndData = new FilenameAndData();
			// set filename
			tempFilenameAndData.filename = filenameList.get(i);
			// Add it to the list
			filenameAndDataList.add(tempFilenameAndData);
		}
	}

	/**
	 * Open each file in the list and perform image analysis
	 * @param filenameAndDataList
	 * 
	 * Subclass that extends AsyncTask to spawn a new thread for the image analysis. Also handles progress bar.
	 */
	private class imageAnalysis extends AsyncTask<List<FilenameAndData>, Integer, Integer> {
		ProgressDialog usrDialog;
		int numValsInList;				//Stores number of frames passed. This way .size() isn't called multiple times
		
		protected void onPreExecute() {
			usrDialog = new ProgressDialog((MainActivity)getActivity());		//Prepping progress bar
			usrDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			usrDialog.setMax(numValsInList);
			usrDialog.setMessage("Analyzing frames");
			usrDialog.show();
			return;
		}
		
		protected Integer doInBackground(List<FilenameAndData>... filenameAndDataList) {		//This is just the old imageAnalysisDriver function
			FilenameAndData tmp = null;
			
			// Set faster options
			rof.setLocateRealEdge(false);
			rof.setOrientationMethod(ROFRider.METHOD_CALIBRATION_ROBUST);
			
			
			// Perform image analysis on each image
			for (int i = 0; i < numValsInList; i++)
			{
				try
				{
					publishProgress(i);				//TODO find a more finite method of calculating progress
					
					// Perform analysis
					rof.singleFrameDiagnostic(loadMat(filenameAndDataList[0].get(i).filename));

					// Show results
					
					if (!rof.foundROF())
						Log.i(TAG, "ROF not found!");
					else
						Log.i(TAG, "Found ROF!");
					
					if (!rof.foundSensor())
						Log.i(TAG, "Sensor not found!");
					else
						Log.i(TAG, "Found Sensor!");
					
					Log.i(TAG, "ROF to edge: " + rof.getMarkToEdge() + "mm");
					Log.i(TAG, "ROF to reference: " + rof.getMarkToRef() + "mm");
					Log.i(TAG, "Sensor to edge: " + rof.getSensorToEdge() + "mm");
					Log.i(TAG, "Sensor to reference: " + rof.getSensorToRef() + "mm");

					
					tmp = filenameAndDataList[0].get(i);
					tmp.foundRof = rof.foundROF();
					tmp.foundSensor = rof.foundSensor();
					
					tmp.markToEdge = rof.getMarkToEdge();
					tmp.markToRef = rof.getMarkToRef();
					tmp.sensorToEdge = rof.getSensorToEdge();
					tmp.sensorToRef = rof.getSensorToRef();

				} 
				catch (IOException e)
				{
					Log.e(TAG, "Image analysis: " + e);
				}
			}

			return 0;
		}
		
		protected void onProgressUpdate(Integer... progress) {
			usrDialog.setProgress(progress[0]);
			
			return;
		}
		
		protected void onPostExecute(Integer result) {			//This contains the function to start the graph. This ensures that
			usrDialog.dismiss();								// android waits for the analysis to be complete before rendering
																// the graph.
			parseLineChartData(filenameAndDataList);

			// Display the graphs
			displayLineGraph();
			
			
			// Get/Display image on graph click:
			// Source: http://stackoverflow.com/a/8789607/2561421
			// Source: http://stackoverflow.com/a/14465500/2561421
			// Needs an image view (initialized before this)
			// enable the chart click events
			mRenderer.setClickEnabled(true);
			mRenderer.setSelectableBuffer(100);
			mChartView.setOnClickListener(new View.OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					SeriesSelection seriesSelection = mChartView
							.getCurrentSeriesAndPoint();
//					double[] xy = mChartView.toRealPoint(0);

					if (seriesSelection == null)
					{
						Toast.makeText(getActivity(), "No chart element was clicked", Toast.LENGTH_SHORT).show();
					}
					else
					{

						/*
						 * Toast.makeText( getActivity(),
						 * "Chart element in series index " +
						 * seriesSelection.getSeriesIndex() + " data point index " +
						 * seriesSelection.getPointIndex() + " was clicked" +
						 * " closest point value X=" + seriesSelection.getXValue() +
						 * ", Y=" + seriesSelection.getValue() +
						 * " clicked point value X=" + (float) xy[0] + ", Y=" +
						 * (float) xy[1], Toast.LENGTH_SHORT).show();
						 */
						// Use x coordinate get filename, based off of index in
						// file and data list
						// Get filename of image for point selected
						String tempFilename = null;

						if (seriesSelection.getXValue() <= filenameAndDataList.size())
						{ 
							// Make sure the x value is within bounds of the list
							// Get the filename for the image at that position
							tempFilename = filenameAndDataList.get((int) seriesSelection.getXValue()).filename; 
							
						}

						if (tempFilename == null)
						{
							System.out.println("Image not found in list!");
							Toast.makeText(getActivity(),
									"Image " + tempFilename + "not found in list!",
									Toast.LENGTH_SHORT).show();
						}
						else
						{
							Toast.makeText(getActivity(), tempFilename + "\nwas clicked", Toast.LENGTH_SHORT).show();
							System.out.println("CLICKED!");

							ImageView topImg;
							topImg = (ImageView) rootView.findViewById(R.id.imageView1);

							ImageView bottomImg;
							bottomImg = (ImageView) rootView.findViewById(R.id.imageView2);

							File f = new File(tempFilename);
							Bitmap bitmap2 = null;

							// This is for debug mode (when loading the pngs
							// from the test folder)
							try
							{
								bitmap2 = BitmapFactory.decodeStream( new FileInputStream(f), null, null);
							} 
							catch (FileNotFoundException e)
							{
								e.printStackTrace();
							}

							/*
							 * // This is for normal mode (since it has to load the
							 * image as a byte array) try { image.setImageBitmap
							 * (RDCVideoFragment.convertByteArrayToBitmap
							 * (loadByteArrayFromDisk(tempFilename))); } catch
							 * (IOException e) {
							 * e.printStackTrace(); }
							 */

							// Update the image view
							if (topImage == true)
							{
								topImg.setImageBitmap(bitmap2);
							}
							else
							{
								bottomImg.setImageBitmap(bitmap2);
							}

						}

						// ///////////////////////////////////////////////////////////////////////////////////////
						// ///////// This was code in case we decided to draw
						// the marks on the images/////////////
						// Put it in a FilenameAndData object so it can get its
						// values (since imageAnalysis takes a FilemnameAndData
						// object)
						// FilenameAndData tempFilenameAndData = new
						// FilenameAndData(); // Create new object
						// tempFilenameAndData.filename = tempFilename; // Set
						// its filename

						// Create bitmap with drawings on it
						// imageAnalysis(tempFilenameAndData, true);
						// We dont need to do this any more, since we are just
						// using a sample to show what the edges/offsets mean

						// Display it
						// imageView.setImageBitmap(bitmapWithAnalysis); // WE
						// NEED AN IMAGE VIEW CREATED BEFORE THIS
						// //////////////////////////////////////////////////////////////////////////////////////////////

					}

				}
			});
			
			graphExists = true;
			
			return;
		}
	}

	/**
	 * Source: http://stackoverflow.com/a/7591216/2561421
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static byte[] readFileByteArray(File file) throws IOException
	{
		// Open file
		RandomAccessFile f = new RandomAccessFile(file, "r");
		try
		{
			// Get and check length
			long longlength = f.length();
			int length = (int) longlength;

			if (length != longlength)
				throw new IOException("File size >= 2 GB");

			// Read file and return data
			byte[] data = new byte[length];
			f.readFully(data);

			return data;
		} 
		finally
		{
			f.close();
		}
	}

	/**
	 * Load a Mat image object from a file
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	private Mat loadMat(String filename) throws IOException
	{
		// Load byte array
		byte[] byteArray = readFileByteArray(new File(filename));

		// Convert byte array to bitmap
		Bitmap bm = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

		// Convert bitmap to mat
		Mat m = new Mat(bm.getHeight(), bm.getWidth(), CvType.CV_32FC3);
		Utils.bitmapToMat(bm, m);

		return m;
	}

	/**
	 * @author Andrew
	 * Class for bundling images with their analysis results
	 */
	private class FilenameAndData
	{
		String filename;

		boolean foundRof;
		boolean foundSensor;
		
		double markToEdge;
		double markToRef;
		double sensorToEdge;
		double sensorToRef;
	}

	/***** Callbacks *****/

	@Override
	public void onClick(View v)
	{
		if (v == imgScan)
			onClickImageAnalysis();
		
		if (v == chooseDirectoryButton)
			onClickChooseDirectory();
		
		if (v == referenceButton)
			onClickReference();

		if (v == saveGraphButton){
			if (graphExists == true){
				onClickSaveGraph();
				Toast.makeText(getActivity(),
						"Graph successfully saved as GraphImage.png\nin the current stream's folder.",
						Toast.LENGTH_SHORT).show();
			}
			else{
				Toast.makeText(getActivity(),
						"There is no graph to save. You must perform image analysis first.",
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void onClickSaveGraph(){
		LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.lineChart);
		Bitmap graphImage = bitmapFromChartView(mChartView, layout.getWidth(), layout.getHeight());
		try {
		       FileOutputStream out = new FileOutputStream(directory2 + "/" + "GraphImage.png");
		       graphImage.compress(Bitmap.CompressFormat.PNG, 100, out);
		       out.close();
		} catch (Exception e) {
		       e.printStackTrace();
		}
	}
	
	/*
	private void saveGraphAsImage(String directoryName){
		Bitmap bm = mChartView.toBitmap();
		OutputStream s = null;
		try {
			s = new FileOutputStream(directoryName + "GraphImage.png");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		bm.compress(CompressFormat.PNG, 100, s);
	}
	*/
	
	/**
	 * Callback for imgScan button
	 * Load images from the selected directory and perform image analysis on each one.
	 * The results will be shown in the graph when complete.
	 */
	private void onClickImageAnalysis()
	{
		// Set the directory equal to the selected directory
		// Set this to "/storage/sdcard0/XeroxRDC/TestImages" for testing
		// String directory = "/storage/sdcard0/XeroxRDC/TestImages/";
		String directory = (String) chooseDirectoryTextView.getText();

		if (directory.isEmpty())
		{
			Toast.makeText(getActivity(),
					"Please choose a directory to load a stream from",
					Toast.LENGTH_SHORT).show();
			return;
		} 
		
		
		// Get switches from preference manager
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		boolean do_marktoedge = pref.getBoolean("pref_do_marktoedge", true);
		boolean do_marktoref = pref.getBoolean("pref_do_marktoref", true);
		boolean do_sensortoedge = pref.getBoolean("pref_do_sensortoedge", true);
		boolean do_sensortoref = pref.getBoolean("pref_do_sensortoref", true);
		
		boolean findsensor = pref.getBoolean("pref_find_sensor", false);
		boolean realedge = pref.getBoolean("pref_find_real_edge", false);
		int orient = pref.getInt("pref_orientation", ROFRider.METHOD_CALIBRATION_ROBUST);
		
		if(!do_marktoedge  && !do_marktoref && !do_sensortoedge && !do_sensortoref){
			Toast.makeText(getActivity(),
					"Please choose at least one feature that you want to analyze in Select Features", Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// Load filenames from a directory
		List<String> filenameList = new ArrayList<String>();
		filenameList = getAndAlphabetizeListFilenames(directory);

		// Store filenames to filenameAndDataList
		filenameAndDataList = new ArrayList<FilenameAndData>();
		addFilenamesToFilenameAndDataList(filenameList, filenameAndDataList);

		// Initialize ROFRider
		// TODO save paths elsewhere
		rof = new ROFRider();
		rof.init("/storage/sdcard0/XeroxRDC/rofmark_tsri.dat",
				"/storage/sdcard0/XeroxRDC/sensor_tsri.dat");
		// Send preferences to ROFRider
		rof.setLocateSensor(findsensor);
		rof.setLocateRealEdge(realedge);
		// not necessary, but recommended
		rof.setOrientationMethod(orient);

		// Perform image analysis, still have to work on this
		// This will be Chris's function
		// ****************************************************
		imageAnalysis task = new imageAnalysis();				//I've switched this to an asynctask, see the subclass I made for more
		task.numValsInList = filenameAndDataList.size();
		task.execute(filenameAndDataList);

		// Input new data into graphs
		// This is for a line graph

		// graphOffsetPaperEdge(filenameAndDataList);
		// graphOffsetROFMark(imageAndDataList);
		// etc...
		// Parse chart data for the datasets for the line graph and the
		// histogram
		// And also create their renderers

		directory2 = directory;
		/*
		LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.lineChart);
		Bitmap graphImage = bitmapFromChartView(mChartView, layout.getWidth(), layout.getHeight());
		try {
		       FileOutputStream out = new FileOutputStream(directory + "/" + "GraphImage.png");
		       graphImage.compress(Bitmap.CompressFormat.PNG, 100, out);
		       out.close();
		} catch (Exception e) {
		       e.printStackTrace();
		}
		*/
	}

	// Source: http://stackoverflow.com/a/19037124/2561421
	// Creative commons license, Chris R from stack overflow 2013
	private Bitmap bitmapFromChartView(View v, int width, int height) {
		Bitmap b = Bitmap.createBitmap(width,
		        height, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);
		v.layout(0, 0, width, height);
		v.draw(c);
		return b;
		}

	// These don't have function-local scope because we want them to be accessible in the future, whenever we want to update the graphs
//	private GraphicalView mChartView;
/*
	private void saveGraphAsImage(String directoryName){
		Bitmap bm = mChartView.toBitmap();
		OutputStream s = null;
		try {
			s = new FileOutputStream(directoryName + "GraphImage.png");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		bm.compress(CompressFormat.PNG, 100, s);
	}
	*/
	private void onClickChooseDirectory()
	{
		FileChooserDialog dialog = new FileChooserDialog(getActivity());
		dialog.setFolderMode(true);

		// dialog.loadFolder(Environment.getExternalStorageDirectory() +
		// "/XeroxRDC/");
		dialog.loadFolder("/storage/sdcard0/XeroxRDC/");

		dialog.addListener(new FileChooserDialog.OnFileSelectedListener()
		{
			public void onFileSelected(Dialog source, File file)
			{
				source.hide();
				Toast toast = Toast.makeText(source.getContext(),
						"Folder selected: " + file.getName(), Toast.LENGTH_LONG);
				chooseDirectoryTextView.setText(file.getAbsolutePath()
						+ File.separator);
				toast.show();
			}

			// Might be able to take this out:
			public void onFileSelected(Dialog source, File folder, String name)
			{
				source.hide();
				Toast toast = Toast.makeText(source.getContext(),
						"File created: " + folder.getName() + "/" + name,
						Toast.LENGTH_LONG);
				toast.show();
			}
		});

		dialog.show();
	}

	/**
	 * Open settings page for turning detection on or off for various features
	 */
	private void onClickReference()
	{
		// Open activity to let the user toggle which features to show
		startActivity(new Intent(getActivity(), FeatureSelectionActivity.class));
	}

	// private XYSeries mCurrentSeries;
	// private XYSeriesRenderer mCurrentRenderer;

	/**
	 * Prepares the data from imageAndDataList, and passes it into the graph API
	 * function to display the graph
	 * @param filenameAndDataList
	 */
	private void parseLineChartData(List<FilenameAndData> filenameAndDataList)
	{
		// Get switches from preference manager
		final boolean do_marktoedge = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_do_marktoedge", true);
		final boolean do_marktoref = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_do_marktoref", true);
		final boolean do_sensortoedge = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_do_sensortoedge", true);
		final boolean do_sensortoref = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_do_sensortoref", true);
		final boolean showBadData = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_show_bad_data", true);
		
		int numFeatures = 0;
		
		// Count the number of features to show
		if (do_marktoedge) numFeatures++;
		if (do_marktoref) numFeatures++;
		if (do_sensortoedge) numFeatures++;
		if (do_sensortoref) numFeatures++;
		
		// Create dataset parameters
		double maxYValue = -Double.MAX_VALUE;
		double minYValue = Double.MAX_VALUE;

		// Title for this set, its an array in case we want to add multiple
		// plots to the graph
		
		int currIndex = 0;
		String[] titles = new String[numFeatures];
		
		if (do_marktoedge)
		{
			titles[currIndex] = "Mark to Edge";
			currIndex++;
		}
		if (do_marktoref)
		{
			titles[currIndex] = "Mark to Ref";
			currIndex++;
		}
		if (do_sensortoedge)
		{
			titles[currIndex] = "Sensor to Edge";
			currIndex++;
		}
		if (do_sensortoref)
		{
			titles[currIndex] = "Sensor to Ref";
		}
		
		// Values to be sent to graph API (y component)
		List<double[]> yComponent = new ArrayList<double[]>(); 
		// Its a list because it allows for multiple lines to be plotted on one graph
		
		// Each of these is the data for one line
		double[] markToEdgeValues = new double[filenameAndDataList.size()]; 
		double[] markToRefValues = new double[filenameAndDataList.size()]; 
		double[] sensorToEdgeValues = new double[filenameAndDataList.size()]; 
		double[] sensorToRefValues = new double[filenameAndDataList.size()]; 

		// Iterate through all values
		for (int i = 0; i < filenameAndDataList.size(); i++)
		{ 
			// Populate dataArray y values (the analysis values)
			// Default to zero if feature wasn't found
			// Remember max and min y values for graph bounds
			
			if (do_marktoedge)
			{
				// Fetch data, or set to zero if not found
				if (showBadData || filenameAndDataList.get(i).foundRof)
					markToEdgeValues[i] = filenameAndDataList.get(i).markToEdge;
				else
					markToEdgeValues[i] = 0;
				
				// Find max y value
				if (markToEdgeValues[i] > maxYValue)
					maxYValue = markToEdgeValues[i];
				// Find min y value
				if (markToEdgeValues[i] < minYValue)
					minYValue = markToEdgeValues[i];
			}

			if (do_marktoref)
			{
				// Fetch data, or set to zero if not found
				if (showBadData || filenameAndDataList.get(i).foundRof)
					markToRefValues[i] = filenameAndDataList.get(i).markToRef;
				else
					markToRefValues[i] = 0;
				
				// Find max y value
				if (markToRefValues[i] > maxYValue)
					maxYValue = markToRefValues[i];
				// Find min y value
				if (markToRefValues[i] < minYValue)
					minYValue = markToRefValues[i];
			}

			if (do_sensortoedge)
			{
				// Fetch data, or set to zero if not found
				if (showBadData || filenameAndDataList.get(i).foundSensor)
					sensorToEdgeValues[i] = filenameAndDataList.get(i).sensorToEdge;
				else
					sensorToEdgeValues[i] = 0;
				
				// Find max y value
				if (sensorToEdgeValues[i] > maxYValue)
					maxYValue = sensorToEdgeValues[i];
				// Find min y value
				if (sensorToEdgeValues[i] < minYValue)
					minYValue = sensorToEdgeValues[i];
			}

			if (do_sensortoref)
			{
				// Fetch data, or set to zero if not found
				if (showBadData || filenameAndDataList.get(i).foundSensor)
					sensorToRefValues[i] = filenameAndDataList.get(i).sensorToRef;
				else
					sensorToRefValues[i] = 0;
				
				// Find max y value
				if (sensorToRefValues[i] > maxYValue)
					maxYValue = sensorToRefValues[i];
				// Find min y value
				if (sensorToRefValues[i] < minYValue)
					minYValue = sensorToRefValues[i];
			}

		}
		
		if (do_marktoedge)
		{
			// Add dataArray for this ONE OFFSET  to the inputValuesToLineGraphList
			yComponent.add(markToEdgeValues); 
		}
		
		if (do_marktoref)
		{
			// Add dataArray for this ONE OFFSET to  the inputValuesToLineGraphList
			yComponent.add(markToRefValues);
		}
		
		if (do_sensortoedge)
		{
			// Add dataArray for this ONE OFFSET to the inputValuesToLineGraphList
			yComponent.add(sensorToEdgeValues);
		}
			
		if (do_sensortoref)
		{
			// Add dataArray for this ONE OFFSET to the inputValuesToLineGraphList
			yComponent.add(sensorToRefValues);
		}
		
		// Create x components (these are just the image numbers)
		// It needs the x components as well (this is the independent variable
		// so it is just 0, 1, 2, 3, ..., max size of list)
		double[] xscale = createIncrementingXValues(filenameAndDataList);
		
		List<double[]> xComponent = new ArrayList<double[]>();
		for (int i = 0; i < numFeatures; i++)
		{
			// Each of the 4 offsets need their corresponding x values (which are all the same)
			xComponent.add(xscale);
		}

		 
		// Convert these components into a data set
		// * If buildDataset doesn't work, then we need to import the file
		// AbstractDemoChart.java from AChartEngine demos *
		mDataset = AbstractDemoChart.buildDataset(titles, xComponent, yComponent); 
		// each of these parameters are arrays because it allows for multiple plots on one graph
		// This is for custom graph visuals
		mRenderer = setUpRendererLineGraph(filenameAndDataList.size(), minYValue, maxYValue, numFeatures); 
	}

	// Incrementing x values for the independent axis on the graph
	private double[] createIncrementingXValues( List<FilenameAndData> filenameAndDataList)
	{
		double[] x = new double[filenameAndDataList.size()];
		for (int i = 0; i < filenameAndDataList.size(); i++)
			x[i] = i;
		
		return x;
	}

	private XYMultipleSeriesRenderer setUpRendererLineGraph(int numImages, double minYValue, double maxYValue, int numFeatures)
	{
		// Get switches from preference manager
		boolean do_marktoedge = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_do_marktoedge", true);
		boolean do_marktoref = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_do_marktoref", true);
		boolean do_sensortoedge = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_do_sensortoedge", true);
		boolean do_sensortoref = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_do_sensortoref", true);
		
		int[] colors = new int[numFeatures];
		PointStyle[] styles = new PointStyle[numFeatures];
		int currIndex = 0; 
		
		if (do_marktoedge == true)
		{
			colors[currIndex] = Color.BLUE;
			styles[currIndex] = PointStyle.CIRCLE;
			currIndex++;
		}
		if (do_marktoref == true)
		{
			colors[currIndex] = Color.GREEN;
			styles[currIndex] = PointStyle.DIAMOND;
			currIndex++;
		}
		if (do_sensortoedge == true)
		{
			colors[currIndex] = Color.CYAN;
			styles[currIndex] = PointStyle.TRIANGLE;
			currIndex++;
		}
		if (do_sensortoref == true)
		{
			colors[currIndex] = Color.YELLOW;
			styles[currIndex] = PointStyle.SQUARE;
		}
		
		

		XYMultipleSeriesRenderer renderer = AbstractDemoChart.buildRenderer(colors, styles);
		int length = renderer.getSeriesRendererCount();
		
		for (int i = 0; i < length; i++)
		{
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);
		}

		// The bounds of the graph
		double yMargin = (maxYValue - minYValue) * 0.1;
		double viewableXMin = -(numImages * 0.1);
		double viewableXMax = numImages + numImages * 0.1 - 1;
		double viewableYMin = minYValue - yMargin;
		double viewableYMax = maxYValue + yMargin;

		AbstractDemoChart.setChartSettings(renderer, "Data Analysis", "Image",
				"Measurements (mm)", viewableXMin, viewableXMax, viewableYMin,
				viewableYMax, Color.LTGRAY, Color.LTGRAY);

		renderer.setXLabels(12);
		renderer.setYLabels(10);
		renderer.setShowGrid(true);
		renderer.setXLabelsAlign(Align.RIGHT);
		renderer.setYLabelsAlign(Align.RIGHT);
		renderer.setZoomButtonsVisible(true);
		renderer.setPanLimits(new double[]
			{ viewableXMin, viewableXMax, viewableYMin, viewableYMax });
		renderer.setZoomLimits(new double[]
			{ viewableXMin, viewableXMax, viewableYMin, viewableYMax });

		renderer.setZoomButtonsVisible(false);
		
		return renderer;
	}

	// This is where the graph is created/drawn
	// This might help us:
	// http://stackoverflow.com/a/4079692/2561421
	// http://stackoverflow.com/a/6910293/2561421
	// http://wptrafficanalyzer.in/blog/android-drawing-time-chart-with-timeseries-in-achartengine/
	// code.google.com/p/achartengine/
	// Also look at XYChartBuilder.java and AverageTemperatureChart.java demos
	// in the api's demos folder
	// And this is for making dynamic graphs:
	// http://www.youtube.com/watch?v=E9fozQ5NlSo

	private void displayLineGraph()
	{

		if (mChartView == null)
		{
			LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.lineChart);
			mChartView = ChartFactory.getLineChartView(getActivity(), mDataset, mRenderer);
			layout.addView(mChartView);
		}
		else
		{
			LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.lineChart);
			layout.removeView(mChartView);
			
			mChartView = ChartFactory.getLineChartView(getActivity(), mDataset,
					mRenderer);
			layout.addView(mChartView);
			//mChartView.repaint();

		}
	}

}
