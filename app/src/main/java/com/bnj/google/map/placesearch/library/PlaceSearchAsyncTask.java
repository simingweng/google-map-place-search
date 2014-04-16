package com.bnj.google.map.placesearch.library;

/**
 *
 */

import android.app.AlertDialog;
import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author simingweng
 */
public class PlaceSearchAsyncTask extends
        AsyncTask<String, Void, List<Address>> {

    private static final String TAG = PlaceSearchAsyncTask.class.getName();
    private static final String GOOGLE_PLACE_API_KEY = "AIzaSyDpryIy62fGHzSSFjnYlsVTXTTWEm1aZ6c";
    private static final String PLACE_REFERENCE_EXTRA_KEY = "com.bnj.google.map.placesearch.extra.REFERENCE";
    private Context mContext;
    private PlaceSearchType searchType;
    private PlaceSearchResultListener mListener;
    private Location mLocation;
    private int mRadius;

    public interface PlaceSearchResultListener {
        public void onPlaceSearchCompleted(List<Address> results);
    }

    /**
     *
     */
    public PlaceSearchAsyncTask(Context context, PlaceSearchType type,
                                Location location, int radius, PlaceSearchResultListener listener) {
        mContext = context;
        searchType = type;
        mLocation = location;
        mRadius = radius;
        mListener = listener;
    }

    @Override
    protected List<Address> doInBackground(String... params) {
        StringBuilder builder = new StringBuilder(searchType.getUrlBase());
        switch (searchType) {
            case TEXT:
                try {
                    builder.append("input=" + URLEncoder.encode(params[0], "UTF-8"));
                } catch (UnsupportedEncodingException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                break;
            case DETAILS:
                builder.append("reference=" + params[0]);
                break;
        }
        builder.append("&sensor=true").append("&key=" + GOOGLE_PLACE_API_KEY);
        if (mLocation != null) {
            builder.append(
                    "&location=" + mLocation.getLatitude() + ","
                            + mLocation.getLongitude()).append(
                    "&radius=" + mRadius);
        }

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
            cancel(true);
            return null;
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(repsponseString.toString());
            JSONArray resultJsonArray = new JSONArray();
            switch (searchType) {
                case TEXT:
                    resultJsonArray = jsonObj.getJSONArray("results");
                    break;
                case DETAILS:
                    resultJsonArray.put(jsonObj.getJSONObject("result"));
                    break;
            }

            List<Address> addresses = new ArrayList<Address>();
            for (int i = 0; i < resultJsonArray.length(); i++) {
                JSONObject result = resultJsonArray.getJSONObject(i);
                Address address = new Address(Locale.US);
                address.setFeatureName(result.optString("name"));
                address.setAddressLine(0, result.optString("formatted_address"));
                address.setLatitude(result.getJSONObject("geometry")
                        .getJSONObject("location").getDouble("lat"));
                address.setLongitude(result.getJSONObject("geometry")
                        .getJSONObject("location").getDouble("lng"));
                address.setPhone(result.optString("international_phone_number"));
                address.setUrl(result.optString("website"));
                Bundle bundle = new Bundle();
                bundle.putString(PLACE_REFERENCE_EXTRA_KEY,
                        result.optString("reference"));
                address.setExtras(bundle);
                addresses.add(address);
            }

            return addresses;
        } catch (JSONException e) {
            Log.e(TAG, "Cannot process JSON results", e);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(List<Address> result) {
        super.onPostExecute(result);
        if (mListener != null) {
            mListener.onPlaceSearchCompleted(result);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.os.AsyncTask#onCancelled()
     */
    @Override
    protected void onCancelled() {
        new AlertDialog.Builder(mContext)
                .setMessage(R.string.text_place_seart_failure)
                .setTitle(R.string.title_place_search_failure)
                .setIcon(R.drawable.ic_dialog_warning)
                .setPositiveButton("OK", null).create().show();
    }

    public enum PlaceSearchType {
        TEXT("https://maps.googleapis.com/maps/api/place/textsearch/json?"), DETAILS(
                "https://maps.googleapis.com/maps/api/place/details/json?");

        private String urlBase;

        PlaceSearchType(String urlBase) {
            this.urlBase = urlBase;
        }

        /**
         * @return the pathElement
         */
        public String getUrlBase() {
            return urlBase;
        }

    }

}
