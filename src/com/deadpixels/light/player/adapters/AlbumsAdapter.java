package com.deadpixels.light.player.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.MediaStore.Audio;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.deadpixels.light.player.HomeActivity;
import com.lazybitz.beta.light.player.R;
import com.deadpixels.light.player.utils.MusicManager;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

public class AlbumsAdapter extends CursorTreeAdapter {

	LayoutInflater mInflater;
	Context mContext;

	public AlbumsAdapter (Cursor parent, Context context) {
		super(parent, context);
		mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	protected void bindChildView(View view, Context context, Cursor cursor,
			boolean isLastChild) {
		ChildViewHolder mHolder = (ChildViewHolder) view.getTag();
		if (mHolder == null) {
			mHolder = new ChildViewHolder();
			mHolder.song = (TextView) view.findViewById(R.id.album_song);
		}		
		mHolder.song.setText(cursor.getString(1));	
	}

	@Override
	protected void bindGroupView(View view, Context context, Cursor cursor,
			boolean isExpanded) {
		ViewHolder mHolder = (ViewHolder) view.getTag();
		if (mHolder == null) {
			mHolder = new ViewHolder();
			mHolder.album = (TextView) view.findViewById(R.id.album_group_album);
			mHolder.artist = (TextView) view.findViewById(R.id.album_group_artist);
			mHolder.dropdown = (ImageView) view.findViewById(R.id.group_icon);
			mHolder.thumb = (ImageView) view.findViewById(R.id.album_group_thumb);
		}			 
		
		String album = cursor.getString(1);
		
		String path = getImageUrl(context, album); 
		if (path == "") {
			path =  cursor.getString(3);
		}		
		
		UrlImageViewHelper.setUrlDrawable(mHolder.thumb, path, R.drawable.dummy_thumb);
		mHolder.album.setText(album);
		mHolder.artist.setText(cursor.getString(2));			 
	}

	@Override
	protected Cursor getChildrenCursor(Cursor groupCursor) {
		String album = groupCursor.getString(groupCursor.getColumnIndex(Audio.Albums.ALBUM));
		if (album != null && album != "") {
			return MusicManager.getAllSongsFromAlbum(mContext, album);
		}			
		return null;			
	}

	@Override
	protected View newChildView(Context context, Cursor cursor,
			boolean isLastChild, ViewGroup parent) {
		View view = mInflater.inflate(R.layout.album_childs, null);
		ChildViewHolder mHolder = new ChildViewHolder();
		mHolder.song = (TextView) view.findViewById(R.id.album_song);
		view.setTag(mHolder);
		return view;
	}

	@Override
	protected View newGroupView(Context context, Cursor cursor,
			boolean isExpanded, ViewGroup parent) {
		View view = mInflater.inflate(R.layout.album_groups, null);
		ViewHolder mHolder = new ViewHolder();
		mHolder.album = (TextView) view.findViewById(R.id.album_group_album);
		mHolder.artist = (TextView) view.findViewById(R.id.album_group_artist);
		mHolder.dropdown = (ImageView) view.findViewById(R.id.group_icon);
		mHolder.thumb = (ImageView) view.findViewById(R.id.album_group_thumb);
		view.setTag(mHolder);
		return view;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	class ViewHolder {
		TextView album;
		TextView artist;
		ImageView thumb;
		ImageView dropdown;
	}

	class ChildViewHolder {
		TextView song;
		TextView artist;
	}

	public String getAlbum(int pos) {
		Cursor mCursor = getGroup(pos);
		if (mCursor == null || mCursor.getCount() < pos) {
			return null;
		}		
		mCursor.moveToPosition(pos);
		String album = mCursor.getString(mCursor.getColumnIndex(Audio.Albums.ALBUM));
		return album;
	}
	
	private String getImageUrl (Context context, String album) {

		Log.v(HomeActivity.TAG, "Loading Image url for: " + album);

		SharedPreferences mPreferences = context.getSharedPreferences("image-map", Context.MODE_PRIVATE);		
		String imageUrl = mPreferences.getString("album-" + album, ""); 

		if (imageUrl == null || imageUrl == "") {	//We don't yet have a preference for this image, never should happen, default to local
			imageUrl = MusicManager.getCachedAlbumArtPath(context, album);
		}
		
		Log.v(HomeActivity.TAG, "Url found: " + imageUrl);

		return imageUrl;

	}
}
