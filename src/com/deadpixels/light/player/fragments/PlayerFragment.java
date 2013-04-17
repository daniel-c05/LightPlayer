package com.deadpixels.light.player.fragments;

import java.lang.ref.WeakReference;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.deadpixels.light.player.HomeActivity;
import com.lazybitz.beta.light.player.R;
import com.deadpixels.light.player.SettingsActivity;
import com.deadpixels.light.player.service.MyMusicService;
import com.deadpixels.light.player.utils.LastFmUtils;
import com.deadpixels.light.player.utils.MusicManager;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

@SuppressLint("HandlerLeak")
public class PlayerFragment extends Fragment {

	public static final String ACTION_UPDATE_ART = "com.lazybitz.commands.update.art";

	public static final int REFRESH = 0;
	public static final int UPDATEINFO = 1;

	public static final int LOCAL = 0;
	public static final int ARTIST = 1;
	public static final int ALBUM = 2;

	public static final String TRACK_TITLE = "track";
	public static final String TRACK_ARTIST = "artist";
	public static final String TRACK_ALBUM = "album";
	public static final String TRACK_ID = "ID";
	public static final String TRACK_DURATION = "duration";
	public static final String TRACK_ART = "art";
	public static final String TRACK_IS_FAVE = "fave";	
	public static final String TRACK_WHAT = "what";
	public static final String TRACK_VALUE = "value";
	public static final String TRACK_EXTRA = "extra";

	private ImageButton buttonPlay, buttonPrevious, buttonNext, buttonShuffle, buttonRepeat;
	private ImageView songArt;
	private TextView textTitle, textArtist, textCurPos, textSongDur;
	public SeekBar seekBar;

	public static boolean needsToRun = false;
	boolean areDownloadsDisabled;

	private static long songDuration, curPosInMs;

	final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(MyMusicService.ACTION_PLAYBACK_FINISHED)) {
				buttonPlay.setImageResource(R.drawable.ic_play_holo_dark);
				needsToRun = false;
			}
			else if (action.equals(MyMusicService.ACTION_SONG_CHANGED)) {
				needsToRun = true;
				updateMusicInfo(intent);				
			}

			else if (action.equals(ACTION_UPDATE_ART)) {
				final int what = intent.getIntExtra(TRACK_WHAT, -1);
				final String songId = intent.getStringExtra(TRACK_ID);
				final String value = intent.getStringExtra(TRACK_VALUE);
				final String extra = intent.getStringExtra(TRACK_EXTRA);
				updateArt(context, songId, what, value, extra);				
			}

		}
	};	 

	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case REFRESH:
				long next = refreshNow();
				queueNextRefresh(next);
				break;                
			default:
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		areDownloadsDisabled = sharedPref.getBoolean(SettingsActivity.KEY_DISABLE_DOWNLOADS, false);

		IntentFilter mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(MyMusicService.ACTION_PLAYBACK_FINISHED);
		mIntentFilter.addAction(MyMusicService.ACTION_SONG_CHANGED);
		mIntentFilter.addAction(ACTION_UPDATE_ART);
		getActivity().registerReceiver(mReceiver, mIntentFilter);		

		long next = refreshNow();
		queueNextRefresh(next);
	}

	protected void updateArt(Context context, String songId, int what, String value, String extra) {
		if (what == LOCAL) {
			setAlbumArt(songArt, songId);
			saveArtPreference(context, songId, "local");
		}
		else {
			new setLastFmImage(context, songId, songArt, what, value).execute(extra);
		}		
	}
	
	private void saveArtPreference(Context context, String songId, String value) {
		if (songId == null || songId == "") {
			return;
		}
		SharedPreferences mPreferences = context.getSharedPreferences("image-map", Context.MODE_PRIVATE);
		Editor mEditor = mPreferences.edit();
		mEditor.putString(songId, value);
		mEditor.commit();
	}
	
	private String getArtPreference(String id) {
		SharedPreferences mPreferences = getActivity().getSharedPreferences("image-map", Context.MODE_PRIVATE);		
		return mPreferences.getString(id, ""); 
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View mView = inflater.inflate(R.layout.player_fragment, container, false);
		
		initViews(mView);

		MyMusicService.getCurTrackDetails();

		if (MyMusicService.isPlaying()) {
			needsToRun = true;
			buttonPlay.setImageResource(R.drawable.ic_play_holo_light);
		}
		else {
			buttonPlay.setImageResource(R.drawable.ic_play_holo_dark);
		}		

		return mView;	        
	}

	@Override
	public void onStop() {
		needsToRun = false;
		super.onStop();
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.v(HomeActivity.TAG, "Resuming");
		if (MyMusicService.isPlaying()) {
			needsToRun = true;			
		}
		else {	//most likely we need to re-start the service
			buttonPlay.setImageResource(R.drawable.ic_play_holo_dark);
		} 					
		refreshNow();
	}

	@Override
	public void onDestroy() {
		getActivity().unregisterReceiver(mReceiver);
		mHandler.removeMessages(REFRESH);
		super.onDestroy();
	}

	/**
	 * @author Andrew Neal
	 * @param delay
	 */
	private void queueNextRefresh(long delay) {
		Message msg = mHandler.obtainMessage(REFRESH);
		mHandler.removeMessages(REFRESH);
		mHandler.sendMessageDelayed(msg, delay);
	}

	/**
	 * I tried to get this to work on my own, but I sincerely could not figure out a better way than this. 
	 * This is a fairly stripped down version of the code he has available at 
	 * <a href="https://github.com/CyanogenMod/android_packages_apps_Apollo/blob/ics/src/com/andrew/apollo/AudioPlayerFragment.java"> github </a>
	 * @author Andrew Neal
	 * @return The current time
	 */
	protected long refreshNow() {

		if (!needsToRun) {
			return 1000;
		}

		if (songDuration == 0) {
			return 1000;
		}

		if (MyMusicService.isPlaying() && songDuration != 0) {			
			curPosInMs = MyMusicService.getCurTrackPos();
			int progress = ((int) (100 * curPosInMs /songDuration));
			if (textCurPos != null && seekBar != null) {	//Fixes FC when onConfigurationChanged.
				textCurPos.setText(MusicManager.getReadableDuration(curPosInMs));
				seekBar.setProgress(progress);			
			}			
		}
		else {
			buttonPlay.setImageResource(R.drawable.button_play_holo);
		}
		return 1000;
	}

	private void initViews(View mView) {

		buttonPlay = (ImageButton) mView.findViewById(R.id.button_play);
		buttonPrevious  = (ImageButton) mView.findViewById(R.id.button_previous);
		buttonNext  = (ImageButton) mView.findViewById(R.id.button_next);
		buttonShuffle  = (ImageButton) mView.findViewById(R.id.button_shuffle);
		buttonRepeat  = (ImageButton) mView.findViewById(R.id.button_repeat);

		textTitle = (TextView) mView.findViewById(R.id.player_song_title);
		textArtist = (TextView) mView.findViewById(R.id.player_song_artist);
		textCurPos = (TextView) mView.findViewById(R.id.cur_time);
		textSongDur = (TextView) mView.findViewById(R.id.total_time);
		songArt = (ImageView) mView.findViewById(R.id.player_art);

		seekBar = (SeekBar) mView.findViewById(R.id.slider);

		seekBar.setMax(100);

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			int mProgress;

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mProgress = seekBar.getProgress();
				seekTo(mProgress);
				refreshNow();
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				mProgress = seekBar.getProgress();
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
			}
		});


		buttonPlay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendBroadcastToService(MyMusicService.ACTION_PLAY_PAUSE, MyMusicService.EXTRA_BOOL_FORCE, false);
				if(MyMusicService.isPlaying() || MyMusicService.getCurTrack() == 0) {
					buttonPlay.setImageResource(R.drawable.ic_play_holo_dark);
					needsToRun = false;
				}
				else {
					buttonPlay.setImageResource(R.drawable.ic_pause_holo_dark);
					needsToRun = true;
				}
			}
		});

		buttonPrevious.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendBroadcastToService(MyMusicService.ACTION_PREVIOUS);
			}
		});

		buttonNext.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendBroadcastToService(MyMusicService.ACTION_NEXT, MyMusicService.EXTRA_BOOL_FORCE, false);
			}
		});

		buttonShuffle.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				switch (MyMusicService.mCurShuffleStatus) {
				case MyMusicService.SHUFFLE_OFF:
					buttonShuffle.setImageResource(R.drawable.ic_shuffle_mode_one);
					break;
				case MyMusicService.SHUFFLE_ON:
					buttonShuffle.setImageResource(R.drawable.ic_shuffle_holo_dark);
					break;
				default:
					break;
				}
				HomeActivity.sendMessage(MyMusicService.MSG_SHUFFLE_STATE_CHANGED);		
			}
		});

		buttonRepeat.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				switch (MyMusicService.mCurRepeatStatus) {
				case MyMusicService.REPEAT_OFF:
					buttonRepeat.setImageResource(R.drawable.ic_repeat_mode_one);
					break;
				case MyMusicService.REPEAT_ON:
					buttonRepeat.setImageResource(R.drawable.ic_repeat_mode_all);
					break;
				case MyMusicService.REPEAT_ALL:
					buttonRepeat.setImageResource(R.drawable.ic_repeat_holo_dark);
				default:
					break;
				}
				HomeActivity.sendMessage(MyMusicService.MSG_REPEAT_STATE_CHANGED);
			}
		});

	}

	protected void seekTo(int progress) {
		int newPos = ((int) (songDuration * progress / 100));
		MyMusicService.seekTo(newPos);
		refreshNow();
	}

	protected void updateMusicInfo(Intent intent) {	
		
		textTitle.setText(intent.getStringExtra(TRACK_TITLE));
		textArtist.setText(intent.getStringExtra(TRACK_ARTIST));
		buttonPlay.setImageResource(R.drawable.ic_pause_holo_dark);

		String artUrl = getArtPreference(intent.getStringExtra(TRACK_ART));
		
		if (artUrl == "" || artUrl.equals("local")) {
			setAlbumArt(songArt, intent.getStringExtra(TRACK_ART));
		}
		else {
			UrlImageViewHelper.setUrlDrawable(songArt, artUrl, R.drawable.dummy_art);
		}
		
		songDuration = MyMusicService.getCurTrackDur();

		seekBar.setProgress(0);	
		seekBar.setMax(100);

		textSongDur.setText(MusicManager.getReadableDuration(songDuration));

		refreshNow();

	}

	private void setAlbumArt(ImageView view, String id) {
		String path = "content://media/external/audio/media/" + id + "/albumart";
		UrlImageViewHelper.setUrlDrawable(view, path, R.drawable.dummy_art);		
	}

	public void sendBroadcastToService (String action) {
		sendBroadcastToService(action, null, -1);
	}

	public void sendBroadcastToService (String action, String key, boolean value) {
		Intent intent = new Intent();
		intent.setAction(action);
		intent.putExtra(key, value);
		getActivity().sendBroadcast(intent);
	}


	public void sendBroadcastToService (String action, String key, int value) {
		Intent intent = new Intent();
		intent.setAction(action);
		if (key != null) {
			intent.putExtra(key, value);
		}
		getActivity().sendBroadcast(intent);
	}

	private class setLastFmImage extends AsyncTask<String, Void, String> {

		Context mContext;
		int what;
		String id;
		String artist;
		WeakReference<ImageView> mImageReference;

		public setLastFmImage (Context context, String songId, ImageView iv, int what, String artist) {
			this.mContext = context;
			this.what = what;
			this.id = songId;
			this.artist = artist;					
			mImageReference = new WeakReference<ImageView>(iv);
		}

		@Override
		protected String doInBackground(String... params) {
			String method = "";
			final String extra = params[0];
			
			switch (what) {
			case LOCAL:
				method = TRACK_TITLE;
				break;
			case ARTIST:
				method = TRACK_ARTIST;
				break;
			case ALBUM:
				method = TRACK_ALBUM;
				break;
			default:
				break;
			}

			SharedPreferences mPreferences = mContext.getSharedPreferences("image-map", Context.MODE_PRIVATE);
			Editor mEditor = mPreferences.edit();
			String imageUrl;

			if (method == TRACK_ARTIST) {
				imageUrl = mPreferences.getString("artist-" + artist, "");	
			}
			else {
				imageUrl = mPreferences.getString(method + "-" + extra, "");
			}

			if (!imageUrl.equals("") && imageUrl != null) {
				return imageUrl;
			}
			
			imageUrl = LastFmUtils.getImageUrl(method, artist, extra, 4);
			
			if (imageUrl != null && imageUrl.length() > 2) {
				if (method == TRACK_ARTIST) {
					mEditor.putString("artist-" + artist, imageUrl);
				}
				else {
					mEditor.putString(method + "-" + extra, imageUrl);
				}				
				mEditor.commit();
			}						
			return imageUrl;
		}

		@Override
		protected void onPostExecute(String result) {
			if (mImageReference.get() != null && result != null && result != "") {
				saveArtPreference(mContext, id, result);
				UrlImageViewHelper.setUrlDrawable(mImageReference.get(), result, R.drawable.dummy_art);
			}			
		}
	}

}
