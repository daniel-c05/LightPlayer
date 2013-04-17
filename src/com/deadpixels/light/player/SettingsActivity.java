package com.deadpixels.light.player;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.deadpixels.light.player.fragments.SettingsFragment;
import com.deadpixels.light.player.service.MyMusicService;

public class SettingsActivity extends FragmentActivity {
	
	public static final String KEY_DISABLE_DOWNLOADS = "pref_key_disable_art_downloads";
	
	 @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);

	        getFragmentManager().beginTransaction()
	                .replace(android.R.id.content, new SettingsFragment())
	                .commit();
	    }
	 
	 @Override
		protected void onResume() {	
			super.onResume();
			Intent intent = new Intent();
			intent.setAction(MyMusicService.ACTION_CANCEL_NOTIFICATION);
			sendBroadcast(intent);		
		}

		@Override
		protected void onPause() {	
			Intent intent = new Intent();
			intent.setAction(MyMusicService.ACTION_DISPLAY_PENDING_NOTIFICATION);
			sendBroadcast(intent);
			super.onPause();
		}
	 
	}
