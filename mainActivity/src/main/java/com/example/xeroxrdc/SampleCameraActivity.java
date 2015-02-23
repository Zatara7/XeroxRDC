/*
 * Copyright 2013 Sony Corporation
 */

package com.example.xeroxrdc;


import com.example.xeroxrdc.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
//import android.content.Context;
//import android.content.ContextWrapper;
//import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Files;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;













//import com.example.sony.cameraremote.SampleApplication;
import com.example.sony.cameraremote.ServerDevice;
import com.example.sony.cameraremote.SimpleCameraEventObserver;
import com.example.sony.cameraremote.SimpleLiveviewSurfaceView;
import com.example.sony.cameraremote.SimpleRemoteApi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
//import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
//import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.Inflater;
import java.io.FileOutputStream;


/**
 * An Activity class of Sample Camera screen.
 */
public class SampleCameraActivity extends Activity {

    private static final String TAG = SampleCameraActivity.class
            .getSimpleName();

    private Handler mHandler;
    private ImageView mImagePictureWipe;
    private RadioGroup mRadiosShootMode;
    private Button mButtonTakePicture;
    private Button mButtonRecStartStop;
    private Button mButtonZoomIn;
    private Button mButtonZoomOut;
    private TextView mTextCameraStatus;

    private ServerDevice mTargetServer;
    private SimpleRemoteApi mRemoteApi;
    private SimpleLiveviewSurfaceView mLiveviewSurface;
    private SimpleCameraEventObserver mEventObserver;
    private final Set<String> mAvailableApiSet = new HashSet<String>();
    private boolean mRadioInitialChecked;
    
    public static boolean startRec = false; //set to true when video recording starts 
    public static boolean stopRec = false; //set to true when video recording stops 
    
    String savedFileName = "";
    int imageCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_sample_camera);

        mHandler = new Handler();
        SampleApplication app = (SampleApplication) getApplication();
        mTargetServer = app.getTargetServerDevice();
        mRemoteApi = new SimpleRemoteApi(mTargetServer);
        mEventObserver = new SimpleCameraEventObserver(mHandler, mRemoteApi);

        mImagePictureWipe = (ImageView) findViewById(R.id.image_picture_wipe);
        mRadiosShootMode = (RadioGroup) findViewById(R.id.radio_group_shoot_mode);
        mButtonTakePicture = (Button) findViewById(R.id.button_take_picture);
        mButtonRecStartStop = (Button) findViewById(R.id.button_rec_start_stop);
        mButtonZoomIn = (Button) findViewById(R.id.button_zoom_in);
        mButtonZoomOut = (Button) findViewById(R.id.button_zoom_out);
        mTextCameraStatus = (TextView) findViewById(R.id.text_camera_status);
        mLiveviewSurface = (SimpleLiveviewSurfaceView) findViewById(R.id.surfaceview_liveview);
        mLiveviewSurface.bindRemoteApi(mRemoteApi);

       
    }

    @Override
    protected void onResume() {
        super.onResume();

        mButtonTakePicture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                takeAndFetchPicture();
            }
        });
      
        
        mButtonTakePicture.setEnabled(true);////edited here
        
        
        mButtonRecStartStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if ("MovieRecording".equals(mEventObserver.getCameraStatus())) {
                    stopMovieRec();
                } else if ("IDLE".equals(mEventObserver.getCameraStatus())) {
                    startMovieRec();
                }
            }
        });
        mEventObserver
                .setEventChangeListener(new SimpleCameraEventObserver.ChangeListener() {

                    @Override
                    public void onShootModeChanged(String shootMode) {
                        Log.d(TAG, "onShootModeChanged() called: " + shootMode);
                        refreshUi();
                    }

                    @Override
                    public void onCameraStatusChanged(String status) {
                        Log.d(TAG, "onCameraStatusChanged() called: " + status);
                        refreshUi();
                    }

                    @Override
                    public void onApiListModified(List<String> apis) {
                        Log.d(TAG, "onApiListModified() called");
                        synchronized (mAvailableApiSet) {
                            mAvailableApiSet.clear();
                            for (String api : apis) {
                                mAvailableApiSet.add(api);
                            }
                            if (!mEventObserver.getLiveviewStatus()
                                    && isApiAvailable("startLiveview")) {
                                if (!mLiveviewSurface.isStarted()) {
                                    mLiveviewSurface.start();
                                }
                            }
                            if (isApiAvailable("actZoom")) {
                                Log.d(TAG,
                                        "onApiListModified(): prepareActZoomButtons()");
                                prepareActZoomButtons(true);
                            } else {
                                prepareActZoomButtons(false);
                            }
                        }
                    }

                    @Override
                    public void onZoomPositionChanged(int zoomPosition) {
                        Log.d(TAG, "onZoomPositionChanged() called = " + zoomPosition);
                        if (zoomPosition == 0) {
                            mButtonZoomIn.setEnabled(true);
                            mButtonZoomOut.setEnabled(false);
                        } else if (zoomPosition == 100) {
                            mButtonZoomIn.setEnabled(false);
                            mButtonZoomOut.setEnabled(true);
                        } else {
                            mButtonZoomIn.setEnabled(true);
                            mButtonZoomOut.setEnabled(true);
                        }
                    }

                    @Override
                    public void onLiveviewStatusChanged(boolean status) {
                        Log.d(TAG, "onLiveviewStatusChanged() called = " + status);
                    }
                });
        mImagePictureWipe.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mImagePictureWipe.setVisibility(View.INVISIBLE);
            }
        });

        mButtonZoomIn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                actZoom("in", "1shot");
            }
        });

        mButtonZoomOut.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                actZoom("out", "1shot");
            }
        });

        mButtonZoomIn.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View arg0) {
                actZoom("in", "start");
                return true;
            }
        });

        mButtonZoomOut.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View arg0) {
                actZoom("out", "start");
                return true;
            }
        });

        mButtonZoomIn.setOnTouchListener(new View.OnTouchListener() {

            long downTime = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (System.currentTimeMillis() - downTime > 500) {
                        actZoom("in", "stop");
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downTime = System.currentTimeMillis();
                }
                return false;
            }
        });

        mButtonZoomOut.setOnTouchListener(new View.OnTouchListener() {

            long downTime = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (System.currentTimeMillis() - downTime > 500) {
                        actZoom("out", "stop");
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downTime = System.currentTimeMillis();
                }
                return false;
            }
        });

        openConnection();

        Log.d(TAG, "onResume() completed.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeConnection();

        Log.d(TAG, "onPause() completed.");
    }

    // Open connection to the camera device to start monitoring Camera events
    // and showing liveview.
    private void openConnection() {
        setProgressBarIndeterminateVisibility(true);
        new Thread() {

            @Override
            public void run() {
                Log.d(TAG, "openConnection(): exec.");
                try {
                    JSONObject replyJson = null;

                    // getAvailableApiList
                    replyJson = mRemoteApi.getAvailableApiList();
                    loadAvailableApiList(replyJson);

                    // check version of the server device
                    if (isApiAvailable("getApplicationInfo")) {
                        Log.d(TAG, "openConnection(): getApplicationInfo()");
                        replyJson = mRemoteApi.getApplicationInfo();
                        if (!isSupportedServerVersion(replyJson)) {
                            toast(R.string.sony_msg_error_non_supported_device);
                            SampleCameraActivity.this.finish();
                            return;
                        }
                    } else {
                        // never happens;
                        return;
                    }

                    // startRecMode if necessary.
                    if (isApiAvailable("startRecMode")) {
                        Log.d(TAG, "openConnection(): startRecMode()");
                        replyJson = mRemoteApi.startRecMode();

                        // Call again.
                        replyJson = mRemoteApi.getAvailableApiList();
                        loadAvailableApiList(replyJson);
                    }

                    // getEvent start
                    if (isApiAvailable("getEvent")) {
                        Log.d(TAG, "openConnection(): EventObserver.start()");
                        mEventObserver.start();
                    }

                    // Liveview start
                    if (isApiAvailable("startLiveview")) {
                        Log.d(TAG, "openConnection(): LiveviewSurface.start()");
                        mLiveviewSurface.start();
                    }

                    // prepare UIs
                    if (isApiAvailable("getAvailableShootMode")) {
                        Log.d(TAG,
                                "openConnection(): prepareShootModeRadioButtons()");
                       
                        prepareShootModeRadioButtons();
                        // Note: hide progress bar on title after this calling.
                    }

                    // prepare UIs
                    if (isApiAvailable("actZoom")) {
                        Log.d(TAG,
                                "openConnection(): prepareActZoomButtons()");
                        prepareActZoomButtons(true);
                    } else {
                        prepareActZoomButtons(false);
                    }

                    Log.d(TAG, "openConnection(): completed.");
                } catch (IOException e) {
                    Log.w(TAG, "openConnection: IOException: " + e.getMessage());
                    setProgressIndicator(false);
                    toast(R.string.sony_msg_error_connection);
                }
            }
        }.start();
    }
    
    
    // OLD method to return to current data and time for use in saving picture 
	/*private String currentDateTime()
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
		Date now = new Date();
		String currentDateTimeFilename = formatter.format(now); // + ".jpg";
		return currentDateTimeFilename;
	} */
    
    // Method to return the current date to create directories
    private String currentDate()
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd", Locale.US);
		Date now = new Date();
		String currentDateDirName = formatter.format(now); // + ".jpg";
		return currentDateDirName;
	}
    
    
    // Finds the last number that was used when creating an image file
    private int createFile(String path)
    {
    	File imageFile = new File(path);
    	File file[] = imageFile.listFiles();
    	String temp = file[file.length -1].getName();
    	int i = 0;
    	for (i = 0; i<temp.length();i++){
    		if (Character.isDigit(temp.charAt(i)) == false){
    			break;
    		}
    	}
    	int ret = Integer.parseInt(temp.substring(0,i));
    	return ret;
    }
	
	

    // Close connection to stop monitoring Camera events and showing liveview.
    private void closeConnection() {
        new Thread() {

            @Override
            public void run() {
                Log.d(TAG, "closeConnection(): exec.");
                try {
                    // Liveview stop
                    Log.d(TAG, "closeConnection(): LiveviewSurface.stop()");
                    mLiveviewSurface.stop();

                    // getEvent stop
                    Log.d(TAG, "closeConnection(): EventObserver.stop()");
                    mEventObserver.stop();

                    // stopRecMode if necessary.
                    if (isApiAvailable("stopRecMode")) {
                        Log.d(TAG, "closeConnection(): stopRecMode()");
                        mRemoteApi.stopRecMode();
                    }

                    Log.d(TAG, "closeConnection(): completed.");
                } catch (IOException e) {
                    Log.w(TAG,
                            "closeConnection: IOException: " + e.getMessage());
                }
            }
        }.start();
    }

    // Refresh UI appearance along current "cameraStatus" and "shootMode".
    private void refreshUi() {
        String cameraStatus = mEventObserver.getCameraStatus();
        String shootMode = mEventObserver.getShootMode();

        // CameraStatus TextView
        mTextCameraStatus.setText(cameraStatus);

        // Recording Start/Stop Button
        if ("MovieRecording".equals(cameraStatus)) {
            mButtonRecStartStop.setEnabled(true);
            mButtonRecStartStop.setText(R.string.sony_button_rec_stop);
        } else if ("IDLE".equals(cameraStatus) && "movie".equals(shootMode)) {
            mButtonRecStartStop.setEnabled(true);
            mButtonRecStartStop.setText(R.string.sony_button_rec_start);
        } else {
            mButtonRecStartStop.setEnabled(false);
        }

        // Take picture Button,
        if ("still".equals(shootMode) && "IDLE".equals(cameraStatus)) {
            mButtonTakePicture.setEnabled(true);
        } else {
            mButtonTakePicture.setEnabled(false);
        }
       
        //check to always make take picture button enabled 
        /*if(true){
        	
        	mButtonTakePicture.setEnabled(true);
        	
        }*/
       

        // Picture wipe Image
        if (!"still".equals(shootMode)) {
            mImagePictureWipe.setVisibility(View.INVISIBLE);
        }

        // Shoot Mode Buttons
        if ("IDLE".equals(cameraStatus)) {
            for (int i = 0; i < mRadiosShootMode.getChildCount(); i++) {
                mRadiosShootMode.getChildAt(i).setEnabled(true);
            }
            View radioButton = mRadiosShootMode.findViewWithTag(shootMode);
            if (radioButton != null) {
                mRadiosShootMode.check(radioButton.getId());
            } else {
                mRadiosShootMode.clearCheck();
            }
        } else {
            for (int i = 0; i < mRadiosShootMode.getChildCount(); i++) {
                mRadiosShootMode.getChildAt(i).setEnabled(false);
            }
        }
    }

    // Retrieve a list of APIs that are available at present.
    private void loadAvailableApiList(JSONObject replyJson) {
        synchronized (mAvailableApiSet) {
            mAvailableApiSet.clear();
            try {
                JSONArray resultArrayJson = replyJson.getJSONArray("result");
                JSONArray apiListJson = resultArrayJson.getJSONArray(0);
                for (int i = 0; i < apiListJson.length(); i++) {
                    mAvailableApiSet.add(apiListJson.getString(i));
                }
            } catch (JSONException e) {
                Log.w(TAG, "loadAvailableApiList: JSON format error.");
            }
        }
    }

    // Check if the indicated API is available at present.
    private boolean isApiAvailable(String apiName) {
        boolean isAvailable = false;
        synchronized (mAvailableApiSet) {
            isAvailable = mAvailableApiSet.contains(apiName);
        }
        return isAvailable;
    }

    // Check if the version of the server is supported in this application.
    private boolean isSupportedServerVersion(JSONObject replyJson) {
        try {
            JSONArray resultArrayJson = replyJson.getJSONArray("result");
            String version = resultArrayJson.getString(1);
            String[] separated = version.split("\\.");
            int major = Integer.valueOf(separated[0]);
            if (2 <= major) {
                return true;
            }
        } catch (JSONException e) {
            Log.w(TAG, "isSupportedServerVersion: JSON format error.");
        } catch (NumberFormatException e) {
            Log.w(TAG, "isSupportedServerVersion: Number format error.");
        }
        return false;
    }

    // Prepare for RadioButton to select "shootMode" by user.
    private void prepareShootModeRadioButtons() {
        new Thread() {

            @Override
            public void run() {
                Log.d(TAG, "prepareShootModeRadioButtons(): exec.");
                JSONObject replyJson = null;
                try {
                    replyJson = mRemoteApi.getAvailableShootMode();

                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    final String currentMode = resultsObj.getString(0);
                    JSONArray availableModesJson = resultsObj.getJSONArray(1);
                    final ArrayList<String> availableModes = new ArrayList<String>();

                    for (int i = 0; i < availableModesJson.length(); i++) {
                        String mode = availableModesJson.getString(i);
                        if (!"still".equals(mode) && !"movie".equals(mode)) {
                            continue;
                        }
                        availableModes.add(mode);
                    }
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            prepareShootModeRadioButtonsUi(
                                    availableModes.toArray(new String[0]),
                                    currentMode);
                            // Hide progress indeterminate on title bar.
                            setProgressBarIndeterminateVisibility(false);
                        }
                    });
                } catch (IOException e) {
                    Log.w(TAG, "prepareShootModeRadioButtons: IOException: "
                            + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG,
                            "prepareShootModeRadioButtons: JSON format error.");
                }
            };
        }.start();
    }

    // Prepare for Radio Button UI of Shoot Mode.
    private void prepareShootModeRadioButtonsUi(String[] availableShootModes,
            String currentMode) {
        mRadiosShootMode.clearCheck();
        mRadiosShootMode.removeAllViews();

        for (int i = 0; i < availableShootModes.length; i++) {
            String mode = availableShootModes[i];
            RadioButton radioBtn = new RadioButton(SampleCameraActivity.this);
            int viewId = 123456 + i; // workaround
            radioBtn.setId(viewId);
            radioBtn.setText(mode);
            radioBtn.setTag(mode);
            radioBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                        boolean isChecked) {
                    if (isChecked) {
                        if (mRadioInitialChecked) {
                            // ignore because this callback is invoked by
                            // initializing.
                            mRadioInitialChecked = false;
                        } else {
                            String mode = buttonView.getText().toString();
                            setShootMode(mode);
                        }
                    }
                }
            });
            mRadiosShootMode.addView(radioBtn);
            if (mode.equals(currentMode)) {
                // Set the flag true to suppress unnecessary API calling.
                mRadioInitialChecked = true;
                mRadiosShootMode.check(viewId);
            }
        }
    }

    // Prepare for Button to select "actZoom" by user.
    private void prepareActZoomButtons(final boolean flag) {
        new Thread() {

            @Override
            public void run() {
                Log.d(TAG, "prepareActZoomButtons(): exec.");
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        prepareActZoomButtonsUi(flag);
                    }
                });
            };
        }.start();
    }

    // Prepare for ActZoom Button UI.
    private void prepareActZoomButtonsUi(boolean flag) {
        if (flag) {
            mButtonZoomOut.setVisibility(View.VISIBLE);
            mButtonZoomIn.setVisibility(View.VISIBLE);
        } else {
            mButtonZoomOut.setVisibility(View.GONE);
            mButtonZoomIn.setVisibility(View.GONE);
        }
    }

    // Call setShootMode
    private void setShootMode(final String mode) {
        new Thread() {

            @Override
            public void run() {
                try {
                    JSONObject replyJson = mRemoteApi.setShootMode(mode);
                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    int resultCode = resultsObj.getInt(0);
                    if (resultCode == 0) {
                        // Success, but no refresh UI at the point.
                    } else {
                        Log.w(TAG, "setShootMode: error: " + resultCode);
                        toast(R.string.sony_msg_error_api_calling);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "setShootMode: IOException: " + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "setShootMode: JSON format error.");
                }
            }
        }.start();
    }

    // Take a picture and retrieve the image data.
    private void takeAndFetchPicture() {
    	
    	
    	
        if (!mLiveviewSurface.isStarted()) {
        	Log.w(TAG,"in this error if ");
            toast(R.string.sony_msg_error_take_picture);
            return;
        }

        new Thread() {

            @Override
            public void run() {
                try {
                	Log.w(TAG, "into the take and fetch picture");
                    JSONObject replyJson = mRemoteApi.actTakePicture();
                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    JSONArray imageUrlsObj = resultsObj.getJSONArray(0);
                    String postImageUrl = null;
                
                    
                    if (1 <= imageUrlsObj.length()) {
                        postImageUrl = imageUrlsObj.getString(0);
                    }
                    if (postImageUrl == null) {
                        Log.w(TAG,
                                "takeAndFetchPicture: post image URL is null.");
                        toast(R.string.sony_msg_error_take_picture);
                        return;
                    }
                    

                    
                    
                 // Default image name 
            		String imageFilenameString;
            		
            		
            		
            	
            			
            	 
                 
                    
                    
                    setProgressIndicator(true); // Show progress indicator
                    URL url = new URL(postImageUrl);
                    InputStream istream = new BufferedInputStream(url.openStream());
                    
                  
                            
                    byte [] b = new byte[2048]; //byte array to act as buffer for jpeg data
                    int length; // used in reading data from buffer 
                    
                
                    
                    String appPath = getResources().getString(R.string.app_path); //use default app_path to set path 
                    
            		// Create directory name based off of current date
            		//String directoryName = appPath + currentDateTime() + File.separator;
                    String directoryName = appPath + currentDate() + File.separator;
            		
            		// Create file object for parent directory
            		File newDirectory = new File(directoryName); 
            		
            		// Make sure path exists
            		newDirectory.mkdirs(); 
            		
            		
            		
            	
            		
            		
            			//imageCounter = createFile(directoryName) + 1;
            			//imageFilenameString = String.format(Locale.US, "%d" + "test",  imageCounter); //make an image file name
            			SimpleDateFormat timeStamp = new SimpleDateFormat("HH_mm_ss", Locale.US);
                		Date time = new Date();            		
                		imageFilenameString = timeStamp.format(time);
            	
            		
            		
            		// Default image name to be time stamp
            		
            		            		
            		latestImageDirectoryName = directoryName;
            		mostRecentTakenPictureName = imageFilenameString;
            		
            		try
            	    { 
            	        String path = directoryName + imageFilenameString + ".jpg";// + ".bmp"; //build path name 
            	        FileOutputStream ostream = new FileOutputStream(path);  //set up an output stream 
            	        try 
            	        {
            	        	//while reading in data to buffer, write it the output stream
            	        	 while((length = istream.read(b)) != -1){
                             	
                             	ostream.write(b, 0, length);
                             	
                             }
            	        	 
            			
            				ostream.close(); //close the output stream 
            			} 
            	        catch (IOException e) 
            	        {
            				e.printStackTrace();
            			} 

            	    } 
            		catch (FileNotFoundException e1) 
            	    { 
            	        e1.printStackTrace(); 
            	    } 
                    
            		
            		//Ask the user for name of the image
            		Intent newIntent = new Intent(SampleCameraActivity.this, EditFileName.class);
        	    	startActivityForResult(newIntent,0);
                    
                    
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4; // irresponsible value
                    final Drawable pictureDrawable = new BitmapDrawable(
                            getResources(), BitmapFactory.decodeStream(istream,
                                    null, options));
                    //Bitmap bm = BitmapFactory.decodeStream(istream);
                 
                    
                    
                    
                    istream.close(); //close the input stream 
                   
                    
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            mImagePictureWipe.setVisibility(View.VISIBLE);
                            mImagePictureWipe.setImageDrawable(pictureDrawable);
                        }
                    });

                } catch (IOException e) {
                    Log.w(TAG, "IOException while closing slicer: " + e.getMessage());
                    toast(R.string.sony_msg_error_take_picture);
                } catch (JSONException e) {
                    Log.w(TAG, "JSONException while closing slicer");
                    Log.d(TAG, e.toString());
                    toast(R.string.sony_msg_error_take_picture);
                } finally {
                    setProgressIndicator(false);
                }
            }
        }.start();
    }

    // Call startMovieRec
    private void startMovieRec() {
        new Thread() {

            @Override
            public void run() {
                try {
                    Log.d(TAG, "startMovieRec: exec.");
                    JSONObject replyJson = mRemoteApi.startMovieRec();
                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    int resultCode = resultsObj.getInt(0);
                    if (resultCode == 0) {
                        toast(R.string.sony_msg_rec_start);
                        startRec = true;
                        stopRec = false;
                        Log.d(TAG, "inside this vid if statement");
                    } else {
                        Log.w(TAG, "startMovieRec: error: " + resultCode);
                        toast(R.string.sony_msg_error_api_calling);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "startMovieRec: IOException: " + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "startMovieRec: JSON format error.");
                }
            }
        }.start();
    }

    // Call stopMovieRec
    private void stopMovieRec() {
        new Thread() {

            @Override
            public void run() {
                try {
                    Log.d(TAG, "stopMovieRec: exec.");
                    JSONObject replyJson = mRemoteApi.stopMovieRec();
                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    String thumbnailUrl = resultsObj.getString(0);
                    if (thumbnailUrl != null) {
                        toast(R.string.sony_msg_rec_stop);
                        startRec = false;
                        stopRec = true;
                    } else {
                        Log.w(TAG, "stopMovieRec: error");
                        toast(R.string.sony_msg_error_api_calling);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "stopMovieRec: IOException: " + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "stopMovieRec: JSON format error.");
                }
            }
        }.start();
    }

    // Call actZoom
    private void actZoom(final String direction, final String movement) {
        new Thread() {

            @Override
            public void run() {
                try {
                    JSONObject replyJson = mRemoteApi.actZoom(direction, movement);
                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    int resultCode = resultsObj.getInt(0);
                    if (resultCode == 0) {
                        // Success, but no refresh UI at the point.
                    } else {
                        Log.w(TAG, "actZoom: error: " + resultCode);
                        toast(R.string.sony_msg_error_api_calling);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "actZoom: IOException: " + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "actZoom: JSON format error.");
                }
            }
        }.start();
    }

    // Show or hide progress indicator on title bar
    private void setProgressIndicator(final boolean visible) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                setProgressBarIndeterminateVisibility(visible);
            }
        });
    }

    // show toast
    private void toast(final int msgId) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(SampleCameraActivity.this, msgId,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
   
    
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	super.onActivityResult(requestCode, resultCode, data);
    	
    		savedFileName= data.getStringExtra("RETURN VALUE");
    		changeLastFileName(latestImageDirectoryName,savedFileName);
 
    }
    
    
    private boolean isEmptyString(String s){
    	if (s == "")
    		return true;
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
    
    
 // Finds the last file, and change its name
    private void changeLastFileName(String path, String desiredName)
    {
    	if (isEmptyString(desiredName)){
    		return;
    	} 
    	desiredName = truncateLeadingSpaces(desiredName);
    	
    	//Cancel the save
    	if (desiredName.equals("pppppppppppp")){
    		File oldFile = new File(path + mostRecentTakenPictureName + ".jpg");
    		oldFile.delete();
    	} else{
    		//Rename the image
    		File oldFile = new File(path + mostRecentTakenPictureName + ".jpg");
			File newFile = new File(path + desiredName + ".jpg");
			oldFile.renameTo(newFile);
    	}
    }
    

    private String latestImageDirectoryName = "";
    private String mostRecentTakenPictureName = "";
}
