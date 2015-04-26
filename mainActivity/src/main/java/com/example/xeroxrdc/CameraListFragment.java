package com.example.xeroxrdc;

/**
 * Camera List Fragment
 * This page displays a list of Camera Definitions.
 * The user can select one of the cameras to connect to.
 * They can also edit, add, or remove definitions in the list.
 * @author Daragh
 */

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class CameraListFragment extends Fragment implements OnClickListener
{
	private static final String TAG = "XeroxRDC::CameraListFragment";

	private MainActivity parent;
	
	// The list of camera definitions
	private ArrayList<CameraDefinition> cameradefs;
	
	private View rootView;
	//private ListView deflist;
	//private Button connectButton;
	//private Button addButton;
	//private Button removeButton;
	private Button sonyButton;
	
	private ArrayAdapter<CameraDefinition> adapter;
	private int selectedPosition = -1;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		parent = (MainActivity)this.getActivity();
		this.cameradefs = new ArrayList<CameraDefinition>();
		
		// Get Views
		rootView = inflater.inflate(R.layout.camera_list, container, false);
		//deflist = (ListView)rootView.findViewById(R.id.cameralist);
		//connectButton = (Button)rootView.findViewById(R.id.camlist_connect);
		//addButton = (Button)rootView.findViewById(R.id.camlist_add);
		//removeButton = (Button)rootView.findViewById(R.id.camlist_remove);
		sonyButton = (Button)rootView.findViewById(R.id.camlist_sony);
		
		// Set onClickListeners
		//addButton.setOnClickListener(this);
		//connectButton.setOnClickListener(this);
		//removeButton.setOnClickListener(this);
		sonyButton.setOnClickListener(this);
		
		//connectButton.setEnabled(false);


		
		// Initialize adapter for displaying CameraDefinitions in ListView containing TextViews.
		adapter = new ArrayAdapter<CameraDefinition>(getActivity(), android.R.layout.simple_list_item_1, cameradefs)
		{
			@Override
			public View getView(int pos, View v, ViewGroup parent)
			{
				View itemView = super.getView(pos, v, parent);

				if (selectedPosition == pos)
					itemView.setBackgroundColor( getResources().getColor(R.color.pressed_color) ); 
				else
					itemView.setBackgroundColor(Color.TRANSPARENT);

				return itemView;
			}
		};
        
		/*deflist.setAdapter(adapter);
		deflist.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id)
			{
				deflist.smoothScrollToPosition(pos); // Scroll to selection
				selectedPosition = pos;
				adapter.notifyDataSetChanged();
				
				connectButton.setEnabled(pos >= 0);
					
			}
		});*/
		
		// Hardcode definitions for now
		addNewCameraDefinition("sigsiu.homeip.net", "Bird Cage");
		addNewCameraDefinition("192.168.43.171", "Raspberry Pi"); // port 8080
		addNewCameraDefinition("firefly.eme.cst.nihon-u.ac.jp", "Classroom"); // port 8090
		
		return rootView;
	}

	/**
	 * Creates a new CameraDefinition with ip and name.
	 * Adds the new definition to the arraylist and displays it in the ListView.
	 * @param ip Address of video stream
	 * @param name 
	 * @return The newly created CameraDefinition
	 */
	private CameraDefinition addNewCameraDefinition(String ip, String name)
	{
		CameraDefinition def = new CameraDefinition(ip, name);
		cameradefs.add(def);
		
		return def;
	}
	
	/***** Callback Functions *****/
	
	@Override
	public void onClick(View v)
	{
		/*if (v == addButton)
			addCamera();
		
		else if (v == connectButton)
			onConnect();
		
		else if (v == removeButton)
			removeCamera();*/
		
		if (v == sonyButton)
			goToSony();
		
		else
			Log.i(TAG, "Unexpected click from " + v.toString());
	}
	
	private void goToSony() {
		Intent intent = new Intent(getActivity(), CameraRemoteSampleApp.class);
		startActivity(intent);
	}
	
	/**
	 * Switch view to the Video page
	 * Start streaming video from the selected camera
	 */
	/*private void onConnect()
	{
		if (parent == null)
		{
			Log.e(TAG, "This fragment has no parent");
			return;
		}
		
		Fragment nextFrag = parent.setCurrentPage(MainActivity.VIDEOPAGE);

		if (selectedPosition < 0)
		{
			Log.e(TAG, "Selected position is less than zero");
			return;
		}
		
		if (nextFrag != null && nextFrag.getClass() == RDCVideoFragment.class)
		{
			// Get the address of the selected camera definition
			// and tell the video page to begin streaming from that address
			RDCVideoFragment vid = (RDCVideoFragment)nextFrag;
			vid.startVideo(cameradefs.get(selectedPosition));
		}
	}*/
	
	/** 
	 * Prompt user for Address and Name of new camera definition
	 * Create new definition and add it to the list
	 */
	public void addCamera()
	{
		// Build an alert dialog to prompt for address and name of new definition
		AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
		alert.setTitle("New Camera Definition");
		
		// Get view from xml layout
		View dialog = getActivity().getLayoutInflater().inflate(R.layout.new_camera_dialog, null);
		alert.setView(dialog);
		final EditText urlText = (EditText)dialog.findViewById(R.id.newcam_url);
		final EditText nameText = (EditText)dialog.findViewById(R.id.newcam_name);
		
		// Set callback for Add button
		alert.setPositiveButton("Add", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int whichButton) 
			{
				  String newUrl = (String) urlText.getText().toString();
				  String newName = (String) nameText.getText().toString();
				  addNewCameraDefinition(newUrl, newName);
			}
		});
		
		// Set callback for Cancel button
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int whichButton) 
			{
			}
		});

		// Display alert
		alert.show();
	}

	/**
	 * Removes a camera definition from the list
	 
	private void removeCamera()
	{
		cameradefs.remove(selectedPosition);
		selectedPosition = -1;
		adapter.notifyDataSetChanged();
		connectButton.setEnabled(false);
	}*/
}
