package com.deadpixels.light.player.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.lazybitz.beta.light.player.R;
import com.deadpixels.light.player.adapters.HeaderAdapter.RowType;

public class ListItem implements Item {

	private final String mLabel;
	private final String mCursorId;
	private final String mHeader;

	public ListItem(String header, String label, String usableId) {
		this.mLabel = label;
		this.mCursorId = usableId;
		this.mHeader = header;
	}

	@Override
	public int getViewType() {
		return RowType.LIST_ITEM.ordinal();
	}

	@Override
	public View getView(LayoutInflater inflater, View convertView) {
		View view;
		if (convertView == null) {
			view = (View) inflater.inflate(R.layout.list_item, null);
		} else {
			view = convertView;
		}

		TextView text1 = (TextView) view.findViewById(R.id.list_item_text);
		text1.setText(mLabel);

		return view;
	}
	
	public String getCursorId () {
		return this.mCursorId;
	}
	
	public String getHeader () {
		return this.mHeader;
	}

}

