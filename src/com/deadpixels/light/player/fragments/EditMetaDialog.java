package com.deadpixels.light.player.fragments;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore.Audio;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.deadpixels.light.player.HomeActivity;
import com.lazybitz.beta.light.player.R;
import com.deadpixels.light.player.utils.MusicManager;

public class EditMetaDialog extends DialogFragment implements OnItemSelectedListener {

	public String [] properties;

	private String title, artist, album;
	private long [] itemIds;
	private Spinner songSpinner;
	private EditText propertyValue;
	private Button save, cancel;
	private String textObserver, lastPropertyAccessed;
	private ContentValues mContentValues;

	public EditMetaDialog () {
		//Empty Constructor required
	}

	public static EditMetaDialog newInstance(ArrayList<Long> songIds) {
		EditMetaDialog f = new EditMetaDialog();
		Bundle args = new Bundle();
		long [] ids = new long [songIds.size()];
		for (int i = 0; i < songIds.size(); i++) {
			ids[i] = songIds.get(i);
		}
		args.putLongArray("list", ids);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		properties = new String [3];
		properties = getResources().getStringArray(R.array.song_properties);
		itemIds = getArguments().getLongArray("list");
		final Cursor mCursor;
		final Context context = getActivity();
		final ContentResolver mContentResolver = context.getContentResolver();
		String selection = Audio.Media._ID + "=" + String.valueOf(itemIds[0]);
		mCursor = mContentResolver.query(MusicManager.URI_SONGS, MusicManager.PROJECTION_SONGS, selection, null, null);
		mCursor.moveToFirst();
		title = mCursor.getString(1);	
		artist = mCursor.getString(2);
		album = mCursor.getString(3);
		mCursor.close();
		mContentValues = new ContentValues();
	}

	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {		

		View root = inflater.inflate(R.layout.edit_meta_fragment, container, false);

		songSpinner = (Spinner) root.findViewById(R.id.edit_meta_property_spinner);
		propertyValue = (EditText) root.findViewById(R.id.edit_meta_input);
		save = (Button) root.findViewById(R.id.edit_meta_save);
		cancel = (Button) root.findViewById(R.id.edit_meta_cancel);
		
		cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		save.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!propertyValue.getText().toString().equals(textObserver)) {
					mContentValues.put(lastPropertyAccessed.toLowerCase(), propertyValue.getText().toString());
					MusicManager.editMetaData(getActivity(), mContentValues, itemIds);
				}
				dismiss();
			}
		});

		getDialog().setTitle(R.string.title_edit_meta_dialog);
		getDialog().getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

		ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, properties);
		mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		songSpinner.setAdapter(mAdapter);
		songSpinner.setOnItemSelectedListener(this);
		textObserver = title;
		propertyValue.setText(textObserver);

		return root;		
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int position,
			long arg3) {

		if (!propertyValue.getText().toString().equals(textObserver)) {
			mContentValues.put(lastPropertyAccessed, propertyValue.getText().toString());
			Log.v(HomeActivity.TAG, lastPropertyAccessed + " Changed to: " + propertyValue.getText().toString());
		}

		switch (position) {
		case 0:
			textObserver = title;	
			break;
		case 1:
			textObserver = artist;
			break;
		case 2:
			textObserver = album;
			break;
		default:
			textObserver = "";	
			break;
		}

		propertyValue.setText(textObserver);		
		lastPropertyAccessed = properties[position];
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {

	}

}
