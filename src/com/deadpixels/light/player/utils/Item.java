package com.deadpixels.light.player.utils;

import android.view.LayoutInflater;
import android.view.View;

public interface Item {
	public int getViewType();
    public View getView(LayoutInflater inflater, View convertView);
}
