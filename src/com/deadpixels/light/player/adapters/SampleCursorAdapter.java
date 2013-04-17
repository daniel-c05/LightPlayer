package com.deadpixels.light.player.adapters;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lazybitz.beta.light.player.R;

public class SampleCursorAdapter extends SimpleCursorAdapter {
	
	private LayoutInflater mInflater;

	 public SampleCursorAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to, int flags) {
	        super(context, layout, cursor, from, to, flags);
	        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    }	
	
	 @Override
	 public View newView(Context context, Cursor cursor, ViewGroup parent) {
		 View v = mInflater.inflate(R.layout.by_song_list_item, parent, false);
		 ViewHolder mHolder = new ViewHolder();
		 mHolder.header = (TextView) v.findViewById(R.id.by_song_title);
		 mHolder.small = (TextView) v.findViewById(R.id.by_song_artist);
		 v.setTag(mHolder);
		 return v;
	 }
	 
	 public String getLocalArtPath (int pos) {
		 if (mCursor == null) {
			return "";
		}
		 
		 if (mCursor.getCount() < pos) {
			 return "";
		}
		 mCursor.moveToPosition(pos);
		 return String.valueOf(mCursor.getLong(0));
	 }
	 
	 public long getSongId (int pos, String columnName) {
		 if (mCursor == null || mCursor.getCount() < pos) {
			 return 0;
		}
		 mCursor.moveToPosition(pos);
		 long id = mCursor.getLong(mCursor.getColumnIndex(columnName));
		 return id;
	 }
	 
	 public Bundle getItemDetails (int pos) {
		 Bundle bundle = new Bundle();
		 if (mCursor == null || mCursor.getCount() < pos) {
			 return null;
		}
		 mCursor.moveToPosition(pos);
		 bundle.putString("_id", String.valueOf(mCursor.getLong(0)));
		 bundle.putString("title", mCursor.getString(1));
		 bundle.putString("artist", mCursor.getString(2));
		 return bundle;
	 }	 
	 
	 @Override
	 public void bindView(View view, Context context, Cursor cursor) {
		 ViewHolder mHolder = (ViewHolder) view.getTag();
		 
		 if (mHolder == null) {
			 mHolder = new ViewHolder();
			 mHolder.header = (TextView) view.findViewById(R.id.by_song_title);
			 mHolder.small = (TextView) view.findViewById(R.id.by_song_artist);
		 }
		 
		 mHolder.header.setText(cursor.getString(1));
		 mHolder.small.setText(cursor.getString(2));
		 
	 }
	 
	 private class ViewHolder {
		 TextView header;
		 TextView small;
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
	 
}

