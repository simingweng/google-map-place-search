package com.bnj.google.map.placesearch.library;


import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

public class PlaceAutoCompleteContentProvider extends ContentProvider {

    private static final String TAG = PlaceAutoCompleteContentProvider.class
            .getName();
    private static final String GOOGLE_PLACE_API_KEY = "AIzaSyDpryIy62fGHzSSFjnYlsVTXTTWEm1aZ6c";
    private static final String GOOGLE_PLACE_AUTOCOMPLETE_URL_BASE = "https://maps.googleapis.com/maps/api/place/autocomplete/json?";

    private static final String[] suggestionColumns = {BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA};

    public PlaceAutoCompleteContentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO: Implement this to handle requests to insert a new row.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onCreate() {
        // TODO: Implement this to initialize your content provider on startup.
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        String query = uri.getLastPathSegment().toLowerCase(Locale.US);
        if (!query.equals(SearchManager.SUGGEST_URI_PATH_QUERY)) {
            Log.i(TAG, "user is querying " + query);
            String encodedQuery = null;
            try {
                encodedQuery = URLEncoder.encode(query, "UTF-8");
            } catch (UnsupportedEncodingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            StringBuilder builder = new StringBuilder(
                    GOOGLE_PLACE_AUTOCOMPLETE_URL_BASE);
            builder.append("input=" + encodedQuery).append("&sensor=true")
                    .append("&key=" + GOOGLE_PLACE_API_KEY);
            StringBuilder repsponseString = new StringBuilder();
            HttpURLConnection conn = null;
            try {
                URL url = new URL(builder.toString());
                conn = (HttpURLConnection) url.openConnection();
                InputStreamReader reader = new InputStreamReader(
                        conn.getInputStream());
                // Load the results into a StringBuilder
                int read;
                char[] buff = new char[1024];
                while ((read = reader.read(buff)) != -1) {
                    repsponseString.append(buff, 0, read);
                }
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            if (repsponseString.length() == 0) {
                return null;
            }

            try {
                // Create a JSON object hierarchy from the results
                JSONObject jsonObj = new JSONObject(repsponseString.toString());
                JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

                MatrixCursor cursor = new MatrixCursor(suggestionColumns);
                for (int i = 0; i < predsJsonArray.length(); i++) {
                    String description = predsJsonArray.getJSONObject(i)
                            .getString("description");
                    String reference = predsJsonArray.getJSONObject(i)
                            .getString("reference");
                    cursor.newRow().add(i).add(description).add(reference);
                }
                return cursor;
            } catch (JSONException e) {
                Log.e(TAG, "Cannot process JSON results", e);
            }
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
