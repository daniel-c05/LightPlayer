package com.deadpixels.light.player.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.deadpixels.light.player.HomeActivity;
import com.lazybitz.beta.light.player.R;
import com.deadpixels.light.player.SettingsActivity;
import com.deadpixels.light.player.utils.LastFmUtils;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

public class ArtistBioFragment extends Fragment {

	public static final String ARTIST = "artist";

	private TextView title;
	private WebView description;
	private ImageView art;
	private String artist;
	private Handler handler;
	private boolean areDownloadsDisabled;

	public ArtistBioFragment () {

	}

	public static ArtistBioFragment newInstance(String artist) {
		ArtistBioFragment f = new ArtistBioFragment();

		Bundle args = new Bundle();
		args.putString(ARTIST, artist);
		f.setArguments(args);

		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		areDownloadsDisabled = sharedPref.getBoolean(SettingsActivity.KEY_DISABLE_DOWNLOADS, false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.bio_fragment, container, false);
		title = (TextView) view.findViewById(R.id.bio_title);
		description = (WebView) view.findViewById(R.id.bio_summary);
		description.setBackgroundColor(getResources().getColor(R.color.dim_foreground_disabled_holo_dark));
		art = (ImageView) view.findViewById(R.id.bio_art);		
		handler = new Handler();

		Bundle args = getArguments();
		if (args != null && args.containsKey(ARTIST)) {
			artist = args.getString(ARTIST);
			SharedPreferences mPreferences = getActivity().getSharedPreferences("image-map", Context.MODE_PRIVATE);
			String artistUrl = mPreferences.getString("artist-" + artist, "");
			if (artistUrl != null && artistUrl.length() > 2) {
				Log.v(HomeActivity.TAG, "Loaded artist image from preferences");
				UrlImageViewHelper.setUrlDrawable(art, artistUrl, R.drawable.dummy_art);
			}
			if (!areDownloadsDisabled) {
				Runnable mRunnable = new Runnable() {    				
					@Override
					public void run() {
						artist = LastFmUtils.Artist.fixString(artist);        					
						final String content = LastFmUtils.Artist.getArtistBio(artist);
						handler.post(new Runnable() {
							@Override
							public void run() {
								if (content != null) {
									description.loadData(content, "text/html", null);
								}
								else {
									description.loadData("No Information available", "text/html", null);
								}	    			            			            	
							}
						});
					}				
				};
				new Thread(mRunnable).start();
			}
			else {
				description.loadData("Artist Information Fetching is currently disabled", 
						"text/html", null);
			}    	
			title.setText(artist);
		}        

		return view;

	}
			

	}

