package com.deadpixels.light.player.fragments;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.lazybitz.beta.light.player.R;

public class SettingsFragment extends PreferenceFragment {
	
	public static final String ACTION_PREF_CHANGED = "com.deadpixels.preference.changed";
	public static final String EXTRA_PREF_NAME = "pref_name";
	public static final String EXTRA_PREF_VALUE = "pref_value";
	
	public static final String KEY_PREF_CUR_THEME = "pref_key_current_theme";
	public static final String KEY_PREF_DISABLE_DOWNLOADS = "pref_key_disable_art_downloads";
	public static final String KEY_PREF_SWIPE_ = "pref_key_swipe_menu_from_right";
	public static final String KEY_PREF_ADD_ALL_TO_QUEUE = "pref_key_add_all_to_queue";
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);   
        
        findPreference(KEY_PREF_DISABLE_DOWNLOADS).setOnPreferenceChangeListener(prefChangeListener);
        findPreference(KEY_PREF_ADD_ALL_TO_QUEUE).setOnPreferenceChangeListener(prefChangeListener);
        
    }
	
	/**
	 * This boy will look after any possible changes to the existing preferece values and will notifify the necessary
	 * members of such changes. 
	 */
	private static Preference.OnPreferenceChangeListener prefChangeListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			//String stringValue = value.toString();
			return true;
		}
	};

}
