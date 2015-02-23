package com.example.xeroxrdc;

/**
 * FeatureSelectionFragment
 * This fragment contains the page for setting image analysis preferences.
 * @author Daragh 
 */

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class FeatureSelectionFragment extends PreferenceFragment
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		// Load preferences from XML resource
		addPreferencesFromResource(R.xml.feature_selection);
	}

}
