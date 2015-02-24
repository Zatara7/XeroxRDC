package com.example.sony.cameraremote;

// This is hayder's comment

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.EditText;

import com.example.xeroxrdc.R;

public class EditVideoFileName extends Activity{
	EditText editVideoFileName = null;
	String savedFileName = "";
	String videoName;
	String path;
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.edit_video_file_name);
		
		
		
		//Get the current name of the video file
		videoName = getIntent().getStringExtra("videoName");
		
		//Get the current directory of the video file 
		path = getIntent().getStringExtra("path");
		
		editVideoFileName = new EditText(EditVideoFileName.this);
		AlertDialog.Builder builder = new AlertDialog.Builder(EditVideoFileName.this);
		builder.setView(editVideoFileName);
    	builder.setTitle("Save As");
    	builder.setMessage("Please enter name for the image, leave blank for timestamp");
    	builder.setNegativeButton("Save",new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				savedFileName = editVideoFileName.getText().toString();
			
				if (savedFileName != ""){//Rename the file here
					File oldFile = new File(path + videoName + ".mp4");
					File newFile = new File(path + savedFileName + ".mp4");
					oldFile.renameTo(newFile);
				}	
				finish();
			}
		});
    	
    	
    	builder.setPositiveButton("Cancel",new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				
				//Delete the file here
				File oldFile = new File(path + videoName + ".mp4");
	    		oldFile.delete();
				
				finish();
			}
		});
    	AlertDialog alert = builder.create();
    	alert.show();

	}
}
