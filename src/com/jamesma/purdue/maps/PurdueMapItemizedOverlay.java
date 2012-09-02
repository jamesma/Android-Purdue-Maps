package com.jamesma.purdue.maps;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay;

/**
 * An overlay using MapViewBalloonsLibrary.
 * Contains convenience methods for creating balloon popups over the location marker.
 * 
 * @author James Ma (http://jamesma.info)
 *
 */
public class PurdueMapItemizedOverlay extends BalloonItemizedOverlay<PurdueMapOverlayItem> {
    
    private ArrayList<PurdueMapOverlayItem> mOverlayItems = new ArrayList<PurdueMapOverlayItem>();
    private Context mContext;

    public PurdueMapItemizedOverlay(Drawable defaultMarker, MapView mapView) {
        super(boundCenter(defaultMarker), mapView);
        mContext = mapView.getContext();
    }

    @Override
    protected PurdueMapOverlayItem createItem(int i) {
        return mOverlayItems.get(i);
    }

    @Override
    public int size() {
        return mOverlayItems.size();
    }
    
    @Override
    public boolean onBalloonTap(int index, PurdueMapOverlayItem item) {
        String address = item.getAddress();
        Intent intent;
        
        if (address == null) {
            // We do not have address data for this item, use latitude and longitude instead
            GeoPoint pt = item.getPoint();
            String lat = Double.toString(pt.getLatitudeE6() / 1e6);
            String lng = Double.toString(pt.getLongitudeE6() / 1e6);
            intent = new Intent(android.content.Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0?q=" + lat + "," + lng + "(" + item.getTitle() + ")"));
            
        } else {
            intent = new Intent(android.content.Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0?q=" + address));
        }
        
        mContext.startActivity(intent);
        
        return false;
    }
    
    /**
     * Add an overlay item to the overlay and refresh it.
     * 
     * @param item
     */
    public void addOverlayItem(PurdueMapOverlayItem item) {
        mOverlayItems.add(item);
        populate();
    }
    
    /**
     * Remove all overlay items.
     */
    public void removeOverlayItems() {
        mOverlayItems.clear();
        populate();
    }

}
