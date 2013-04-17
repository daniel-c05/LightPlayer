package com.deadpixels.light.player.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore.Audio;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;

import com.deadpixels.light.player.PlayerHolder;
import com.lazybitz.beta.light.player.R;
import com.deadpixels.light.player.adapters.AlbumsAdapter;
import com.deadpixels.light.player.service.MyMusicService;
import com.deadpixels.light.player.utils.MusicManager;

public class AlbumsFragment extends Fragment {

	private ExpandableListView mListView;
	private AlbumsAdapter mAdapter;	

	public AlbumsFragment () {		
		//Empty constructor is needed. 
	}

	public static AlbumsFragment newInstance(String artist) {
		AlbumsFragment f = new AlbumsFragment();

		Bundle args = new Bundle();
		args.putString(ArtistBioFragment.ARTIST, artist);
		f.setArguments(args);

		return f;
	}

	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.albums_fragment, container, false);
		mListView = (ExpandableListView) root.findViewById(R.id.exp_list);
		mListView.setOnGroupClickListener(new OnGroupClickListener() {

			@Override
			public boolean onGroupClick(ExpandableListView parent, View v,
					int groupPosition, long id) {
				return false;
			}
		});	

		mListView.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				startPlayer(groupPosition, childPosition);
				return false;
			}
		});			

		Bundle args = getArguments();

		if (args != null && args.containsKey(Audio.Artists.ARTIST)) {
			String artist = args.getString(Audio.Artists.ARTIST);
			mAdapter = new AlbumsAdapter(MusicManager.getAllAlbumsFromArtist(getActivity(), artist), getActivity());
		}
		else {			
			mAdapter = new AlbumsAdapter(MusicManager.getAllAlbums(getActivity()), getActivity());
		}

		mListView.setAdapter(mAdapter);
		return root;		
	}	

	protected void startPlayer(int groupPosition, int childPosition) {						
		final Cursor cursor = mAdapter.getChild(groupPosition, childPosition);
		long id = cursor.getLong(0);
		cursor.close();
		Intent playIntent = new Intent();
		playIntent.setAction(MyMusicService.ACTION_PLAY);
		playIntent.putExtra("_id", id);
		getActivity().sendBroadcast(playIntent);
		Intent playerIntent = new Intent(getActivity(), PlayerHolder.class);
		startActivity(playerIntent);			
	}
}
