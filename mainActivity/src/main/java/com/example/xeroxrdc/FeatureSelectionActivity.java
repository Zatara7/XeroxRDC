package com.example.xeroxrdc;

/**
 * FeatureSelectionActivity
 * This activity contains the page for setting image analysis preferences.
 * @author Daragh 
 */

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class FeatureSelectionActivity extends PreferenceActivity 
{
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(this, R.xml.feature_selection, false);
		
		// Display the fragment as the main content.
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new FeatureSelectionFragment())
				.commit();
	}

}
