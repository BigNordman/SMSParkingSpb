/**
 * Created by s_vershinin on 30.12.2015.
 * GPS operations
 */

package com.nordman.big.smsparkingspb;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;


public class GeoManager implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 5;

    private Location curLocation;
    Boolean connected = false;
    Context context;
    GeometryFactory factory;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private int gpsMeasuresNum = 0;

    public GeoManager(Context context) {
        this.context = context;
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this.context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
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
            if (zone.getZoneNumber()==zoneNumber){
                return zone;
            }
        }

        return null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("LOG", "...GoogleApiClient - onConnected...");
        if (mLocationRequest == null) {
            createLocationRequest();
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationUpdate();
    }

    public void locationUpdate() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    public void disconnect(){
        if (mGoogleApiClient.isConnected()) {
            Log.d("LOG","...disconnect()");
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("LOG", "...GoogleApiClient - onConnectionSuspended...");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("LOG", "...GoogleApiClient - onConnectionFailed...");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("LOG", "...onConnectionChanged...");
        gpsMeasuresNum++;


        if(gpsMeasuresNum >= 3) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            gpsMeasuresNum = 0;
        }

        curLocation = location;
    }
}
