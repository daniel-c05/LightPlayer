package com.deadpixels.light.player.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.lazybitz.beta.light.player.R;
import com.deadpixels.light.player.adapters.PlaylistSpinnerAdapter;
import com.deadpixels.light.player.service.MyMusicService;
import com.deadpixels.light.player.utils.MusicManager;

public class AddToPlaylistDialog extends DialogFragment implements OnItemSelectedListener {
	
	private long [] songIds;
	private PlaylistSpinnerAdapter mSpinnerAdapter;
	private Spinner playlistSpinner;
	private Cursor mPlaylistCursor;
	private long curPlaylist;
	private Button buttonNewPlaylist, buttonSave, buttonCancel;
	private EditText plEditor;
	private boolean wasPlaylistCreated = false;
	
	public AddToPlaylistDialog () {
		//Empty Constructor required
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			songIds = (long[]) args.getLongArray("songs");
		}
	}
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {	
		getDialog().setTitle("Add to playlist");
		View view = inflater.inflate(R.layout.playlist_chooser_dialog, null);
		
		mPlaylistCursor = MusicManager.getPlaylistNames(getActivity());
		
		plEditor = (EditText) view.findViewById(R.id.playlist_chooser_new_pl_name);
		playlistSpinner = (Spinner) view.findViewById(R.id.playlist_chooser_spinner);
		mSpinnerAdapter = new PlaylistSpinnerAdapter(getActivity(), 
				android.R.layout.simple_spinner_item, mPlaylistCursor, 
				new String [] {MusicManager.PROJECTION_PLAYLIST_NAMES[1]}, 
				new int [] {android.R.id.text1}, SimpleCursorAdapter.NO_SELECTION);
		mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		playlistSpinner.setAdapter(mSpinnerAdapter);
		playlistSpinner.setOnItemSelectedListener(this);
		buttonNewPlaylist = (Button) view.findViewById(R.id.playlist_chooser_new);
		buttonNewPlaylist.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showNewPlaylistNameEditor();
			}
		});
		
		buttonSave = (Button) view.findViewById(R.id.playlist_chooser_save);
		buttonSave.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				handleSavePlaylist();		
			}
		});
		
		buttonCancel = (Button) view.findViewById(R.id.playlist_chooser_cancel);
		buttonCancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		
		return view;
	}
	
	protected void handleSavePlaylist() {
		if (plEditor.getVisibility() == 0) {	//0 is visible
			String name = plEditor.getText().toString();
			if (name == "") {
				Toast.makeText(getActivity(), "Please enter a valid name.", Toast.LENGTH_SHORT).show();
			}
			curPlaylist = MusicManager.createPlaylist(getActivity(), name);
			wasPlaylistCreated  = true;
		}
		
		if (curPlaylist == -1) {
			Toast.makeText(getActivity(), "Playlist was not created, please try again.", Toast.LENGTH_SHORT).show();
		}		
		MusicManager.addToPlaylist(getActivity(), curPlaylist, songIds);
		Intent data = new Intent();
		data.setAction(MyMusicService.ACTION_DATASET_CHANGED);
		data.putExtra("playlist", wasPlaylistCreated);
		getActivity().sendBroadcast(data);	
		getDialog().dismiss();
	}

	protected void showNewPlaylistNameEditor() {
		plEditor.setVisibility(0);
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int position,
			long arg3) {
		curPlaylist = mSpinnerAdapter.getPlaylistId(position);
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		
	}

}
