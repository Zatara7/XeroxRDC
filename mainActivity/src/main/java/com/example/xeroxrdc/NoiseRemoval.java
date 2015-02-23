package com.example.xeroxrdc;



import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class NoiseRemoval extends Activity{
    private static final String  TAG = "OCVSample::Activity";

    private Button noiseSaveButton;
    private Button noiseCleanButton;
    private Button histogramEqualizationButton;
    private ImageView noiseImageView;
    private SeekBar noiseSizeSeekBar;
    private TextView nonsense;
    private String imageName;
    private String tempImageName;
    private String directoryPath;
    private int structuringElementSize;
    
    Mat tempImage = null;
    Mat currentImage = null;
    
    public NoiseRemoval() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }


 
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.noise_removal);

        noiseSaveButton = (Button)findViewById(R.id.noiseSaveButton);
        noiseCleanButton = (Button)findViewById(R.id.noiseCleanButton);
        histogramEqualizationButton = (Button)findViewById(R.id.histogramEqualizationButton);
        noiseImageView = (ImageView)findViewById(R.id.noiseImageView);
        noiseSizeSeekBar = (SeekBar)findViewById(R.id.noiseSizeSeekBar);
        nonsense = (TextView)findViewById(R.id.nonsense);
        structuringElementSize = 1;
        
        
        
        //Hardcoded for now
        //directoryPath = "/storage/sdcard0/XeroxRDC/2014_11_10/";
        //imageName = directoryPath + "test.jpg";
        directoryPath = getIntent().getStringExtra("directoryPath") + File.separator;
        imageName = directoryPath + getIntent().getStringExtra("fileName");
        tempImageName = imageName.substring(0,imageName.length()-4) + "_analyzed.jpg";
        
        
        noiseImageView.setImageBitmap(BitmapFactory.decodeFile(new File(imageName).getAbsolutePath()));
        currentImage = Highgui.imread(new File(imageName).getAbsolutePath());
        tempImage = Highgui.imread(new File(imageName).getAbsolutePath());
   
    
        setListeners();
     
    }
    
    public void opening(){
    	Imgproc.erode(tempImage,tempImage,Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(structuringElementSize,structuringElementSize)));
    	Imgproc.dilate(tempImage,tempImage,Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(structuringElementSize,structuringElementSize)));
    }
    
    public void save(){
    	if (tempImage != null)
    		Highgui.imwrite(tempImageName, tempImage);
    }
    
    public void setListeners(){
    	noiseSaveButton.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				Intent newIntent = new Intent(NoiseRemoval.this, EditFileName.class);
    	    	startActivityForResult(newIntent,0);
			}
    	});
    	
    	noiseSizeSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				//Size limited to 0 to 10
				structuringElementSize = (int) (9.00 * arg0.getProgress() / arg0.getMax()) + 1;
				nonsense.setText("Current Structuring Element Size: " + structuringElementSize);
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
			} 
    		
    	});
    	
    	noiseCleanButton.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				opening();
				save();
				noiseImageView.setImageBitmap(BitmapFactory.decodeFile(new File(tempImageName).getAbsolutePath()));
			}
    		
    	});
    	
    	histogramEqualizationButton.setOnClickListener(new OnClickListener(){
    		public void onClick(View arg0) {
    			histogramEqualization();
    			save();
				noiseImageView.setImageBitmap(BitmapFactory.decodeFile(new File(tempImageName).getAbsolutePath()));
    		}
    	});
    }
    
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	super.onActivityResult(requestCode, resultCode, data);
    		String newTempImageName= data.getStringExtra("RETURN VALUE");
    		changeLastFileName(directoryPath,newTempImageName);
 
    }
    
    private boolean isEmptyString(String s){
    	if (s == ""){
    		return true;
    	}
    	int counter = 0;
    	for (int i = 0; i < s.length(); i++){
    		if (s.charAt(i) == ' '){
    			counter ++;
    		} else
    			return false;
    	}
    	return (counter == s.length());
    }
    
    private String truncateLeadingSpaces(String s){
    	int i = 0;
    	for (; i <s.length() ; i++){
    		if (s.charAt(i) != ' '){
    			break;
    		}
    	}
    	return s.substring(i);
    }
    

    private void changeLastFileName(String path, String desiredName)
    {

    	File oldFile = new File(tempImageName);
    	
    	if (isEmptyString(desiredName)){
            return;
    	} 
    	desiredName = truncateLeadingSpaces(desiredName);
    	
    	//Cancel the save
    	if (desiredName.equals("pppppppppppp")){
    		oldFile.delete();
    		tempImage = currentImage;
    		noiseImageView.setImageBitmap(BitmapFactory.decodeFile(new File(imageName).getAbsolutePath()));
    	} else{
    		
        	File newFile = new File(path + desiredName + ".jpg");
        	tempImageName = path + desiredName + ".jpg";
        	oldFile.renameTo(newFile);
        	noiseImageView.setImageBitmap(BitmapFactory.decodeFile(new File(tempImageName).getAbsolutePath()));
    	}
    	
    }
    
    
    public void histogramEqualization(){
    	List<Mat> hsv_planes = new ArrayList<Mat>();
    	Core.split(tempImage, hsv_planes);
        for (Mat x : hsv_planes)
        	Imgproc.equalizeHist(x, x);
        Core.merge(hsv_planes, tempImage);
    }
    
    
}
