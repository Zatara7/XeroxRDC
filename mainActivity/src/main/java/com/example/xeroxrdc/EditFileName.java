package com.example.xeroxrdc;

import com.example.xeroxrdc.MainActivity.SectionsPagerAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Window;
import android.widget.EditText;

public class EditFileName extends FragmentActivity {
	EditText edit_file_name;
	String savedFileName;
	
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.edit_file_name);
		
		
		edit_file_name = new EditText(EditFileName.this);
		AlertDialog.Builder builder = new AlertDialog.Builder(EditFileName.this);
		builder.setView(edit_file_name);
    	builder.setTitle("Save As");
    	builder.setMessage("Please enter name for the image, leave blank for timestamp");
    	builder.setNegativeButton("Save",new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				savedFileName = edit_file_name.getText().toString();
				
				
				//Return the string to calling parent activity
				Intent resultData = new Intent();
				resultData.putExtra("RETURN VALUE", savedFileName);
				setResult(Activity.RESULT_OK, resultData);
				finish();
			}
		});
    	
    	
    	builder.setPositiveButton("Cancel",new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				
				
				//Return the string to calling parent activity
				Intent resultData = new Intent();
				resultData.putExtra("RETURN VALUE", "pppppppppppp");
				setResult(Activity.RESULT_OK, resultData);
				finish();
			}
		});
    	AlertDialog alert = builder.create();
    	alert.show();
	}
	
}
