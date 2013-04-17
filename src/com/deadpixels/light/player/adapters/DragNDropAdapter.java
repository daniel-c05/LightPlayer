package com.deadpixels.light.player.adapters;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore.Audio;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.deadpixels.light.player.HomeActivity;
import com.lazybitz.beta.light.player.R;
import com.deadpixels.light.player.utils.MusicManager;
import com.mobeta.android.dslv.DragSortListView.DragSortListener;

public class DragNDropAdapter extends SimpleCursorAdapter implements DragSortListener {

	private LayoutInflater mInflater;
	private long mPlaylistId;
	private boolean isInEditMode = false;

	public DragNDropAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to, int flags, long playlistId) {
		super(context, layout, cursor, from, to, flags);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mPlaylistId = playlistId;
	}	

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View v = mInflater.inflate(R.layout.drag_n_drop_item, parent, false);
		ViewHolder mHolder = new ViewHolder();
		mHolder.header = (TextView) v.findViewById(R.id.drag_n_drop_title);
		mHolder.small = (TextView) v.findViewById(R.id.drag_n_drop_subtitle);
		mHolder.drag = (ImageView) v.findViewById(R.id.drag_handle);
		mHolder.dragDiv = (View) v.findViewById(R.id.drag_divider);
		v.setTag(mHolder);
		return v;
	}

	public long getItemFromColumn (int pos, String columnName) {
		if (mCursor == null || mCursor.getCount() < pos) {
			return 0;
		}
		mCursor.moveToPosition(pos);
		long id = mCursor.getLong(mCursor.getColumnIndex(columnName));
		return id;
	}

	public void setPlaylistId (long id) {
		this.mPlaylistId = id;
	}

	public void setInEditMode (boolean mode) {
		this.isInEditMode = mode;
		notifyDataSetChanged();
	}

	public void toggleInEditMode () {
		if (this.isInEditMode) {
			this.isInEditMode = false;
		}
		else {
			this.isInEditMode = true;
		}	 
		notifyDataSetChanged();
	}

	public boolean getInEditMode () {
		return this.isInEditMode;
	}

	public long getPlaylistId () {
		return this.mPlaylistId;
	} 

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder mHolder = (ViewHolder) view.getTag();

		if (mHolder == null) {
			mHolder = new ViewHolder();
			mHolder.header = (TextView) view.findViewById(R.id.drag_n_drop_title);
			mHolder.small = (TextView) view.findViewById(R.id.drag_n_drop_subtitle);
			mHolder.drag = (ImageView) view.findViewById(R.id.drag_handle);
			mHolder.dragDiv = (View) view.findViewById(R.id.drag_divider);
		}

		mHolder.header.setText(cursor.getString(1));
		mHolder.small.setText(cursor.getString(2));
		if (isInEditMode) {
			mHolder.drag.setVisibility(View.VISIBLE);
			mHolder.dragDiv.setVisibility(View.VISIBLE);
		}
		else {
			mHolder.drag.setVisibility(View.GONE);
			mHolder.dragDiv.setVisibility(View.GONE);
		}	 
	}

	private class ViewHolder {
		TextView header;
		TextView small;
		ImageView drag;
		View dragDiv;	 
	}

	public long[] getAllIds(String columnName) {
		if (mCursor == null || mCursor.isClosed()) {
			return null;
		}

		long [] ids = new long [mCursor.getCount()];

		mCursor.moveToFirst();

		for (int i = 0; i < ids.length; i++) {
			mCursor.getLong(mCursor.getColumnIndex(columnName));
		}		
		return ids;

	}

	@Override
	public void drop(int from, int to) {	
		Log.v(HomeActivity.TAG, "Moving item from position: " + from + " to position: " + to);

		if (from == to)
			return;

		if (MusicManager.movePlaylistItemTo(mContext, mPlaylistId, from, to)) {
			Log.v(HomeActivity.TAG, "Refreshing data");
			swapCursor(MusicManager.getPlaylistItems(mContext, mPlaylistId));
			notifyDataSetChanged();
		}	
	}

	@Override
	public void drag(int from, int to) {
		//Scroll
	}

	@Override
	public void remove(int which) {
		long songId = getItemFromColumn(which, Audio.Playlists.Members.AUDIO_ID);
		Log.v(HomeActivity.TAG, "Removing id: " + songId + " song name: " + mCursor.getString(1));
		MusicManager.removeFromPlaylist(mContext, mPlaylistId, songId);
		swapCursor(MusicManager.getPlaylistItems(mContext, mPlaylistId));
		notifyDataSetChanged();
	}

}
