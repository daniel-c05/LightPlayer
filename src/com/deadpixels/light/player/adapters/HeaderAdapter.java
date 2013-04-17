package com.deadpixels.light.player.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.deadpixels.light.player.utils.Item;
import com.deadpixels.light.player.utils.ListItem;

public class HeaderAdapter extends ArrayAdapter<Item>{
	
	private LayoutInflater mInflater;

    public enum RowType {
        LIST_ITEM, HEADER_ITEM
    }
    
    public static final int LIST_ITEM = 0;
    public static final int HEADER_ITEM = 1;

    private List<Item> items;

    public HeaderAdapter(Context context, List<Item> items) {
        super(context, 0, items);
        this.items = items;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getViewTypeCount() {
        return RowType.values().length;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getViewType();
    }
    
    public String getCursorId(int position) {
    	Item item = items.get(position);
    	if (item.getViewType() == LIST_ITEM) {
    		return ((ListItem)item).getCursorId();
		}        
    	return "";
    }
    
    public String getItemHeader(int position) {
    	Item item = items.get(position);
    	if (item.getViewType() == LIST_ITEM) {
    		return ((ListItem)item).getHeader();
		}        
    	return "";
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return items.get(position).getView(mInflater, convertView);
    }
}
