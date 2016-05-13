package ro.pub.cs.systems.eim.lab10.googlemapsgeofencing.graphicuserinterface;

import java.util.ArrayList;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import ro.pub.cs.systems.eim.lab10.R;
import ro.pub.cs.systems.eim.lab10.googlemapsgeofencing.general.Constants;
import ro.pub.cs.systems.eim.lab10.googlemapsgeofencing.general.Utilities;
import ro.pub.cs.systems.eim.lab10.googlemapsgeofencing.service.GeofenceTrackerIntentService;

public class GoogleMapsActivity extends Activity
		implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener, ResultCallback<Status> {

	private GoogleMap googleMap = null;
	private GoogleApiClient googleApiClient = null;

	private LocationRequest locationRequest = null;
	private Location lastLocation = null;

	private EditText latitudeEditText = null;
	private EditText longitudeEditText = null;
	private EditText radiusEditText = null;

	private Button geofenceStatusButton = null;

	private TextView currentLocationTextView = null;

	private ArrayList<Geofence> geofenceList = null;

	private boolean geofenceStatus = false;

	private PendingIntent geofenceTrackerPendingIntent = null;

	private GeofenceStatusButtonListener geofenceStatusButtonListener = new GeofenceStatusButtonListener();

	private class GeofenceStatusButtonListener implements Button.OnClickListener {

		@Override
		public void onClick(View view) {
			String latitude = latitudeEditText.getText().toString();
			String longitude = longitudeEditText.getText().toString();
			String radius = radiusEditText.getText().toString();

			if (!geofenceStatus) {
				addGeofence(latitude, longitude, radius);
			} else {
				removeGeofence();
			}
		}
	}

	private void addGeofence(String latitude, String longitude, String radius) {
		if (googleApiClient == null || !googleApiClient.isConnected()) {
			Toast.makeText(GoogleMapsActivity.this, "Google API Client is null or not connected!", Toast.LENGTH_SHORT)
					.show();
			return;
		}
		if (latitude == null || latitude.isEmpty() || longitude == null || longitude.isEmpty() || radius == null
				|| radius.isEmpty()) {
			Toast.makeText(GoogleMapsActivity.this, "All fields (gps coordinates, radius) should be filled!",
					Toast.LENGTH_SHORT).show();
			return;
		}
		geofenceList.add(new Geofence.Builder()
				.setRequestId(Utilities.generateGeofenceIdentifier(Constants.GEOFENCE_IDENTIFIER_LENGTH))
				.setCircularRegion(Double.parseDouble(latitude), Double.parseDouble(longitude),
						Float.parseFloat(radius))
				.setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
				.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT).build());
		GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
		builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
		builder.addGeofences(geofenceList);
		GeofencingRequest geofencingRequest = builder.build();
		LocationServices.GeofencingApi.addGeofences(googleApiClient, geofencingRequest, geofenceTrackerPendingIntent)
				.setResultCallback(GoogleMapsActivity.this);
	}

	private void removeGeofence() {
		if (googleApiClient == null || !googleApiClient.isConnected()) {
			Toast.makeText(GoogleMapsActivity.this, "Google API Client is null or not connected!", Toast.LENGTH_SHORT)
					.show();
			return;
		}
		latitudeEditText.setText(new String());
		longitudeEditText.setText(new String());
		radiusEditText.setText(new String());
		geofenceList.clear();
		LocationServices.GeofencingApi.removeGeofences(googleApiClient, geofenceTrackerPendingIntent)
				.setResultCallback(GoogleMapsActivity.this);
	}

	private void navigateToLocation(double latitude, double longitude) {
		currentLocationTextView
				.setText("Latitude: " + latitude + System.getProperty("line.separator") + "Longitude: " + longitude);
		CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(latitude, longitude))
				.zoom(Constants.CAMERA_ZOOM).build();
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
		radiusEditText = (EditText) findViewById(R.id.radius_edit_text);

		geofenceStatusButton = (Button) findViewById(R.id.geofence_status_button);
		geofenceStatusButton.setOnClickListener(geofenceStatusButtonListener);
		currentLocationTextView = (TextView) findViewById(R.id.current_location_text_view);

		geofenceList = new ArrayList<Geofence>();

		googleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).addApi(LocationServices.API).build();

		locationRequest = new LocationRequest();
		locationRequest.setInterval(Constants.LOCATION_REQUEST_INTERVAL);
		locationRequest.setFastestInterval(Constants.LOCATION_REQUEST_FASTEST_INTERVAL);
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		Intent intent = new Intent(this, GeofenceTrackerIntentService.class);
		geofenceTrackerPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(Constants.TAG, "onStart() callback method was invoked");
		googleApiClient.connect();
		if (googleMap == null) {
			googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.google_map)).getMap();
			if (googleMap == null) {
				Toast.makeText(this, "Unable to instantiate Google Maps!", Toast.LENGTH_SHORT).show();
			}
		}
		if (googleApiClient != null && googleApiClient.isConnected()) {
			startLocationUpdates();
		}
		restoreValues();
	}

	@Override
	protected void onStop() {
		Log.i(Constants.TAG, "onStop() callback method was invoked");
		saveValues();
		stopLocationUpdates();
		removeGeofence();
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

	protected void saveValues() {
		Utilities.setInformationIntoSharedPreferences(this, lastLocation, geofenceStatus,
				latitudeEditText.getText().toString(), longitudeEditText.getText().toString(),
				radiusEditText.getText().toString());
	}

	protected void restoreValues() {
		lastLocation = Utilities.getLastLocationFromSharedPreferences(this);
		geofenceStatus = Utilities.getGeofenceStatusFromSharedPreferences(this);
		if (geofenceStatus) {
			latitudeEditText.setText(Utilities.getGeofenceLatitudeFromSharedPreferences(this));
			longitudeEditText.setText(Utilities.getGeofenceLongitudeFromSharedPreferences(this));
			radiusEditText.setText(Utilities.getGeofenceRadiusFromSharedPreferences(this));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.google_maps, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		Log.i(Constants.TAG, "onConnected() callback method has been invoked");
		lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
		startLocationUpdates();
		if (geofenceStatus) {
			geofenceStatus = !geofenceStatus;
			addGeofence(latitudeEditText.getText().toString(), longitudeEditText.getText().toString(),
					radiusEditText.getText().toString());
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
		LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
		googleMap.setMyLocationEnabled(true);
		if (lastLocation != null) {
			navigateToLocation(lastLocation);
		}
	}

	protected void stopLocationUpdates() {
		LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
		googleMap.setMyLocationEnabled(false);
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.i(Constants.TAG, "onLocationChanged() callback method has been invoked");
		lastLocation = location;
		navigateToLocation(lastLocation);
	}

	@Override
	public void onResult(Status status) {
		Log.i(Constants.TAG, "onResult() callback method has been invoked: " + status.isSuccess());
		if (status.isSuccess()) {
			geofenceStatus = !geofenceStatus;
			if (geofenceStatus) {
				geofenceStatusButton.setText(getResources().getString(R.string.remove_from_geofence));
				geofenceStatusButton.setBackground(getResources().getDrawable(R.color.green));
			} else {
				geofenceStatusButton.setText(getResources().getString(R.string.add_to_geofence));
				geofenceStatusButton.setBackground(getResources().getDrawable(R.color.red));
			}
		} else {
			String errorMessage = null;
			switch (status.getStatusCode()) {
			case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
				errorMessage = Constants.GEOFENCE_NOT_AVAILABLE_ERROR;
				break;
			case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
				errorMessage = Constants.GEOFENCE_TOO_MANY_GEOFENCES_ERROR;
				break;
			case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
				errorMessage = Constants.GEOFENCE_TOO_MANY_PENDING_INTENTS_ERROR;
				break;
			default:
				errorMessage = Constants.GEOFENCE_UNKNOWN_ERROR;
				break;
			}
			Log.e(Constants.TAG, "An exception has occurred while turning the geofencing on/off: "
					+ status.getStatusCode() + " " + errorMessage);
		}
	}
}