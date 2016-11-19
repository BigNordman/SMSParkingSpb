package com.nordman.big.smsparkingspb;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import com.lylc.widget.circularprogressbar.CircularProgressBar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class ParkingActivity extends Activity {
    private static final long TICK_INTERVAL = 5000;

    SmsManager smsMgr;
    Timer timer = null;
    AdView mAdView = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking);

        smsMgr = new SmsManager(this);
        smsMgr.restoreState();

        setProgress();

        if (timer==null){
            timer = new Timer();
            timer.schedule(new UpdateTimeTask(), 0, TICK_INTERVAL); //тикаем каждые 5 сек
        }

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();

        /*
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("93D1A3842F58520E64C8C72C4A9B897C")
                .build();
        */

        mAdView.loadAd(adRequest);

        //AppRater.showRateDialog(this, null);
        AppRater.app_launched(this);

    }

    protected void onResume() {
        super.onResume();

        if (smsMgr.startParkingDate!=null) {
            Log.d("LOG", "smsMgr.startParkingDate = " + smsMgr.startParkingDate);

            Resources res = getResources();
            DateFormat df = new SimpleDateFormat("kk:mm", Locale.getDefault());
            ((TextView) this.findViewById(R.id.timerText)).setText(String.format(res.getString(R.string.parking_from),df.format(smsMgr.startParkingDate)));

            Log.d("LOG", "smsMgr.startParkingDate formatted = " + df.format(smsMgr.startParkingDate));
        }
    }

    @Override
    protected void onStop() {
        Log.d("LOG","...onStop...");
        super.onStop();
    }

    private void setProgress() {
        CircularProgressBar pb = (CircularProgressBar) findViewById(R.id.circularprogressbar1);
        int progress = smsMgr.getProgress();
        pb.setProgress(progress);
        pb.setTitle(String.valueOf(smsMgr.getMinutes()) + " мин");

        if (progress==0) {
            smsMgr.stopParking();
            smsMgr.appStatus=SmsManager.STATUS_INITIAL;
            smsMgr.saveState();
            mAdView.destroy();

            finish();
        }
    }


    private class UpdateTimeTask extends TimerTask {
        public void run() {
            h.sendEmptyMessage(0);
        }
    }

    final Handler h = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            // обрабатываем сообщение таймера
            setProgress();

            // если произошло возвращение из смс-приложения, то проверим, была ли отослана смс
            if (smsMgr.appStatus==SmsManager.STATUS_WAITING_OUT){
                if(smsMgr.IsSent(getResources().getString(R.string.smsNumber))) {
                    // смс о досрочном прекращении отослана - возвращаемся на стартовый экран
                    Log.d("LOG","...Парковка завершена досрочно...");
                    smsMgr.stopParking();
                    smsMgr.appStatus=SmsManager.STATUS_INITIAL;
                    smsMgr.saveState();
                    mAdView.destroy();

                    finish();
                }
            }

            return false;
        }
    });

    public void stopParkingButtonOnClick(View view) {
        Uri uri = Uri.parse("smsto:" + getResources().getString(R.string.smsNumber));
        Intent it = new Intent(Intent.ACTION_SENDTO, uri);
        it.putExtra("sms_body", "S");
        startActivity(it);

        smsMgr.appStatus = SmsManager.STATUS_WAITING_OUT;
        smsMgr.sendDate = new Date();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            moveTaskToBack(true);
            mAdView.destroy();
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void qButton1OnClick(View view) {
        smsMgr.stopParking();
        smsMgr.appStatus=SmsManager.STATUS_INITIAL;
        smsMgr.saveState();
        mAdView.destroy();
        finish();
    }

}
