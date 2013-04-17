package com.deadpixels.light.player.adapters;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore.Audio;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.lazybitz.beta.light.player.R;
import com.deadpixels.light.player.utils.LastFmUtils;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

public class AllArtistsAdapter extends SimpleCursorAdapter {

	LayoutInflater mInflater;
	boolean doNotdownload;

	public AllArtistsAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to, int flags, boolean downloads) {
		super(context, layout, cursor, from, to, flags);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		doNotdownload = downloads;
	}	

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		GridHolder mHolder = (GridHolder) view.getTag();

		if (mHolder == null) {
			mHolder = new GridHolder();
			mHolder.thumbnail = (ImageView) view.findViewById(R.id.artist_thumb);
			mHolder.artist = (TextView) view.findViewById(R.id.artist_name);
		}
		new SetArtistImage(context, mHolder.thumbnail).execute(cursor.getString(cursor.getColumnIndex(Audio.Artists.ARTIST)));
		mHolder.artist.setText(cursor.getString(cursor.getColumnIndex(Audio.Artists.ARTIST)));		 
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View v = mInflater.inflate(R.layout.by_artists_list_item, parent, false);
		GridHolder mHolder = new GridHolder();
		mHolder.thumbnail = (ImageView) v.findViewById(R.id.artist_thumb);
		mHolder.artist = (TextView) v.findViewById(R.id.artist_name);
		v.setTag(mHolder);
		return v;
	}

	public Bundle getArtistDetails (int pos) {
		if (mCursor == null || mCursor.getCount() < pos) {
			return null;
		}
		mCursor.moveToPosition(pos);
		Bundle bundle = new Bundle();
		bundle.putString(Audio.Artists.ARTIST, mCursor.getString(mCursor.getColumnIndex(Audio.Artists.ARTIST)));
		bundle.putLong("_id", mCursor.getLong(mCursor.getColumnIndex(Audio.Artists._ID)));
		return bundle;
	}
	

	public String getArtistName(int pos) {
		if (mCursor == null || mCursor.getCount() < pos) {
			return null;
		}
		mCursor.moveToPosition(pos);
		String artist = mCursor.getString(mCursor.getColumnIndex(Audio.Artists.ARTIST));
		return artist;
	}

	private class GridHolder {
		ImageView thumbnail;
		TextView artist;
	}

	private class SetArtistImage extends AsyncTask<String, Void, String> {

		Context mContext;
		WeakReference<ImageView> mImageReference;

		public SetArtistImage (Context context, ImageView iv) {
			this.mContext = context;
			mImageReference = new WeakReference<ImageView>(iv);
		}

		@Override
		protected String doInBackground(String... params) {
			String artist = params[0];			
			SharedPreferences mPreferences = mContext.getSharedPreferences("image-map", Context.MODE_PRIVATE);
			Editor mEditor = mPreferences.edit();
			String artistUrl = mPreferences.getString("artist-" + artist, "");
			if (!artistUrl.equals("") && artistUrl != null) {
				return artistUrl;
			}
			if (!doNotdownload) {	//Only proceed to download if we don´t already have the info cached and if there is an active connection. 
				artistUrl = LastFmUtils.Artist.getArtistImageUrl(artist, 4);
				if (artistUrl != null && artistUrl.length() > 2) {
					mEditor.putString("artist-" + artist, artistUrl);
					mEditor.commit();
				}			
			}			
			return artistUrl;
		}

		@Override
		protected void onPostExecute(String result) {
			if (mImageReference.get() != null) {
				UrlImageViewHelper.setUrlDrawable(mImageReference.get(), result, R.drawable.dummy_art);
			}			
		}

	}

}
