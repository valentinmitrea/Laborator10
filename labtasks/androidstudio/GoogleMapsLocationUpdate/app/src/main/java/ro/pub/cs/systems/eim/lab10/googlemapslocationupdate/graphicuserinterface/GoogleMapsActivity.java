package ro.pub.cs.systems.eim.lab10.googlemapslocationupdate.graphicuserinterface;

import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import ro.pub.cs.systems.eim.lab10.R;
import ro.pub.cs.systems.eim.lab10.googlemapslocationupdate.general.Constants;

public class GoogleMapsActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap googleMap = null;
    private GoogleApiClient googleApiClient = null;

    private LocationRequest locationRequest = null;
    private Location lastLocation = null;

    private EditText latitudeEditText = null;
    private EditText longitudeEditText = null;
    private Button navigateToLocationButton = null;

    private Button locationUpdatesStatusButton = null;

    private Spinner mapTypeSpinner = null;

    private boolean locationUpdatesStatus = false;

    private NavigateToLocationButtonListener navigateToLocationButtonListener = new NavigateToLocationButtonListener();

    private class NavigateToLocationButtonListener implements Button.OnClickListener {

        @Override
        public void onClick(View view) {
            String latitudeContent = latitudeEditText.getText().toString();
            String longitudeContent = longitudeEditText.getText().toString();

            if (latitudeContent == null || latitudeContent.isEmpty() ||
                    longitudeContent == null || longitudeContent.isEmpty()) {
                Toast.makeText(GoogleMapsActivity.this, "GPS coordinates should be filled!", Toast.LENGTH_SHORT).show();
                return;
            }

            double latitudeValue = Double.parseDouble(latitudeContent);
            double longitudeValue = Double.parseDouble(longitudeContent);
            navigateToLocation(latitudeValue, longitudeValue);
        }
    }

    private LocationUpdatesStatusButtonListener locationUpdatesStatusButtonListener = new LocationUpdatesStatusButtonListener();

    private class LocationUpdatesStatusButtonListener implements Button.OnClickListener {

        @Override
        public void onClick(View view) {
            String content = locationUpdatesStatusButton.getText().toString();
            if (content.equals(getResources().getString(R.string.start_location_updates))) {
                startLocationUpdates();
            }
            if (content.equals(getResources().getString(R.string.stop_location_updates))) {
                stopLocationUpdates();
            }
        }
    }

    private MapTypeSpinnerListener mapTypeSpinnerListener = new MapTypeSpinnerListener();

    private class MapTypeSpinnerListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            switch (position) {
                case Constants.NORMAL_MAP_TYPE:
                    googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    break;
                case Constants.SATELLITE_MAP_TYPE:
                    googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    break;
                case Constants.TERRAIN_MAP_TYPE:
                    googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    break;
                case Constants.HYBRID_MAP_TYPE:
                    googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    break;
                default:
                    googleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }

    }

    private void navigateToLocation(double latitude, double longitude) {
        latitudeEditText.setText(String.valueOf(latitude));
        longitudeEditText.setText(String.valueOf(longitude));
        CameraPosition cameraPosition = new CameraPosition.Builder().target(
                new LatLng(latitude, longitude)
        ).zoom(Constants.CAMERA_ZOOM)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void navigateToLocation(Location location) {
        navigateToLocation(location.getLatitude(), location.getLongitude());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(Constants.TAG, "onCreate() callback method was invoked");
        setContentView(R.layout.activity_google_maps);

        latitudeEditText = (EditText) findViewById(R.id.latitude_edit_text);
        longitudeEditText = (EditText) findViewById(R.id.longitude_edit_text);
        navigateToLocationButton = (Button) findViewById(R.id.navigate_to_location_button);
        navigateToLocationButton.setOnClickListener(navigateToLocationButtonListener);

        locationUpdatesStatusButton = (Button) findViewById(R.id.location_updates_status_button);
        locationUpdatesStatusButton.setOnClickListener(locationUpdatesStatusButtonListener);

        mapTypeSpinner = (Spinner) findViewById(R.id.map_type_spinner);
        mapTypeSpinner.setOnItemSelectedListener(mapTypeSpinnerListener);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(Constants.LOCATION_REQUEST_INTERVAL);
        locationRequest.setFastestInterval(Constants.LOCATION_REQUEST_FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (savedInstanceState != null) {
            restoreValues(savedInstanceState);
        }
    }

    @Override
    protected void onStart() {
        Log.i(Constants.TAG, "onStart() callback method was invoked");
        super.onStart();
        googleApiClient.connect();
        if (googleMap == null) {
            ((MapFragment) getFragmentManager().findFragmentById(R.id.google_map)).getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap readyGoogleMap) {
                    googleMap = readyGoogleMap;
                }
            });
        }
        if (googleApiClient != null && googleApiClient.isConnected() && locationUpdatesStatus) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        Log.i(Constants.TAG, "onStop() callback method was invoked");
        stopLocationUpdates();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i(Constants.TAG, "onDestroy() callback method was invoked");
        googleApiClient = null;
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.i(Constants.TAG, "onSaveInstanceState() callback method was invoked");
        saveValues(savedInstanceState);
        super.onSaveInstanceState(savedInstanceState);
    }

    protected void saveValues(Bundle state) {
        state.putBoolean(Constants.LOCATION_UPDATES_STATUS, locationUpdatesStatus);
        state.putParcelable(Constants.LAST_LOCATION, lastLocation);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i(Constants.TAG, "onRestoreInstanceState() callback method was invoked");
        super.onRestoreInstanceState(savedInstanceState);
        restoreValues(savedInstanceState);
    }

    protected void restoreValues(Bundle state) {
        if (state.keySet().contains(Constants.LAST_LOCATION)) {
            lastLocation = state.getParcelable(Constants.LAST_LOCATION);
        }
        if (state.keySet().contains(Constants.LOCATION_UPDATES_STATUS)) {
            locationUpdatesStatus = state.getBoolean(Constants.LOCATION_UPDATES_STATUS);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(Constants.TAG, "onConnected() callback method has been invoked");
        try {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (locationUpdatesStatus) {
                startLocationUpdates();
            }
        } catch (SecurityException securityException) {
            Log.e(Constants.TAG, securityException.getMessage());
            if (Constants.DEBUG) {
                securityException.printStackTrace();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(Constants.TAG, "onConnectionSuspended() callback method has been invoked");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(Constants.TAG, "onConnectionFailed() callback method has been invoked");
    }

    protected void startLocationUpdates() {
        // TODO exercise 7a
        // invoke the requestLocationUpdates() method from FusedLocationProviderApi class
        // enable the locationUpdatesStatus
        // enable the current location on Google Map
        // update the locationUpdatesStatusButton text & color
        // navigate to current position (lastLocation)
        // disable the latitudeEditText, longitudeEditText, navigateToLocationButton widgets
    }

    protected void stopLocationUpdates() {
        // TODO exercise 7b
        // invoke the removeLocationUpdates() method from FusedLocationProviderApi class
        // disable the locationUpdatesStatus
        // disable the current location on Google Map
        // update the locationUpdatesStatusButton text & color
        // enable the latitudeEditText, longitudeEditText, navigateToLocationButton widgets	and reset their content
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(Constants.TAG, "onLocationChanged() callback method has been invoked");
        lastLocation = location;
        navigateToLocation(lastLocation);
    }
}
