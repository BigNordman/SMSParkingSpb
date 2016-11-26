package com.nordman.big.smsparkingspb;

import android.content.Context;
import android.content.Intent;

import com.nordman.big.smsparkinglib.BaseSmsManager;

class SmsManager extends BaseSmsManager {
    SmsManager(Context context) {
        super(context);
    }

    @Override
    public void updateSms() {
        sms = "";

        if (currentZone==null) {
            sms += "*";
        } else {
            sms += currentZone.getZoneNumber().toString() + "*";
        }
        sms += regNum.toUpperCase() + "*" + hours + "*B";
    }

    @Override
    public void showParkingScreen() {
        Intent intent = new Intent(context, ParkingActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void showMainScreen() {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }
}

