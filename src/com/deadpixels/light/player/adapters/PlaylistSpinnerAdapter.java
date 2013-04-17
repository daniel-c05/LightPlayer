package com.deadpixels.light.player.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PlaylistSpinnerAdapter extends SimpleCursorAdapter {

	private LayoutInflater mInflater;

	public PlaylistSpinnerAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to, int flags) {
		super(context, layout, cursor, from, to, flags);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View v = mInflater.inflate(android.R.layout.simple_spinner_item, parent, false);
		ViewHolder mHolder = new ViewHolder();
		mHolder.header = (TextView) v.findViewById(android.R.id.text1);
		v.setTag(mHolder);
		return v;
	}

	public long getPlaylistId (int pos) {
		if (mCursor == null || mCursor.getCount() < pos) {
			return 0;
		}
		return mCursor.getLong(0);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder mHolder = (ViewHolder) view.getTag();

		if (mHolder == null) {
			mHolder = new ViewHolder();
			mHolder.header = (TextView) view.findViewById(android.R.id.text1);
		}

		mHolder.header.setText(cursor.getString(1));		 
	}

	private class ViewHolder {
		TextView header;
	}

	public String getPlaylistName(int lastVisiblePosition) {
		if (mCursor == null || mCursor.getCount() < lastVisiblePosition) {
			return "";
		}
		mCursor.moveToPosition(lastVisiblePosition);
		return mCursor.getString(1);
	}
}
