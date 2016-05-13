package ro.pub.cs.systems.eim.lab10.googlemapsgeofencing.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import ro.pub.cs.systems.eim.lab10.R;
import ro.pub.cs.systems.eim.lab10.googlemapsgeofencing.general.Constants;
import ro.pub.cs.systems.eim.lab10.googlemapsgeofencing.graphicuserinterface.GoogleMapsGeofenceEventActivity;

public class GeofenceTrackerIntentService extends IntentService {

	public GeofenceTrackerIntentService() {
		super(Constants.TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO: exercise 9
		// obtain GeofencingEvent from the calling intent, using GeofencingEvent.fromIntent(intent);
		// check whether the GeofencingEvent hasError(), log it and exit the method
		// get the geofence transition using getGeofenceTransition() method
		// build a detailed message
		// - include the transition type
		// - include the request identifier (getRequestId()) for each  geofence that triggered the event (getTriggeringGeofences())
		// send a notification with the detailed message (sendNotification())
	}

	private void sendNotification(String notificationDetails) {
		Intent notificationIntent = new Intent(getApplicationContext(), GoogleMapsGeofenceEventActivity.class);
		notificationIntent.putExtra(Constants.NOTIFICATION_DETAILS, notificationDetails);

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(GoogleMapsGeofenceEventActivity.class);
		stackBuilder.addNextIntent(notificationIntent);

		PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setSmallIcon(R.drawable.ic_launcher)
				.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher)).setColor(Color.RED)
				.setContentTitle(Constants.GEOFENCE_TRANSITION_EVENT).setContentText(notificationDetails)
				.setContentIntent(notificationPendingIntent);
		builder.setAutoCancel(true);

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(0, builder.build());
	}
}
