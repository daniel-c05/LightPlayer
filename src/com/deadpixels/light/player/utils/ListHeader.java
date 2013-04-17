package com.deadpixels.light.player.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.lazybitz.beta.light.player.R;
import com.deadpixels.light.player.adapters.HeaderAdapter.RowType;

public class ListHeader implements Item {

	private final String name;
	private final int iconResource;

	public ListHeader(String name, int icRes) {
		this.name = name;
		this.iconResource = icRes;
	}

	@Override
	public int getViewType() {
		return RowType.HEADER_ITEM.ordinal();
	}

	@Override
	public View getView(LayoutInflater inflater, View convertView) {
		View view;
		if (convertView == null) {
			view = (View) inflater.inflate(R.layout.list_header, null);
			// Do some initialization
		} else {
			view = convertView;
		}

		TextView text = (TextView) view.findViewById(R.id.list_header_text);
		text.setText(name);
		ImageView icon = (ImageView) view.findViewById(R.id.list_headericon);
		icon.setImageResource(iconResource);
		view.setClickable(false);
		
		return view;
	}
	
	public String getCursorId () {
		return "";
	}

}
