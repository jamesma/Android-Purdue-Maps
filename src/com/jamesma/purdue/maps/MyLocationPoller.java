package com.jamesma.purdue.maps;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * 
 * @see http://stackoverflow.com/questions/3145089/what-is-the-simplest-and-most-robust-way-to-get-the-users-current-location-in-a
 * @author James Ma (http://jamesma.info) Credits to Fedor on stackoverflow.com
 *
 */
public class MyLocationPoller {
    
    private static final int TEN_SECONDS = 10000;
    
    private Timer timer;
    private LocationManager locManager;
    private LocationResult locResult;
    private boolean gpsEnabled = false;
    private boolean networkEnabled = false;
    
    /**
     * Check what providers are enabled. Use location listeners on any available provider.
     * Use a timeout timer if location updates are not received in time.
     * 
     * @param context
     * @param result
     * @return
     */
    public boolean getLocation(Context context, LocationResult result) {
        // Use LocationResult callback class to pass location value from MyLocationPoller to user impl.
        locResult = result;
        if (locManager == null)
            locManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        
        gpsEnabled = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        networkEnabled = locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        // Don't start listeners if providers are disabled
        if (!gpsEnabled && !networkEnabled)
            return false;
        
        if (gpsEnabled)
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsLocationListener);
        if (networkEnabled)
            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkLocationListener);
        
        timer = new Timer();
        // If location update has not been triggered the specified time, execute TimerTask
        timer.schedule(new GetLastLocation(), TEN_SECONDS);
        
        return true;
    }
    
    LocationListener gpsLocationListener = new LocationListener() {
        
        @Override
        public void onLocationChanged(Location location) {
            // Stop listeners and timer
            timer.cancel();
            locResult.gotLocation(location);
            locManager.removeUpdates(this);
            locManager.removeUpdates(networkLocationListener);
        }
        
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        
        @Override
        public void onProviderEnabled(String provider) {}
        
        @Override
        public void onProviderDisabled(String provider) {}
        
    };
    
    LocationListener networkLocationListener = new LocationListener() {
        
        @Override
        public void onLocationChanged(Location location) {
            // Stop listeners and timer
            timer.cancel();
            locResult.gotLocation(location);
            locManager.removeUpdates(this);
            locManager.removeUpdates(gpsLocationListener);
        }
        
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        
        @Override
        public void onProviderEnabled(String provider) {}
        
        @Override
        public void onProviderDisabled(String provider) {}
        
    };
    
    /**
     * Timer task that is executed when we do not get any location updates in time.
     * We choose the most recent last known locations from available providers instead to 
     * update the location reticle.
     */
    class GetLastLocation extends TimerTask {

        @Override
        public void run() {
            locManager.removeUpdates(gpsLocationListener);
            locManager.removeUpdates(networkLocationListener);
            
            Location gpsLastLoc = null;
            Location networkLastLoc = null;
            
            // Note: getLastKnownLocation() is a non-blocking call and assuming the application has just started,
            // there is a possibility that there is no "last known location" of the system.
            if (gpsEnabled)
                gpsLastLoc = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (networkEnabled)
                networkLastLoc = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            
            // If both locations exist use the most recent one
            if (gpsLastLoc != null && networkLastLoc != null) {
                if (networkLastLoc.getTime() > gpsLastLoc.getTime())
                    locResult.gotLocation(networkLastLoc);
                else
                    locResult.gotLocation(gpsLastLoc);
                return;
            }
            
            if (gpsLastLoc != null) {
                locResult.gotLocation(gpsLastLoc);
                return;
            }
            
            if (networkLastLoc != null) {
                locResult.gotLocation(networkLastLoc);
                return;
            }
            
            locResult.gotLocation(null);
        }
        
    }

    /**
     * Extension point.
     */
    public static abstract class LocationResult {
        public abstract void gotLocation(Location location);
    }

}
