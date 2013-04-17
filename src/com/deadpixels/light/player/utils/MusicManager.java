package com.deadpixels.light.player.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.Playlists;
import android.util.Log;
import android.widget.Toast;

import com.deadpixels.light.player.HomeActivity;
import com.deadpixels.light.player.service.MyMusicService;

@SuppressLint("DefaultLocale")
public class MusicManager {

	public static final Uri URI_SONGS = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
	public static final Uri URI_SEARCH = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
	public static final Uri URI_ARTISTS = android.provider.MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
	public static final Uri URI_ALBUMS = android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
	public static final Uri URI_PLAYLIST_NAMES = android.provider.MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;

	public static final int META_TITLE = 0;
	public static final int META_ARTIST = 1;
	public static final int META_ALBUM = 2;

	/**
	 * "All" is deceiving, as I only included the ones I cared for. 
	 */
	public static final String [] ALL_MEDIA_COLS = {
		MediaStore.Audio.Media._ID,		
		MediaStore.Audio.Media.TITLE,
		MediaStore.Audio.Media.ALBUM,
		MediaStore.Audio.Media.ARTIST,
		MediaStore.Audio.Media.DATA,
	};

	public static final String [] PROJECTION_SONGS = {
		MediaStore.Audio.Media._ID,		
		MediaStore.Audio.Media.TITLE,
		MediaStore.Audio.Media.ARTIST,
		MediaStore.Audio.Media.ALBUM,
	};

	public static final String [] PROJECTION_ARTISTS = {
		MediaStore.Audio.Artists._ID,		
		MediaStore.Audio.Artists.ARTIST,
		MediaStore.Audio.Artists.ARTIST_KEY
	};

	public static final String [] PROJECTION_ARTIST_ALBUMS = {
		MediaStore.Audio.Artists.Albums.ALBUM_ART		
	};

	public static final String [] PROJECTION_ALBUMS = {
		MediaStore.Audio.Albums._ID,		
		MediaStore.Audio.Albums.ALBUM,
		MediaStore.Audio.Albums.ARTIST,	
		MediaStore.Audio.Albums.ALBUM_ART
	};

	public static final String [] PROJECTION_PLAYLIST_NAMES = {
		MediaStore.Audio.Playlists._ID,		
		MediaStore.Audio.Playlists.NAME,
	};

	public static final String [] PROJECTION_PLAYLIST_ITEMS = {
		MediaStore.Audio.Playlists.Members._ID,
		MediaStore.Audio.Playlists.Members.TITLE,
		MediaStore.Audio.Playlists.Members.ARTIST,
		MediaStore.Audio.Playlists.Members.AUDIO_ID
	};

	public static final String SELECTION_GENERAL = MediaStore.Audio.Media.IS_MUSIC + "!=0";

	private static Cursor getCursor (final Context context, final Uri uri, final String [] projection, final String selection, final String [] selectionArgs, final String sortOrder) {
		ContentResolver mContentResolver = context.getContentResolver();
		Cursor cursor = mContentResolver.query(uri, projection, selection, selectionArgs, sortOrder);
		return cursor;
	}

	public static Cursor getAllSongs (final Context context) {
		return getCursor(context, URI_SONGS, PROJECTION_SONGS, SELECTION_GENERAL, null, MediaStore.Audio.Media.TITLE);
	}

	public static Cursor getAllSongsFromArtist (final Context context, final String artist) {
		String selection = MediaStore.Audio.Media.ARTIST + "= '" + artist + "'";
		return getCursor(context, URI_SONGS, PROJECTION_SONGS, selection, null, MediaStore.Audio.Media.TITLE);
	}

	public static long [] getAllSongsIdsFromArtist (final Context context, final String artist) {
		Cursor mCursor = getAllSongsFromArtist(context, artist);
		long [] songIds = null;
		if (!mCursor.moveToFirst()) {
			return songIds;
		}
		songIds = new long [mCursor.getCount()];
		for (int i = 0; i < mCursor.getCount(); i++) {
			songIds[i] = (mCursor.getLong(0));
			mCursor.moveToNext();
		}
		mCursor.close();
		return songIds;
	}

	public static long [] getAllSongsIdsFromAlbum (final Context context, final String album) {
		Cursor mCursor = getAllSongsFromAlbum(context, album);
		long [] songIds = null;
		if (!mCursor.moveToFirst()) {
			return songIds;
		}
		songIds = new long [mCursor.getCount()];
		for (int i = 0; i < mCursor.getCount(); i++) {
			songIds[i] = (mCursor.getLong(0));
			mCursor.moveToNext();
		}
		mCursor.close();
		return songIds;
	}

	public static Cursor getAllSongsFromAlbum (final Context context, final String album) {
		String selection = MediaStore.Audio.Media.ALBUM + "= '" + album + "'";
		return getCursor(context, URI_SONGS, PROJECTION_SONGS, selection, null, MediaStore.Audio.Media.TITLE);
	}

	public static String getAlbumName (final Context context, final long albumId) {		
		ContentResolver mContentResolver = context.getContentResolver();
		String where = MediaStore.Audio.Albums._ID + "= '" + albumId + "'"; 
		Cursor mCursor = mContentResolver.query(URI_ALBUMS, PROJECTION_ALBUMS, where, null, null);
		String name = "";
		if (mCursor.moveToFirst()) {
			name = mCursor.getString(1);
		}		
		mCursor.close();	
		return name;
	}

	public static Cursor getPlaylistNames (final Context context) {
		return getCursor(context, URI_PLAYLIST_NAMES, PROJECTION_PLAYLIST_NAMES, null, null, MediaStore.Audio.Playlists.NAME);
	}

	public static Cursor getAllArtists (final Context context) {
		return getCursor(context, URI_ARTISTS, PROJECTION_ARTISTS, null, null, MediaStore.Audio.Artists.ARTIST);
	}

	public static Cursor getAllAlbums (final Context context) {		
		return getCursor(context, URI_ALBUMS, PROJECTION_ALBUMS, null, null, MediaStore.Audio.Albums.ALBUM);
	}

	public static Cursor getAllAlbumsFromArtist (final Context context, final String artist) {
		String selection = MediaStore.Audio.Albums.ARTIST + "='" + artist + "'";
		return getCursor(context, URI_ALBUMS, PROJECTION_ALBUMS, selection, null, MediaStore.Audio.Albums.ALBUM);
	}

	/**
	 * Add a specific song to an existing playlist. 
	 * @param context The context
	 * @param playlistId The playlist id you want to add the song to. 
	 * @param songId The song id you want to add to the playlist. 
	 */
	public static void addToPlaylist (final Context context, final long playlistId, final long [] songIds) {

		if (songIds == null || playlistId == 0) {
			return;
		}

		ContentResolver mContentResolver = context.getContentResolver();
		Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId); 
		Cursor cursor = getCursor(context, uri, PROJECTION_PLAYLIST_ITEMS, null	, null, null);
		cursor.moveToFirst();
		int len = cursor.getCount();	//The amount of songs in the playlist, we do not want to mess with the ordering already there.
		String where = null;
		ContentValues values = null;
		for (int i = 0; i < songIds.length; i++) {		
			where = MediaStore.Audio.Playlists.Members.AUDIO_ID + "='" + songIds[i] + "'"; 	//Does the playlist already contain this song id?		
			cursor = getCursor(context, uri, PROJECTION_PLAYLIST_ITEMS, where, null, null);			
			if (!cursor.moveToFirst()) {	//If the cursor is empty, that means we have a green light, let's add the song. 
				values = new ContentValues();
				values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songIds[i]);
				values.put(Playlists.Members.PLAY_ORDER, len + i + 1);
				mContentResolver.insert(uri, values);
			}						
		}
		cursor.close();
		Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show();		
	}

	public static boolean movePlaylistItemTo(final Context context, final long playlistId, int from, int to) {
		if (playlistId == 0) {
			return false;
		}

		ContentResolver mContentResolver = context.getContentResolver();
		return Audio.Playlists.Members.moveItem(mContentResolver, playlistId, from, to);	
}

/**
 * 
 * @param context
 * @param playlistId
 * @param songId
 * @param to
 */
public static void addToPlaylist(final Context context, final long playlistId, final long [] songIds, int to) {

	if (songIds == null || playlistId == 0) {
		return;
	}

	ContentResolver mContentResolver = context.getContentResolver();
	Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId); 
	Cursor cursor = getCursor(context, uri, PROJECTION_PLAYLIST_ITEMS, null	, null, null);
	if (cursor.moveToFirst()) {
		int len = cursor.getCount();	//The amount of songs in the playlist.
		Log.v(HomeActivity.TAG, "Playlist lenght: " + cursor.getCount());
		if (len > to) {
			Log.v(HomeActivity.TAG, "Now adding songs to playlist position: " + to);
			cursor.moveToPosition(to);
			String where = null;
			ContentValues values = null;
			for (int i = 0; i < songIds.length; i++) {		
				where = MediaStore.Audio.Playlists.Members.AUDIO_ID + "='" + songIds[i] + "'"; 	//Does the playlist already contain this song id?		
				cursor = getCursor(context, uri, PROJECTION_PLAYLIST_ITEMS, where, null, null);			
				if (!cursor.moveToFirst()) {	//If the cursor is empty, that means we have a green light, let's add the song. 
					Log.v(HomeActivity.TAG, "Cursor lenght: " + cursor.getCount());
					values = new ContentValues();
					values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songIds[i]);
					values.put(Playlists.Members.PLAY_ORDER, to + i);
					mContentResolver.insert(uri, values);
					Log.v(HomeActivity.TAG, "Added id: " + songIds[i] + " to position: " + (to + i));
					Log.v(HomeActivity.TAG, "Playlist lenght: " + cursor.getCount());
				}						
			}
		}
		else {
			//If the position is greater than the amount of songs in the cur playlist
			// Add them normally, at the end of the playlist.
			Log.v(HomeActivity.TAG, "Defaulting, adding items to end of playlist ");
			addToPlaylist(context, playlistId, songIds);
		}
	}
	cursor.close();
	Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show();		
}

/**
 * Remove a specific song from an existing playlist. 
 * @param context The context
 * @param playlistId The playlist id you want to remove the song from. 
 * @param songId The song id you want to add to the playlist. 
 */
public static void removeFromPlaylist (final Context context, final long playlistId, final long songId) {

	if (songId == 0 || playlistId == 0) {
		Log.v(HomeActivity.TAG, "Returning from invalid request to remove id: " + playlistId + " from playlist");
		return;
	}

	ContentResolver mContentResolver = context.getContentResolver();
	Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
	String where = MediaStore.Audio.Playlists.Members.AUDIO_ID + "='" + songId + "'";
	Cursor cursor = getCursor(context, uri, PROJECTION_PLAYLIST_ITEMS, where, null, null);		
	if (cursor.moveToFirst()) {	//Song exists in playlist	
		int rows = mContentResolver.delete(uri, where, null);
		Log.v(HomeActivity.TAG, "Items removed from playlist: " + rows);
	}	
	cursor.close();		
	Toast.makeText(context, "Removed from playlist", Toast.LENGTH_SHORT).show();		
}

/**
 * 
 * @param context
 * @param playlistName
 * @return The id of the playlist that is created or -1 if the playlist was not created. 
 */
public static long createPlaylist (final Context context, final String playlistName) {

	if (playlistName == null || playlistName == "") {	//If no name was entered, the query will fail. 
		return -1;
	}

	ContentResolver mContentResolver = context.getContentResolver();
	String selection = MediaStore.Audio.Playlists.NAME + " = '" + playlistName + "'";
	Cursor cursor = mContentResolver.query(URI_PLAYLIST_NAMES, PROJECTION_PLAYLIST_NAMES, selection, null, null);
	if (cursor.getCount() == 0) {	//If the cursor is empty, meaning there isn't a playlist already named as such. 
		ContentValues values = new ContentValues();
		values.put(MediaStore.Audio.Playlists.NAME, playlistName);
		Uri uri = mContentResolver.insert(URI_PLAYLIST_NAMES, values);
		cursor.close();
		return Long.parseLong(uri.getLastPathSegment());
	}
	cursor.close();
	return -1;		
}


public static void renamePlaylist(final Context context, final String playlistId, String newName) {

	if (playlistId == "" || newName == null || newName.length() < 1) {
		return;
	}

	Log.v(HomeActivity.TAG, "Renaming to: " + newName);

	ContentResolver mContentResolver = context.getContentResolver();
	String selection = MediaStore.Audio.Playlists._ID + " = '" + playlistId + "'";
	Cursor cursor = mContentResolver.query(URI_PLAYLIST_NAMES, PROJECTION_PLAYLIST_NAMES, selection, null, null);

	if (cursor.moveToFirst()) { 	//If the query is not empty. 
		ContentValues values = new ContentValues();
		values.put(MediaStore.Audio.Playlists.NAME, newName);
		mContentResolver.update(URI_PLAYLIST_NAMES, values, selection, null);
	}

	cursor.close();		
}

public static Cursor getPlaylistItems (final Context context, long playlistId) {
	Uri uri = Audio.Playlists.Members.getContentUri("external", playlistId);
	return getCursor(context, uri, PROJECTION_PLAYLIST_ITEMS, null, null, Playlists.Members.PLAY_ORDER);
}	

public static ArrayList<Long> getSongsFromPlaylist(final Context context, final long playlistId) {

	ContentResolver mContentResolver = context.getContentResolver();
	Uri uri = Audio.Playlists.Members.getContentUri("external", playlistId);
	Cursor mCursor = mContentResolver.query(uri, PROJECTION_PLAYLIST_ITEMS, null, null, Playlists.Members.PLAY_ORDER);
	ArrayList<Long> ids = new ArrayList<Long>();		
	mCursor.moveToFirst();
	for (int i = 0; i < mCursor.getCount(); i++) {
		ids.add(mCursor.getLong(3));
		mCursor.moveToNext();
	}
	mCursor.close();
	return ids;
}

/**
 * 
 * @param context
 * @param curSongId
 */
public static void addToFavorites(final Context context, final long curSongId) {
	ContentResolver mContentResolver = context.getContentResolver();
	String where = MediaStore.Audio.Playlists.NAME + "='Favorites'";
	Cursor mCursor = mContentResolver.query(URI_PLAYLIST_NAMES, PROJECTION_PLAYLIST_NAMES, where, null, null);
	long favoritesId = 0;
	if (mCursor.moveToFirst()) {	//If the cursor is not empty, meaning the playlist already exists, simply get the id. 
		favoritesId = mCursor.getLong(0);
	}
	else {							//Else, create the playlist and get the id. 
		favoritesId = createPlaylist(context, "Favorites");
	}

	mCursor.close();			
	addToPlaylist(context, favoritesId, new long [] {curSongId});
}

public static boolean isFavorite (final Context context, final long songId) {
	final ContentResolver mContentResolver = context.getContentResolver();
	String where = MediaStore.Audio.Playlists.NAME + "='Favorites'";
	Cursor mCursor = mContentResolver.query(URI_PLAYLIST_NAMES, PROJECTION_PLAYLIST_NAMES, where, null, null);
	if (!mCursor.moveToFirst()) {	//No favorites added yet, return.  
		mCursor.close();
		return false;
	}
	long favoritesId = mCursor.getLong(0);
	final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", favoritesId);
	where = MediaStore.Audio.Playlists.Members.AUDIO_ID + "='" + songId + "'"; 		
	mCursor = getCursor(context, uri, PROJECTION_PLAYLIST_ITEMS, where, null, null);
	if (mCursor.moveToFirst()) {	//If the song was there, great, return true.
		mCursor.close();
		return true;
	}
	mCursor.close();
	return false;
}

/**
 * 
 * @param context
 * @param ids
 * @return
 */
public static ArrayList<String> batchSongDetailQuery (final Context context, final long [] ids) {
	ArrayList<String> titles = new ArrayList<String>(ids.length);
	ContentResolver mContentResolver = context.getContentResolver();
	Cursor mCursor = null;
	for (int i = 0; i < ids.length; i++) {
		String where = MediaStore.Audio.Media._ID + "= '" + ids[i] + "'";
		mCursor = mContentResolver.query(URI_SONGS, PROJECTION_SONGS, where, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		if (mCursor.moveToFirst()) {
			titles.add(mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
		}				
	}		
	mCursor.close();		
	return titles;
}

/**
 * 
 * @param context
 * @param artistId
 * @param artist
 * @return
 */
public static String getArtistFirstAlbumArt (final Context context, final long artistId, final String artist) {
	ContentResolver mContentResolver = context.getContentResolver();
	String where = MediaStore.Audio.Artists.Albums.ARTIST + "='" + artist + "'";
	Uri uri = MediaStore.Audio.Artists.Albums.getContentUri("external", artistId); 
	Cursor mCursor = mContentResolver.query(uri, PROJECTION_ARTIST_ALBUMS, where, null, null);
	mCursor.moveToFirst();
	String path = mCursor.getString(0);
	mCursor.close();
	return path;
}

public static String getArtistName(final Context context, Long artistId) {
	ContentResolver mContentResolver = context.getContentResolver();
	String where = MediaStore.Audio.Artists._ID + "='" + artistId + "'"; 
	Cursor mCursor = mContentResolver.query(URI_ARTISTS, PROJECTION_ARTISTS, where, null, null);
	String name = "";
	if (mCursor.moveToFirst()) {
		name = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
	}

	mCursor.close();		
	return name;
}


public static String getAlbumArtist(final Context context, Long albumId) {
	ContentResolver mContentResolver = context.getContentResolver();
	String where = MediaStore.Audio.Albums._ID + "='" + albumId + "'"; 
	Cursor mCursor = mContentResolver.query(URI_ALBUMS, PROJECTION_ALBUMS, where, null, null);
	String name = "";
	if (mCursor.moveToFirst()) {
		name = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
	}

	return name;
}

public static String getCachedAlbumArtPath(Context context, String album) {
	ContentResolver mContentResolver = context.getContentResolver();
	String where = MediaStore.Audio.Albums.ALBUM + "='" + album + "'"; 
	Cursor mCursor = mContentResolver.query(URI_ALBUMS, PROJECTION_ALBUMS, where, null, null);
	String path = "";
	if (mCursor.moveToFirst()) {
		path = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
	}

	return path;
}


/**
 * 
 * Call EditMetaData when you need to modify information for a specified song. 
 * 
 * @param _ids An array containing the id of all songs currently being edited. This comes from the MediaStore.Audio.Media._ID column, and is unique for each Song on the device. 
 * @param property An array declaring what properties are being changed. 0 for Title, 1 for Artist, 2 for Album. 
 * @param value An array containing the new values to be inserted on the database. Values should be organized in a way that the Title is the first item on the Array, and the Album is the last item. 
 */
public static void editMetaData (final Context context, ContentValues values, final long [] _ids) {

	String [] projection = {
			MediaStore.Audio.Media._ID,
			MediaStore.Audio.Media.ALBUM,
			MediaStore.Audio.Media.TITLE,
			MediaStore.Audio.Media.ARTIST
	};

	ContentResolver mContentResolver = context.getContentResolver();

	Cursor mCursor = mContentResolver.query(URI_SONGS, projection, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
	int itemsUpdated = 0;
	String where = MediaStore.Audio.Media._ID + "=";

	if (mCursor == null) {} 		
	else if (!mCursor.moveToFirst()) {} 
	else {			
		for (int i = 0; i < _ids.length; i++) {
			itemsUpdated = mContentResolver.update(URI_SONGS, values, where + "\"" + String.valueOf(_ids[i]) + "\"", null);
		}		
	}
	Log.v(HomeActivity.TAG, "Items modified succesfully: " + itemsUpdated);
	mCursor.close();
	Intent metaChanged = new Intent(MyMusicService.ACTION_META_CHANGED);
	context.sendBroadcast(metaChanged);		
}

/**
 * Delete a song from both the content provider database as well as the sdcard. 
 * @param context The context, used to obtain the content resolver.
 * @param ids An array containing the ids of the songs you intent to delete. 
 */
public static void deleteSong (final Context context, final ArrayList<Long>ids) {
	ContentResolver mContentResolver = context.getContentResolver();
	Cursor mCursor = null;
	String songPath;	//The path to the song in order to delete from the sdcard. 
	File file;			//The file is used to call file.delete()
	for (int i = 0; i < ids.size(); i++) {
		String where = MediaStore.Audio.Media._ID + "= '" + ids.get(i) + "'";
		mCursor = mContentResolver.query(URI_SONGS, ALL_MEDIA_COLS, where, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		mCursor.moveToFirst();	//There should only be one result per query. 
		songPath = mCursor.getString(mCursor.getColumnIndex(Audio.Media.DATA));	//Data stores the actual path to the song.
		//Now that we know the path, let's delete from content provider.  
		mContentResolver.delete(URI_SONGS, where, null);	 //You cannot do this first because you needed the path first.
		file = new File(songPath);
		file.delete();
	}		
	Intent metaChanged = new Intent(MyMusicService.ACTION_META_CHANGED);
	context.sendBroadcast(metaChanged);	//Notify that the song list should be updated. 	
	mCursor.close();									
}

/**
 * Delete a playlist from the content provider. 
 * @param context
 * @param playlistId
 */
public static void deletePlaylist (final Context context, final long playlistId) {
	ContentResolver mContentResolver = context.getContentResolver();
	String where = MediaStore.Audio.Playlists._ID + "='" + playlistId + "'";
	mContentResolver.delete(URI_PLAYLIST_NAMES, where, null);				
}

@SuppressLint("DefaultLocale")
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public static String getReadableDuration(long duration) {
	String readableDuration = null;
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
		readableDuration = String.format("%02d min, %02d sec", 
				TimeUnit.MILLISECONDS.toMinutes(duration),
				TimeUnit.MILLISECONDS.toSeconds(duration) - 
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
				);
	}

	else {
		final int seconds = (int) (duration/1000) % 60;
		final int minutes = (int) ((duration / (1000*60)) % 60);
		final String secondsStr = seconds < 10 ? "0" + seconds : String.valueOf(seconds);
		readableDuration = minutes + ":" + secondsStr;
	}

	return readableDuration;
}

}