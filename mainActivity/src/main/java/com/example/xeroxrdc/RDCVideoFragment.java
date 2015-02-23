package com.example.xeroxrdc;

/**
 * RDCVideoFragment
 * This page uses video replay functionality to review videos that have been taken.
 * @author Freddie
 * @author Jen 
 */

import java.io.File;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import ar.com.daidalos.afiledialog.FileChooserDialog;

public class RDCVideoFragment extends Fragment 
{

	private View rootView;
		
	private String previousDirectoryChosen = "/storage/sdcard0/XeroxRDC/";
	TextView videoTextResults;
	VideoView videoView;
	
	private boolean videoSelected = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.videopage, container, false);
		
		/* 
		Button play_pause = (Button)rootView.findViewById(R.id.playpause);
		
		play_pause.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
            	//Toast.makeText(VideoPlay.this, "Button Clicked", Toast.LENGTH_SHORT).show();
            	AlertDialog.Builder builder = new AlertDialog.Builder(VideoPlay.this);
            	builder.setTitle("Hello");
            	builder.setPositiveButton("Ok",null);
            	builder.setMessage("Message");
            	AlertDialog alert = builder.create();
            	alert.show();
            	
            }
        }); */
 
        
        Button select_video_button = (Button)rootView.findViewById(R.id.selectVideoButton);
        
        // Select a video to review from list of directories
        select_video_button.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
            	 FileChooserDialog dialog = new FileChooserDialog(getActivity());
    			 dialog.loadFolder(previousDirectoryChosen);
    		     dialog.setFilter(".*mp4");	// select only mp4 files
    		     dialog.setShowOnlySelectable(true);	// only show these files in list
    			     dialog.addListener(new FileChooserDialog.OnFileSelectedListener() {
    			         public void onFileSelected(Dialog source, File file) {
    			             source.hide();
    			             Toast toast = Toast.makeText(source.getContext(), "File selected: " + file.getName(), Toast.LENGTH_LONG);
    			             toast.show();

    			            // videoTextResults.setText(file.getAbsolutePath());
    			           //videoView.setVideoPath(previousDirectoryChosen + "");
    					 	
    			             //previousDirectoryChosen = file.getParent() + File.separator;
    			             
    			             videoSelected = true;
    			             
    			             videoView = (VideoView)rootView.findViewById(R.id.videoView);
    			             //videoView.setBackgroundColor(Color.BLACK);
    			             final MediaController mc = new MediaController(getActivity());
    		    			 videoView.setMediaController(mc);
    		    			 mc.show();
    		    			 videoView.setVideoURI(Uri.parse(file.getAbsolutePath()));
    		    			 videoView.seekTo(10);
    		    			 
    		    			/* if (videoSelected){
    			            	 videoView.start();
    			            	 videoView.pause();
    			             }	*/
    		    			 
    		    			 videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
								
    		    				 // seek to beginning to replay the video
								@Override
								public void onCompletion(MediaPlayer mp) {
									//videoView.seekTo(videoView.getDuration());
									videoView.seekTo(1);
									videoView.start();
									videoView.pause();
									//videoView.setVisibility(View.GONE);
									//videoView = null;
								}
							});
    		    			 							
    		    			 /*
    		    			 videoView.setOnPreparedListener(new OnPreparedListener(){

								@Override
								public void onPrepared(MediaPlayer mp) {
									// TODO Auto-generated method stub
									//videoView.start();
									//videoView.pause();
								}
    		    				 
    		    				 
    		    			 });	 
    		  */
    		    			 
    		    			 //videoView.start();
    			         }
    			         // We can remove this... as long as it doesnt break it
    			         public void onFileSelected(Dialog source, File folder, String name) {
    			             source.hide();
    			             Toast toast = Toast.makeText(source.getContext(), "File created: " + folder.getName() + "/" + name, Toast.LENGTH_LONG);
    			             toast.show();
    			         }
    			     });
    			     
    			dialog.show();
    			//if (videoSelected == true){     
    				
    			//}
            }
        });
	
		return rootView;
	}

	public void openImage()
	{
//		imageView.setImageURI(Uri.fromFile(new File("drawable/computer_problems.png")));
	}
	
	/* Callback functions */

}
