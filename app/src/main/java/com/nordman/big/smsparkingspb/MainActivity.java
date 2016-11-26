package com.nordman.big.smsparkingspb;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import com.nordman.big.smsparkinglib.ParkZone;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_SMS = 1;
    private static final int PERMISSION_ACCESS_FINE_LOCATION = 2;
    static final int PAGE_COUNT = 3;
    public static final long TICK_INTERVAL = 1000;
    public static final long MAX_TICK_WAITING = 60;

    SmsManager smsMgr;
    GeoManager geoMgr;
    SparseArray<View> views = new SparseArray<>();
    Timer tick = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        geoMgr = new GeoManager(this);
        smsMgr = new SmsManager(this);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
        if (mViewPager != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);

            mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                RadioButton radioButton1 = (RadioButton) findViewById(R.id.radioButton1);
                RadioButton radioButton2 = (RadioButton) findViewById(R.id.radioButton2);
                RadioButton radioButton3 = (RadioButton) findViewById(R.id.radioButton3);
                View buttonLeft = findViewById(R.id.buttonLeft);
                View buttonRight = findViewById(R.id.buttonRight);
                @Override
                public void onPageSelected(int position) {
                    switch (position) {
                        case 0:
                            radioButton1.setChecked(true);
                            radioButton2.setChecked(false);
                            radioButton3.setChecked(false);
                            buttonLeft.setEnabled(false);
                            buttonRight.setEnabled(true);

                            break;
                        case 1:
                            radioButton1.setChecked(false);
                            radioButton2.setChecked(true);
                            radioButton3.setChecked(false);
                            buttonLeft.setEnabled(true);
                            buttonRight.setEnabled(true);
                            break;
                        case 2:
                            radioButton1.setChecked(false);
                            radioButton2.setChecked(false);
                            radioButton3.setChecked(true);
                            buttonLeft.setEnabled(true);
                            buttonRight.setEnabled(false);
                            break;
                    }
                }

                @Override
                public void onPageScrolled(int position, float positionOffset,
                                           int positionOffsetPixels) {
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            });
        }
    }

    @Override
    protected void onStart() {
        Log.d("LOG", "...onStart...");

        geoMgr.connected=true;
        if (smsMgr.appStatus==SmsManager.STATUS_INITIAL) {
            smsMgr.restoreState();
            smsMgr.currentZone = geoMgr.getParkZone();
            smsMgr.saveState();
        }

        if (tick==null){
            tick = new Timer();
            tick.schedule(new UpdateTickTask(), 0, TICK_INTERVAL); //тикаем каждую секунду
        }

        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d("LOG", "onStop...");
        super.onStop();

        if (tick!=null) {
            tick.cancel();
            tick = null;
        }

        smsMgr.saveState();
    }


    @Override
    protected void onResume() {
        Log.d("LOG", "...onResume...");
        smsMgr.restoreState();

        if (smsMgr.parkingActive()){
            Log.d("LOG", "smsMgr.parkingActive...");
            smsMgr.showParkingScreen();
        } else {
            if (smsMgr.appStatus==SmsManager.STATUS_PARKING) {
                smsMgr.appStatus=SmsManager.STATUS_INITIAL;
                smsMgr.saveState();
            }
        }

        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        private View.OnClickListener keyboardListener = new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SmsManager smsMgr = ((MainActivity)getActivity()).smsMgr;

                Log.d("LOG", (String) ((Button) v).getText());
                String keyPressed = (String) ((Button) v).getText();
                if (keyPressed.equals("<-")) {
                    if (smsMgr.regNum.length()>0) {
                        smsMgr.regNum = smsMgr.regNum.substring(0,smsMgr.regNum.length()-1);
                    }
                } else {
                    smsMgr.regNum = smsMgr.regNum + keyPressed;
                }
                smsMgr.saveState();
                ((MainActivity)getActivity()).updateView();
            }
        };

        private View.OnClickListener buttonListListener = new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                MainActivity mainActivity = (MainActivity)getActivity();
                GeoManager geoMgr = mainActivity.geoMgr;
                PopupMenu popup = new PopupMenu(mainActivity,v);
                Menu mnu = popup.getMenu();

                // заполняем меню из xml с парковочными зонами
                ArrayList<ParkZone> zones = geoMgr.getParkZoneList();

                for(ParkZone zone : zones){
                    mnu.add(0,zone.getZoneNumber(),zone.getZoneNumber(),zone.getZoneNumber().toString());
                }

                popup.setOnMenuItemClickListener(menuListener);
                popup.getMenuInflater().inflate(R.menu.menu_zone, mnu);

                popup.show();
            }
        };

        PopupMenu.OnMenuItemClickListener menuListener = new PopupMenu.OnMenuItemClickListener(){
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                MainActivity mainActivity = (MainActivity)getActivity();
                SmsManager smsMgr = mainActivity.smsMgr;
                GeoManager geoMgr = mainActivity.geoMgr;

                smsMgr.currentZone = geoMgr.getParkZone(item.getItemId());

                if ( (smsMgr.appStatus==SmsManager.STATUS_SMS_NOT_SENT) ||(smsMgr.appStatus==SmsManager.STATUS_SMS_NOT_RECEIVED)) smsMgr.appStatus=SmsManager.STATUS_INITIAL;

                smsMgr.saveState();
                mainActivity.updateView();
                return false;
            }
        } ;

        private View.OnClickListener buttonGPSListener = new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                MainActivity mainActivity = (MainActivity)getActivity();

                if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(mainActivity, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, PERMISSION_ACCESS_FINE_LOCATION);
                }

                mainActivity.tryToGetParkZone();

            }
        };

        private View.OnClickListener buttonHourListener = new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                MainActivity mainActivity = (MainActivity)getActivity();
                SmsManager smsMgr = mainActivity.smsMgr;

                if ( (smsMgr.appStatus==SmsManager.STATUS_SMS_NOT_SENT) ||(smsMgr.appStatus==SmsManager.STATUS_SMS_NOT_RECEIVED)) smsMgr.appStatus=SmsManager.STATUS_INITIAL;

                if (v.getId()==R.id.buttonPlus) {
                    smsMgr.hours = String.valueOf((Integer.parseInt(smsMgr.hours) + 1));
                } else {
                    smsMgr.hours = String.valueOf((Integer.parseInt(smsMgr.hours) - 1));
                }

                Log.d("LOG","smsMgr.hours = " + smsMgr.hours );

                smsMgr.saveState();
                mainActivity.updateView();
            }
        };

        private View.OnClickListener buttonPayListener = new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                MainActivity mainActivity = (MainActivity)getActivity();

                if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.READ_SMS},  MY_PERMISSIONS_REQUEST_READ_SMS);
                }

                SmsManager smsMgr = mainActivity.smsMgr;
                smsMgr.updateSms();
                Log.d("LOG", "sms = " + smsMgr.sms);
                Uri uri = Uri.parse("smsto:" + getResources().getString(R.string.smsNumber));
                Intent it = new Intent(Intent.ACTION_SENDTO, uri);
                it.putExtra("sms_body", smsMgr.sms);
                startActivity(it);

                smsMgr.sendDate = new Date();
                smsMgr.appStatus = SmsManager.STATUS_WAITING_OUT;
                smsMgr.saveState();
            }
        };

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onResume() {
            Log.d("LOG", "Fragment onResume...");
            ((MainActivity)getActivity()).updateView();
            super.onResume();
        }

        @Override
        public void onDestroyView() {
            int position = getArguments().getInt(ARG_SECTION_NUMBER);
            Log.d("LOG", "View " + position + " destroyed");
            ((MainActivity)getActivity()).views.delete(position);

            super.onDestroyView();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = null;
            int position = getArguments().getInt(ARG_SECTION_NUMBER);
            switch (position){
                case 1:
                    rootView = inflater.inflate(R.layout.fragment_1, container, false);
                    (rootView.findViewById(R.id.button0)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.button1)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.button2)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.button3)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.button4)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.button5)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.button6)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.button7)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.button8)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.button9)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.buttonA)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.buttonB)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.buttonE)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.buttonK)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.buttonM)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.buttonH)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.buttonO)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.buttonP)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.buttonC)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.buttonT)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.buttonY)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.buttonX)).setOnClickListener(keyboardListener);
                    (rootView.findViewById(R.id.buttonBack)).setOnClickListener(keyboardListener);

                    ((MainActivity)getActivity()).views.append(position,rootView);
                    break;
                case 2:
                    rootView = inflater.inflate(R.layout.fragment_2, container, false);

                    (rootView.findViewById(R.id.buttonList)).setOnClickListener(buttonListListener);
                    (rootView.findViewById(R.id.buttonGPS)).setOnClickListener(buttonGPSListener);
                    (rootView.findViewById(R.id.buttonPlus)).setOnClickListener(buttonHourListener);
                    (rootView.findViewById(R.id.buttonMinus)).setOnClickListener(buttonHourListener);

                    ((MainActivity)getActivity()).views.append(position, rootView);
                    break;
                case 3:
                    rootView = inflater.inflate(R.layout.fragment_3, container, false);
                    ((MainActivity)getActivity()).views.append(position, rootView);

                    (rootView.findViewById(R.id.buttonPay)).setOnClickListener(buttonPayListener);
                    break;
            }
            Log.d("LOG", "View " + position + " created");

            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }
    }


    private void updateView(){
        smsMgr.restoreState();
        //Log.d("LOG","Статус = " + smsMgr.appStatus);

        Resources res = getResources();
        View buttonGPS = this.findViewById(R.id.buttonGPS);
        View progressBar = findViewById(R.id.progressBar);
        TextView statusMessage = (TextView) this.findViewById(R.id.statusMessage);

        for(int i = 0; i < views.size(); i++) {
            int key = views.keyAt(i);
            // get the object by the key.
            View view = views.get(key);
            switch (key) {
                case 1:
                    // Рег. номер
                    ((TextView) view.findViewById(R.id.regNumText)).setText(smsMgr.regNum);
                    // Клавиатура
                    switch (smsMgr.regNum.length()) {
                        case 0:
                        case 4:
                        case 5:
                            setLettersEnabled(view, true);
                            setDigitsEnabled(view, false);
                            break;
                        case 1:
                        case 2:
                        case 3:
                        case 6:
                        case 7:
                        case 8:
                            setLettersEnabled(view, false);
                            setDigitsEnabled(view, true);
                            break;
                        default:
                            setLettersEnabled(view, false);
                            setDigitsEnabled(view, false);
                            break;
                    }
                    break;
                case 2:
                    if (buttonGPS != null) buttonGPS.setEnabled(true);

                    TextView zoneDesc = (TextView) this.findViewById(R.id.zoneDesc);
                    if (smsMgr.currentZone != null) {
                        ((TextView) view.findViewById(R.id.parkNumText)).setText(String.format(Locale.getDefault(), "%d",smsMgr.currentZone.getZoneNumber()));
                        if (zoneDesc != null ) {
                            zoneDesc.setText(smsMgr.currentZone.getZoneDesc());
                            zoneDesc.setTextColor(Color.BLACK);
                        }
                    } else {
                        ((TextView) view.findViewById(R.id.parkNumText)).setText("");
                        if (zoneDesc != null ) {
                            zoneDesc.setText(R.string.parking_undefined);
                            zoneDesc.setTextColor(Color.RED);
                        }
                    }

                    if (Integer.parseInt(smsMgr.hours) <= 1) {
                        (view.findViewById(R.id.buttonMinus)).setEnabled(false);
                        ((TextView) view.findViewById(R.id.hourText)).setText(String.format(res.getString(R.string.one_hour),smsMgr.hours));
                    } else if (Integer.parseInt(smsMgr.hours) >= 8) {
                        (view.findViewById(R.id.buttonPlus)).setEnabled(false);
                        ((TextView) view.findViewById(R.id.hourText)).setText(String.format(res.getString(R.string.five_hours),smsMgr.hours));
                    } else if (Integer.parseInt(smsMgr.hours) >= 5) {
                        (view.findViewById(R.id.buttonMinus)).setEnabled(true);
                        (view.findViewById(R.id.buttonPlus)).setEnabled(true);
                        ((TextView) view.findViewById(R.id.hourText)).setText(String.format(res.getString(R.string.five_hours),smsMgr.hours));
                    } else {
                        (view.findViewById(R.id.buttonMinus)).setEnabled(true);
                        (view.findViewById(R.id.buttonPlus)).setEnabled(true);
                        ((TextView) view.findViewById(R.id.hourText)).setText(String.format(res.getString(R.string.two_hours),smsMgr.hours));
                    }
                    break;
                case 3:
                    (view.findViewById(R.id.buttonPay)).setEnabled(true);
                    ((TextView) view.findViewById(R.id.regNumText)).setText(smsMgr.regNum);
                    TextView parkNumText =(TextView) view.findViewById(R.id.parkNumText);
                    if (smsMgr.currentZone != null) {
                        parkNumText.setText(String.format(Locale.getDefault(), "%d",smsMgr.currentZone.getZoneNumber()));
                        parkNumText.setTextColor(ContextCompat.getColor(this, R.color.colorMaterialGrey));

                    } else {
                        parkNumText.setText(R.string.undefined);
                        parkNumText.setTextColor(Color.RED);
                        (view.findViewById(R.id.buttonPay)).setEnabled(false);
                    }

                    if (Integer.parseInt(smsMgr.hours) == 1) {
                        ((TextView) view.findViewById(R.id.hourText)).setText(String.format(res.getString(R.string.one_hour),smsMgr.hours));
                    }
                    else if ((Integer.parseInt(smsMgr.hours) > 1) && (Integer.parseInt(smsMgr.hours) < 5)) {
                        ((TextView) view.findViewById(R.id.hourText)).setText(String.format(res.getString(R.string.two_hours),smsMgr.hours));
                    } else {
                        ((TextView) view.findViewById(R.id.hourText)).setText(String.format(res.getString(R.string.five_hours),smsMgr.hours));
                    }

                    switch (smsMgr.appStatus) {
                        case SmsManager.STATUS_INITIAL:
                            if (progressBar != null) progressBar.setVisibility(View.INVISIBLE);
                            if (statusMessage != null) {
                                statusMessage.setText(smsMgr.statusMessage);
                                statusMessage.setTextColor(Color.RED);
                            }
                            break;
                        case SmsManager.STATUS_WAITING_OUT:
                            Log.d("LOG", "waiting outgoing sms...");
                            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                            if (statusMessage != null) {
                                statusMessage.setText(getResources().getString(R.string.outgoingSmsWaiting));
                                statusMessage.setTextColor(Color.BLACK);
                            }
                            break;
                        case SmsManager.STATUS_WAITING_IN:
                            Log.d("LOG", "waiting incoming sms...");
                            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                            if (statusMessage != null) {
                                statusMessage.setText(getResources().getString(R.string.incomingSmsWaiting));
                                statusMessage.setTextColor(Color.BLACK);
                            }
                            break;
                        case SmsManager.STATUS_SMS_SENT:
                            Log.d("LOG", "sms was sent...");
                            if (progressBar != null) progressBar.setVisibility(View.INVISIBLE);
                            if (statusMessage != null) {
                                statusMessage.setText(getResources().getString(R.string.sendSmsWaiting));
                                statusMessage.setTextColor(Color.BLACK);
                            }
                            break;
                        case SmsManager.STATUS_SMS_NOT_SENT:
                            Log.d("LOG", "sms wasn't sent...");
                            if (progressBar != null) progressBar.setVisibility(View.INVISIBLE);
                            if (statusMessage != null) {
                                statusMessage.setText(getResources().getString(R.string.sendSmsFailed));
                                statusMessage.setTextColor(Color.RED);
                            }
                            break;
                        case SmsManager.STATUS_SMS_NOT_RECEIVED:
                            Log.d("LOG", "sms wasn't received...");
                            if (progressBar != null) progressBar.setVisibility(View.INVISIBLE);
                            if (statusMessage != null) {
                                statusMessage.setText(getResources().getString(R.string.incomingSmsFailed));
                                statusMessage.setTextColor(Color.RED);
                            }
                            break;
                        case SmsManager.STATUS_SMS_PERMISSION_NOT_GRANTED:
                            Log.d("LOG", "permission not granted...");
                            if (progressBar != null) progressBar.setVisibility(View.INVISIBLE);
                            if (statusMessage != null) {
                                statusMessage.setText(getResources().getString(R.string.permissionNotGranted));
                                statusMessage.setTextColor(Color.RED);
                            }
                            break;
                    }
                    break;
            }
        }
    }

    private class UpdateTickTask extends TimerTask {
        public void run() {
            tickHandler.sendEmptyMessage(0);
        }
    }

    final Handler tickHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            // обрабатываем сообщение таймера
            //Log.d("LOG", "***tick! ");

            if(smsMgr.appStatus==SmsManager.STATUS_WAITING_OUT){
                // ждем исходящее смс
                Log.d("LOG", "***ждем исходящее смс");

                if (smsMgr.IsSent(getResources().getString(R.string.smsNumber))){
                    // обнаружили, что смс отправлена. Меняем статус на ожидание входящего смс
                    smsMgr.appStatus=SmsManager.STATUS_WAITING_IN;
                    smsMgr.sendDate = new Date();
                    smsMgr.saveState();
                }


                Log.d("LOG","тиков ожидания: " + (int)((new Date().getTime()-smsMgr.sendDate.getTime())/TICK_INTERVAL));
                if ((int)((new Date().getTime()-smsMgr.sendDate.getTime())/TICK_INTERVAL)>=MAX_TICK_WAITING){
                    // время ожидания исходящего смс истекло
                    Log.d("LOG","время ожидания исходящего смс истекло");
                    smsMgr.appStatus=SmsManager.STATUS_SMS_NOT_SENT;
                    smsMgr.saveState();
                }
            }

            if(smsMgr.appStatus==SmsManager.STATUS_WAITING_IN){
                // ждем входящее смс
                Log.d("LOG", "***ждем входящее смс");

                String smsText = smsMgr.GetIncomingSms(getResources().getString(R.string.smsNumber));
                if (smsText!=null){
                    // какая-то смс с искомого номера пришла...
                    if (smsText.contains(getResources().getString(R.string.smsOrderPaid))){
                        // если смс именно с подтверждением оплаты, то меняем интерфейс на "припарковано"
                        smsMgr.sendDate = new Date();
                        smsMgr.startParkingDate = smsMgr.sendDate;
                        smsMgr.appStatus = SmsManager.STATUS_PARKING;
                        smsMgr.saveState();
                        smsMgr.startParking();
                    } else {
                        // если какая-то другая смс - просто выводим ее содержимое
                        smsMgr.statusMessage = smsText;
                        smsMgr.appStatus = SmsManager.STATUS_INITIAL;
                        smsMgr.saveState();
                    }

                }
                Log.d("LOG","тиков ожидания: " + (int)((new Date().getTime()-smsMgr.sendDate.getTime())/TICK_INTERVAL));
                if ((int)((new Date().getTime()-smsMgr.sendDate.getTime())/TICK_INTERVAL)>=(MAX_TICK_WAITING*2)){
                    // время ожидания исходящего смс истекло - все равно переходим на интерфейс "припарковано"
                    /*
                    smsMgr.appStatus=SmsManager.STATUS_SMS_NOT_RECEIVED;
                    smsMgr.saveState();
                    */
                    smsMgr.sendDate = new Date();
                    smsMgr.startParkingDate = smsMgr.sendDate;
                    smsMgr.appStatus = SmsManager.STATUS_PARKING;
                    smsMgr.saveState();
                    smsMgr.startParking();
                }

            }

            updateView();
            return false;
        }
    });


    private void setLettersEnabled(View view, boolean flag) {
        (view.findViewById(R.id.buttonA)).setEnabled(flag);
        (view.findViewById(R.id.buttonB)).setEnabled(flag);
        (view.findViewById(R.id.buttonE)).setEnabled(flag);
        (view.findViewById(R.id.buttonK)).setEnabled(flag);
        (view.findViewById(R.id.buttonM)).setEnabled(flag);
        (view.findViewById(R.id.buttonH)).setEnabled(flag);
        (view.findViewById(R.id.buttonO)).setEnabled(flag);
        (view.findViewById(R.id.buttonP)).setEnabled(flag);
        (view.findViewById(R.id.buttonC)).setEnabled(flag);
        (view.findViewById(R.id.buttonT)).setEnabled(flag);
        (view.findViewById(R.id.buttonY)).setEnabled(flag);
        (view.findViewById(R.id.buttonX)).setEnabled(flag);
    }

    private void setDigitsEnabled(View view, boolean flag) {
        (view.findViewById(R.id.button0)).setEnabled(flag);
        (view.findViewById(R.id.button1)).setEnabled(flag);
        (view.findViewById(R.id.button2)).setEnabled(flag);
        (view.findViewById(R.id.button3)).setEnabled(flag);
        (view.findViewById(R.id.button4)).setEnabled(flag);
        (view.findViewById(R.id.button5)).setEnabled(flag);
        (view.findViewById(R.id.button6)).setEnabled(flag);
        (view.findViewById(R.id.button7)).setEnabled(flag);
        (view.findViewById(R.id.button8)).setEnabled(flag);
        (view.findViewById(R.id.button9)).setEnabled(flag);
    }

    public void qClick(View view) {
        smsMgr.sendDate = new Date();
        smsMgr.startParkingDate = smsMgr.sendDate;
        smsMgr.appStatus = SmsManager.STATUS_PARKING;
        smsMgr.saveState();
        smsMgr.startParking();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_SMS:
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    smsMgr.appStatus = SmsManager.STATUS_SMS_PERMISSION_NOT_GRANTED;
                    smsMgr.saveState();
                    Log.d("LOG",".......permission not granted");
                }
                break;
            case PERMISSION_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Toast.makeText(this, "Location permission granted!", Toast.LENGTH_SHORT).show();
                    Log.d("LOG",".......Location permission granted");
                    geoMgr = new GeoManager(this);
                    this.tryToGetParkZone();
                } else {
                    Toast.makeText(this, "Необходимо разрешение на определение местонахождения!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void rightSlide(View view) {
        slide(1);
    }

    public void leftSlide(View view) {
        slide(-1);
    }

    private void slide(int direction) {
        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
        if (mViewPager != null) mViewPager.setCurrentItem(mViewPager.getCurrentItem()+direction);

    }

    private void tryToGetParkZone(){
        GeoManager geoMgr = this.geoMgr;
        SmsManager smsMgr = this.smsMgr;
        Log.d("LOG", geoMgr.getCoordinates());
        Toast.makeText(this, geoMgr.getCoordinates(), Toast.LENGTH_LONG).show();

        if ( (smsMgr.appStatus==SmsManager.STATUS_SMS_NOT_SENT) ||(smsMgr.appStatus==SmsManager.STATUS_SMS_NOT_RECEIVED)) smsMgr.appStatus=SmsManager.STATUS_INITIAL;

        smsMgr.currentZone = geoMgr.getParkZone();

        smsMgr.saveState();
        this.updateView();

    }
}
