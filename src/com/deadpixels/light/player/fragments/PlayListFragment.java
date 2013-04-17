package com.deadpixels.light.player.fragments;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.MediaStore.Audio;
import android.support.v4.app.Fragment;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.deadpixels.light.player.HomeActivity;
import com.deadpixels.light.player.PlayerHolder;
import com.lazybitz.beta.light.player.R;
import com.deadpixels.light.player.adapters.SampleCursorAdapter;
import com.deadpixels.light.player.service.MyMusicService;
import com.deadpixels.light.player.utils.MusicManager;

public class PlayListFragment extends Fragment {
		
		boolean inActionMode = false;		
		private ArrayList<Integer> itemsChecked;		
		private ArrayList<Long> idsChecked;
		private ListView mListView;
		private SampleCursorAdapter mAdapter;
		private String artist;
		private boolean isAllMusicDisplayed;
	
		public PlayListFragment() {
		}
		
		public static PlayListFragment newInstance(String artist) {
			PlayListFragment f = new PlayListFragment();

	        Bundle args = new Bundle();
	        args.putString(ArtistBioFragment.ARTIST, artist);
	        f.setArguments(args);

	        return f;
	    }
		
		 @Override
		    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			 View root = inflater.inflate(R.layout.playlist_fragment, container, false);
			 
			 mListView = (ListView) root.findViewById(android.R.id.list);
			 mListView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
					startPlayer(position);
				}
			});
			 
			 mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
			 mListView.setMultiChoiceModeListener(modeListener);
			 
			 Bundle args = getArguments();
			 
			 if (args != null && args.containsKey(ArtistBioFragment.ARTIST)) {
				 isAllMusicDisplayed = false;
				 artist = args.getString(ArtistBioFragment.ARTIST);
				 mAdapter = new SampleCursorAdapter(getActivity(),
							R.layout.by_song_list_item, 
							MusicManager.getAllSongsFromArtist(getActivity(), artist), 
							HomeActivity.from, HomeActivity.to, 
							SampleCursorAdapter.NO_SELECTION);
			 }
			 else {
				 isAllMusicDisplayed = true;
				 mAdapter = new SampleCursorAdapter(getActivity(),
							R.layout.by_song_list_item, 
							MusicManager.getAllSongs(getActivity()), 
							HomeActivity.from, HomeActivity.to, 
							SampleCursorAdapter.NO_SELECTION);
			 }			 
			 			 
			 mListView.setAdapter(mAdapter);		
			 
			 return root;
		 }
		 
		 protected void startPlayer(int position) {						
			long id = mAdapter.getSongId(position, Audio.Media._ID);
			Intent playIntent = new Intent();
			playIntent.setAction(MyMusicService.ACTION_PLAY);
			playIntent.putExtra("_id", id);
			getActivity().sendBroadcast(playIntent);
			Intent playerIntent = new Intent(getActivity(), PlayerHolder.class);
			startActivity(playerIntent);			
		}

		@Override 
		 public void onCreate (Bundle savedInstanceState) {
			 super.onCreate(savedInstanceState);			 
			 IntentFilter mFilter = new IntentFilter();
			 mFilter.addAction(MyMusicService.ACTION_META_CHANGED);
			 getActivity().registerReceiver(mBroadcastReceiver, mFilter);
	
		 }
		 
		 @Override
		 public void onDestroy () {
			 getActivity().unregisterReceiver(mBroadcastReceiver);
			 super.onDestroy();
		 }
		
		private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(MyMusicService.ACTION_META_CHANGED)) {
					updateListCursor();
				}
			}
		};
		
		private void updateListCursor() {
			if (isAllMusicDisplayed) {
				mAdapter.swapCursor(MusicManager.getAllSongs(getActivity()));				
			}
			else {
				mAdapter.swapCursor(MusicManager.getAllSongsFromArtist(getActivity(), artist));
			}				
			mAdapter.notifyDataSetChanged();
		}
		 
		
		private MultiChoiceModeListener modeListener = new MultiChoiceModeListener() {
			
			 @Override
			    public void onItemCheckedStateChanged(ActionMode mode, int position,
			                                          long id, boolean checked) {	
			    	if (checked) {
			    		itemsChecked.add(position);
			    		idsChecked.add(mAdapter.getSongId(position, Audio.Media._ID));
					}
			    	else {
			    		int index = itemsChecked.indexOf(position);
			    		itemsChecked.remove(index);
			    		idsChecked.remove(mAdapter.getSongId(position, Audio.Media._ID));
			    	}
			    	
			    }

			    @Override
			    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			        switch (item.getItemId()) {
			            case 0:
			                mode.finish();
			                return true;
			            case R.id.menu_enqueue:
			            	long [] songIds = new long [idsChecked.size()];
			            	for (int i = 0; i < itemsChecked.size(); i++) {
			            		songIds[i] = idsChecked.get(i);
							}
			            	Intent intent = new Intent();
							intent.setAction(MyMusicService.ACTION_ENQUEUE);
							intent.putExtra(MyMusicService.EXTRA_QUEUE_SONGS, songIds);
							intent.putExtra(MyMusicService.EXTRA_QUEUE_PLAY, false);
							getActivity().sendBroadcast(intent);
			            	mode.finish();
			            	return true;
			            case R.id.menu_edit_meta:
			            	EditMetaDialog mEditMetaFragment = EditMetaDialog.newInstance(idsChecked);
			            	mEditMetaFragment.show(getFragmentManager(), null);
			            	mode.finish();
			            	return true;
			            case R.id.menu_remove_from_list:
			            	showDeleteSongsDialog();
			            	mode.finish();
			            	return false;
			            case R.id.menu_select_all:		
			            	for (int j = 0; j < mAdapter.getCount(); j++) {
								mListView.setItemChecked(j, true);
							}
			            	return true;
			            case R.id.menu_add_to_playlist:
			            	showAddToPlaylistFragment();
			            	mode.finish();
			            	return true;
			            default:
			                return false;
			        }
			    }

			    @Override
			    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			    	inActionMode = true;
			    	itemsChecked = new ArrayList<Integer>();
			    	idsChecked = new ArrayList<Long>();
			        MenuInflater inflater = mode.getMenuInflater();
			        inflater.inflate(R.menu.song_items_context_menu, menu);
			        return true;
			    }

			    @Override
			    public void onDestroyActionMode(ActionMode mode) {
			    	inActionMode = false;
			    }

			    @Override
			    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			        return false;
			    }
		};

		protected void showAddToPlaylistFragment() {
			AddToPlaylistDialog dialog = new AddToPlaylistDialog();
			long [] songIds = new long [idsChecked.size()];
			for (int i = 0; i < songIds.length; i++) {
				songIds[i] = idsChecked.get(i);
			}
			Bundle args = new Bundle();
			args.putLongArray("songs", songIds);
			dialog.setArguments(args);
			dialog.show(getFragmentManager(), "playlist");
		}

		protected void showDeleteSongsDialog() {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());    		
			builder.setMessage("This will delete the media from your SD card")
		       .setTitle("Confirm Action")
		       .setPositiveButton("Ok", new OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (MyMusicService.isPlaying()) {
						if (idsChecked.contains(MyMusicService.getCurTrack())) {
							Intent broadcast = new Intent();
							broadcast.setAction(MyMusicService.ACTION_NEXT);
							broadcast.putExtra(MyMusicService.EXTRA_BOOL_FORCE, false);
							getActivity().sendBroadcast(broadcast);
						}
					}
					MusicManager.deleteSong(getActivity(), idsChecked);
					updateListCursor();
				}
			})
		       .setCancelable(true).setNegativeButton("Cancel", new OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			AlertDialog dialog = builder.create();
			dialog.show();
		}
				
}
