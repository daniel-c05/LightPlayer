package com.deadpixels.light.player.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.lazybitz.beta.light.player.R;
import com.deadpixels.light.player.adapters.SampleCursorAdapter;
import com.deadpixels.light.player.service.MyMusicService;
import com.deadpixels.light.player.utils.MusicManager;

public class EditPlaylistDialog extends DialogFragment {

	public static final String EXTRA_PLAYLIST_ID = "playlistId";
	public static final String EXTRA_PLAYLIST_NAME = "playlistName";

	private TextView playlistTitle;
	private EditText input;
	private Button cancel;
	private Button save;

	long playlistId;
	String playlistName;

	SampleCursorAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		playlistId = getArguments().getLong(EXTRA_PLAYLIST_ID);		
		playlistName = getArguments().getString(EXTRA_PLAYLIST_NAME);	
	}

	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {		
		View layout = inflater.inflate(R.layout.edit_playlist_dialog, null);		
		getDialog().setTitle(R.string.title_edit_playlist_dialog);	
		setupViews(layout);

		return layout;
	}

	private void setupViews(View layout) {
		playlistTitle = (TextView) layout.findViewById(R.id.edit_playlist_title);
		input = (EditText) layout.findViewById(R.id.edit_playlist_input);
		cancel = (Button) layout.findViewById(R.id.edit_playlist_cancel);
		save = (Button) layout.findViewById(R.id.edit_playlist_save);

		playlistTitle.setText(playlistName);

		cancel.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				getDialog().dismiss();
			}
		});

		save.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				MusicManager.renamePlaylist(getActivity(), "" + playlistId, input.getText().toString());
				Intent data = new Intent();
				data.setAction(MyMusicService.ACTION_DATASET_CHANGED);
				data.putExtra("playlist", true);
				getActivity().sendBroadcast(data);	
				getDialog().dismiss();
			}
		});
	}

}


