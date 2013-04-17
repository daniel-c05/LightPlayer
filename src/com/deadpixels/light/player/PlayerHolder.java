package com.deadpixels.light.player;

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
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.deadpixels.light.player.adapters.DragNDropAdapter;
import com.deadpixels.light.player.adapters.PlaylistSpinnerAdapter;
import com.deadpixels.light.player.adapters.QueueDragDropAdapter;
import com.deadpixels.light.player.adapters.SampleCursorAdapter;
import com.deadpixels.light.player.fragments.AddToPlaylistDialog;
import com.deadpixels.light.player.fragments.EditPlaylistDialog;
import com.deadpixels.light.player.fragments.PlayerFragment;
import com.deadpixels.light.player.service.MyMusicService;
import com.deadpixels.light.player.utils.MusicManager;
import com.lazybitz.beta.light.player.R;
import com.mobeta.android.dslv.DragSortListView;
import com.slidingmenu.lib.SlidingMenu;

/**
 * 
 * @author Daniel Alvarado
 *	This is the placeholder for the @PlayerFragment
 */
public class PlayerHolder extends FragmentActivity {

	public static final String KEY_QUEUE_POSITION = "position";
	public static final String KEY_FRAGMENT = "fragment";

	private SlidingMenu menu;
	private DragSortListView playlist;
	private Spinner playlistsSpinner;
	private PlaylistSpinnerAdapter mSpinnerAdapter;
	private DragNDropAdapter mDragAdapter;
	protected long mCurPlaylistId;
	private long [] ids;	
	private TextView queueTitle;
	PlayerFragment mPlayerFragment;

	final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(MyMusicService.ACTION_CURRENT_QUEUE_CHANGED)) {
				ids = intent.getLongArrayExtra("queue");
				fillQueueList();
			}
			else if (action.equals(MyMusicService.ACTION_DATASET_CHANGED)) {
				boolean playlistCreated = intent.getBooleanExtra("playlist", false);
				updateCursors(playlistCreated);
			}
		}
	};	

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		switch (v.getId()) {
		case R.id.queue_title:
			if (ids.length != 0) {
				inflater.inflate(R.menu.queue_context_menu, menu);	
			}			
			break;
		case R.id.playlist_spinner:
			if (mCurPlaylistId != 0) {
				inflater.inflate(R.menu.playlist_context_menu, menu);
			}			
			break;
		default:
			break;
		}		
	}
	
	private DragSortListView queue;
	private QueueDragDropAdapter queueAdapter;
	
	
	private DragSortListView.DropListener onDrop =
	        new DragSortListView.DropListener() {
	            @Override
	            public void drop(int from, int to) {
	            	queueAdapter.moveItemTo(from, to);
	                MyMusicService.moveQueueItemTo(from, to);
	            }
	        };

	    private DragSortListView.RemoveListener onRemove = 
	        new DragSortListView.RemoveListener() {
	            @Override
	            public void remove(int which) {
	            	MyMusicService.removeQueueItem(which);
	            	queueAdapter.removeItem(which);
	            	queueAdapter.notifyDataSetChanged();
	            }
	        };

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.menu_play_all:
			intent = new Intent();
			intent.setAction(MyMusicService.ACTION_PLAYLIST);
			intent.putExtra(MyMusicService.EXTRA_PLAYLIST_ID, mCurPlaylistId);
			sendBroadcast(intent);
			return true;
		case R.id.menu_clear_queue:
			intent = new Intent();
			intent.setAction(MyMusicService.ACTION_CLEAR_QUEUE);
			sendBroadcast(intent);	
			ids = new long [0];
			intent = new Intent(this, HomeActivity.class);
			startActivity(intent);
			this.finish();
			return true;
		case R.id.menu_save_as_playlist:
			showAddToPlaylistDialog();
			return true;
		case R.id.menu_delete_playlist:
			MusicManager.deletePlaylist(this, mCurPlaylistId);
			updateCursors(true);
			return true;
		case R.id.menu_rename_playlist:
			showUpdatePlaylistNameDialog();
			return true;
		case R.id.menu_edit_playlist:
			mDragAdapter.toggleInEditMode();
			return true;			
		case R.id.menu_edit_queue:
			queueAdapter.toggleInEditMode();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	private void showUpdatePlaylistNameDialog() {
		EditPlaylistDialog dialog = new EditPlaylistDialog();
		Bundle args = new Bundle();
		args.putLong(EditPlaylistDialog.EXTRA_PLAYLIST_ID, mCurPlaylistId);
		args.putString(EditPlaylistDialog.EXTRA_PLAYLIST_NAME, mSpinnerAdapter.getPlaylistName(playlistsSpinner.getLastVisiblePosition()));
		dialog.setArguments(args);
		dialog.show(getSupportFragmentManager(), "playlist");
	}

	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event)  {

		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			Intent search = new Intent(this, SearchResult.class);
			startActivity(search);
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	private void showAddToPlaylistDialog() {
		AddToPlaylistDialog dialog = new AddToPlaylistDialog();
		Bundle args = new Bundle();
		args.putLongArray("songs", ids);
		dialog.setArguments(args);
		dialog.show(getSupportFragmentManager(), "playlist");
	}

	protected void fillQueueList () {
		ArrayList<String> queueTitles = new ArrayList<String>();
		if (ids != null) {
			queueTitles = MusicManager.batchSongDetailQuery(this, ids);
		}
		queueAdapter = new QueueDragDropAdapter(this, queueTitles, false);
		queue.setAdapter(queueAdapter);
	}

	protected void fillPlaylistItems(long id) {
		mDragAdapter = new DragNDropAdapter(this,
				R.layout.by_song_list_item, 
				MusicManager.getPlaylistItems(this, id), 
				HomeActivity.from, HomeActivity.to, SampleCursorAdapter.NO_SELECTION, id);	
		playlist.setAdapter(mDragAdapter);
	}

	protected void updateCursors(boolean playlist) {

		if (playlist) {	//Only update the spinner if there was a playlist created
			mSpinnerAdapter.swapCursor(MusicManager.getPlaylistNames(this));
			mSpinnerAdapter.notifyDataSetChanged();	
		}		
		if (mCurPlaylistId != 0) {
			mDragAdapter.setPlaylistId(mCurPlaylistId);
			mDragAdapter.swapCursor(MusicManager.getPlaylistItems(this, mCurPlaylistId));
			mDragAdapter.notifyDataSetChanged();
		}		
	}

	private void initSlidingMenu() {
		menu = new SlidingMenu(this);
		menu.setMode(SlidingMenu.LEFT_RIGHT);
		menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		menu.setShadowWidthRes(R.dimen.shadow_width);
		menu.setShadowDrawable(R.drawable.shadow);
		menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		menu.setFadeDegree(0.35f);
		menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
		menu.setMenu(R.layout.playlist_viewer);
		menu.setSecondaryMenu(R.layout.queue_viewer);
	}

	private void registerIntentFilters() {
		IntentFilter mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(MyMusicService.ACTION_DATASET_CHANGED);
		mIntentFilter.addAction(MyMusicService.ACTION_CURRENT_QUEUE_CHANGED);
		registerReceiver(mReceiver, mIntentFilter);	
	}

	protected void startPlayer(int position) {	
		long id = ids[position];
		Intent playIntent = new Intent();
		playIntent.setAction(MyMusicService.ACTION_PLAY);
		playIntent.putExtra("_id", id);
		sendBroadcast(playIntent);	
	}

	protected void startPlayerFromPlaylist (int position) {	
		long id = mDragAdapter.getItemFromColumn(position, Audio.Playlists.Members.AUDIO_ID);
		Intent playIntent = new Intent();
		playIntent.setAction(MyMusicService.ACTION_PLAY);
		playIntent.putExtra("_id", id);
		sendBroadcast(playIntent);
		Intent playerIntent = new Intent(this, PlayerHolder.class);
		startActivity(playerIntent);			
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_player_holder);
		initSlidingMenu();
		registerIntentFilters();

		findViewById(R.id.my_playack_controls).setVisibility(View.GONE);

		mPlayerFragment = new PlayerFragment();

		playlist = (DragSortListView) findViewById(android.R.id.list);
		playlist.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				startPlayerFromPlaylist(arg2);
			}			
		});
		playlist.setEmptyView(findViewById(R.id.empty_playlist));

		queueTitle = (TextView) findViewById(R.id.queue_title);		
		registerForContextMenu(queueTitle);
		queue = (DragSortListView) findViewById(R.id.queue_list_view);
		queue.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				startPlayer(arg2);
			}			
		});
		queue.setEmptyView(findViewById(R.id.empty));
		queue.setDropListener(onDrop);
		queue.setRemoveListener(onRemove);

		playlistsSpinner = (Spinner) findViewById(R.id.playlist_spinner);
		registerForContextMenu(playlistsSpinner);
		mSpinnerAdapter = new PlaylistSpinnerAdapter(this, 
				android.R.layout.simple_spinner_item, MusicManager.getPlaylistNames(this), 
				new String [] {MusicManager.PROJECTION_PLAYLIST_NAMES[1]}, 
				new int [] {android.R.id.text1}, SimpleCursorAdapter.NO_SELECTION);
		mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		playlistsSpinner.setAdapter(mSpinnerAdapter);
		playlistsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				mCurPlaylistId = mSpinnerAdapter.getPlaylistId(arg2);
				fillPlaylistItems(mCurPlaylistId);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {	
				mCurPlaylistId = 0;
			}			
		});

		MyMusicService.notifyQueueChanged();		
		FragmentTransaction mFragmentTransaction = getSupportFragmentManager().beginTransaction();
		mFragmentTransaction.replace(R.id.frame, mPlayerFragment).commit();			
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_empty_holder, menu);
		return true;
	}	

	@Override
	public void onDestroy() {
		unregisterReceiver(mReceiver);
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Intent intent = new Intent();
		intent.setAction(MyMusicService.ACTION_DISPLAY_PENDING_NOTIFICATION);
		sendBroadcast(intent);
		super.onPause();
	}

	@Override
	protected void onResume() {	
		super.onResume();
		startService(new Intent(this, MyMusicService.class));		
		Intent intent = new Intent();
		intent.setAction(MyMusicService.ACTION_CANCEL_NOTIFICATION);
		sendBroadcast(intent);		
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = null;
		switch (item.getItemId()) {
		case android.R.id.home:
			intent = new Intent(this, HomeActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			NavUtils.navigateUpTo(this, intent);
			return true;
		case R.id.menu_settings:	
			intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		case R.id.menu_favorite:
			long id = MyMusicService.getCurTrack();
			MusicManager.addToFavorites(PlayerHolder.this, id);
			intent = new Intent();
			intent.setAction(MyMusicService.ACTION_DATASET_CHANGED);
			intent.putExtra("playlist", true);
			sendBroadcast(intent);	
			return true;
		case R.id.menu_add_to_playlist:
			showAddToPlaylistFragment();
			return true;
		case R.id.menu_download_art:
			showDownloadArtDialog();
			return true;
		case R.id.menu_search:
			Intent search = new Intent(this, SearchResult.class);
			startActivity(search);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	private void showDownloadArtDialog() {
		final Bundle info = MyMusicService.getCurTrackBundle();
		CharSequence [] items = new CharSequence [] {
				"For Artist: " + info.getString(PlayerFragment.TRACK_ARTIST),
				"For Album: " + info.getString(PlayerFragment.TRACK_ALBUM),
				getResources().getString(R.string.text_back_to_local)
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);    		
		builder.setTitle("Select a method to get your Image:")
		.setItems(items, new OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent broadcast = new Intent();
				broadcast.setAction(PlayerFragment.ACTION_UPDATE_ART);
				broadcast.putExtra(PlayerFragment.TRACK_ID, info.getString(PlayerFragment.TRACK_ID));
				broadcast.putExtra(PlayerFragment.TRACK_VALUE, info.getString(PlayerFragment.TRACK_ARTIST));
				switch (which) {
				case 2:
					broadcast.putExtra(PlayerFragment.TRACK_WHAT, PlayerFragment.LOCAL);
					break;
				case 0:					
					broadcast.putExtra(PlayerFragment.TRACK_WHAT, PlayerFragment.ARTIST);
					break;
				case 1:
					broadcast.putExtra(PlayerFragment.TRACK_WHAT, PlayerFragment.ALBUM);
					broadcast.putExtra(PlayerFragment.TRACK_EXTRA, info.getString(PlayerFragment.TRACK_ALBUM));
					break;
				default:
					break;
				}
				sendBroadcast(broadcast);
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

	protected void showAddToPlaylistFragment() {
		AddToPlaylistDialog dialog = new AddToPlaylistDialog();
		long [] songId = new long [] {MyMusicService.getCurTrack()};
		Bundle args = new Bundle();
		args.putLongArray("songs", songId);
		dialog.setArguments(args);
		dialog.show(getSupportFragmentManager(), "playlist");
	}

}
