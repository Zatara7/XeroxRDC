package com.example.xeroxrdc;

/**
 * RDC Graph Fragment
 * This page shows the image analysis results in graph format.
 * It also allows the user to view two frames side by side.
 * @author Andrew
 * @author Daragh
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import ar.com.daidalos.afiledialog.FileChooserDialog;


import com.androidplot.xy.XYPlot;

public class RDCGraphFragment extends Fragment implements OnClickListener
{
	// List of valid extensions for images 
	private static final String[] IMAGE_EXTENSIONS = new String[] {"jpg", "jpeg", "png"};

	private View rootView;
	private Button imgScan;
	private Button chooseDirectoryButton;
	private Button referenceButton;
	private Button saveGraphButton;
	public static boolean topImage = true;
	public static boolean graphExists = false;

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
	 * @param filename - The filename to read the extension of
	 * @return String - returns the extension of the file
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
	 * @param filename - The filename to find the correct file types based on the extension
	 * @return boolean - Returns true if the file is valid and false otherwise
	 */
	private static boolean imageFileFilter(String filename)
	{
		String ext = getExtension(filename);
		
		// Check against valid extensions
		for(String str : IMAGE_EXTENSIONS)
		{
			// If extension is good, return true now
			if (ext.equalsIgnoreCase(str))
				return true;
		}
		// The extension isn't on the list, so return false
		return false;
	}
	
	// Source: http://stackoverflow.com/a/5694473/2561421
	/**
	 * 
	 * @param parentDir - The parent directory
	 * @return results - The files in the directory in alphabetical order
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
	 * 
	 * Subclass that extends AsyncTask to spawn a new thread for the image analysis. Also handles progress bar.
	 */
	private class imageAnalysis extends AsyncTask<List<FilenameAndData>, Integer, Integer> {
		ProgressDialog usrDialog;
		int numValsInList;				//Stores number of frames passed. This way .size() isn't called multiple times
		
		protected void onPreExecute() {
			usrDialog = new ProgressDialog(getActivity());		//Prepping progress bar
			usrDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			usrDialog.setMax(numValsInList);
			usrDialog.setMessage("Analyzing frames");
			usrDialog.show();
		}
		
		@SafeVarargs
        protected final Integer doInBackground(List<FilenameAndData>... filenameAndDataList) {		//This is just the old imageAnalysisDriver function
			return 0;
		}

/*		protected void onProgressUpdate(Integer... progress) {
			usrDialog.setProgress(progress[0]);
		}
*/
		protected void onPostExecute(Integer result) {
            // Display the graphs                               //This contains the function to start the graph. This ensures that
            displayLineGraph();                    			    // android waits for the analysis to be complete before rendering
																// the graph.
			graphExists = true;
            usrDialog.hide();
		}
	}

	/**
	 * @author Andrew
	 * Class for bundling images with their analysis results
	 */
	private class FilenameAndData
	{
		String filename;
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
			if (graphExists){
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
/*		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
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
*/
		// Load filenames from a directory
		List<String> filenameList;
		filenameList = getAndAlphabetizeListFilenames(directory);

		// Store filenames to filenameAndDataList
        List<FilenameAndData> filenameAndDataList = new ArrayList<FilenameAndData>();
		addFilenamesToFilenameAndDataList(filenameList, filenameAndDataList);

		// Perform image analysis, still have to work on this
		// This will be Chris's function
		// ****************************************************
		imageAnalysis task = new imageAnalysis();				//I've switched this to an asynctask, see the subclass I made for more
		task.numValsInList = filenameAndDataList.size();
        task.execute(filenameAndDataList);
    }

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
        XmlObjects xmlObjects = new XmlObjects();
        MyPlotActivity plotActivity = new MyPlotActivity();
        plotActivity.setView(rootView);
        plotActivity.initializePlot(xmlObjects);
        //XYPlot plot = plotActivity.getXyPlot();
        //XYPlot layout = (XYPlot) rootView.findViewById(R.id.lineChart);
        //layout.addView(view);

            /*MyPlotActivity plotActivity = new MyPlotActivity();
            XYPlot plot = plotActivity.getXyPlot();
            LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.lineChart);
            view = plot.getRootView();
            layout.removeView(view);
            layout.addView(view);*/

/*		if (mChartView == null)
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
*/

    }

}
