package com.jamesma.purdue.maps;

import java.util.List;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.jamesma.purdue.maps.MyLocationPoller.LocationResult;
import com.jamesma.purdue.maps.database.DatabaseHelper;
import com.readystatesoftware.maps.OnSingleTapListener;
import com.readystatesoftware.maps.TapControlledMapView;

/**
 * 
 * @author James Ma (http://jamesma.info)
 *
 */
public class PurdueMapActivity extends MapActivity {
    
    private static final String LOC_DIALOG_TITLE = "Location services are disabled";
    private static final String LOC_DIALOG_MESSAGE = "This App requires location services to work. Do you want you enable it?";
    private static final String LOC_DIALOG_POS_BUTTON = "Yes";
    private static final String LOC_DIALOG_NEG_BUTTON = "No, Leave App";
    private static final int BALLOON_OFFSET = 25;
    private static final int ZOOM_LEVEL = 17;
    
    private MyLocationOverlay myLocationOverlay;
    private TapControlledMapView mapView;
    private PurdueMapItemizedOverlay itemizedOverlay;
    private MapController mapController;
    private LocationManager locManager;
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // This verification should be done during onStart() because the system calls
        // this method when the user returns to the activity, which ensures the desired
        // location provider is enabled each time the activity resumes from the stopped state.
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final boolean gpsEnabled = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        final boolean networkEnabled = locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        if (!gpsEnabled && !networkEnabled) {
            // Build an alert dialog here that requests that the user enable
            // the location services, then when the user clicks the "OK" button,
            // call enableLocationSettings()
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(LOC_DIALOG_TITLE);
            dialog.setMessage(LOC_DIALOG_MESSAGE);
            
            dialog.setPositiveButton(LOC_DIALOG_POS_BUTTON, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    enableLocationSettings();
                }
            });
            
            dialog.setNegativeButton(LOC_DIALOG_NEG_BUTTON, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            
            dialog.show();
        } else {
            animateToCurrentLocation();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.purdue_map_activity);
        initializeMapView();
        handleIntent(getIntent());
    }
    
    /**
     * Called for activities that set launchMode to "singleTop".
     * If an instance of the activity already exists at the top of the target task, 
     * the system routes the intent to that instance through a call to its onNewIntent() method, 
     * rather than creating a new instance of the activity.
     * 
     * @param intent
     */
    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Register for location updates
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableCompass();
        mapView.setReticleDrawMode(MapView.ReticleDrawMode.DRAW_RETICLE_OVER);
    }
    
    @Override
    protected void onPause() {
        super.onPause();

        // Unregister from location updates
        myLocationOverlay.disableMyLocation();
        myLocationOverlay.disableCompass();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.purdue_map_activity, menu);
        return true;
    }
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.map_search:
                // Open Search Dialog
                onSearchRequested();
                break;
            case R.id.current_location:
                // Animate to current location
                animateToCurrentLocation();
                break;
            case R.id.layers:
                // Layer menu
                SubMenu subMenu = item.getSubMenu();
                MenuItem trafficItem = subMenu.findItem(R.id.traffic);
                MenuItem satelliteItem = subMenu.findItem(R.id.satellite);
                trafficItem.setChecked(mapView.isTraffic());
                satelliteItem.setChecked(mapView.isSatellite());
                break;
            case R.id.traffic:
                mapView.setTraffic(!mapView.isTraffic());
                item.setChecked(mapView.isTraffic());
                break;
            case R.id.satellite:
                mapView.setSatellite(!mapView.isSatellite());
                item.setChecked(mapView.isSatellite());
                break;
            default:
                break;
        }
        return true;
    }
    
    /**
     * Animate the MapController to current location of user.
     */
    private void animateToCurrentLocation() {
        
        // Use a simple and robust way to get user's current location
        LocationResult locResult = new LocationResult() {
            
            @Override
            public void gotLocation(Location location) {
                if (location == null)
                    return;
                
                // Animate to the last known (or current) location of user
                GeoPoint initPt = new GeoPoint(
                        (int) (location.getLatitude() * 1e6), 
                        (int) (location.getLongitude() * 1e6));
                animateMapControllerTo(initPt);
            }
            
        };
        
        MyLocationPoller myLocationPoller = new MyLocationPoller();
        myLocationPoller.getLocation(this, locResult);
    }
    
    /**
     * Initialize the {@link #mapView} with {@link Overlay}s.
     */
    private void initializeMapView() {
        // TapControlledMapView mimics behavior of Google Maps and iOS MapKit
        mapView = (TapControlledMapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        
        mapController = mapView.getController();
        
        // Dismiss balloon upon single tap of MapView (iOS behavior) 
        mapView.setOnSingleTapListener(new OnSingleTapListener() {
            @Override
            public boolean onSingleTap(MotionEvent e) {
                itemizedOverlay.hideAllBalloons();
                return true;
            }
        });
        
        // Create an overlay that shows our current location
        myLocationOverlay = new MyLocationOverlay(this, (MapView)mapView);
        
        // Create an overlay that shows the user target location
        Drawable marker = this.getResources().getDrawable(R.drawable.blue_dot_marker);
        itemizedOverlay = new PurdueMapItemizedOverlay(marker, mapView);
        
        // Set iOS behavior attributes for overlay
        itemizedOverlay.setShowClose(false);
        itemizedOverlay.setShowDisclosure(true);
        itemizedOverlay.setSnapToCenter(false);
        
        // Set bottom padding
        itemizedOverlay.setBalloonBottomOffset(BALLOON_OFFSET);
        
        // Add the current location overlay to the MapView and refresh it
        List<Overlay> mapOverlays = mapView.getOverlays();
        mapOverlays.add(myLocationOverlay);
        mapView.postInvalidate();
    }
    
    /**
     * Code to handle the search intent. Both {@link #onCreate(Bundle)} and {@link #onNewIntent(Intent)} can create it.
     * 
     * @param intent
     */
    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // Handle a search intent
            // TODO
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // Handle a suggestion click (because the suggestions all use ACTION_VIEW)
            
            // Remove all overlay items
            itemizedOverlay.removeOverlayItems();
            
            // Add the new overlay item
            String data = intent.getDataString();
            Bundle extras = intent.getExtras();
            String address = extras.getString(SearchManager.EXTRA_DATA_KEY);
            
            String[] dataTokens = data.split(DatabaseHelper.DATA_SEPARATOR);
            if (dataTokens.length != 3) {
                throw new Error("There was something wrong with the search");
            }
            
            String loc_name = dataTokens[0];
            String abbr = dataTokens[1];
            String coords = dataTokens[2];
            
            String[] lat_and_lng = coords.split(",");
            if (lat_and_lng.length != 2) {
                throw new Error("There was something wrong with the search");
            }
            
            int lat = (int) (Float.valueOf(lat_and_lng[0]) * 1e6);
            int lng = (int) (Float.valueOf(lat_and_lng[1]) * 1e6);
            GeoPoint pt = new GeoPoint(lat, lng);
            
            PurdueMapOverlayItem item = new PurdueMapOverlayItem(pt, loc_name, abbr, address);
            
            // Add item to overlay and refresh map view
            itemizedOverlay.addOverlayItem(item);
            
            List<Overlay> mapOverlays = mapView.getOverlays();
            if (!mapOverlays.contains(itemizedOverlay)) {
                mapOverlays.add(itemizedOverlay);
            }
            
            mapView.postInvalidate();
            
            // Show the balloon for the location and zoom to it.
            itemizedOverlay.onTap(0); // Only item, index is always 0
            animateMapControllerTo(pt);
        }
    }
    
    /**
     * Brings the user to the configuration menu for location settings.
     */
    private void enableLocationSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }
    
    /**
     * Animate MapController to GeoPoint.
     * 
     * @param pt
     */
    private void animateMapControllerTo(GeoPoint pt) {
        mapController.setZoom(ZOOM_LEVEL);
        mapController.animateTo(pt);
    }
    
}
