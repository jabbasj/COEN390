package com.coen390.teamc.cardiograph;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;
import java.util.ArrayList;



public class MainActivity extends AppCompatActivity {

    /** Error handling **/
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private String BLUETOOTH_NOT_ENABLED = "BLUETOOTH NOT ENABLED!";
    private String NO_DEVICES_FOUND = "NO DEVICES FOUND!";
    private String ZEPHYR_NOT_CONNECTED = "ZEPHYR NOT CONNECTED!";
    private String NO_EMERGENCY_CONTACTS = "NO EMERGENCY CONTACTS!";
    private String SETTINGS_MISSING = "SETTINGS MISSING!";
    protected ArrayList<String> PROBLEMS_DETECTED;


    /** UI **/
    private ToolTipView mErrorWarningToolTipView;

    protected boolean batteryDialogShown = false;
    protected AlertDialog batteryLowDialog;

    static protected boolean alertContactsDialogShown = false;
    protected AlertDialog alertContactsDialog;
    protected CountDownTimer alertContactsCounter;

    protected boolean deviceDisconnectedDialogShown = false;
    protected AlertDialog deviceDisconnectedDialog;

    private FloatingActionButton warning_fab;
    private TextView warning_tv;
    protected TextView live_pulse_tv;
    protected TextView sensor_battery_tv;
    protected TextView counter_tv;
    static protected Button measure_btn;
    static protected  Button stop_measure_btn;
    static protected Button start_recording_btn;
    private Button view_data_btn;

    /** Backend **/
    protected DB_Helper myDBHelper;
    private BluetoothConnection mBluetoothConnection;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;
    private SharedPreferences prefs;

    static protected String mCurrentRecord = "";


    /******                 ENTRY                 ******/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setLogo(R.mipmap.ic_launcher);
        setSupportActionBar(toolbar);

        ask_user_permissions();

        myDBHelper = new DB_Helper(this);

        live_pulse_tv = (TextView)findViewById(R.id.live_pulse);
        mBluetoothConnection = new BluetoothConnection(this);

        initializeLowBatteryDialog();
        initializeAlertContactsDialog();
        initializeDeviceDisconnectedDialog();
        setPreferenceChangeListener();

        measure_btn = (Button) findViewById(R.id.measure);
        stop_measure_btn = (Button) findViewById(R.id.stop_measure);
        start_recording_btn = (Button) findViewById(R.id.start_record);
        view_data_btn = (Button) findViewById(R.id.view_data);
        warning_fab = (FloatingActionButton) findViewById(R.id.warning_fab);
        warning_tv = (TextView) findViewById(R.id.warning_tv);
        sensor_battery_tv = (TextView) findViewById(R.id.sensor_battery);


        warning_fab.setOnClickListener(new CustomClickLister());
        measure_btn.setOnClickListener(new CustomClickLister());
        view_data_btn.setOnClickListener(new CustomClickLister());
        start_recording_btn.setOnClickListener(new CustomClickLister());
        stop_measure_btn.setOnClickListener(new CustomClickLister());
    }


    /******                 FLOW CONTROL                  ******/

    @Override
    public void onResume(){
        super.onResume();
        detectProblems();
        mBluetoothConnection.setBluetoothListener();
        getSupportActionBar().setSubtitle(mCurrentRecord);

        if (!PROBLEMS_DETECTED.isEmpty()){
            showWarning();
            hideMeasureBtn();
            hideStopMeasureBtn();
            start_recording_btn.setCompoundDrawablesWithIntrinsicBounds(null, null , null, null);
            start_recording_btn.setVisibility(View.GONE);
            mBluetoothConnection.disconnectListener();
        } else {
            hideWarning();
            showMeasureBtn();
            showStopMeasureBtn();
        }
    }

    protected void detectProblems(){
        PROBLEMS_DETECTED = new ArrayList<>();

        if (!mBluetoothConnection.isBluetoothAvailable()){
            PROBLEMS_DETECTED.add(BLUETOOTH_NOT_ENABLED);
        }

        if (mBluetoothConnection.mZephyr == null) {
            PROBLEMS_DETECTED.add(ZEPHYR_NOT_CONNECTED);
        }

        if (mBluetoothConnection.mDeviceList.isEmpty()) {
            PROBLEMS_DETECTED.add(NO_DEVICES_FOUND);
        }

        if (getBooleanPreference("alert_contacts_switch", true)) {
            if (myDBHelper.getAllContacts().getCount() == 0) {
                PROBLEMS_DETECTED.add(NO_EMERGENCY_CONTACTS);
            }
        }

        String missing_setting = checkSettings();

        if (!missing_setting.equals("")) {
            PROBLEMS_DETECTED.add(SETTINGS_MISSING);
        }

    }

    private class CustomClickLister implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.measure:
                    showToast("Starting...");
                    mBluetoothConnection.connectListener();
                    mBluetoothConnection._bt.start();

                    showRecordBtn();
                    showToast("Started!");
                    break;

                case R.id.stop_measure:
                    mBluetoothConnection.disconnectListener();
                    if (mBluetoothConnection.recordMeasurement) {
                        showToast("Stopped!");
                    }
                    mBluetoothConnection.recordMeasurement = false;
                    hideRecordBtn();
                    break;

                case R.id.start_record:
                    if (mCurrentRecord != "") {

                        mBluetoothConnection.recordMeasurement = true;
                        alertContactsDialogShown = false;
                        mBluetoothConnection.reset_problem_detection();
                        Drawable left = getResources().getDrawable(android.R.drawable.presence_online);
                        start_recording_btn.setCompoundDrawablesWithIntrinsicBounds(left, null, null, null);
                        showToast("Recording!");
                    } else {
                        showToast("Create New Record first!");
                        Intent data_graph_page = new Intent(MainActivity.this, DataGraph.class);
                        startActivity(data_graph_page);
                    }
                    break;

                case R.id.view_data:
                    Intent data_graph_page = new Intent(MainActivity.this, DataGraph.class);
                    startActivity(data_graph_page);
                    break;

                case R.id.warning_fab:
                    if (BLUETOOTH_NOT_ENABLED.equals(PROBLEMS_DETECTED.get(0))) {

                        mBluetoothConnection.enableBluetooth();

                    } else if (NO_DEVICES_FOUND.equals(PROBLEMS_DETECTED.get(0)) || ZEPHYR_NOT_CONNECTED.equals(PROBLEMS_DETECTED.get(0))) {

                        if (mBluetoothConnection.mBluetoothAdapter.isDiscovering()) {
                            showToast("Scanning takes a few seconds!");
                        } else {
                            mBluetoothConnection.mBluetoothAdapter.startDiscovery();
                        }
                    } else if (NO_EMERGENCY_CONTACTS.equals(PROBLEMS_DETECTED.get(0))){
                        Intent contacts_page = new Intent(MainActivity.this, ContactsPage.class);
                        startActivity(contacts_page);
                    } else if (SETTINGS_MISSING.equals(PROBLEMS_DETECTED.get(0))) {
                        showToast(checkSettings() + " setting missing!");
                        Intent settings_page = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(settings_page);
                    }
                    break;
            }
        }
    }


    /******                 OVERRIDES                  ******/

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy(){
        mBluetoothConnection.disconnectListener();
        unregisterReceiver(mBluetoothConnection.mReceiver);
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            Intent main_settings_intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(main_settings_intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    /******                 UI                  ******/

    protected void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    static protected void showMeasureBtn(){
        measure_btn.setVisibility(View.VISIBLE);
    }

    static protected void hideMeasureBtn() {
        measure_btn.setVisibility(View.GONE);
    }

    static protected void showStopMeasureBtn() {
        stop_measure_btn.setVisibility(View.VISIBLE);
    }

    static protected void hideStopMeasureBtn() {
        stop_measure_btn.setVisibility(View.GONE);
    }

    static protected void showRecordBtn() {
        start_recording_btn.setVisibility(View.VISIBLE);
        hideMeasureBtn();
    }

    static protected void hideRecordBtn() {
        start_recording_btn.setVisibility(View.GONE);
        start_recording_btn.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        showMeasureBtn();
    }





    /******              WARNING UI               ******/

    private void showWarning(){
        warning_fab.show();
        warning_tv.setVisibility(View.VISIBLE);
        warning_tv.setText(PROBLEMS_DETECTED.get(0));
        showErrorWarningTooltip();
    }

    private void hideWarning(){
        warning_fab.hide();
        warning_tv.setVisibility(View.GONE);
        hideErrorWarningTooltip();
    }

    private void hideErrorWarningTooltip() {
        if (mErrorWarningToolTipView != null) {
            mErrorWarningToolTipView.remove();
        }
    }

    private void showErrorWarningTooltip() {

        hideErrorWarningTooltip();

        ToolTipRelativeLayout mToolTipFrameLayout = (ToolTipRelativeLayout) findViewById(R.id.tooltipRelativeLayout);

        ToolTip toolTip = new ToolTip()
                .withText("Click to Resolve !")
                .withColor(Color.parseColor("#ffdada"))
                .withTextColor(Color.BLACK)
                .withShadow()
                .withAnimationType(ToolTip.AnimationType.FROM_TOP);

        mErrorWarningToolTipView = mToolTipFrameLayout.showToolTipForView(toolTip, findViewById(R.id.warning_fab));
    }

    private void initializeLowBatteryDialog() {
        batteryLowDialog = new AlertDialog.Builder(this)
                .setTitle("Battery Low!")
                .setMessage("Please recharge ZEPHYR..")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        batteryDialogShown = false;
                        onResume();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //batteryDialogShown = false; escape alerts ?
                        onResume();
                    }
                })
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert).create();
    }

    protected void showBatteryLowDialog(){
        if (!batteryDialogShown) {

            boolean notifications_enabled = getBooleanPreference("notifications_enabled", true);
            if (notifications_enabled) {

                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                                .setContentTitle("Cardiograph")
                                .setContentText("Battery Low!");

                Intent resultIntent = new Intent(this, MainActivity.class);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent resultPendingIntent =
                        PendingIntent.getActivity(
                                this,
                                0,
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                mBuilder.setContentIntent(resultPendingIntent);

                NotificationManager mNotifyMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                mNotifyMgr.notify(777, mBuilder.build());
                playAlertRingTone();
                if (getBooleanPreference("notifications_vibrate", true)) {
                    vibratePhone();
                }
            }

            batteryLowDialog.show();
            batteryDialogShown = true;
        }
    }

    private void initializeAlertContactsDialog() {
        counter_tv = new TextView (this);

        alertContactsDialog = new AlertDialog.Builder(this)
                .setView(counter_tv)
                .setTitle("Danger Detected!")
                .setMessage("Do you want to send an automated text message warning to your emergency contacts?\n\n" +
                        "If danger persists for 2 minutes and you do not cancel, the messages will be sent.\n")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        alertContactsDialogShown = false;
                        mBluetoothConnection.reset_problem_detection();
                        alertContactsCounter.cancel();
                        alertContactsCounter.onFinish();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertContactsCounter.cancel();
                        showToast("Contacts will no longer be alerted!");
                    }
                })
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert).create();
    }

    protected void showAlertContactsDialog(final String danger_message) {
        if (!alertContactsDialogShown) {

            boolean notifications_enabled = getBooleanPreference("notifications_enabled", true);
            boolean alert_contacts_enabled = getBooleanPreference("alert_contacts_switch", false);

            if (notifications_enabled) {
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                                .setContentTitle("Cardiograph")
                                .setContentText(danger_message);

                Intent resultIntent = new Intent(this, MainActivity.class);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent resultPendingIntent =
                        PendingIntent.getActivity(
                                this,
                                0,
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                mBuilder.setContentIntent(resultPendingIntent);

                NotificationManager mNotifyMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                mNotifyMgr.notify(888, mBuilder.build());
                playAlertRingTone();
                if (getBooleanPreference("notifications_vibrate", true)) {
                    vibratePhone();
                }
            }

            if (alert_contacts_enabled) {
                alertContactsDialog.show();
                alertContactsCounter = new CountDownTimer(120000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        counter_tv.setText("Time remaining: " + (millisUntilFinished / 1000) + " seconds");
                    }

                    @Override
                    public void onFinish() {
                        alertContacts(danger_message);
                    }
                };
                alertContactsCounter.start();
            }

            alertContactsDialogShown = true;
        }
    }

    private void initializeDeviceDisconnectedDialog() {
        deviceDisconnectedDialog = new AlertDialog.Builder(this)
                .setTitle("Device Disconnected!")
                .setMessage("Make sure device is strapped and restart!")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deviceDisconnectedDialogShown = false;
                        stop_measure_btn.callOnClick();
                        onResume();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deviceDisconnectedDialogShown = false;
                        onResume();
                    }
                })
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert).create();
    }

    protected void showDeviceDisconnectedDialog() {
        if (!deviceDisconnectedDialogShown) {

            boolean notifications_enabled = getBooleanPreference("notifications_enabled", true);
            if (notifications_enabled) {

                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                                .setContentTitle("Cardiograph")
                                .setContentText("Device Disconnected!");

                Intent resultIntent = new Intent(this, MainActivity.class);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent resultPendingIntent =
                        PendingIntent.getActivity(
                                this,
                                0,
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                mBuilder.setContentIntent(resultPendingIntent);

                NotificationManager mNotifyMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                mNotifyMgr.notify(666, mBuilder.build());
                playAlertRingTone();
                if (getBooleanPreference("notifications_vibrate", true)) {
                    vibratePhone();
                }
            }
            deviceDisconnectedDialog.show();
            deviceDisconnectedDialogShown = true;
        }
    }

    private void playAlertRingTone() {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        String strRingtonePreference = preference.getString("notifications_ringtone", "");

        if (!strRingtonePreference.equals("")) {
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), Uri.parse(strRingtonePreference));
            r.play();
        }
    }

    private void vibratePhone() {
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);
    }

    /******              LOCATION               ******/

    @TargetApi(23)
    private void ask_user_permissions() {

        int hasCoarseLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }

        int hasSendSMSPermission = checkSelfPermission(Manifest.permission.SEND_SMS);

        if (hasSendSMSPermission != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch(requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ask_user_permissions();
                } else {
                    //location permission denied
                    showToast("App cannot work without permissions!");
                    ask_user_permissions();
                }
        }
    }

    private void setPreferenceChangeListener(){
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

                if(key.equals("battery_level")) {
                    batteryDialogShown = false;
                }

                if(key.equals("max_heart_rate") || key.equals("min_heart_rate")) {
                    DataGraph.removeLimitLines();
                }

                if (key.equals("alert_contacts_switch")) {
                    alertContactsDialogShown = false;
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }

    protected String getStringPreference(String pref_string, String default_string) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getString(pref_string, default_string);
    }

    protected Boolean getBooleanPreference(String pref_string, boolean default_val) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(pref_string, default_val);
    }

    protected String checkSettings() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String setting_string = "";
        try {
            setting_string = "Battery Level";
            Integer.parseInt(sp.getString("battery_level", ""));

            if (getBooleanPreference("alert_contacts_switch", true)) {
                setting_string = "Max HRV Score";
                Integer.parseInt(sp.getString("max_hrv_score", ""));
                setting_string = "HRV Score Duration";
                Integer.parseInt(sp.getString("hrv_score_duration", ""));
                setting_string = "Min Heart Rate";
                Integer.parseInt(sp.getString("min_heart_rate", ""));
                setting_string = "Min Heart Rate Duration";
                Integer.parseInt(sp.getString("min_heart_rate_duration", ""));
                setting_string = "Max Heart Rate";
                Integer.parseInt(sp.getString("max_heart_rate", ""));
                setting_string = "Max Heart Rate Duration";
                Integer.parseInt(sp.getString("max_heart_rate_duration", ""));
            }

        }catch (Exception e) {
            return setting_string;
        }
        return "";
    }

    protected void alertContacts(String danger_message) {
        SmsManager smsManager = SmsManager.getDefault();
        PendingIntent sentPI;
        Cursor cursor = myDBHelper.getAllContacts();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String contactName = cursor.getString(0);
                String contactPhone = cursor.getString(1);
                sentPI = PendingIntent.getBroadcast(this, 0,new Intent("android.telephony.SmsManager.STATUS_ON_ICC_SENT"), 0);
                String sms_message = "This is an automated msg from " + getStringPreference("user_name", "UNDEFINED")
                        + "'s Cardiograph. " + danger_message + ", please contact them ASAP.";
                smsManager.sendTextMessage(contactPhone, null, sms_message, sentPI, null);
            } while (cursor.moveToNext());
            cursor.close();
            showToast("Messages sent!");
            mBluetoothConnection.reset_problem_detection();

            if (cursor.getCount() == 0) {
                showToast("No contacts found!");
                onResume();
            }
        }
    }

}
