package com.xeroxrdc.mjpeg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.xeroxrdc.R;

public class MjpegView extends SurfaceView implements SurfaceHolder.Callback
{
	private static final String TAG = "MjpegView";

	public final static int POSITION_UPPER_LEFT = 9;
	public final static int POSITION_UPPER_RIGHT = 3;
	public final static int POSITION_LOWER_LEFT = 12;
	public final static int POSITION_LOWER_RIGHT = 6;

	public final static int SIZE_STANDARD = 1;
	public final static int SIZE_BEST_FIT = 4;
	public final static int SIZE_FULLSCREEN = 8;

	// Counter to number the images
	public static int jpegNumber = 0; 


	private Queue<ImageAndTimestamp> imgQueue;
	
	private MjpegViewThread thread;
	private MjpegInputStream mIn = null;
	private boolean saveFrames = false;
	private String framePath;
	
	private boolean showFps = false;
	private boolean mRun = false;
	private boolean surfaceDone = false;
	
	private Paint overlayPaint;
	private int overlayTextColor;
	private int overlayBackgroundColor;
	private int ovlPos;
	private int dispWidth;
	private int dispHeight;
	private int displayMode;

	private boolean isPlaying = false;
	
	public MjpegView(Context context)
	{
		super(context);
		init(context);
	}
	
	public MjpegView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context);
	}
	
	private void init(Context context)
	{
		imgQueue  = new LinkedList<ImageAndTimestamp>();
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		thread = new MjpegViewThread(holder, context, imgQueue);
		setFocusable(true);
		overlayPaint = new Paint();
		overlayPaint.setTextAlign(Paint.Align.LEFT);
		overlayPaint.setTextSize(12);
		overlayPaint.setTypeface(Typeface.DEFAULT);
		overlayTextColor = Color.WHITE;
		overlayBackgroundColor = Color.BLACK;
		ovlPos = MjpegView.POSITION_LOWER_RIGHT;
		displayMode = MjpegView.SIZE_STANDARD;
		dispWidth = getWidth();
		dispHeight = getHeight();
		framePath = getResources().getString(R.string.app_path);
	}

	/**
	 * Creates a new thread with the same properties as the current thread.
	 * This is necessary in order to restart an existing thread.
	 * @return
	 */
	private MjpegViewThread cloneThread()
	{
		MjpegViewThread tmp = null;
		
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		
		// Create new thread
		tmp = new MjpegViewThread(holder, getContext(), imgQueue);
		
		// Copy settings to new thread
		tmp.setRecordEnabled(thread.isRecordingEnabled());
		tmp.setLongRecord(thread.isLongRecord());
		
		return tmp;
	}
	
	public void startPlayback()
	{
		// If thread is dead, reinitialize so it can run again
		if (thread.getState() == Thread.State.TERMINATED)
			thread = cloneThread();
		
		if (mIn != null)
		{
			mRun = true;
			thread.start();
		}
	}

	public void stopPlayback()
	{
		mRun = false;
		boolean retry = true;
		while (retry)
		{
			try
			{
				thread.join();
				retry = false;
			} 
			catch (InterruptedException e)
			{
				e.getStackTrace();
				Log.d(TAG, "catch IOException hit in stopPlayback", e);
			}
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int f, int w, int h)
	{
		thread.setSurfaceSize(w, h);
	}

	public void surfaceDestroyed(SurfaceHolder holder)
	{
		surfaceDone = false;
		stopPlayback();
	}

	public void surfaceCreated(SurfaceHolder holder)
	{
		surfaceDone = true;
	}

	public void saveFrames(boolean b)
	{
		saveFrames = b;
	}
	
	public void setFramePath(String path)
	{
		framePath = path;
	}
	
	public void showFps(boolean b)
	{
		showFps = b;
	}

	public void setSource(MjpegInputStream source)
	{
		mIn = source;
		startPlayback();
	}

	public void setOverlayPaint(Paint p)
	{
		overlayPaint = p;
	}

	public void setOverlayTextColor(int c)
	{
		overlayTextColor = c;
	}

	public void setOverlayBackgroundColor(int c)
	{
		overlayBackgroundColor = c;
	}

	public void setOverlayPosition(int p)
	{
		ovlPos = p;
	}

	public void setDisplayMode(int s)
	{
		displayMode = s;
	}
	
	public boolean isPlaying()
	{
		return isPlaying;
	}
	
	public void setRecordEnabled(boolean enabled)
	{
		thread.setRecordEnabled(enabled);
	}
	
	public boolean isRecordingEnabled()
	{
		return thread.isRecordingEnabled();
	}
	
	public void setLongRecord(boolean enabled)
	{
		thread.setLongRecord(enabled);
	}
	
	public boolean isLongRecord()
	{
		return thread.isLongRecord();
	}
	
	public Queue<ImageAndTimestamp>getImageQueue()
	{
		return imgQueue;
	}
	
	/**
	 *  Class for grouping images and timestamps 
	 */
	public class ImageAndTimestamp
	{
		public byte[] image;
		private long timestamp;

		ImageAndTimestamp()
		{
			image = null;
			timestamp = 0;
		}
		
		ImageAndTimestamp(byte[] image, long timestamp)
		{
			this.image = image;
			this.timestamp = timestamp;
		}
		
		public void setImage(byte[] image)
		{
			this.image = image;
		}
		
		public byte[] getImage()
		{
			return image;
		}
		
		public long getTimestamp()
		{
			return timestamp;
		}
	}
	
	public class MjpegViewThread extends Thread
	{
		private static final long TEN_SECONDS = 10;
		// Maximum queue size (in bytes)
		private static final int MAXSIZE_B = 500 * 1048576;
		
		private Queue<ImageAndTimestamp> imgQueue;
		private SurfaceHolder mSurfaceHolder;
		private int frameCounter = 0;
		private long start;
		private Bitmap ovl;
		
		// Enable saving frames to memory
		private boolean recordEnabled = true;
		// Keep recording past ten seconds
		private boolean longRecord = false;

		public MjpegViewThread(SurfaceHolder surfaceHolder, Context context, Queue<ImageAndTimestamp> imgQueue)
		{
			mSurfaceHolder = surfaceHolder;
			this.imgQueue = imgQueue;
		}

		private Rect destRect(int bmw, int bmh)
		{
			int tempx;
			int tempy;
			if (displayMode == MjpegView.SIZE_STANDARD)
			{
				tempx = (dispWidth / 2) - (bmw / 2);
				tempy = (dispHeight / 2) - (bmh / 2);
				return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
			}
			if (displayMode == MjpegView.SIZE_BEST_FIT)
			{
				float bmasp = (float) bmw / (float) bmh;
				bmw = dispWidth;
				bmh = (int) (dispWidth / bmasp);
				if (bmh > dispHeight)
				{
					bmh = dispHeight;
					bmw = (int) (dispHeight * bmasp);
				}
				tempx = (dispWidth / 2) - (bmw / 2);
				tempy = (dispHeight / 2) - (bmh / 2);
				return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
			}
			if (displayMode == MjpegView.SIZE_FULLSCREEN)
			{
				return new Rect(0, 0, dispWidth, dispHeight);
			}
			return null;
		}

		public void setSurfaceSize(int width, int height)
		{
			synchronized (mSurfaceHolder)
			{
				dispWidth = width;
				dispHeight = height;
			}
		}

		private Bitmap makeFpsOverlay(Paint p, String text)
		{
			Rect b = new Rect();
			p.getTextBounds(text, 0, text.length(), b);
			int bwidth = b.width() + 2;
			int bheight = b.height() + 2;
			Bitmap bm = Bitmap.createBitmap(bwidth, bheight,
					Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(bm);
			p.setColor(overlayBackgroundColor);
			c.drawRect(0, 0, bwidth, bheight, p);
			p.setColor(overlayTextColor);
			c.drawText(text, -b.left + 1,
					(bheight / 2) - ((p.ascent() + p.descent()) / 2) + 1, p);
			return bm;
		}
		
		private int addToCircularQueue(Queue<ImageAndTimestamp> theQueue, byte[] image)
		{

			if (longRecord) // Keep recording past 10 seconds
			{ 
				// Only delete values once the queue exceeds max size
				if (getQueueMemory(theQueue) > MAXSIZE_B)
				{
					// Start flushing out the end of the queue
					// We wanted to make it automatically save the stream once this occurred, however
					// we ran out of time so now we just dump the oldest frames so that it doesnt crash
					while ( !theQueue.isEmpty() && getQueueMemory(theQueue) > MAXSIZE_B)
					{
						theQueue.remove();
					}
					
					// The code below was for saving the stream whenever it exceeded MAXSIZE_B
					// However, we didn't have enough time to get this working, so we did the above instead
					//return 1; // This retval is used in MjpegView to display a
								// dialog box and stop recording
				}

			}
			else // Only keep the most recent 10 seconds of video
			{ 
				// Discard images older than 10 seconds
				while ( !theQueue.isEmpty() && 
						(System.currentTimeMillis() / 1000) - ((ImageAndTimestamp)theQueue.peek()).getTimestamp() > TEN_SECONDS)
				{ // We might need () around theQueue.peek()
					theQueue.remove();
				}
			}

			// Add new image to queue
			ImageAndTimestamp tempObject = new ImageAndTimestamp(image, System.currentTimeMillis() / 1000);
			theQueue.add(tempObject);

			return 0;
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
		
		public void setRecordEnabled(boolean enabled)
		{
			recordEnabled = enabled;
		}
		
		public boolean isRecordingEnabled()
		{
			return recordEnabled;
		}
		
		public void setLongRecord(boolean enabled)
		{
			longRecord = enabled;
		}
		
		public boolean isLongRecord()
		{
			return longRecord;
		}

		public void run() 
		{
			start = System.currentTimeMillis();
			PorterDuffXfermode mode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
			Bitmap bm;
			int width;
			int height;
			Rect destRect;
			Canvas c = null;
			Paint p = new Paint();
			String fps;
			
			while (mRun)
			{
				// The commented out part is a rough method for pausing the stream
				// I made a strange observation: when its not streaming:
				//	It takes a little less time (1 second as opposed to 2) to save the individual frames to disk,
				//	BUT it takes more time (9 seconds as opposed to 3) to encode the images to video when its NOT streaming (the weird part).
				// The heap has to constantly be re-grown due to frag cases.

				if (surfaceDone)
				{
					try
					{
						c = mSurfaceHolder.lockCanvas();
						synchronized (mSurfaceHolder)
						{
							try
							{
								// We modified readMjpegFrame so that it returns a byte[]
								byte[] byteArray = mIn.readMjpegFrame(); 
								bm = BitmapFactory.decodeStream(new ByteArrayInputStream(byteArray));
								
								destRect = destRect(bm.getWidth(), bm.getHeight());
								c.drawColor(Color.BLACK);
								c.drawBitmap(bm, null, destRect, p);
								
								/***** Display FPS counter *****/
								
								if (showFps)
								{
									p.setXfermode(mode);
									if (ovl != null)
									{
										height = ((ovlPos & 1) == 1) ? destRect.top : destRect.bottom - ovl.getHeight();
										width = ((ovlPos & 8) == 8) ? destRect.left : destRect.right - ovl.getWidth();
										c.drawBitmap(ovl, width, height, null);
									}
									
									p.setXfermode(null);
									frameCounter++;
									
									if ((System.currentTimeMillis() - start) >= 1000)
									{
										fps = String.valueOf(frameCounter) + " fps";
										frameCounter = 0;
										start = System.currentTimeMillis();
										ovl = makeFpsOverlay(overlayPaint, fps);
									}
								}

								/***** Save individual frames *****/
								
								// Insert this where we had it saving files before
								
								if (recordEnabled)
								{
									if (addToCircularQueue(imgQueue, byteArray/*bm*/) == 1)
									{ 
										// This returns 1 only when the queue
										// exceeds MAX_QUEUE_SIZE_MEGABYTES

										// Display dialog box that notifies the
										// user that it stopped recording
										// because it exceeded max file size
										
										/*
										 
										// We would do this if we wanted to stop recording once the stream exceeded
										// the MAXSIZE_B however, we ran out of time so an alternative solution we came up with
										// is inside addToCircularQueue()
										System.out.println( "Maximum File Size " + 
											MAXSIZE_B + "MB reached. Recording stopped. Saving file to disk." );
										
										Toast.makeText(getContext(),
												 "Maximum File Size " + MAXSIZE_B +
												 "MB reached. Recording stopped. Saving file to disk.",
												 Toast.LENGTH_SHORT)
												.show();
										*/
										//TODO Handle error condition
										// ***If we could call stopRecord() right here, it should work***
										
										
									}
								}
								
							} 
							catch (IOException e)
							{
								e.getStackTrace();
								Log.d(TAG, "catch IOException hit in run", e);
							}
						}
					} 
					finally
					{
						if (c != null)
						{
							mSurfaceHolder.unlockCanvasAndPost(c);
						}
					}
				}
			}
		}
	}
}
