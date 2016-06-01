package com.ddelp.volvoce.helpers;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;


/**
 * http://stackoverflow.com/questions/1513485/how-do-i-get-the-current-gps-location-programmatically-in-android
 */
public class LocationHelper extends Service {

    /** Tag for logging */
    private static final String TAG = "LocationHelper";
    /** Singleton to prevent multiple instances */
    private static LocationHelper sInstance;
    /** Context for application */
    private final Context context;
    /** Flag if GPS location is enabled */
    boolean isGPSEnabled = false;
    /** Flag if network location is enabled */
    boolean isNetworkEnabled = false;
    /** Location */
    private Location currentBestLocation;
    /** The minimum distance to change Updates in meters */
    private static final long MIN_DISTANCE_CHANGE = 0;
    /** The minimum time between updates in milliseconds */
    private static final long MIN_TIME_CHANGE = 0;
    /** Location Manager for this service */
    protected LocationManager locationManager;

    /** Listener for gps callbacks */
    private GPSListener listener;

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    /**
     * Call getInstance to prevent memory leaks.
     *
     * @param context Application context
     * @return instance of service
     */
    public static synchronized LocationHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LocationHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Private constructor only getInstance can call.
     *
     * @param context Application context
     */
    private LocationHelper(Context context) {
        this.context = context;
        this.listener = null;
        getLocation();
    }

    /**
     * Get the location of the device
     *
     * @return Location of device
     */
    public Location getLocation() {
        Location location = null;

        if (ActivityCompat.checkSelfPermission(context.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Need to request permission for fine location");
        }
        if (ActivityCompat.checkSelfPermission(context.getApplicationContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Need to request permission for coarse location");
        }

        try {
            locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                Log.i(TAG, "GPS and Network provider disabled. Can't get location");
            } else {
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_CHANGE, MIN_DISTANCE_CHANGE, locationListenerNetwork);
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }
                // If GPS enabled, get latitude/longitude using GPS Services
                if (isGPSEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                MIN_TIME_CHANGE, MIN_DISTANCE_CHANGE, locationListenerGps);
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return location;
    }

    public Location getCurrentBestLocation() {
        return currentBestLocation;
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app.
     **/
    public void stopUsingGPS() {
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(context.getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context.getApplicationContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Log.i(TAG, "Removing updates from location manager");
            locationManager.removeUpdates(locationListenerGps);
            locationManager.removeUpdates(locationListenerNetwork);
        } else {
            Log.i(TAG, "Location manager was null, no updates to remove");
        }
    }

    LocationListener locationListenerGps = new LocationListener() {
        public void onLocationChanged(Location location) {
            if(isBestLocation(location)) {
//                Log.i(TAG, "New Best GPS location: " +
//                        location.getLatitude() + ", " + location.getLongitude());
                currentBestLocation = location;
                if(listener != null) {
                    listener.onGPSChanged(location);
                }
            }
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    LocationListener locationListenerNetwork = new LocationListener() {
        public void onLocationChanged(Location location) {
            if(isBestLocation(location)) {
//                Log.i(TAG, "New Best Network location: " +
//                        location.getLatitude() + ", " + location.getLongitude());
                currentBestLocation = location;
                if(listener != null) {
                    listener.onGPSChanged(location);
                }
            }
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    /********************************** Best Location Checker **********************************/

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     */
    protected boolean isBestLocation(Location location) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }


    /********************************** Callback Implementation **********************************/


    /**
     * Interface definition for GPS callback
     */
    public interface GPSListener {
        public void onGPSChanged(Location location);
    }

    /**
     * Assign the listener implementing events interface that will receive the events
     *
     * @param listener
     */
    public void setGPSListener(GPSListener listener) {
        this.listener = listener;
    }
}