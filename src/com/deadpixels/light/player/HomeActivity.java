package com.deadpixels.light.player;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.deadpixels.light.player.adapters.DragNDropAdapter;
import com.deadpixels.light.player.adapters.PlaylistSpinnerAdapter;
import com.deadpixels.light.player.adapters.QueueDragDropAdapter;
import com.deadpixels.light.player.adapters.SampleCursorAdapter;
import com.deadpixels.light.player.fragments.AddToPlaylistDialog;
import com.deadpixels.light.player.fragments.AlbumsFragment;
import com.deadpixels.light.player.fragments.ArtistFragment;
import com.deadpixels.light.player.fragments.EditPlaylistDialog;
import com.deadpixels.light.player.fragments.PlayListFragment;
import com.deadpixels.light.player.fragments.PlayerFragment;
import com.deadpixels.light.player.service.MyMusicService;
import com.deadpixels.light.player.utils.MusicManager;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.lazybitz.beta.light.player.R;
import com.mobeta.android.dslv.DragSortListView;
import com.slidingmenu.lib.SlidingMenu;

public class HomeActivity extends FragmentActivity implements ServiceConnection, ActionBar.TabListener, ViewPager.OnPageChangeListener  {

	public static final String TAG = "Light Player";

	/*
	 * Activity-Exclusive variable first. 
	 */

	/**
	 * In charge of informing the service of queue changes, as well as receiving information from it. 
	 */
	public static Messenger mOutGoingMessenger = null;

	public static MyMusicService mService;

	private int mCurPagerItem = 0;

	private SectionsPagerAdapter mSectionsPagerAdapter;
	private ViewPager mViewPager;

	static boolean isBound = false;
	boolean inEditPLMode = false;


	/**
	 * In charge of receiving intents and updating the Ui as needed. 
	 */
	final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(MyMusicService.ACTION_PLAYBACK_FINISHED)) {
				buttonPlay.setImageResource(R.drawable.ic_play_holo_dark);
			}
			else if (action.equals(MyMusicService.ACTION_SONG_CHANGED)) {
				updateMusicInfo(intent);
			}
			else if (action.equals(MyMusicService.ACTION_CURRENT_QUEUE_CHANGED)) {
				ids = intent.getLongArrayExtra("queue");
				fillQueueList();
			}
			else if (action.equals(MyMusicService.ACTION_DATASET_CHANGED)) {
				boolean playlistCreated = intent.getBooleanExtra("playlist", false);
				updateCursors(playlistCreated);
			}
		}
	};

	/*
	 * Activity Lifecycles.
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		initSlidingMenu();
		setupControls();
		registerIntentFilters();

		if (mService == null) {	//This always happens. 
			startService(new Intent(this, MyMusicService.class));
		}

		if (!isBound) {
			bindService(new Intent(this, MyMusicService.class), this, Context.BIND_AUTO_CREATE);
			isBound = true;
		}

		mViewPager = (ViewPager) findViewById(R.id.pager);

		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		mViewPager.setAdapter(mSectionsPagerAdapter);			
		mViewPager.setOnPageChangeListener(this);

		setupActionBar();

		MyMusicService.notifyQueueChanged();	

		if (!MyMusicService.isPlaying()) {
			mControlsLayout.setVisibility(View.GONE);
		}
	}	

	@Override
	protected void onResume() {	
		super.onResume();
		Intent intent = new Intent();
		intent.setAction(MyMusicService.ACTION_CANCEL_NOTIFICATION);
		sendBroadcast(intent);		
		if (!MyMusicService.isPlaying() && mControlsLayout != null) {
			buttonPlay.setImageResource(R.drawable.ic_play_holo_dark);
		}
	}

	@Override
	protected void onPause() {	
		Intent intent = new Intent();
		intent.setAction(MyMusicService.ACTION_DISPLAY_PENDING_NOTIFICATION);
		sendBroadcast(intent);
		super.onPause();
	}

	@Override
	protected void onDestroy() {	
		if (isBound) {
			unbindService(this);
		}		
		unregisterReceiver(mReceiver);
		isBound = false;
		super.onDestroy();
	}

	/*
	 * Activity-Overrides.
	 */

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent settings = new Intent(this, SettingsActivity.class);
			startActivity(settings);
			return true;
		case R.id.menu_search:
			Intent search = new Intent(this, SearchResult.class);
			startActivity(search);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mOutGoingMessenger = new Messenger(service);
		isBound = true;
		if (MyMusicService.isPlaying()) {
			MyMusicService.getCurTrackDetails();
		}		
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mOutGoingMessenger = null;
		isBound = false;
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {}

	@Override
	public void onPageSelected(int arg0) {
		getActionBar().setSelectedNavigationItem(arg0);
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		switch (tab.getPosition()) {
		case 0:
			if (tab.getPosition() == mCurPagerItem) {
				final ListView list = (ListView) findViewById(android.R.id.list);
				list.setSelection(0);
			}
			break;
		case 1: 
			if (tab.getPosition() == mCurPagerItem) {
				final GridView list = (GridView) findViewById(R.id.artist_grid);
				list.setSelection(0);				
			}
			break;
		case 2:
			if (tab.getPosition() == mCurPagerItem) {
				final ExpandableListView list = (ExpandableListView) findViewById(R.id.exp_list);
				list.setSelection(0);
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		final int pos = tab.getPosition();
		mViewPager.setCurrentItem(pos);
		mCurPagerItem = pos;
		if (menu.isMenuShowing()) {
			menu.toggle();
		}		
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}

	/*
	 * Activity-Methods
	 */

	private void setupActionBar() {
		final ActionBar mActionBar = getActionBar();

		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		//mActionBar.setHomeButtonEnabled(false);
		//mActionBar.setDisplayShowHomeEnabled(false);
		//mActionBar.setDisplayShowTitleEnabled(false);
		

		mActionBar.addTab(mActionBar.newTab().setIcon(R.attr.action_bar_songs).setTabListener(this));
		mActionBar.addTab(mActionBar.newTab().setIcon(R.attr.action_bar_artists).setTabListener(this));
		mActionBar.addTab(mActionBar.newTab().setIcon(R.attr.action_bar_albums).setTabListener(this));
	}    

	/**
	 * Used to send messages to the service. 
	 * @param what See {@link MyMusicService} for details on what messages can be received.
	 */
	public static void sendMessage(int what) {
		if (!isBound) return;
		Message message = Message.obtain(null, what, 0, 0);
		try {
			mOutGoingMessenger.send(message);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/*
	 * SlidingMenu-related variables. These are very much the same items we use on all Activities in the project. 
	 * They are necessary for keeping the actions of the sliding menu ongoing. 
	 */

	private SlidingMenu menu;

	private PlaylistSpinnerAdapter mSpinnerAdapter;
	private DragNDropAdapter mDragAdapter;
	
	private DragSortListView playlist;
	private Spinner playlistsSpinner;
	private RelativeLayout mControlsLayout;
	private TextView queueTitle, textTitle, textArtist;
	private ImageButton buttonPlay, buttonPrevious, buttonNext;
	private ImageView songArt;

	private long [] ids;
	protected long mCurPlaylistId;

	public static final int [] to = {
		R.id.by_song_title,
		R.id.by_song_artist
	};

	public static final String [] from = {
		MediaStore.Audio.Media.TITLE,
		MediaStore.Audio.Media.ARTIST
	};
	
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

	/*
	 * ContextMenu applied to sliding menu items.
	 */

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		switch (v.getId()) {
		case R.id.queue_title:
			if (ids == null || ids.length != 0) {
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

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Intent broadcast = new Intent();
		switch (item.getItemId()) {
		case R.id.menu_play_all:			
			broadcast.setAction(MyMusicService.ACTION_PLAYLIST);
			broadcast.putExtra(MyMusicService.EXTRA_PLAYLIST_ID, mCurPlaylistId);
			sendBroadcast(broadcast);
			return true;
		case R.id.menu_clear_queue:
			broadcast.setAction(MyMusicService.ACTION_CLEAR_QUEUE);
			sendBroadcast(broadcast);
			ids = new long [0];
			mControlsLayout.setVisibility(View.GONE);
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


	/*
	 * SldingMenu Helpers
	 */

	private void initSlidingMenu() {
		menu = new SlidingMenu(this);
		menu.setMode(SlidingMenu.LEFT_RIGHT);
		menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		menu.setShadowWidthRes(R.dimen.shadow_width);
		menu.setShadowDrawable(R.drawable.shadow);
		menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		menu.setFadeDegree(0.35f);
		menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
		menu.setMenu(R.layout.playlist_viewer);
		menu.setSecondaryMenu(R.layout.queue_viewer);
	}

	private void setupControls() {

		playlist = (DragSortListView) findViewById(android.R.id.list);
		queue = (DragSortListView) findViewById(R.id.queue_list_view);
		playlistsSpinner = (Spinner) findViewById(R.id.playlist_spinner);
		mControlsLayout = (RelativeLayout)findViewById(R.id.my_playack_controls);
		buttonPlay = (ImageButton) findViewById(R.id.button_play);
		buttonPrevious  = (ImageButton) findViewById(R.id.button_previous);
		buttonNext  = (ImageButton) findViewById(R.id.button_next);
		queueTitle = (TextView) findViewById(R.id.queue_title);
		textTitle = (TextView) findViewById(R.id.controls_song);
		textArtist = (TextView) findViewById(R.id.controls_artist);
		songArt = (ImageView) findViewById(R.id.controls_art);


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

		registerForContextMenu(playlistsSpinner);
		registerForContextMenu(queueTitle);

		mControlsLayout.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				startPlayerWithNoAction();
			}
		});

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

		playlist.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				startPlayerFromPlaylist(arg2);
			}			
		});
		playlist.setEmptyView(findViewById(R.id.empty_playlist));

		buttonPlay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendBroadcastToService(MyMusicService.ACTION_PLAY_PAUSE, MyMusicService.EXTRA_BOOL_FORCE, false);
				if(MyMusicService.isPlaying() || MyMusicService.getCurTrack() == 0) {
					buttonPlay.setImageResource(R.drawable.ic_play_holo_dark);
				}
				else {
					buttonPlay.setImageResource(R.drawable.ic_pause_holo_dark);
				}
			}
		});

		buttonPrevious.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendBroadcastToService(MyMusicService.ACTION_PREVIOUS);
			}
		});

		buttonNext.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendBroadcastToService(MyMusicService.ACTION_NEXT, MyMusicService.EXTRA_BOOL_FORCE, false);
			}
		});
	}

	protected void updateMusicInfo(Intent intent) {

		if (!mControlsLayout.isShown()) {
			mControlsLayout.setVisibility(View.VISIBLE);
		}

		textTitle.setText(intent.getStringExtra(PlayerFragment.TRACK_TITLE));
		textArtist.setText(intent.getStringExtra(PlayerFragment.TRACK_ARTIST));
		setAlbumArt(songArt, intent.getStringExtra(PlayerFragment.TRACK_ART));		
		buttonPlay.setImageResource(R.drawable.ic_pause_holo_dark);
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

	private void setAlbumArt(ImageView view, String id) {
		String path = getImageUrl(id);
		if (path == null || path == "") {
			path = "content://media/external/audio/media/" + id + "/albumart";
		}		
		UrlImageViewHelper.setUrlDrawable(view, path, R.drawable.dummy_art);		
	}	

	private String getImageUrl (String id) {

		Log.v(HomeActivity.TAG, "Loading Image url for: " + id);

		SharedPreferences mPreferences = getSharedPreferences("image-map", Context.MODE_PRIVATE);		
		String imageUrl = mPreferences.getString(id, ""); 

		if (imageUrl == null || imageUrl == "") {	//We don't yet have a preference for this image, never should happen, default to local
			imageUrl = "local";
		}

		if (imageUrl.equals("local")) {
			imageUrl = "content://media/external/audio/media/" + id + "/albumart";			
		}

		Log.v(HomeActivity.TAG, "Url found: " + imageUrl);

		return imageUrl;
	}

	private void showUpdatePlaylistNameDialog() {
		EditPlaylistDialog dialog = new EditPlaylistDialog();
		Bundle args = new Bundle();
		args.putLong(EditPlaylistDialog.EXTRA_PLAYLIST_ID, mCurPlaylistId);
		args.putString(EditPlaylistDialog.EXTRA_PLAYLIST_NAME, mSpinnerAdapter.getPlaylistName(playlistsSpinner.getLastVisiblePosition()));
		dialog.setArguments(args);
		dialog.show(getSupportFragmentManager(), "playlist");
	}

	private void showAddToPlaylistDialog() {
		AddToPlaylistDialog dialog = new AddToPlaylistDialog();
		Bundle args = new Bundle();
		args.putLongArray("songs", ids);
		dialog.setArguments(args);
		dialog.show(getSupportFragmentManager(), "playlist");
	}

	public void sendBroadcastToService (String action) {
		Intent intent = new Intent();
		intent.setAction(action);
		sendBroadcast(intent);
	}

	public void sendBroadcastToService (String action, String key, boolean value) {
		Intent intent = new Intent();
		intent.setAction(action);
		intent.putExtra(key, value);
		sendBroadcast(intent);
	}

	protected void startPlayerWithNoAction () {
		Intent playerIntent = new Intent(this, PlayerHolder.class);
		startActivity(playerIntent);			
	}

	protected void startPlayer(int position) {	
		long id = ids[position];
		Intent playIntent = new Intent();
		playIntent.setAction(MyMusicService.ACTION_PLAY);
		playIntent.putExtra("_id", id);
		sendBroadcast(playIntent);
		Intent playerIntent = new Intent(this, PlayerHolder.class);
		startActivity(playerIntent);			
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

	private void registerIntentFilters() {
		IntentFilter mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(MyMusicService.ACTION_PLAYBACK_FINISHED);
		mIntentFilter.addAction(MyMusicService.ACTION_SONG_CHANGED);
		mIntentFilter.addAction(MyMusicService.ACTION_DATASET_CHANGED);
		mIntentFilter.addAction(MyMusicService.ACTION_CURRENT_QUEUE_CHANGED);
		registerReceiver(mReceiver, mIntentFilter);	
	}

	protected void fillPlaylistItems(long id) {
		mDragAdapter = new DragNDropAdapter(this,
				R.layout.by_song_list_item, 
				MusicManager.getPlaylistItems(this, id), 
				from, to, SampleCursorAdapter.NO_SELECTION, id);	
		playlist.setAdapter(mDragAdapter);
	}

	protected void fillQueueList () {
		ArrayList<String> queueTitles = new ArrayList<String>();
		if (ids != null) {
			queueTitles = MusicManager.batchSongDetailQuery(this, ids);
		}
		queueAdapter = new QueueDragDropAdapter(this, queueTitles, false);
		queue.setAdapter(queueAdapter);
	}

	/**
	 * The adapter for the actionbar. 
	 *
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				Fragment allSongs = new PlayListFragment();
				return allSongs;
			case 1:
				Fragment allArtists = new ArtistFragment();
				return allArtists;
			case 2:
				Fragment mAlbumsFragment = new AlbumsFragment();
				return mAlbumsFragment;
			default:
				break;
			}
			return null;
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return getString(R.string.title_list_all_songs);
			case 1:
				return getString(R.string.title_list_artists);
			case 2:
				return getString(R.string.title_list_albums);
			}
			return null;
		}
	}
}
