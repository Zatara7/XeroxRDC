package com.example.xeroxrdc;

/**
 * Main Activity for XeroxRDC
 * This activity is primarily responsible for managing the fragment pages. 
 * @author Daragh
 * @author Andrew
 */
 
import java.util.Locale;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

//import com.example.xeroxrdc.VideoPlay;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
//import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

public class MainActivity extends FragmentActivity
{
	private static final String TAG = "XeroxRDC::MainActivity";
	
	public static final int PAGECOUNT = 4;
	
	public static final int CONNECTPAGE = 0;
	public static final int VIDEOPAGE = 3;
	public static final int IMAGEPAGE = 1;
	public static final int GRAPHPAGE = 2;
	
	public SectionsPagerAdapter mSectionsPagerAdapter;


	// The ViewPager that will host the section contents.
	public ViewPager mViewPager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);
		
		
		// Create the adapter that will return a fragment for each of the four
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter( getSupportFragmentManager() );

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOffscreenPageLimit(PAGECOUNT);
		
		// Set default values for app preferences
		PreferenceManager.setDefaultValues(this, R.xml.app_preferences, false);
		
		setCurrentPage(CONNECTPAGE);
		
		Intent newIntent = new Intent(this, NoiseRemoval.class);
		//startActivity(newIntent);
		
		//Call play pause
				//Intent playvideointent = new Intent(MainActivity.this, VideoPlay.class);
				//startActivity(playvideointent);
	}

	/*@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate actionbar menu
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}*/
	
	@Override
	public void onResume()
	{
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_7, this, mLoaderCallback);
	}
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
	{
		@Override
		public void onManagerConnected(int status)
		{
			switch (status)
			{
				case LoaderCallbackInterface.SUCCESS:
				{
					Log.i(TAG, "OpenCV loaded successfully");
				}
				break;
				
				default:
				{
					super.onManagerConnected(status);
				}
				break;
			}
		}
	};
	
	/**
	 * Set the current page.
	 * @param pagenum
	 * @return Fragment object for the selected page. If pagenum is invalid, returns null.
	 */
	public Fragment setCurrentPage(int pagenum)
	{
		if (pagenum >= 0 && pagenum < PAGECOUNT)
		{
			// Smooth scroll to page
			mViewPager.setCurrentItem(pagenum, true);
			// Use tag format to find the fragment
			return getSupportFragmentManager().findFragmentByTag("android:switcher:" + mViewPager.getId() + ":" + pagenum);
		}
		return null;
	}
	
	/***** Callback Functions *****/
	
	/*public void appSettings(MenuItem item)
	{
		startActivity( new Intent(this, AppSettingsActivity.class) );
	}

	public void cameraSettings(MenuItem item)
	{
		startActivity( new Intent(this, CameraSettingsActivity.class) );
	}*/
	
	public void onExit(MenuItem item)
	{
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
	
	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter
	{

		public SectionsPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		/**
		 * Initialize the fragment for each page
		 */
		@Override
		public Fragment getItem(int position)
		{			
			Fragment fragment;
			
			switch (position)
			{
			case(CONNECTPAGE):
				fragment = new CameraListFragment();
				break;
			case(VIDEOPAGE):
				fragment = new RDCVideoFragment();
				break;
			case(IMAGEPAGE):
				fragment = new RDCImageFragment();
				break;
			case(GRAPHPAGE):
				fragment = new RDCGraphFragment();
				break;
			default:
				fragment = null;
				break;
			}
			
			return fragment;
		}

		/**
		 * Number of pages
		 */
		@Override
		public int getCount()
		{
			return PAGECOUNT;
		}

		/**
		 * Returns the title of the page at the given index
		 */
		@Override
		public CharSequence getPageTitle(int position)
		{
			Locale loc = Locale.getDefault();
			switch (position)
			{
			case CONNECTPAGE: 
				return getString(R.string.title_cameralist).toUpperCase(loc);
			case VIDEOPAGE:
				return getString(R.string.title_video).toUpperCase(loc);
			case IMAGEPAGE:
				return getString(R.string.title_image).toUpperCase(loc);
			case GRAPHPAGE:
				return getString(R.string.title_graph).toUpperCase(loc);
			}
			return null;
		}
	}
}