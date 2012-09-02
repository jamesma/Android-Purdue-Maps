package com.jamesma.purdue.maps;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

/**
 * 
 * @author James Ma (http://jamesma.info)
 *
 */
public class PurdueMapOverlayItem extends OverlayItem {
    
    private String address;

    public PurdueMapOverlayItem(GeoPoint pt, String locationName, String abbreviation, String addr) {
        super(pt, locationName, abbreviation);
        this.address = addr;
    }
    
    public String getAddress() {
        return this.address;
    }

}
