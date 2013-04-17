package com.deadpixels.light.player.service;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore.Audio;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.deadpixels.light.player.HomeActivity;
import com.deadpixels.light.player.PlayerHolder;
import com.lazybitz.beta.light.player.R;
import com.deadpixels.light.player.fragments.PlayerFragment;
import com.deadpixels.light.player.utils.MusicManager;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

public class MyMusicService extends Service {

	public static final int MSG_PLAYSTATE_CHANGED = 0;
	public static final int MSG_CURRENT_QUEUE_CHANGED = 1;
	public static final int MSG_SHUFFLE_STATE_CHANGED = 2;
	public static final int MSG_REPEAT_STATE_CHANGED = 3;
	public static final int MSG_META_CHANGED = 4;
	public static final int MSG_HEADSET_STATE_CHANGED = 5;

	public static final int SHUFFLE_OFF = 0;
	public static final int SHUFFLE_ON = 1;	

	public static final int REPEAT_OFF = 0;
	public static final int REPEAT_ON = 1;
	public static final int REPEAT_ALL = 2;

	public static final int MIN_MILSECS_TO_REPEAT = 5 * 1000;
	public static final int KILL_ME_TIMEOUT = 30 * 1000;

	private boolean showNotifaction = false;

	public static final String ACTION_STOP = "com.deadpixels.musiccommands.stop";
	public static final String ACTION_PLAY_PAUSE = "com.deadpixels.musiccommands.playorpause";
	public static final String ACTION_PLAY = "com.deadpixels.musiccommands.play";
	public static final String ACTION_NEXT = "com.deadpixels.musiccommands.next";
	public static final String ACTION_PLAYLIST = "com.deadpixels.musiccommands.change.playlist";
	public static final String ACTION_ENQUEUE = "com.deadpixels.musiccommands.change.queue";
	public static final String ACTION_CLEAR_QUEUE = "com.deadpixels.musiccommands.clear.queue";
	public static final String ACTION_PREVIOUS = "com.deadpixels.musiccommands.previous";
	public static final String ACTION_DISPLAY_PENDING_NOTIFICATION = "com.deadpixels.musiccommands.display.notification";
	public static final String ACTION_CANCEL_NOTIFICATION = "com.deadpixels.musiccommands.cancel.notification";
	public static final String ACTION_PLAYBACK_FINISHED = "com.deadpixels.musiccommands.playback.finished";
	public static final String ACTION_SONG_CHANGED = "com.deadpixels.musiccommands.song.changed";
	public static final String ACTION_META_CHANGED = "com.deadpixels.musiccommands.meta.changed";
	public static final String ACTION_DATASET_CHANGED = "com.deadpixels.musiccommands.dataset.changed";
	public static final String ACTION_CURRENT_QUEUE_CHANGED = "com.deadpixels.musiccommands.queue.changed";

	public static final String EMPTY_STRING = "";

	private static int mCurPosition;
	public static int mCurShuffleStatus = SHUFFLE_OFF;
	public static int mCurRepeatStatus = REPEAT_OFF;
	private static ArrayList<Long> curQueue;
	private Notification mNotification;

	public static final String EXTRA_POS = "position";
	public static final String EXTRA_PLAYLIST_ID = "playlistid";
	public static final String EXTRA_QUEUE_SONGS = "queuesongs";
	public static final String EXTRA_BOOL_FORCE = "com.deadpixels.light.player.force";
	public static final String EXTRA_NOTIFY = "notify";
	public static final String EXTRA_QUEUE_PLAY = "queueplay";

	private static NotificationManager mNotificationManager;
	private static final int mId = 1;

	private static Context mContext;
	private static Cursor mCursor;

	private final Messenger mMessenger = new Messenger(new IncomingHandler());
	protected static long curSongId;
	private boolean pausedByCall = false;
	private TelephonyManager mgr;

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();

			if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
				if (mPlayer != null && mPlayer.isPlaying()) {
					mPlayer.pause();
					if (showNotifaction) {
						mNotification.contentView.setImageViewResource(R.id.notification_button_play, R.drawable.button_play_holo);				
						mNotificationManager.notify(mId, mNotification);
					}		
				}
			}

			else if (action.equals(ACTION_PLAYLIST)) {
				changePlaylist(intent.getLongExtra(EXTRA_PLAYLIST_ID, 0));
			}

			else if (action.equals(ACTION_DISPLAY_PENDING_NOTIFICATION)) {
				if (isPlaying()) {
					Log.v(HomeActivity.TAG, "Attempting to show notification");
					showNotifaction = true;
					showNotification();
				}				
			}

			else if (action.equals(ACTION_CANCEL_NOTIFICATION)) {
				Log.v(HomeActivity.TAG, "Attempting to cancel notification");
				showNotifaction = false;
				stopForeground(true);
			}

			else if (action.equals(ACTION_ENQUEUE)) {
				final long [] songs = intent.getLongArrayExtra(EXTRA_QUEUE_SONGS);
				final boolean play = intent.getBooleanExtra(EXTRA_QUEUE_PLAY, false);
				updateCurQueue(songs, play);
			}

			else if (action.equals(ACTION_CLEAR_QUEUE)) {
				clearQueue();
			}

			else if (action.equals(ACTION_NEXT)) {
				playNextSong(true, intent.getBooleanExtra(EXTRA_BOOL_FORCE, true));
			}

			else if (action.equals(ACTION_PREVIOUS)) {
				playPreviousSong(intent.getBooleanExtra(EXTRA_BOOL_FORCE, true));
			}

			else if (action.equals(ACTION_STOP)) {
				stopPlayBack();				
			}

			else if (action.equals(ACTION_PLAY_PAUSE)) {			
				playOrPause(intent.getBooleanExtra(EXTRA_BOOL_FORCE, true));
			}

			else if (action.equals(ACTION_PLAY)) {
				curSongId = intent.getExtras().getLong("_id");
				if (curQueue.contains(curSongId)) {
					mCurPosition = curQueue.indexOf(curSongId);
				}
				else {
					mCurPosition = 0;
				}
				playSong(curSongId, intent.getBooleanExtra(EXTRA_NOTIFY, false));
			}
		}
	};

	PhoneStateListener phoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			if (state == TelephonyManager.CALL_STATE_RINGING) {
				if (mPlayer != null) {
					if (mPlayer.isPlaying()) {
						mPlayer.pause();
						pausedByCall = true;
					}
				}	        	
			} else if(state == TelephonyManager.CALL_STATE_IDLE) {
				if (mPlayer != null) {
					if (!mPlayer.isPlaying() && pausedByCall) {
						Runnable runnable = new Runnable() {
							@Override
							public void run() {
								try {
									Thread.sleep(1000 * 2);
									mPlayer.start();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}		
							}
						};
						new Thread(runnable).start();	        			
					}	        		
				}	        	
			} else if(state == TelephonyManager.CALL_STATE_OFFHOOK) {
				if (mPlayer != null) {
					if (mPlayer.isPlaying()) {
						mPlayer.pause();
						pausedByCall = true;
					}
				}	        
			}
			super.onCallStateChanged(state, incomingNumber);
		}
	};

	public static MediaPlayer mPlayer;

	static class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_PLAYSTATE_CHANGED:
				break;
			case MSG_CURRENT_QUEUE_CHANGED:

				break;
			case MSG_REPEAT_STATE_CHANGED:
				toggleRepeat();
				break;
			case MSG_SHUFFLE_STATE_CHANGED:
				toggleShuffle();
				break;
			case MSG_HEADSET_STATE_CHANGED:
				if (mPlayer != null && mPlayer.isPlaying()) {
					mPlayer.pause();
				}
				break;
			default:
				break;
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(HomeActivity.TAG, "Received start id " + startId + ": " + intent);
		return START_STICKY;
	}

	protected void notifyMetaChanged() {
		Message msg = Message.obtain(null, MSG_META_CHANGED, 0, 0);
		try {
			mMessenger.send(msg);
		} catch (Exception e) {
			Log.v(HomeActivity.TAG, e.toString());
		}
	}
	
	public static void moveQueueItemTo(int from, int to) {
		long item = curQueue.remove(from);
		curQueue.add(to, item);
	}
	
	public static void removeQueueItem(int pos) {
		if (curQueue.size() < pos || pos < 0) {
			return;
		}
		curQueue.remove(pos);
		if ((curQueue.size()) == mCurPosition) {
			mCurPosition--;
		}		
	}

	protected void playPreviousSong(boolean show) {
		if (mPlayer == null) {
			return;
		}
		if (curQueue.size() == 0) {
			return;
		}
		if (mPlayer.getCurrentPosition() < MIN_MILSECS_TO_REPEAT && mCurPosition > 0) {
			mCurPosition--;
		}		
		playSong(curQueue.get(mCurPosition), show);
	}

	public static void toggleShuffle() {
		switch (mCurShuffleStatus) {
		case SHUFFLE_OFF:
			mCurShuffleStatus = SHUFFLE_ON;
			break;
		case SHUFFLE_ON:
			mCurShuffleStatus = SHUFFLE_OFF;
		default:
			break;
		}
	}

	public static void toggleRepeat() {
		switch (mCurRepeatStatus) {
		case REPEAT_OFF:
			mCurRepeatStatus = REPEAT_ON;
			break;
		case REPEAT_ON:
			mCurRepeatStatus = REPEAT_ALL;
			break;
		case REPEAT_ALL:
			mCurRepeatStatus = REPEAT_OFF;
			break;
		default:
			break;
		}
	}

	public static boolean isPlaying() {
		if (mPlayer != null) {
			return mPlayer.isPlaying();
		}		
		return false;
	}

	public void startSelfDestroySequence (final int timeout) {
		if (mPlayer == null) {
			return;
		}
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(timeout);
					if (!mPlayer.isPlaying()) {
						stopPlayBack();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}		
			}
		};
		new Thread(runnable).start();
	}

	public static long getCurTrack() {
		if (curQueue.size() == 0) {
			return 0;
		}
		return curQueue.get(mCurPosition);
	}

	public static long getCurTrackDur() {
		return mPlayer.getDuration();
	}

	public static long getCurTrackPos() {		
		return mPlayer.getCurrentPosition();
	}

	protected void playNextSong(boolean force) {
		Log.v(HomeActivity.TAG, "Value of showNotification: " + showNotifaction);
		playNextSong(force, showNotifaction);
	}

	@SuppressLint("NewApi")
	protected void playNextSong(boolean force, boolean show) {

		if (mPlayer == null) {
			return;
		}

		if (curQueue.size() == 0) {
			return;
		}

		if(mCurRepeatStatus == REPEAT_ON) {
			if (!force) {
				repeatSong(show);
				return;
			}			
		}

		else if (mCurRepeatStatus == REPEAT_ALL) {
			if (!force) {
				repeatQueue(show);
				return;
			}	
		} 

		if (mCurPosition == (curQueue.size() - 1)) {
			if (!mPlayer.isPlaying()) {
				if (showNotifaction) {
					mNotification.contentView.setImageViewResource(R.id.notification_button_play, R.drawable.button_play_holo);
					mNotificationManager.notify(mId, mNotification);
				}				
				Intent songFinishedIntent = new Intent(ACTION_PLAYBACK_FINISHED);
				getApplication().sendBroadcast(songFinishedIntent);
				startSelfDestroySequence(KILL_ME_TIMEOUT);
			}
			else {
				repeatQueue(show);
			}
			return;
		}

		mCurPosition++;
		playSong(curQueue.get(mCurPosition), show);		
	}
	
	private void repeatQueue (boolean show) {
		mCurPosition = 0;
		playSong(curQueue.get(mCurPosition), show);
	}

	private void repeatSong(boolean show) {
		playSong(curQueue.get(mCurPosition), show);
	}

	@SuppressLint("NewApi")
	protected void playOrPause(boolean show) {

		if (curQueue == null) {
			restoreQueue();
			playSong(curQueue.get(mCurPosition), show);
			return;
		}

		if (curQueue.size() == 0) {			
			return;
		}

		if(mPlayer == null) {
			playSong(curQueue.get(mCurPosition), show);
			return;
		}

		if(mPlayer.isPlaying()) {
			mPlayer.pause();
			if (show) {
				mNotification.contentView.setImageViewResource(R.id.notification_button_play, R.drawable.button_play_holo);				
				mNotificationManager.notify(mId, mNotification);
			}			
		}

		else {
			if (mCursor == null || mCursor.isClosed()) {
				playSong(curQueue.get(mCurPosition), show);
			}
			else {
				mPlayer.start();
				if (show) {
					mNotification.contentView.setImageViewResource(R.id.notification_button_play, R.drawable.button_pause_holo);
					//mNotification.bigContentView.setImageViewResource(R.id.expanded_notification_button_play, R.drawable.ic_pause_holo_dark);
					mNotificationManager.notify(mId, mNotification);
				}			
			}			
		}

	}

	protected void stopPlayBack() {
		if(mPlayer.isPlaying()) {
			mPlayer.reset();
			mPlayer.release();
			mPlayer = null;
		}		
		stopForeground(true);
		stopSelf();
	}

	public void updateCurQueue(long [] songs, boolean play) {
		int lastSongOnCurQueue = curQueue.size();
		int ph = 0;
		for (int i = 0; i < songs.length; i++) {
			if (!curQueue.contains(songs[i])) {				
				curQueue.add(lastSongOnCurQueue + ph, songs[i]);
				ph++;
			}					
		}

		notifyQueueChanged();

		if (mPlayer.isPlaying()) {
			return;
		}
		else {
			playSong(curQueue.get(lastSongOnCurQueue), false);
		}

	}	

	protected  void playSong(long songId, boolean show) {

		if (!curQueue.contains(songId)) {
			curQueue.add(songId);
			notifyQueueChanged();
		}

		mCurPosition = curQueue.indexOf(songId); 
		curSongId = songId;

		try {
			String selection = Audio.Media._ID + "=" + String.valueOf(songId);
			mCursor = mContext.getContentResolver().query(MusicManager.URI_SONGS, MusicManager.ALL_MEDIA_COLS, selection, null, null);
			if (mCursor.moveToFirst()) {			
				if (mPlayer == null) {
					initiatePlayer();
				}

				mPlayer.reset();
				mPlayer.setDataSource(mCursor.getString((mCursor.getColumnIndex(Audio.Media.DATA))));
				mPlayer.prepare();
				mPlayer.start();

				getCurTrackDetails();

				if (show) {
					showNotification();
				}
			}
		} catch (Exception e) {
			Log.v(HomeActivity.TAG, e.toString());
		}	

	}

	private String getImageUrl (String id) {

		Log.v(HomeActivity.TAG, "Loading Image url for: " + id);

		SharedPreferences mPreferences = getSharedPreferences("image-map", Context.MODE_PRIVATE);		
		String imageUrl = mPreferences.getString(id, ""); 

		if (imageUrl == null || imageUrl == "") {	//We don't yet have a preference for this image, never should happen, default to local
			imageUrl = "local";
		}

		if (imageUrl.equals("local")) {
			imageUrl = "content://media/external/audio/media/" + curSongId + "/albumart";			
		}

		Log.v(HomeActivity.TAG, "Url found: " + imageUrl);

		return imageUrl;

	}

	@SuppressLint("NewApi")
	public void showNotification() {	
		
		Log.v(HomeActivity.TAG, "Call to show notification received, building");

		if (mCursor.isClosed() || mCursor == null) {
			return;
		}

		if (mPlayer == null) {
			return;
		}	

		
		Bitmap bitmap = null;
		Drawable d;

		Log.v(HomeActivity.TAG, "Current song Id is: " + curSongId);

		d = UrlImageViewHelper.getImmediateMutableDrawable(getImageUrl(""+ curSongId));

		if (d != null) {
			Log.v(HomeActivity.TAG, "Drawable found for: " + curSongId);
			bitmap = ((BitmapDrawable)d).getBitmap();			
		}		
		
		
		//Build small notification

		RemoteViews views = new RemoteViews(getPackageName(), R.layout.player_notification);
		
		Intent mediaButtonIntent = new Intent(ACTION_PLAY_PAUSE);
		mediaButtonIntent.putExtra(EXTRA_BOOL_FORCE, true);		
		PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
				1, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);		
		views.setOnClickPendingIntent(R.id.notification_button_play, mediaPendingIntent);		

		mediaButtonIntent = new Intent(ACTION_NEXT);
		mediaButtonIntent.putExtra(EXTRA_NOTIFY, true);
		mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 2,
				mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.notification_button_next, mediaPendingIntent);

		mediaButtonIntent = new Intent(ACTION_STOP);
		mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 3,
				mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.notification_button_exit, mediaPendingIntent);		
		
		int playIconResource;

		if (isPlaying()) {
			playIconResource = R.drawable.button_pause_holo;
		} 

		else {
			playIconResource = R.drawable.button_play_holo;
		}		
		
		views.setImageViewResource(R.id.notification_button_play, playIconResource);
		
		views.setTextViewText(R.id.notification_song_artist, mCursor.getString(mCursor.getColumnIndex(Audio.Media.ARTIST)));
		views.setTextViewText(R.id.notification_song_title, mCursor.getString(mCursor.getColumnIndex(Audio.Media.TITLE)));
			
		if (bitmap != null) {
			Log.v(HomeActivity.TAG, "Updated bitmap");
			views.setImageViewBitmap(R.id.notification_default_drawable, bitmap);
		}
		else {
			views.setImageViewResource(R.id.notification_default_drawable, R.drawable.dummy_thumb);
		}

		mNotification = new Notification();		
		mNotification.flags = Notification.FLAG_ONGOING_EVENT;
		mNotification.icon = R.drawable.ic_launcher;
		mNotification.contentIntent = PendingIntent
				.getActivity(this, 0, new Intent(this, PlayerHolder.class)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), 0);	
		
		mNotification.contentView = views;
		
		startForeground(mId, mNotification);
	}

	@Override
	public void onCreate () {
		Log.v(HomeActivity.TAG, "Service Created");

		mContext = getApplicationContext();		

		IntentFilter commandFilter = new IntentFilter();
		commandFilter.addAction(ACTION_STOP);
		commandFilter.addAction(ACTION_NEXT);
		commandFilter.addAction(ACTION_PREVIOUS);
		commandFilter.addAction(ACTION_PLAY);
		commandFilter.addAction(ACTION_PLAY_PAUSE);
		commandFilter.addAction(Intent.ACTION_HEADSET_PLUG);
		commandFilter.addAction(ACTION_META_CHANGED);
		commandFilter.addAction(ACTION_PLAYLIST);
		commandFilter.addAction(ACTION_ENQUEUE);
		commandFilter.addAction(ACTION_CLEAR_QUEUE);
		commandFilter.addAction(ACTION_DISPLAY_PENDING_NOTIFICATION);	
		commandFilter.addAction(ACTION_CANCEL_NOTIFICATION);
		registerReceiver(mReceiver, commandFilter);

		mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

		if(mgr != null) {
			mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		}

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		curQueue = new ArrayList<Long>();
		restoreQueue();
		initiatePlayer();
	}

	private void initiatePlayer () {
		mPlayer = new MediaPlayer();
		mPlayer.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				playNextSong(false);				
			}
		});
	}

	@Override
	public void onDestroy () {
		Log.v(HomeActivity.TAG, "Service Destroyed");
		saveQueue();
		if (mCursor != null) {
			mCursor.close();
		}		
		mNotificationManager.cancel(mId);
		unregisterReceiver(mReceiver);
		mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
	}

	@Override
	public void onLowMemory() {
		Log.v(HomeActivity.TAG, "Saving Queue");
		saveQueue();		
	}	

	private void saveQueue() {
		SharedPreferences prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE);
		Editor mEditor = prefs.edit();
		mEditor.clear();
		for (int i = 0; i < curQueue.size(); i++) {
			mEditor.putLong(String.valueOf(i), curQueue.get(i));
		}
		mEditor.putInt("queuesize", curQueue.size());
		mEditor.putInt("lastSongPlayed", mCurPosition);
		mEditor.commit();
	}

	private void restoreQueue () {
		SharedPreferences prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE);
		int size = prefs.getInt("queuesize", 0);
		if (size == 0) {
			return;
		}
		for (int i = 0; i < size; i++) {
			curQueue.add(prefs.getLong(String.valueOf(i), 0));
		}		

		mCurPosition = prefs.getInt("lastSongPlayed", 0);
		//This causes the broadcast to be sent twice, and therefore the cursors are updated twice, but otherwise HomeActivity seems not to catch it a first. 
		notifyQueueChanged(); 						
	}

	private void clearQueue () {
		if (curQueue != null) {
			if (mPlayer.isPlaying()) {
				mPlayer.pause();
				mPlayer.reset();
			}
			curQueue.clear();
			mCurPosition = 0;
			notifyQueueChanged();
			saveQueue();
		}		
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	public static void getCurTrackDetails() {
		if (mCursor == null || curQueue.size() == 0) {
			return;
		}
		Bundle bundle = new Bundle();
		bundle.putString(PlayerFragment.TRACK_TITLE, mCursor.getString(mCursor.getColumnIndex(Audio.Media.TITLE)));
		bundle.putLong(PlayerFragment.TRACK_DURATION, mPlayer.getDuration());
		bundle.putString(PlayerFragment.TRACK_ARTIST, mCursor.getString(mCursor.getColumnIndex(Audio.Media.ARTIST)));
		bundle.putString(PlayerFragment.TRACK_ART, mCursor.getString(mCursor.getColumnIndex(Audio.Media._ID)));	
		bundle.putString(PlayerFragment.TRACK_ID, mCursor.getString(mCursor.getColumnIndex(Audio.Media._ID)));
		Intent songFinishedIntent = new Intent(ACTION_SONG_CHANGED);
		songFinishedIntent.putExtras(bundle);
		mContext.sendBroadcast(songFinishedIntent);
	}

	public static Bundle getCurTrackBundle() {
		if (mCursor == null || curQueue.size() == 0) {
			return null;
		}
		Bundle bundle = new Bundle();
		bundle.putString(PlayerFragment.TRACK_TITLE, mCursor.getString(mCursor.getColumnIndex(Audio.Media.TITLE)));
		bundle.putString(PlayerFragment.TRACK_ALBUM, mCursor.getString(mCursor.getColumnIndex(Audio.Media.ALBUM)));
		bundle.putString(PlayerFragment.TRACK_ARTIST, mCursor.getString(mCursor.getColumnIndex(Audio.Media.ARTIST)));
		bundle.putString(PlayerFragment.TRACK_ID, mCursor.getString(mCursor.getColumnIndex(Audio.Media._ID)));
		return bundle;
	}

	public static void notifyQueueChanged () {
		if (curQueue == null) {
			return;
		}		
		Intent broadcast = new Intent();
		broadcast.setAction(ACTION_CURRENT_QUEUE_CHANGED);
		if (curQueue.size() > 0) {
			long [] queue = new long [curQueue.size()];
			for (int i = 0; i < curQueue.size(); i++) {
				queue[i] = curQueue.get(i);
				broadcast.putExtra("queue", queue);
			}
		}		
		mContext.sendBroadcast(broadcast);
	}

	public  void changePlaylist(long playlistId) {
		if (mPlayer.isPlaying()) {
			mPlayer.reset();
		}		
		mCurPosition = 0;	
		curQueue = MusicManager.getSongsFromPlaylist(mContext, playlistId);		
		notifyQueueChanged();
		if (curQueue.size() != 0) {
			playSong(curQueue.get(mCurPosition), false);
		}		
		saveQueue();	//We wouldn't want the user to go back to the old queue in case service is destroyed. 
	}

	public static void seekTo(int newPos) {
		if (mPlayer == null) {
			return;
		}
		mPlayer.seekTo(newPos);
	}

}