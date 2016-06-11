/**
 * Created by s_vershinin on 30.12.2015.
 * GPS operations
 */

package com.nordman.big.smsparkingspb;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;


public class GeoManager {
    private Location curLocation;
    Boolean connected = false;
    Context context;
    GeometryFactory factory;

    public GeoManager(Context context) {
        this.context = context;
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Criteria crta = new Criteria();
        crta.setAccuracy(Criteria.ACCURACY_FINE);
        crta.setAltitudeRequired(false);
        crta.setBearingRequired(false);
        crta.setCostAllowed(true);
        crta.setPowerRequirement(Criteria.POWER_LOW);
        String gpsProvider = locationManager.getBestProvider(crta, true);
        Log.d("LOG","...Provider = " + gpsProvider + "...");

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            curLocation = locationManager.getLastKnownLocation(gpsProvider);

            LocationListener locationListener = new LocationListener() {

                @Override
                public void onLocationChanged(Location location) {
                    curLocation = location;
                }

                @Override
                public void onProviderDisabled(String provider) {
            /*
            criticalErr = "Provider " + provider + " disabled.";
            updateUI();
            */
                    //TODO: что-то сделать с ошибками
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

            };
            locationManager.requestLocationUpdates(gpsProvider, 1000, 0, locationListener);
        }

        factory = new GeometryFactory();
    }

    public String getCoordinates() {
        Point currentPoint = this.getCurrentPoint();
        if (currentPoint!=null) return currentPoint.toString();
        else return "";
    }

    private Point getCurrentPoint() {
        Point result = null;

        if (curLocation != null) {
            result = factory.createPoint(new Coordinate(curLocation.getLatitude(),curLocation.getLongitude()));
        }

        //result = factory.createPoint(new Coordinate(55.749644,37.599726));
        return result;
    }

    public ArrayList<ParkZone> getParkZoneList(){
        ArrayList<ParkZone> result = new ArrayList<>();
        ArrayList<Coordinate> coords = null;
        Polygon polygon;
        Integer zoneNumber = null;
        String zoneDesc = null;

        // парсим xml с координатами полигонов
        try {
            XmlPullParser xpp = context.getResources().getXml(R.xml.park_zones);
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                switch (xpp.getEventType()) {
                    // начало тэга
                    case XmlPullParser.START_TAG:
                        switch (xpp.getName()) {
                            case "zone":
                                // массив точек полигона. Пустой
                                coords = new ArrayList<>();
                                zoneNumber = Integer.parseInt(xpp.getAttributeValue(null,"zone_number")) ;
                                zoneDesc = xpp.getAttributeValue(null,"zone_desc");
                                break;
                            case "point":
                                // точка полигона
                                Double lat = Double.parseDouble(xpp.getAttributeValue(0));
                                Double lon = Double.parseDouble(xpp.getAttributeValue(1));
                                if (coords!=null) {
                                    coords.add(new Coordinate(lat, lon));
                                }
                                break;
                            default:
                                break;
                        }

                        break;
                    // конец тэга
                    case XmlPullParser.END_TAG:
                        if (xpp.getName().equals("zone")) {
                            // Зона определена - создаем объект ParkZone и зохраняем в результате
                            if (coords!=null) {
                                polygon = factory.createPolygon(coords.toArray(new Coordinate[coords.size()]));
                                //Log.d("LOG", polygon.toString());
                                ParkZone zone = new ParkZone(polygon,zoneNumber,zoneDesc);
                                result.add(zone);
                            }
                        }
                        break;

                    default:
                        break;
                }
                xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }

        return result;
    }


    public ParkZone getParkZone(){
        ArrayList<ParkZone> zones = this.getParkZoneList();
        Point currentPoint = this.getCurrentPoint();

        if (currentPoint==null) return null;

        for(ParkZone zone : zones){
            if (zone.getZonePolygon().contains(currentPoint)){
                return zone;
            }
        }

        return null;
    }

    public ParkZone getParkZone(int zoneNumber){
        ArrayList<ParkZone> zones = this.getParkZoneList();

        for(ParkZone zone : zones){
            if (zone.getZoneNumber() == zoneNumber){
                return zone;
            }
        }

        return null;
    }

}
