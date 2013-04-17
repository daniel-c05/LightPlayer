package com.deadpixels.light.player;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.deadpixels.light.player.adapters.HeaderAdapter;
import com.deadpixels.light.player.adapters.SampleCursorAdapter;
import com.deadpixels.light.player.fragments.ArtistBioFragment;
import com.deadpixels.light.player.fragments.PlayerFragment;
import com.deadpixels.light.player.service.MyMusicService;
import com.deadpixels.light.player.utils.Item;
import com.deadpixels.light.player.utils.ListHeader;
import com.deadpixels.light.player.utils.ListItem;
import com.deadpixels.light.player.utils.MusicManager;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.lazybitz.beta.light.player.R;

public class SearchResult extends Activity {

	public static final String PATH_MEDIA = "content://media/external/audio/media/";
	public static final String PATH_ARTIST = "content://media/external/audio/artists/";
	public static final String PATH_ALBUM = "content://media/external/audio/albums/";

	public static final String HEADER_ARTISTS = "Artists";
	public static final String HEADER_ALBUMS = "Albums";
	public static final String HEADER_SONGS = "Songs";

	/**
	 * Used for clearing the listview items
	 */
	public static final ArrayList<String> EMPTY_ARRAY = new ArrayList<String>();
	public ArrayAdapter<String> EMPTY_ADAPTER;

	private TextView albumTitle, albumArtist;
	private ListView resultList;
	private ImageView albumImage;

	/**
	 * Used for filling up the album songs list.
	 */
	private SampleCursorAdapter mAdapter;

	/**
	 * Used to fill the listview based on the query text.
	 */
	private HeaderAdapter adapter;
	
	/**
	 * The search widget located on the actionBar;
	 */
	private SearchView searchView;
	
	/**
	 * The menu item corresponding to the search function.
	 */
	private MenuItem mSearchMenu;

	private static final int [] to = {
		android.R.id.text1
	};

	private static final String [] from = {
		MediaStore.Audio.Media.TITLE
	};
	protected static final int SUGGEST_MIN_CHARS = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search_result);
		getActionBar().setDisplayHomeAsUpEnabled(true);		
		setupViews();
	}

	@Override
	protected void onResume() {	
		super.onResume();
		Intent intent = new Intent();
		intent.setAction(MyMusicService.ACTION_CANCEL_NOTIFICATION);
		sendBroadcast(intent);		
	}

	@Override
	protected void onPause() {	
		Intent intent = new Intent();
		intent.setAction(MyMusicService.ACTION_DISPLAY_PENDING_NOTIFICATION);
		sendBroadcast(intent);
		super.onPause();
	}

	private void setupViews() {

		//Start the adapter for empty items.

		EMPTY_ADAPTER = new ArrayAdapter<String>(SearchResult.this, android.R.layout.simple_list_item_1, android.R.id.text1);

		albumTitle = (TextView) findViewById(R.id.search_album_title);
		albumArtist = (TextView) findViewById(R.id.search_album_artist);
		resultList = (ListView) findViewById(R.id.search_result_list);
		albumImage = (ImageView) findViewById(R.id.search_album_image);
		
		resultList.setOnItemClickListener(mSearchResultsClickListener);

		albumArtist.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent browseArtistIntent = new Intent(SearchResult.this, ArtistBrowser.class);
				browseArtistIntent.putExtra(ArtistBioFragment.ARTIST, albumArtist.getText().toString());
				startActivity(browseArtistIntent);
			}
		});
	}

	private void searchAndShowResults(String query) {

		String selection = "artist like " + "'%" + query + "%'";
		ContentResolver mContentResolver = getContentResolver();
		Cursor cursor = mContentResolver.query(MusicManager.URI_ARTISTS, MusicManager.PROJECTION_ARTISTS, selection, null, null);		

		List<Item> items = new ArrayList<Item>();

		adapter = new HeaderAdapter(this, items);

		int count;

		if (cursor != null) {
			items.add(new ListHeader(HEADER_ARTISTS, R.attr.action_bar_artists));
			count = cursor.getCount();
			if (cursor.moveToFirst()) {
				for (int i = 0; i < count; i++) {
					items.add(new ListItem(HEADER_ARTISTS, cursor.getString(cursor.getColumnIndex("artist")), cursor.getString(cursor.getColumnIndex(Audio.Artists._ID))));
				}
			}
			else {
				items.add(new ListItem(HEADER_ARTISTS, "No Artists Found", ""));
			}				

			Log.v(HomeActivity.TAG, "Found " + count + " artists");
		} 

		selection = "album like " + "'%" + query + 
				"%' or artist like " + "'%" + query + "%'";
		cursor = mContentResolver.query(MusicManager.URI_ALBUMS, MusicManager.PROJECTION_ALBUMS, selection, null, null);

		if (cursor != null) {
			items.add(new ListHeader(HEADER_ALBUMS, R.attr.action_bar_albums));
			count = cursor.getCount();
			if (cursor.moveToFirst()) {
				for (int i = 0; i < count; i++) {
					items.add(new ListItem(HEADER_ALBUMS, cursor.getString(cursor.getColumnIndex("album")), cursor.getString(cursor.getColumnIndex(Audio.Albums._ID))));
					cursor.moveToNext();
				}
			}
			else {
				items.add(new ListItem(HEADER_ALBUMS, "No Albums Found", ""));
			}	
			Log.v(HomeActivity.TAG, "Found " + count + " albums");
		} 

		selection = "title like " + "'%" + query + 
				"%' or artist like " + "'%" + query + 
				"%' or album like " + "'%" + query + "%'";
		cursor = mContentResolver.query(MusicManager.URI_SONGS, 
				MusicManager.ALL_MEDIA_COLS, selection, null, null);	

		if (cursor != null) {
			items.add(new ListHeader(HEADER_SONGS, R.attr.action_bar_songs));
			count = cursor.getCount();
			if (cursor.moveToFirst()) {
				for (int i = 0; i < count; i++) {
					items.add(new ListItem(HEADER_SONGS, cursor.getString(cursor.getColumnIndex("title")), cursor.getString(cursor.getColumnIndex(Audio.Media._ID))));
					cursor.moveToNext();
				}				
			}
			else {
				items.add(new ListItem(HEADER_SONGS, "No Songs Found", ""));
			}	
			Log.v(HomeActivity.TAG, "Found " + count + " songs");
		}

		if (items != null) {
			resultList.setAdapter(adapter);	
		}		
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			onSearchRequested();
			return true;
		}		
		return super.onKeyDown(keyCode, event);
		
	}
	
	@Override
    public boolean onSearchRequested() {
        if (mSearchMenu != null) {
            mSearchMenu.expandActionView();
        }
        return false;
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_search_result, menu);

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		mSearchMenu = menu.findItem(R.id.menu_search);		
		searchView = (SearchView) mSearchMenu.getActionView();
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		mSearchMenu.expandActionView();
		searchView.requestFocusFromTouch();		
		searchView.setOnQueryTextListener(new OnQueryTextListener() {

			@Override
			public boolean onQueryTextSubmit(String query) {
				//Do nothing, results for the string supplied are already shown.
				//Just collapse search widget
				mSearchMenu.collapseActionView();
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				if (newText.length() > SUGGEST_MIN_CHARS) {
					searchAndShowResults(newText); 
				}	
				else if (newText.length() == 0) {		
					//When search is cleared, delete all results shown, 
					//and hide any album data that may be visible.
					resultList.setAdapter(EMPTY_ADAPTER);
					albumArtist.setVisibility(View.GONE);
					albumImage.setVisibility(View.GONE);
					albumTitle.setVisibility(View.GONE);
				}
				return true;
			}
		});
		

		return true;
	}
	
	/**
	 * Listens for clicks when the listview {@link #resultList} is displaying album items only, meaning when the user
	 * clicked on an album name on the search results. See {@link #mSearchResultsClickListener} for details.
	 */
	private OnItemClickListener mAlbumListClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
				long arg3) {
			Long cursorId = mAdapter.getSongId(pos, Audio.Media._ID);
			Intent playMediaIntent = new Intent(MyMusicService.ACTION_PLAY);
			playMediaIntent.putExtra("_id", cursorId);
			sendBroadcast(playMediaIntent);
			playMediaIntent = new Intent(SearchResult.this, PlayerHolder.class);
			startActivity(playMediaIntent);
		}
		
	};
	
	/**
	 * Listens for clicks when the listview {@link #resultList} is displaying search results. 
	 * If the user selects an artist, the {@link ArtistBrowser} will be shown for the supplied artist. 
	 * If the user selects an album, the {@link #resultList} is reset, and will now show all songs on the supplied album. 
	 * 		The {@link #mAlbumListClickListener} will now be set to the list to handle the new items. 
	 * If the user selects a song, the {@link PlayerFragment} will be shown for the supplied song. 
	 */
	private OnItemClickListener mSearchResultsClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
				long arg3) {
			Log.v(HomeActivity.TAG, "Handling list item Click");
			if (adapter.getItemViewType(pos) == HeaderAdapter.HEADER_ITEM) {
				//We do not handle clicks on headers.
				return;
			}
			else {

				String cursorId = adapter.getCursorId(pos);
				String headerType = adapter.getItemHeader(pos);
				
				if (cursorId != "") {
					if (headerType.equals(HEADER_ARTISTS)) {
						String artist = MusicManager.getArtistName(SearchResult.this, Long.valueOf(cursorId));
						Intent browseArtistIntent = new Intent(SearchResult.this, ArtistBrowser.class);
						browseArtistIntent.putExtra(ArtistBioFragment.ARTIST, artist);
						startActivity(browseArtistIntent);
					}
					else if (headerType.equals(HEADER_ALBUMS)) {
						String album = MusicManager.getAlbumName(SearchResult.this, Long.valueOf(cursorId));
						albumTitle.setVisibility(View.VISIBLE);
						albumImage.setVisibility(View.VISIBLE);
						albumArtist.setVisibility(View.VISIBLE);
						albumTitle.setText(album);
						albumArtist.setText(MusicManager.getAlbumArtist(SearchResult.this, Long.valueOf(cursorId)));
						mAdapter = new SampleCursorAdapter(SearchResult.this, android.R.layout.simple_list_item_1, MusicManager.getAllSongsFromAlbum(SearchResult.this, album), from, to, 0);
						resultList.setAdapter(mAdapter);
						//Switch to handle clicks for album items only. 
						resultList.setOnItemClickListener(mAlbumListClickListener);
						UrlImageViewHelper.setUrlDrawable(albumImage, getImageUrl(album), R.drawable.dummy_art);
					}
					else if (headerType.equals(HEADER_SONGS)) {
						Intent playMediaIntent = new Intent(MyMusicService.ACTION_PLAY);
						playMediaIntent.putExtra("_id", Long.valueOf(cursorId));
						sendBroadcast(playMediaIntent);
						playMediaIntent = new Intent(SearchResult.this, PlayerHolder.class);
						startActivity(playMediaIntent);
					}
				}
			}
		}		
	}; 
	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent upIntent = new Intent(this, HomeActivity.class);
			upIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			NavUtils.navigateUpTo(this, upIntent);
			return true;
		case R.id.menu_settings:	
			Intent settings = new Intent(this, SettingsActivity.class);
			startActivity(settings);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private String getImageUrl (String album) {

		Log.v(HomeActivity.TAG, "Loading Image url for: " + album);

		SharedPreferences mPreferences = getSharedPreferences("image-map", Context.MODE_PRIVATE);		
		String imageUrl = mPreferences.getString("album-" + album, ""); 

		if (imageUrl == null || imageUrl == "") {	//We don't yet have a preference for this image, never should happen, default to local
			imageUrl = MusicManager.getCachedAlbumArtPath(this, album);
		}

		Log.v(HomeActivity.TAG, "Url found: " + imageUrl);

		return imageUrl;

	}

}
