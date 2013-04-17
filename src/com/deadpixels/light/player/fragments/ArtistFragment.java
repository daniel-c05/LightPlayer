package com.deadpixels.light.player.fragments;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Audio;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.deadpixels.light.player.ArtistBrowser;
import com.lazybitz.beta.light.player.R;
import com.deadpixels.light.player.SettingsActivity;
import com.deadpixels.light.player.adapters.AllArtistsAdapter;
import com.deadpixels.light.player.adapters.SampleCursorAdapter;
import com.deadpixels.light.player.service.MyMusicService;
import com.deadpixels.light.player.utils.MusicManager;

public class ArtistFragment extends Fragment {

	public ArtistFragment () {}

	private GridView mGridView;	
	private AllArtistsAdapter mAdapter;
	private boolean areDownloadsDisabled;

	private String [] from = {
			Audio.Artists.ARTIST,
	};

	private int [] to = {
			R.id.artist_name
	};
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.artist_context_menu, menu);		
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		if (info == null) {
			return false;
		}
		int pos = info.position;		
		switch (item.getItemId()) {
		case R.id.menu_play_all_from_artist:
			Intent intent = new Intent();
			intent.setAction(MyMusicService.ACTION_ENQUEUE);
			intent.putExtra(MyMusicService.EXTRA_QUEUE_SONGS, MusicManager.getAllSongsIdsFromArtist(getActivity(), mAdapter.getArtistName(pos)));
			getActivity().sendBroadcast(intent);
			return true;
		case R.id.menu_add_to_playlist:
			
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		areDownloadsDisabled = sharedPref.getBoolean(SettingsActivity.KEY_DISABLE_DOWNLOADS, false);		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {		

		View view = inflater.inflate(R.layout.artists_grid_container, container, false); 
		mAdapter = new AllArtistsAdapter(getActivity(),
				R.layout.by_song_list_item, 				
				MusicManager.getAllArtists(getActivity()), 
				from, 
				to,
				SampleCursorAdapter.NO_SELECTION, 
				areDownloadsDisabled);

		mGridView = (GridView) view.findViewById(R.id.artist_grid);
		mGridView.setAdapter(mAdapter);
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {
				Bundle info = mAdapter.getArtistDetails(position);
				String artist = info.getString(Audio.Artists.ARTIST);			
				Intent intent = new Intent(getActivity(), ArtistBrowser.class);
				intent.putExtra(ArtistBioFragment.ARTIST, artist);
				startActivity(intent);
			}        	
		});
		
		registerForContextMenu(mGridView);
		
		return view;
	}

}
