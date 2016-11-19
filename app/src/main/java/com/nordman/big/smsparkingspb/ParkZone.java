package com.nordman.big.smsparkingspb;

import com.vividsolutions.jts.geom.Polygon;

/**
 * Created by s_vershinin on 11.01.2016.
 *
 */
class ParkZone {
    private Integer zoneNumber;
    private String zoneDesc;
    private Polygon zonePolygon;

    ParkZone(Polygon zonePolygon, Integer zoneNumber, String zoneDesc) {
        this.zonePolygon = zonePolygon;
        this.zoneNumber = zoneNumber;
        this.zoneDesc = zoneDesc;
    }

    Polygon getZonePolygon() {
        return zonePolygon;
    }

    String getZoneDesc() {

        return zoneDesc;
    }


    Integer getZoneNumber() {

        return zoneNumber;
    }

}
