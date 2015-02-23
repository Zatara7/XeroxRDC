package com.xeroxrdc.mjpeg;

/**
 * MjpegReader
 * Runs an asynchronous task that connects to a server
 * and initiates an Mjpeg stream.
 * @author Andrew
 * @author Daragh
 */

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;

public class MjpegReader extends AsyncTask<String, Void, MjpegInputStream>
{
	private String tag;
	private MjpegView mv;
	
	//TODO Define this in settings instead of hardcoding
	private static final int PORT_NUMBER = 8080;

	public MjpegReader(MjpegView mv, String tag)
	{
		this.mv = mv;
		this.tag = tag;
	}
	
	protected MjpegInputStream doInBackground(String... url)
	{
		// TODO: if camera has authentication deal with it and don't just not work
		HttpResponse res = null;
		DefaultHttpClient httpclient = new DefaultHttpClient();
		Log.d(tag, "1. Sending http request");
		
		try
		{
			res = httpclient.execute(new HttpGet(URI.create(url[0])));
			Log.d(tag, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
			if (res.getStatusLine().getStatusCode() == 401)
			{
				// You must turn off camera User Access Control before this will work
				return null;
			}
			return new MjpegInputStream(res.getEntity().getContent());
		} 
		catch (ClientProtocolException e)
		{
			e.printStackTrace();
			Log.d(tag, "Request failed-ClientProtocolException", e);
			// Error connecting to camera
		} 
		catch (IOException e)
		{
			e.printStackTrace();
			Log.d(tag, "Request failed-IOException", e);
			// Error connecting to camera
		}

		return null;
	}
	
	protected void onPostExecute(MjpegInputStream result)
	{
		mv.setSource(result);
		mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
		mv.showFps(true);
	}

	/**
	 * Given the short URL of the data stream, 
	 * prepend http and append port number and stream command
	 * @param url of the form "www.example.com"
	 * @return
	 */
	public static String getStreamURL(String url)
	{
		return "http://" + url + ":" + PORT_NUMBER + "/?action=stream";
	}
}
