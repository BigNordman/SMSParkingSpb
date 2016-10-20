package com.nordman.big.smsparkingspb;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.Date;

/**
 * Created by s_vershinin on 15.01.2016.
 * Класс для формирования целевого СМС-сообщения
 */
public class SmsManager{
    public static final long MILLIS_IN_HOUR = 3600000;
    private static final long MILLIS_IN_MINUTE = 60000;

    public static final int STATUS_INITIAL = 1;
    public static final int STATUS_WAITING_OUT = 2;
    public static final int STATUS_WAITING_IN = 3;
    public static final int STATUS_SMS_SENT = 4;
    public static final int STATUS_SMS_NOT_SENT = 5;
    public static final int STATUS_SMS_NOT_RECEIVED = 6;
    public static final int STATUS_PARKING = 7;
    public static final int STATUS_SMS_PERMISSION_NOT_GRANTED = 8;
    int appStatus = STATUS_INITIAL;

    Context context;
    GeoManager geoMgr;

    Date sendDate;
    Date startParkingDate;

    String sms = null;
    String regNum = "";
    ParkZone currentZone = null;
    String hours = "1";
    String statusMessage = "";

    public SmsManager(Context context) {

        this.context = context;
        geoMgr = new GeoManager(context);
    }

    public void updateSms() {
        sms = "";

        if (currentZone==null) {
            sms += "*";
        } else {
            sms += currentZone.getZoneNumber().toString() + "*";
        }
        sms += regNum.toUpperCase() + "*" + hours + "*B";
    }

    public int getProgress(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long lh = Long.parseLong(prefs.getString("LastHours", "1"));
        long lpt = Long.parseLong(prefs.getString("LastParkTime", "0"));
        long current = (new Date()).getTime();
        return (int) (100 * (lh*MILLIS_IN_HOUR - (current - lpt) )/(lh*MILLIS_IN_HOUR));
    }

    public int getMinutes(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long lh = Long.parseLong(prefs.getString("LastHours", "1"));
        long lpt = Long.parseLong(prefs.getString("LastParkTime", "0"));
        long current = (new Date()).getTime();
        return (int) ((lh*MILLIS_IN_HOUR - (current - lpt))/MILLIS_IN_MINUTE);
    }

    public boolean parkingActive(){
        return (getProgress()>0);
    }

    /// отправлена ли смс указанному адресату
    public boolean IsSent(String toWhom){
        boolean result = false;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.d("LOG","... permission problems");
            return false;
        }

        ContentResolver cr = context.getContentResolver();

        Cursor c = cr.query(Uri.parse("content://sms/sent"),
                new String[] { "date", "address", "body" },
                "address = '" + toWhom + "'",
                null,
                "date DESC");

        assert c != null;
        int totalSMS = c.getCount();

        if (c.moveToFirst()) {
            for (int i = 0; i < totalSMS; i++) {
                if (Long.parseLong(c.getString(0)) > sendDate.getTime()) {
                    result=true;
                    break;
                }
                c.moveToNext();
            }
        }

        c.close();

        return result;
    }

    public String GetIncomingSms(String fromWhom){
        String result = null;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.d("LOG","... permission problems");
            return context.getResources().getString(R.string.permissionNotGranted);
        }

        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(Uri.parse("content://sms/inbox"),
                new String[] { "date", "address", "body" },
                (fromWhom==null) ? null : "address = '" + fromWhom + "'",
                null,
                "date DESC");

        assert c != null;
        int totalSMS = c.getCount();

        if (c.moveToFirst()) {
            for (int i = 0; i < totalSMS; i++) {
                if (Long.parseLong(c.getString(0)) > sendDate.getTime()) {
                    result=c.getString(2);
                    break;
                }
                c.moveToNext();
            }
        }

        c.close();

        return result;
    }

    public void startParking() {
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(context);
        if (startParkingDate==null) startParkingDate=new Date();
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString("LastParkTime", String.valueOf(startParkingDate.getTime()));
        ed.putString("LastHours", hours);
        ed.apply();

        showParkingScreen();
    }

    public void stopParking() {
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(context);
        startParkingDate=null;
        appStatus = STATUS_INITIAL;

        SharedPreferences.Editor ed = prefs.edit();
        ed.putString("LastParkTime", "0");
        ed.putString("LastHours", hours);
        ed.apply();

        showMainScreen();
    }

    public  void showParkingScreen() {
        Intent intent = new Intent(context, ParkingActivity.class);
        context.startActivity(intent);
    }

    public  void showMainScreen() {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }

    public void saveState() {
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString("regNum", regNum);
        ed.putInt("status", appStatus);
        ed.putLong("sendDate", sendDate != null ? sendDate.getTime() : 0);
        ed.putLong("startParkingDate", startParkingDate != null ? startParkingDate.getTime() : 0);
        ed.putInt("zoneNumber", currentZone != null ? currentZone.getZoneNumber() : 0);
        ed.putString("hours", hours);
        ed.apply();
        //Log.d("LOG", "...saveState()...");
    }

    public void restoreState() {
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(context);
        regNum = prefs.getString("regNum", "");
        appStatus = prefs.getInt("status", STATUS_INITIAL);
        sendDate = new Date(prefs.getLong("sendDate",0));
        startParkingDate = new Date(prefs.getLong("startParkingDate",0));
        //if (appStatus==STATUS_WAITING_IN || appStatus==STATUS_WAITING_OUT)
        currentZone = geoMgr.getParkZone(prefs.getInt("zoneNumber", 0));
        hours = prefs.getString("hours", "1");
        //Log.d("LOG", "...restoreState()...");
    }
}

