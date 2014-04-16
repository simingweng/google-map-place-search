package com.bnj.google.map.placesearch.library;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Address;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.widget.SearchView;

import com.bnj.google.map.placesearch.library.PlaceSearchAsyncTask.PlaceSearchResultListener;
import com.bnj.google.map.placesearch.library.PlaceSearchAsyncTask.PlaceSearchType;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends FragmentActivity implements
        ConnectionCallbacks, OnConnectionFailedListener {

    // Global constants
    /*
     * Define a request code to send to Google Play services This code is
	 * returned in Activity.onActivityResult
	 */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final String TAG = MapActivity.class.getName();
    private static final String ACTION_SEARCH_DETAIL = "com.bnj.place.action.SEARCH_DETAIL";
    private static final String ADDRESS_EXTRA_KEY = "com.bnj.google.map.placesearch.library.extra.PLACE";
    private static final String INITIAL_LOCATION_EXTRA_KEY = "com.bnj.google.map.placesearch.library.extra.INITIAL_LOCATION";
    private GoogleMap mMap;
    private LocationClient mLocationClient;
    private PlaceSearchResultListener mPlaceSearchListener;
    private Map<Marker, Address> mSearchResultMarkers = new HashMap<Marker, Address>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (savedInstanceState == null) {
            mapFragment.setRetainInstance(true);
        } else {
            mMap = mapFragment.getMap();
        }
        setUpMapIfNeeded();
        setUpMapListeners();

        mLocationClient = new LocationClient(this, this, this);
        mPlaceSearchListener = new PlaceSearchListener();

        if (servicesConnected()) {
            // Connect the client.
            mLocationClient.connect();
        }
        if (getIntent() != null
                && getIntent().hasExtra(INITIAL_LOCATION_EXTRA_KEY)) {
            Address address = getIntent().getParcelableExtra(
                    INITIAL_LOCATION_EXTRA_KEY);
            LatLng initialLocation = new LatLng(address.getLatitude(),
                    address.getLongitude());
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(initialLocation)
                    .icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .title(address.getFeatureName())
                    .snippet(getString(R.string.snippet_search_result_marker)));
            mSearchResultMarkers.put(marker, address);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    initialLocation, 18f));
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.FragmentActivity#
     * onRetainCustomNonConfigurationInstance()
     */
    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.support.v4.app.FragmentActivity#onNewIntent(android.content.Intent
     * )
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
            Log.i(TAG,
                    "search for place "
                            + intent.getStringExtra(SearchManager.QUERY));
            new PlaceSearchAsyncTask(this, PlaceSearchType.TEXT,
                    mMap.getMyLocation(), 50 * 1000, mPlaceSearchListener)
                    .execute(intent.getStringExtra(SearchManager.QUERY));
        } else if (intent.getAction().equals(ACTION_SEARCH_DETAIL)) {
            Log.i(TAG, "search for detail of a suggested place by reference "
                    + intent.getStringExtra(SearchManager.EXTRA_DATA_KEY));
            new PlaceSearchAsyncTask(this, PlaceSearchType.DETAILS,
                    mMap.getMyLocation(), 50 * 1000, mPlaceSearchListener)
                    .execute(intent
                            .getStringExtra(SearchManager.EXTRA_DATA_KEY));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.FragmentActivity#onStop()
     */
    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        mLocationClient.disconnect();
        super.onStop();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        MenuItem searchItem = menu.findItem(R.id.search_address);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchItem.setOnActionExpandListener(new OnActionExpandListener() {

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mMap.clear();
                mSearchResultMarkers.clear();
                return true;
            }

        });
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));
        return super.onCreateOptionsMenu(menu);
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the
        // map.
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.mapFragment)).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                Log.i(TAG, "set initial configuration of google map");
                // The Map is verified. It is now safe to manipulate the map.
                mMap.getUiSettings().setTiltGesturesEnabled(false);
                mMap.getUiSettings().setCompassEnabled(true);
                mMap.setMyLocationEnabled(true);
                mMap.setBuildingsEnabled(true);
            }
        }
    }

    private void setUpMapListeners() {

        mMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick(Marker marker) {
                Intent intent = new Intent();
                intent.putExtra(ADDRESS_EXTRA_KEY,
                        mSearchResultMarkers.get(marker));
                setResult(RESULT_OK, intent);
                finish();
            }

        });
    }

    private class PlaceSearchListener implements PlaceSearchResultListener {
        @Override
        public void onPlaceSearchCompleted(List<Address> results) {
            if (results == null) {
                return;
            }
            for (Marker marker : mSearchResultMarkers.keySet()) {
                marker.remove();
            }
            mSearchResultMarkers.clear();
            LatLngBounds.Builder builder = LatLngBounds.builder();
            for (Address address : results) {
                Marker marker = mMap
                        .addMarker(new MarkerOptions()
                                .position(
                                        new LatLng(address.getLatitude(),
                                                address.getLongitude()))
                                .icon(BitmapDescriptorFactory
                                        .defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                .title(address.getFeatureName())
                                .snippet(
                                        getString(R.string.snippet_search_result_marker)));
                builder.include(marker.getPosition());
                mSearchResultMarkers.put(marker, address);
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                    builder.build(), 50));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int,
     * android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Decide what to do based on the original request code
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
			/*
			 * If the result code is Activity.RESULT_OK, try to connect again
			 */
                switch (resultCode) {
                    case FragmentActivity.RESULT_OK:
                        servicesConnected();
                        break;
                }
        }
    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates", "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            showErrorDialog(resultCode);
            return false;
        }
    }

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
        if (result.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                result.startResolutionForResult(this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
			/*
			 * If no resolution is available, display a dialog to the user with
			 * the error.
			 */
            showErrorDialog(result.getErrorCode());
        }
    }

    private void showErrorDialog(int errorCode) {
        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode,
                this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {
            // Create a new DialogFragment for the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();
            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);
            // Show the error dialog in the DialogFragment
            errorFragment.show(getSupportFragmentManager(), "Location Updates");
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (!getIntent().hasExtra(INITIAL_LOCATION_EXTRA_KEY)) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                    mLocationClient.getLastLocation().getLatitude(),
                    mLocationClient.getLastLocation().getLongitude()), 18f));
        }
    }

    @Override
    public void onDisconnected() {
        // TODO Auto-generated method stub

    }

}
