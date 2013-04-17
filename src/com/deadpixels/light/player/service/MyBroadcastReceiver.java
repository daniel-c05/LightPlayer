package com.deadpixels.light.player.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.deadpixels.light.player.HomeActivity;

public class MyBroadcastReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Intent shareInfoIntent = new Intent();
		
		if (action.equals(Intent.ACTION_HEADSET_PLUG) || action.equals(Intent.ACTION_ANSWER) ||  action.equals(Intent.ACTION_CALL) || action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {			
			Log.v(HomeActivity.TAG, "Headset Plugged");
			HomeActivity.sendMessage(MyMusicService.MSG_HEADSET_STATE_CHANGED);
			return;
		}
				
		else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
			shareInfoIntent.setAction(MyMusicService.ACTION_STOP);
			context.sendBroadcast(shareInfoIntent);
			return;
		}
		
	}
	
	
	
}
