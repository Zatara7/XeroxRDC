/*
 * Copyright 2013 Sony Corporation
 */

package com.example.sony.cameraremote;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.sony.cameraremote.utils.SimpleLiveviewSlicer;
import com.example.sony.cameraremote.utils.SimpleLiveviewSlicer.Payload;
import com.example.xeroxrdc.R;
import com.example.xeroxrdc.SampleCameraActivity;
import com.xeroxrdc.mjpeg.MjpegView.ImageAndTimestamp;


import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A SurfaceView based class to draw liveview frames serially.
 */
public class SimpleLiveviewSurfaceView extends SurfaceView implements
        SurfaceHolder.Callback {

    private static final String TAG = SimpleLiveviewSurfaceView.class
            .getSimpleName();

    private SimpleRemoteApi mRemoteApi;
    private boolean mWhileFetching;
    private final BlockingQueue<byte[]> mJpegQueue = new ArrayBlockingQueue<byte[]>(2);
    private final boolean mInMutableAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    private Thread mDrawerThread;
    private int mPreviousWidth = 0;
    private int mPreviousHeight = 0;
    private final Paint mFramePaint;
    private int numofpayloads = 0;
    

    private Queue<byte[]> imgQueue; //queue to hold the jpeg data coming in from liveview stream 
  
 // Maximum queue size (in bytes)
 	//private static final int MAXSIZE_B = 500 * 1048576;
 	private int numOfImageAdds = 0; //number of images added to queue 
    

    /**
     * Contractor
     * 
     * @param context
     */
    public SimpleLiveviewSurfaceView(Context context) {
        super(context);
        getHolder().addCallback(this);
        mFramePaint = new Paint();
        mFramePaint.setDither(true);
      
        imgQueue  = new LinkedList<byte[]>(); //added
    }

    /**
     * Contractor
     * 
     * @param context
     * @param attrs
     */
    public SimpleLiveviewSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        mFramePaint = new Paint();
        mFramePaint.setDither(true);
     
        imgQueue  = new LinkedList<byte[]>(); //added
    }

    /**
     * Contractor
     * 
     * @param context
     * @param attrs
     * @param defStyle
     */
    public SimpleLiveviewSurfaceView(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        getHolder().addCallback(this);
        mFramePaint = new Paint();
        mFramePaint.setDither(true);
       
        imgQueue  = new LinkedList<byte[]>(); //added
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        // do nothing.
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // do nothing.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mWhileFetching = false;
    }

   
    
    /**
     * Bind a Remote API object to communicate with Camera device. Need to call
     * this method before calling start() method.
     * 
     * @param remoteApi
     */
    public void bindRemoteApi(SimpleRemoteApi remoteApi) {
        mRemoteApi = remoteApi;
    }

    /**
     * Start retrieving and drawing liveview frame data by new threads.
     * 
     * @return true if the starting is completed successfully, false otherwise.
     * @exception IllegalStateException when Remote API object is not set.
     * @see SimpleLiveviewSurfaceView#bindRemoteApi(SimpleRemoteApi)
     */
    public boolean start() {
        if (mRemoteApi == null) {
            throw new IllegalStateException("RemoteApi is not set.");
        }
        if (mWhileFetching) {
            Log.w(TAG, "start() already starting.");
            return false;
        }

        mWhileFetching = true;

        // A thread for retrieving liveview data from server.
        new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "Starting retrieving liveview data from server.");
              
                SimpleLiveviewSlicer slicer = null;

                try {
                    // Prepare for connecting.
                    JSONObject replyJson = null;

                    replyJson = mRemoteApi.startLiveview();
                    if (!isErrorReply(replyJson)) {
                        JSONArray resultsObj = replyJson.getJSONArray("result");
                        String liveviewUrl = null;
                        if (1 <= resultsObj.length()) {
                            // Obtain liveview URL from the result.
                            liveviewUrl = resultsObj.getString(0);
                        }
                        if (liveviewUrl != null) {
                            // Create Slicer to open the stream and parse it.
                            slicer = new SimpleLiveviewSlicer();
                            slicer.open(liveviewUrl);
                        }
                    }

                    if (slicer == null) {
                        mWhileFetching = false;
                        return;
                    }

                    while (mWhileFetching) {
                        final Payload payload = slicer.nextPayload();
                        if (payload == null) { // never occurs
                            Log.e(TAG, "Liveview Payload is null.");
                            continue;
                        }

                        if (mJpegQueue.size() == 2) {
                            mJpegQueue.remove();
                        }
                        
                       //this was added to capture the jpeg payload and save it to the image queue before it is shown on stream
                        //add to queue here if start rec is true 
                      
                        if(SampleCameraActivity.startRec == true){
                        	
                        	//option to delete picture from queue if gets too large, but added would slow down stream 
                        	// Only delete values once the queue exceeds max size
            				/*if (getQueueMemory(imgQueue) > MAXSIZE_B)
            				{
            					// Start flushing out the end of the queue
            					// We wanted to make it automatically save the stream once this occurred, however
            					// we ran out of time so now we just dump the oldest frames so that it doesnt crash
            					while ( !imgQueue.isEmpty() && getQueueMemory(imgQueue) > MAXSIZE_B)
            					{
            						imgQueue.remove();
            					}
            					
            					
            				}*/
            				
            				// Add new image to queue
            				
                        	imgQueue.add(payload.jpegData);
                        	++numOfImageAdds;
                        	
                        	//when stop recording happens
                        }else if(SampleCameraActivity.stopRec == true){
                        	
                        	
                    		
                    		//make path name from app_path 
                    		String appPath = getResources().getString(R.string.app_path);
                    		// Create directory name based off of current date, sub directories of time
                    		String directoryName = appPath + currentDate() + File.separator + currentTime() + File.separator;
                    		// Create file object for parent directory
                    		File newDirectory = new File(directoryName); 
                    		// Make sure path exists
                    		newDirectory.mkdirs(); 

                    		// Save the files in the queue
                    		writeQueueToDisk(imgQueue, directoryName);

                    		// Convert images to video here
                    		
                    		createAndSaveVideo(directoryName, imgQueue); 
                    			
                    		

                    		// Clear queue so another fresh recording can take
                    		imgQueue.clear();
                    		SampleCameraActivity.stopRec = false;
                    		
                    		
                    		
                    		//Fred edited 11/5/2014s
                    		//Ask the user for name of the image
                    		/*
                    		Intent newIntent = new Intent(getContext(), EditVideoFileName.class);
                    		newIntent.putExtra("videoName", "video");
                    		newIntent.putExtra("path", directoryName);
                    		getContext().startActivity(newIntent);
                        	*/
                        	
                        }
                        
                       /////-----/////
                        
                        mJpegQueue.add(payload.jpegData);
                        ++numofpayloads;
                    }
                } catch (IOException e) {
                    Log.w(TAG, "IOException while fetching: " + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "JSONException while fetching");
                } finally {
                    // Finalize
                    try {
                        if (slicer != null) {
                            slicer.close();
                        }
                        mRemoteApi.stopLiveview();
                    } catch (IOException e) {
                        Log.w(TAG, "IOException while closing slicer: " + e.getMessage());
                    }

                    if (mDrawerThread != null) {
                        mDrawerThread.interrupt();
                    }

                    mJpegQueue.clear();
                    mWhileFetching = false;
                }
            }
        }.start();

        // A thread for drawing liveview frame fetched by above thread.
        mDrawerThread = new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "Starting drawing liveview frame.");
                Bitmap frameBitmap = null;

                BitmapFactory.Options factoryOptions = new BitmapFactory.Options();
                factoryOptions.inSampleSize = 1;
                if (mInMutableAvailable) {
                    initInBitmap(factoryOptions);
                }

                while (mWhileFetching) {
                    try {
                        byte[] jpegData = mJpegQueue.take();
                        frameBitmap = BitmapFactory.decodeByteArray(
                                jpegData, 0,
                                jpegData.length, factoryOptions);
                    } catch (IllegalArgumentException e) {
                        if (mInMutableAvailable) {
                            clearInBitmap(factoryOptions);
                        }
                        continue;
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Drawer thread is Interrupted.");
                        break;
                    }

                    if (mInMutableAvailable) {
                        setInBitmap(factoryOptions, frameBitmap);
                    }
                    drawFrame(frameBitmap);
                }

                if (frameBitmap != null) {
                    frameBitmap.recycle();
                }
                mWhileFetching = false;
            }
        };
        mDrawerThread.start();
        return true;
    }

    /**
     * Request to stop retrieving and drawing liveview data.
     */
    public void stop() {
        mWhileFetching = false;
    }

    /**
     * Check to see whether start() is already called.
     * 
     * @return true if start() is already called, false otherwise.
     */
    public boolean isStarted() {
        return mWhileFetching;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void initInBitmap(BitmapFactory.Options options) {
        options.inBitmap = null;
        options.inMutable = true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void clearInBitmap(BitmapFactory.Options options) {
        if (options.inBitmap != null) {
            options.inBitmap.recycle();
            options.inBitmap = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setInBitmap(BitmapFactory.Options options, Bitmap bitmap) {
        options.inBitmap = bitmap;
    }

    // Draw frame bitmap onto a canvas.
    private void drawFrame(Bitmap frame) {
        if (frame.getWidth() != mPreviousWidth
                || frame.getHeight() != mPreviousHeight) {
            onDetectedFrameSizeChanged(frame.getWidth(), frame.getHeight());
            return;
        }
        Canvas canvas = getHolder().lockCanvas();
        if (canvas == null) {
            return;
        }
        int w = frame.getWidth();
        int h = frame.getHeight();
        Rect src = new Rect(0, 0, w, h);

        float by = Math
                .min((float) getWidth() / w, (float) getHeight() / h);
        int offsetX = (getWidth() - (int) (w * by)) / 2;
        int offsetY = (getHeight() - (int) (h * by)) / 2;
        Rect dst = new Rect(offsetX, offsetY, getWidth() - offsetX,
                getHeight() - offsetY);
        canvas.drawBitmap(frame, src, dst, mFramePaint);
        getHolder().unlockCanvasAndPost(canvas);
    }

    // Called when the width or height of liveview frame image is changed.
    private void onDetectedFrameSizeChanged(int width, int height) {
        Log.d(TAG, "Change of aspect ratio detected");
        mPreviousWidth = width;
        mPreviousHeight = height;
        drawBlackFrame();
        drawBlackFrame();
        drawBlackFrame(); // delete triple buffers
    }

    // Draw black screen.
    private void drawBlackFrame() {
        Canvas canvas = getHolder().lockCanvas();
        if (canvas == null) {
            return;
        }

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);

        canvas.drawRect(new Rect(0, 0, getWidth(), getHeight()), paint);
        getHolder().unlockCanvasAndPost(canvas);
    }

    // Parse JSON and returns a error code.
    private static boolean isErrorReply(JSONObject replyJson) {
        boolean hasError = (replyJson != null && replyJson.has("error"));
        return hasError;
    }
    
    
   
    

    
    /**
	 * Get the amount of memory (in bytes) taken up by the queue
	 * @param queue
	 * @return Size in bytes
	 */
	public long getQueueMemory(Queue<ImageAndTimestamp> queue)
	{
		long imgsize = 0;
		// Get number of elements in queue
		long qsize = queue.size();
		
		if (qsize == 0)
			return 0;
		
		// Get size of first image (in bytes)
		// We assume that all images are the same size
		ImageAndTimestamp img = queue.peek();
		byte[] bm = img.getImage();
		imgsize = bm.length;
		
		// The total size is image size times number of elements
		return qsize*imgsize;
	}
	
	
	public void createAndSaveVideo(String directory, Queue<byte[]> theQueue)
	{
		// Create video file name
		// If we change video.mp4, it will have side effects when loading the video on the graphs page
		String fullPath = directory + "video.mp4"; // Hardcoded ;)

		// This API seems promising:
		// http://stackoverflow.com/a/16529673/2561421
		// http://jcodec.org/
		// This uses the Android version of jcodec, as the regular version supports buffered images, and the android version supports Bitmaps
	    SequenceEncoder encoder = null;
		try
		{
			encoder = new SequenceEncoder(new File(fullPath));
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}	// This sets up the output file it will write to
	    
		Iterator<byte[]> it = theQueue.iterator();
		while(it.hasNext())
		{
			// You may or may not have to write out explicitly like I did
	        //ImageAndTimestamp tempImageAndTimestamp = (Ima it.next();	
	        try
	        {
	    		Log.i(TAG, "Encoding an image to video.");
	    		// We need to encode the image (Byte Array) in a video
	    		// To do this, the image must be converted to a Picture
	    		byte[] image = it.next();
				encoder.encodeNativeFrame(fromBitmap(convertByteArrayToBitmap(image)));
				Log.i(TAG, "Image encoded.");
			}
	        catch (IOException e)
	        {
				e.printStackTrace();
			}
	    }
	    try
	    {
			encoder.finish();
			/*
			Intent newIntent = new Intent(getContext(), EditVideoFileName.class);
    		newIntent.putExtra("videoName", "video");
    		newIntent.putExtra("path", directory);
    		getContext().startActivity(newIntent);
    		
    		*/
		}
	    catch (IOException e)
	    {
			e.printStackTrace();
		}
	}
	
	public static Bitmap convertByteArrayToBitmap(byte[] byteArray)
	{
		return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
	}
    
	
	// Source/Copyright: http://stackoverflow.com/a/16596284/2561421
		// Creative Commons License
		public static Picture fromBitmap(Bitmap src) 
		{
			Picture dst = Picture.create((int)src.getWidth(), (int)src.getHeight(), ColorSpace.RGB);
			fromBitmap(src, dst);
			return dst;
		}
		
		
		// Source/Copyright: http://stackoverflow.com/a/16596284/2561421
		// Creative Commons License
		public static void fromBitmap(Bitmap src, Picture dst)
		{
			int[] dstData = dst.getPlaneData(0);
			int[] packed = new int[src.getWidth() * src.getHeight()];

			src.getPixels(packed, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());

			for (int i = 0, srcOff = 0, dstOff = 0; i < src.getHeight(); i++)
			{
				for (int j = 0; j < src.getWidth(); j++, srcOff++, dstOff += 3)
				{
					int rgb = packed[srcOff];
					dstData[dstOff] = (rgb >> 16) & 0xff;
					dstData[dstOff + 1] = (rgb >> 8) & 0xff;
					dstData[dstOff + 2] = rgb & 0xff;
				}
			}
		}
		
		// Used to create directory of the current date to store all videos and images if not already created
		private String currentDate()
		{
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd", Locale.US);
			Date now = new Date();
			String currentDate = formatter.format(now);
			return currentDate;
		}
		
		// Used to create a sub-directory for video with the time it was created
		private String currentTime()
		{
			//SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
			SimpleDateFormat formatter = new SimpleDateFormat("HH_mm_ss", Locale.US);
			Date now = new Date();
			String currentTime = formatter.format(now); // + ".jpg";
			return currentTime;
		}
		
		// Save individual image to disk
		private void saveImage(byte[] image, String directoryName, long imageNumber)
		{
			// The %016d makes adds leading zeroes for the incrementing filename (so they can be alphabetized)
			// So the image filename will always be 16 digit wide (the zeroes will pad accordingly)
			
			// the sub directory to video and images is the time when the video was taken
			String imagePathName = String.format(Locale.US, "%d" , imageNumber); 
			
			try
		    { 
		        String path = directoryName + imagePathName + ".jpg";// + ".bmp"; 
		        
		        FileOutputStream stream = new FileOutputStream(path); 
		       // Log.i(TAG, "saved image to disk " + imageNumber);
		        try 
		        {
					stream.write(image);
					stream.close();
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
		}
		
		
		// Writes everything in the queue to disk
		private void writeQueueToDisk(Queue<byte[]> theQueue, String directoryName)
		{
			//Log.i(TAG, "in the write queue to disk function ");
			long imageNumber = 0;
			Iterator<byte[]> it = theQueue.iterator();
			//(it.hasNext())
			for(int i = 0; i < numOfImageAdds; ++i) 
			{
				// You may or may not have to cast its type like I did
				//ImageAndTimestamp tempObject = (ImageAndTimestamp) it.next();
				// This is where it saves the file
				//System.out.println("Writing image " + imageNumber + " to disk.");
				//Log.i(TAG, "before save");
				byte[] imageToSave = it.next();
				saveImage(imageToSave, directoryName, imageNumber);
				//Log.i(TAG, "saved image" + imageNumber);
				//System.out.println("Image " + imageNumber + " written to disk.");
				imageNumber++;
			}
		}
	
}
