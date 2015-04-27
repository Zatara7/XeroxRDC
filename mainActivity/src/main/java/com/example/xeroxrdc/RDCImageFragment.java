package com.example.xeroxrdc;

/**
 * RDCImageFragment
 * This page shows two images selected from the filesystem side-by-side.
 * @author Daragh
 * @author Andrew 
 */

import java.io.File;

import android.view.View.OnTouchListener;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.SeekBar.OnSeekBarChangeListener;

import ar.com.daidalos.afiledialog.FileChooserDialog;

public class RDCImageFragment extends Fragment implements OnClickListener
{
	private View rootView;
	private Button openImage0;
    private Button contours_btn;
	private Button openImage1;
	private Button analysis0;
	private Button analysis1;
	private ImageView imageView0;
	private ImageView imageView1;
	private TextView txtResults0;
	private TextView txtResults1;
	private String previousDirectoryChosen = "/storage/sdcard0/XeroxRDC/";
	private ToggleButton showGridButton;
	private SeekBar gridHeight;
	private SeekBar gridWidth;
	
	private int horizontal_distance = 100;
	private int vertical_distance = 100;
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.imagepage, container, false);
		
		openImage0 = (Button) rootView.findViewById(R.id.openimage0);
        contours_btn = (Button) rootView.findViewById(R.id.button_contours);
        openImage1 = (Button) rootView.findViewById(R.id.openimage1);
		analysis0 = (Button) rootView.findViewById(R.id.analysis0);
		analysis1 = (Button) rootView.findViewById(R.id.analysis1);
		imageView0 = (ImageView) rootView.findViewById(R.id.imageView0);
		imageView1 = (ImageView) rootView.findViewById(R.id.imageView1);
		txtResults0 = (TextView) rootView.findViewById(R.id.imgresults0);
		txtResults1 = (TextView) rootView.findViewById(R.id.imgresults1);
		showGridButton = (ToggleButton) rootView.findViewById(R.id.toggleButton1);
		gridHeight = (SeekBar) rootView.findViewById(R.id.seekBar1);
		gridWidth = (SeekBar) rootView.findViewById(R.id.seekBar2);
		
		openImage0.setOnClickListener(this);
		openImage1.setOnClickListener(this);
		
		setGridOnButtonListener();
		setSeekBarListeners();

		//Set image onTouchListener
		setImageOnTouchListener("left");
		setImageOnTouchListener("right");
		
		setAnalysisButtonListener();
		
		// ************************************************************
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		return rootView;
	}

	public void openImage()
	{
//		imageView.setImageURI(Uri.fromFile(new File("drawable/computer_problems.png")));
	}


	@Override
	public void onClick(View v)
	{
        if (v==contours_btn) {
            Intent intent = new Intent(rootView.getContext(), contours.class);
            startActivityForResult(intent, 0);
        }
		if (v==openImage0 || v==openImage1)
		{
		     FileChooserDialog dialog = new FileChooserDialog(getActivity());
			 dialog.loadFolder(previousDirectoryChosen);
		     dialog.setFilter(".*jpg|.*png|.*JPG|.*PNG");
		     dialog.setShowOnlySelectable(true);	// only show these files in list
			
		     if (v == openImage0){
			     dialog.addListener(new FileChooserDialog.OnFileSelectedListener() {
			         public File onFileSelected(Dialog source, File file) {
			             source.hide();
			             Toast toast = Toast.makeText(source.getContext(), "File selected: " + file.getName(), Toast.LENGTH_LONG);
			             toast.show();
			             leftFile = file;
		            	 txtResults0.setText(file.getAbsolutePath());
						 if (gridOn)
						 {	
		            	 	drawCanvas("left",imageView0);
						 }
		            	 else
		            	 {
		            		 imageView0.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
		            	 }
		            	 previousDirectoryChosen = file.getParent() + File.separator;
		            	 
		            	 
		            		
		 				// get path of image
		 				directoryPathLeft = txtResults0.getText().toString();
		 				int i = directoryPathLeft.lastIndexOf("/");
		 				fileNameLeft = directoryPathLeft.substring(i + 1);
		 				directoryPathLeft = directoryPathLeft.substring(0,i);


                         return file;
                     }
			         // We can remove this... as long as it doesnt break it
			         public void onFileSelected(Dialog source, File folder, String name) {
			             source.hide();
			             Toast toast = Toast.makeText(source.getContext(), "File created: " + folder.getName() + "/" + name, Toast.LENGTH_LONG);
			             toast.show();
			         }
			     });
		     }
		     else if (v == openImage1){
			     dialog.addListener(new FileChooserDialog.OnFileSelectedListener() {
			         public File onFileSelected(Dialog source, File file) {
			             source.hide();
			             Toast toast = Toast.makeText(source.getContext(), "File selected: " + file.getName(), Toast.LENGTH_LONG);
			             toast.show();
			             
			             rightFile = file;

						 txtResults1.setText(file.getAbsolutePath());
						 if (gridOn)
						 {
							 drawCanvas("right",imageView1);
			 			 }
						 else
						 {
							 imageView1.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
						 }
					 	 previousDirectoryChosen = file.getParent() + File.separator;
			         
			         
					 	// get path of image
			 				directoryPathRight = txtResults1.getText().toString();
			 				int i = directoryPathRight.lastIndexOf("/");
			 				fileNameRight = directoryPathRight.substring(i + 1);
			 				directoryPathRight = directoryPathRight.substring(0,i);
                         return file;
                     }
			         // We can remove this... as long as it doesnt break it
			         public void onFileSelected(Dialog source, File folder, String name) {
			             source.hide();
			             Toast toast = Toast.makeText(source.getContext(), "File created: " + folder.getName() + "/" + name, Toast.LENGTH_LONG);
			             toast.show();
			         }
			     });
		     
		     }
		     dialog.show();
		}
	}
	
	private void setGridOnButtonListener(){
		showGridButton.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				if (gridOn == true){
					gridOn = false;
					if (leftFile != null)
						imageView0.setImageBitmap(BitmapFactory.decodeFile(leftFile.getAbsolutePath()));
					if (rightFile != null)
						imageView1.setImageBitmap(BitmapFactory.decodeFile(rightFile.getAbsolutePath()));
				}else{
					gridOn = true;
					if (leftFile != null)
						drawCanvas("left", imageView0);
					if (rightFile != null)
						drawCanvas("right", imageView1);	
				}
				showGridButton.setSelected(gridOn);
			}
		});
	}
	
	private void setSeekBarListeners(){
		gridHeight.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				vertical_distance = (int) (300.00 * arg0.getProgress() / arg0.getMax()) + 100;
				if (leftFile != null && gridOn == true)
					drawCanvas("left", imageView0);
				if (rightFile != null && gridOn == true)
					drawCanvas("right", imageView1);
			}
			public void onStartTrackingTouch(SeekBar arg0) {
			}
			public void onStopTrackingTouch(SeekBar arg0) {
			} 
    	});
		gridWidth.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				horizontal_distance = (int) (300.00 * arg0.getProgress() / arg0.getMax()) + 100;
				if (leftFile != null && gridOn == true)
					drawCanvas("left", imageView0);
				if (rightFile != null && gridOn == true)
					drawCanvas("right", imageView1);
			}
			public void onStartTrackingTouch(SeekBar arg0) {
			}
			public void onStopTrackingTouch(SeekBar arg0) {
			} 
    	});
	}
	
	private void drawCanvas(String which, ImageView view){
		File file;
		if (which.equals("left"))
			file = leftFile;
		else
			file = rightFile;
		
		
		//Initialize canvas
   	 	Paint myPaint = new Paint();
   	 	myPaint.setColor(Color.RED);
   	 	myPaint.setStrokeWidth(10);
   	 	myPaint.setStyle(Paint.Style.STROKE);
		Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
		Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
		Canvas tempCanvas = new Canvas(tempBitmap);
		tempCanvas.drawBitmap(myBitmap, 0, 0, null);
		//imageView0.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
		
		//Draw overlay
		for (int i = 0; i < tempBitmap.getHeight() / vertical_distance + 1; i++){
			tempCanvas.drawLine(0, i*vertical_distance,tempBitmap.getWidth(), i*vertical_distance, myPaint);
		}
		

		for (int i = 0; i < tempBitmap.getWidth() / horizontal_distance + 1; i++){
			tempCanvas.drawLine(i*horizontal_distance, 0,i*horizontal_distance,tempBitmap.getHeight(), myPaint);
		}
		
		view.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
	}

	private Boolean gridOn = false;
	private File leftFile;
	private File rightFile;
	
	
	
	private void setImageOnTouchListener(String s){
		if (s == "left"){
			imageView0.setOnTouchListener(new OnTouchListener(){

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					
					//Get coordinates
					float y = event.getRawY();
					
				
					//Bottom 
					if (y < v.getHeight()/2){
						leftImageRotationAngle += 90;
						imageView0.setRotation(leftImageRotationAngle);
						
					} else {
						//Top half
						leftImageRotationAngle -= 90;
						imageView0.setRotation(leftImageRotationAngle);
					}
					
					return false;
				}
				
			});
		} else{
			imageView1.setOnTouchListener(new OnTouchListener(){

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					
					//Get coordinates
					float y = event.getRawY();
					
					//Bottom 
					if (y < v.getHeight()/2){
						rightImageRotationAngle += 90;
						imageView1.setRotation(rightImageRotationAngle);
						
					} else {
						//Top half
						rightImageRotationAngle -= 90;
						imageView1.setRotation(rightImageRotationAngle);
					}
					
					return false;
				}


				
			});
		}
			
		
	}
	
	private float leftImageRotationAngle = 0;
	private float rightImageRotationAngle = 0;
	
	
	// Variables to pass images to noise removal activity
	private String fileNameLeft = "";
	private String fileNameRight = "";
	private String directoryPathLeft;
	private String directoryPathRight;
	
	// listener for left analysis button clicked
	private void setAnalysisButtonListener(){
		analysis0.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				if (fileNameLeft == "")
					return;
				
			
				
				// send to noise removal
				Intent newIntent = new Intent(getActivity(),NoiseRemoval.class);
				newIntent.putExtra("fileName", fileNameLeft);
				newIntent.putExtra("directoryPath",directoryPathLeft);
				startActivity(newIntent);
			}
			
		});
        contours_btn.setOnClickListener(new OnClickListener(){
            public void onClick(View v){

                Intent newIntent = new Intent(getActivity(), contours.class);
                startActivity(newIntent);

            }

        });
		// listener for right analysis button clicked
		analysis1.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				
				if (fileNameRight == "")
					return;
			
				// send to noise removal
				Intent newIntent = new Intent(getActivity(),NoiseRemoval.class);
				newIntent.putExtra("fileName", fileNameRight);
				newIntent.putExtra("directoryPath",directoryPathRight);				
				startActivity(newIntent);
			}
		});
	}
	
	
	
	
}
