package com.deadpixels.light.player.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lazybitz.beta.light.player.R;

public class QueueDragDropAdapter extends BaseAdapter {

	private LayoutInflater mInflater;
	private boolean isInEditMode = false;
	private ArrayList<String> mItems;

	public QueueDragDropAdapter (Context context, ArrayList<String> items, boolean editMode) {
		this.isInEditMode = editMode;
		this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.mItems= items; 
	}

	@Override
	public int getCount() {
		if (mItems != null) {
			return mItems.size();
		}
		return 0;
	}

	@Override
	public Object getItem(int position) {
		if (mItems != null) {
			return mItems.get(position);
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		if (mItems != null) {
			return mItems.size();
		}
		return 0;
	}
	
	public String removeItem(int position) {
		if (position < 0 || mItems.size() < position) {
			return null;
		}
		return mItems.remove(position);
	}
	
	public void addItem(String item, int position) {
		if (position < 0 || mItems.size() < position) {
			return;
		}
		mItems.add(position, item);
	}
	
	public void moveItemTo(int from, int to) {
		if (from < 0 || mItems.size() < from) {
			return;
		}
		String item = this.removeItem(from);
		this.addItem(item, to);
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		ViewHolder mHolder;
		if (view == null) {
			view = mInflater.inflate(R.layout.drag_n_drop_item, parent, false);
			mHolder = new ViewHolder();
			mHolder.header = (TextView) view.findViewById(R.id.drag_n_drop_title);
			mHolder.small = (TextView) view.findViewById(R.id.drag_n_drop_subtitle);
			mHolder.drag = (ImageView) view.findViewById(R.id.drag_handle);
			mHolder.dragDiv = (View) view.findViewById(R.id.drag_divider);
			view.setTag(mHolder);
		}
		else {
			mHolder = (ViewHolder) view.getTag();
		}
		
		mHolder.header.setText(mItems.get(position));
		mHolder.small.setVisibility(View.GONE);
		
		if (isInEditMode) {
			mHolder.drag.setVisibility(View.VISIBLE);
			mHolder.dragDiv.setVisibility(View.VISIBLE);
		}
		else {
			mHolder.drag.setVisibility(View.GONE);
			mHolder.dragDiv.setVisibility(View.GONE);
		}	 

		return view;
	}
	
	public void setInEditMode (boolean mode) {
		this.isInEditMode = mode;
		notifyDataSetChanged();
	}

	public void toggleInEditMode () {
		if (this.getCount() == 0) {
			return;
		}
		if (this.isInEditMode) {
			this.isInEditMode = false;
		}
		else {
			this.isInEditMode = true;
		}	 
		notifyDataSetChanged();
	}

	private class ViewHolder {
		TextView header;
		TextView small;
		ImageView drag;
		View dragDiv;	 
	}
}
