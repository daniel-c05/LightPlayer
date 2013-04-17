package com.deadpixels.light.player.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.deadpixels.light.player.HomeActivity;

public class LastFmUtils {

	public static final String API_KEY = "1ee2c13883308f958e2ec6db96bf80e2";
	public static final String API_SECRET = "456e00ea7524440db94b5018028e448e";

	public static final String REQUEST_BASE = "http://ws.audioscrobbler.com/2.0/?";

	public static JSONObject getJsonResponse (final String uri) {				

		StringBuilder mBuilder = new StringBuilder();

		HttpClient mClient = new DefaultHttpClient();
		HttpGet mGet = new HttpGet(uri);
		
		JSONObject json = null;

		try {
			HttpResponse response = mClient.execute(mGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					mBuilder.append(line);// + "\n");
				}
				content.close();
				json = new JSONObject(mBuilder.toString());
			} else {
				Log.e(LastFmUtils.class.toString(), "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		
		return json;

	}

	public static class Artist {

		public static final String OBJECT_ARTIST = "artist";
		public static final String ARRAY_IMAGE = "image";
		public static final String OBJECT_TEXT = "#text";
		public static final int IMAGE_SIZE_EXTRA_LARGE = 3;
		public static final int IMAGE_SIZE_LARGE = 2;
		public static final String OBJECT_BIO = "bio";
		public static final String OBJECT_CONTENT = "content";
		public static final String image = "";

		/**
		 * 
		 * @param artist The artist you want the image Url for. 
		 * @param imageSize	The Image Size you need. 2 for Large, 3 for Extra Large, 4 for Mega. 
		 * @return
		 */
		public static String getArtistImageUrl (final String artist, final int imageSize) {
			
			if (artist.contains(">") || artist.contains("<")) {				
				return null;					
			}

			final String mMethod = "method=artist.getinfo&";
			final String mArtist = "artist=" + fixString(artist);
			final String mKey = "&api_key=" + API_KEY;
			final String format = "&format=json";

			String uri = REQUEST_BASE + mMethod + mArtist + mKey + format;
			
			try {

				final JSONObject json = getJsonResponse(uri);
				JSONObject jArtist = json.getJSONObject(OBJECT_ARTIST);
				JSONArray jImage = jArtist.getJSONArray(ARRAY_IMAGE);
				
				String imageUrl = jImage.getJSONObject(imageSize).getString(OBJECT_TEXT);
				return imageUrl;

			}
			catch (Exception e) {
				e.printStackTrace();
			}		
			
			return null;
			
		}	
		
		public static String getArtistImageUrl (final String artist) {
			
			if (artist.contains(">") || artist.contains("<")) {				
				return null;					
			}

			final String mMethod = "method=artist.getinfo&";
			final String mArtist = "artist=" + artist;
			final String mKey = "&api_key=" + API_KEY;
			final String format = "&format=json";

			String uri = REQUEST_BASE + mMethod + mArtist + mKey + format;			
			
			try {

				final JSONObject json = getJsonResponse(uri);
				JSONObject jArtist = json.getJSONObject(OBJECT_ARTIST);
				JSONArray jImage = jArtist.getJSONArray(ARRAY_IMAGE);
				
				String imageUrl = jImage.getJSONObject(IMAGE_SIZE_EXTRA_LARGE).getString(OBJECT_TEXT);
				return imageUrl;

			}
			catch (Exception e) {
				e.printStackTrace();
			}		
			
			return null;
			
		}	
		
		public static String getArtistBio (final String artist) {

			final String mMethod = "method=artist.getinfo&";
			final String mArtist = "artist=" + artist;
			final String mKey = "&api_key=" + API_KEY;
			final String format = "&format=json";

			String uri = REQUEST_BASE + mMethod + mArtist + mKey + format;
			Log.v(HomeActivity.TAG, uri);
			
			try {

				final JSONObject json = getJsonResponse(uri);
				JSONObject jArtist = json.getJSONObject(OBJECT_ARTIST);
				JSONObject jBio = jArtist.getJSONObject(OBJECT_BIO);
				
				String content = jBio.getString(OBJECT_CONTENT);
				return content;

			}
			catch (Exception e) {
				e.printStackTrace();
			}		
			
			return null;
			
		}	
		
		/**
		 * Takes a string and replaces any empty space for "%20" as otherwise doing an API call with spaces would return in error. 
		 * @param word The word to be "fixed"
		 * @return The same word with replaced spaces, or the same word unchanged if no empty spaces found. 
		 */
		public static String fixString(String word) {
			final String spaceChar = "%20";
			word = word.replace(" ", spaceChar);
			return word;
		}
		
	}

	public static String getImageUrl(String method, String artist, String extra, int i) {
		
		Log.v(HomeActivity.TAG, "Calling lastFm api");
		
		if (artist.contains(">") || artist.contains("<") || artist == null) {				
			return null;					
		}

		final String mMethod = "method=" + method + ".getinfo&";
		final String mArtist = "artist=" + Artist.fixString(artist);		
		final String mKey = "&api_key=" + API_KEY;
		final String format = "&format=json";		
		final String mExtraParam;
		String uri;
		if (method == "artist") {
			uri = REQUEST_BASE + mMethod + mArtist + mKey + format;
		}		
		else {			
			mExtraParam = "&" + method + "=" + Artist.fixString(extra);
			uri = REQUEST_BASE + mMethod + mArtist + mExtraParam + mKey + format;
		}					
		
		try {

			final JSONObject json = getJsonResponse(uri);			
			JSONObject jRoot;
			JSONObject jAlbum;
			JSONArray jImage;
			if (method == "track") {
				jRoot = json.getJSONObject("track");
				jAlbum = json.getJSONObject("album");
				jImage = jAlbum.getJSONArray(Artist.ARRAY_IMAGE);
			}
			else {
				jRoot = json.getJSONObject(method);
				jImage = jRoot.getJSONArray(Artist.ARRAY_IMAGE);
			}
						
			String imageUrl = jImage.getJSONObject(Artist.IMAGE_SIZE_EXTRA_LARGE).getString(Artist.OBJECT_TEXT);
			return imageUrl;

		}
		catch (Exception e) {
			e.printStackTrace();
		}				
		return null;
	}
}
