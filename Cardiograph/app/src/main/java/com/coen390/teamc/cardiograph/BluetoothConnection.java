package com.coen390.teamc.cardiograph;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import zephyr.android.HxMBT.BTClient;
import zephyr.android.HxMBT.ZephyrProtocol;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class BluetoothConnection {

    protected BTClient _bt;
    protected ZephyrProtocol _protocol;
    protected NewConnectedListener _NConnListener;

    protected BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    protected ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    protected BluetoothDevice mZephyr;
    private Context mMainPageContext;
    private MainActivity mMainActivity;
    private ProgressDialog scanningDialog;

    static protected boolean recordMeasurement = false;
    private final int HEART_RATE = 0x100;
    private final int BATTERY_PERCENT = 0x102;
    private final int HEART_BEAT_TIMESTAMPS = 0x103;
    private final int HEART_BEAT_NUMBER = 0x104;

    protected int previous_hr_num = 0;
    protected ArrayList<DataGraph.heartRate> problem_heartRates = new ArrayList<>();
    protected int time_max_hr = 0;
    protected int time_min_hr = 0;

    protected ArrayList<DataGraph.rrInterval> problem_HRVScores = new ArrayList<>();
    protected int time_max_hrv = 0;

    protected ArrayList<DataGraph.rrInterval> last_two_RR_intervals = new ArrayList<>();
    protected double old_RMSSD = 0;
    protected int total_RR_intervals = 0;

    protected int MAX_HR_LIMIT ;
    protected int MAX_HR_DURATION;

    protected int MIN_HR_LIMIT;
    protected int MIN_HR_DURATION;

    protected int MAX_HRV_SCORE;
    protected int HRV_SCORE_DURATION;

    protected String danger_cause = "";

    protected BluetoothConnection(Context ctx) {
        mMainPageContext = ctx;
        mMainActivity = (MainActivity)ctx;

        scanningDialog = new ProgressDialog(mMainActivity);
        scanningDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        scanningDialog.setMessage("Scanning. Please wait...");
        scanningDialog.setIndeterminate(true);
        scanningDialog.setCanceledOnTouchOutside(false);
    }

    protected boolean isBluetoothAvailable() {
        return (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled());
    }

    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void connectWithZephyr() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mMainPageContext);
        String previous_mac = sp.getString("zephyr_mac_address", "");

        for (int i = 0; i < mDeviceList.size(); i++) {

            if (previous_mac.equals(mDeviceList.get(i).getAddress())) {
                /** previous mac address found **/
                mZephyr = mDeviceList.get(i);
                if (mZephyr.getBondState() != BluetoothDevice.BOND_BONDED) {
                    pairDevice(mZephyr);
                }

                /** update name in case it has changed **/
                SharedPreferences.Editor prefEditor = sp.edit();
                prefEditor.putString("zephyr_name", mZephyr.getName());
                prefEditor.commit();

                mMainActivity.onResume();
                break;

            } else if ((mDeviceList.get(i).getName() != null && mDeviceList.get(i).getName().startsWith("HXM"))
                    || (mDeviceList.get(i).getName() != null && sp.getString("zephyr_name", "HXM").equals(mDeviceList.get(i).getName()))) {

                /** Ask if this device that starts with 'HXM' or has predefined name is theirs **/
                showSaveDeviceDialog(mDeviceList.get(i));

                if (mZephyr != null) {
                    break;
                }
            }
        }
    }

    protected void connectListener() {

        previous_hr_num = 0;
        time_max_hr = 0;
        time_min_hr = 0;
        problem_heartRates = new ArrayList<>();
        last_two_RR_intervals = new ArrayList<>();
        old_RMSSD = 0;
        total_RR_intervals = 0;

        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) mMainActivity.getSystemService(ns);
        nMgr.cancel(666);
        nMgr.cancel(777);
        nMgr.cancel(888);

        _bt = new BTClient(mBluetoothAdapter, mZephyr.getAddress());
        _NConnListener = new NewConnectedListener(Newhandler, Newhandler);
        _bt.addConnectedEventListener(_NConnListener);
    }

    protected void disconnectListener() {
        /*This disconnects listener from acting on received messages*/
        if (_bt != null) {
            _bt.removeConnectedEventListener(_NConnListener);
        /*Close the communication with the device & throw an exception if failure*/
            _bt.Close();
        }
    }

    final Handler Newhandler = new Handler(){
        public void handleMessage(Message msg) {
            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager nMgr = (NotificationManager) mMainActivity.getSystemService(ns);
            switch (msg.what)
            {
                case HEART_RATE:
                    String HeartRatetext = msg.getData().getString("HeartRate");

                    if (mMainActivity.live_pulse_tv != null) mMainActivity.live_pulse_tv.setText(HeartRatetext + " bpm");

                    if (recordMeasurement) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String currentTimeStamp = dateFormat.format(new Date());

                        if (Integer.parseInt(HeartRatetext) > 0) {
                            mMainActivity.myDBHelper.insertInstantaneousHeartRate(currentTimeStamp, HeartRatetext, mMainActivity.mCurrentRecord);

                            if (DataGraph.current_graph.equals("HR") && DataGraph.mChart.isShown()) {
                                /** UPDATE LIVE GRAPH **/
                                DataGraph.updateLiveHeartRate(currentTimeStamp, HeartRatetext, mMainActivity.mCurrentRecord);
                            }

                            /** Check if MAX/MIN Heart Rate reached **/
                            if (checkSettings()) {
                                int HR = Integer.parseInt(HeartRatetext);

                                    if (HR >= MAX_HR_LIMIT) {
                                        problem_heartRates.add(new DataGraph.heartRate(currentTimeStamp, HR, mMainActivity.mCurrentRecord));
                                        time_max_hr += 1;

                                        if (time_max_hr >= MAX_HR_DURATION) {
                                            /* problem */
                                            //mMainActivity.showToast("MAX HR REACHED FOR TOO LONG!");
                                            mMainActivity.showAlertContactsDialog("Pulse over: " + String.valueOf(HR) + " bpm for " + String.valueOf(MAX_HR_DURATION) + " secs");
                                            time_max_hr = 0;
                                            problem_heartRates = new ArrayList<>();
                                            danger_cause = "max_hr";
                                        }

                                    } else {
                                        time_max_hr = 0;
                                        problem_heartRates = new ArrayList<>();

                                        if (mMainActivity.alertContactsDialog.isShowing() && danger_cause.equals("max_hr")) {
                                            mMainActivity.alertContactsDialogShown = false;
                                            mMainActivity.alertContactsDialog.dismiss();
                                            mMainActivity.alertContactsCounter.cancel();
                                            nMgr.cancel(888);
                                        }
                                    }


                                    if (HR <= MIN_HR_LIMIT) {
                                        problem_heartRates.add(new DataGraph.heartRate(currentTimeStamp, HR, mMainActivity.mCurrentRecord));
                                        time_min_hr += 1;

                                        if (time_min_hr >= MIN_HR_DURATION) {
                                            /* problem */
                                            //mMainActivity.showToast("MIN HR REACHED FOR TOO LONG!");
                                            mMainActivity.showAlertContactsDialog("Pulse under: " + String.valueOf(HR) + " bpm for " + String.valueOf(MIN_HR_DURATION) + " secs");
                                            time_max_hr = 0;
                                            problem_heartRates = new ArrayList<>();
                                            danger_cause = "min_hr";
                                        }

                                    } else {
                                        time_min_hr = 0;
                                        problem_heartRates = new ArrayList<>();

                                        if (mMainActivity.alertContactsDialog.isShowing() && danger_cause.equals("min_hr")) {
                                            mMainActivity.alertContactsDialogShown = false;
                                            mMainActivity.alertContactsDialog.dismiss();
                                            mMainActivity.alertContactsCounter.cancel();
                                            nMgr.cancel(888);
                                        }
                                    }
                            }
                        }

                    }
                    break;

                case BATTERY_PERCENT:
                    String BatteryPercent = msg.getData().getString("BatteryPercent");
                    mMainActivity.sensor_battery_tv.setText("Sensor Battery: " + BatteryPercent + "% ");
                    if (BatteryPercent != null) {

                        updateSensorBatteryIcon(Integer.parseInt(BatteryPercent));

                        /** CHECK IF BATTERY LOW **/
                        String min_battery_level = mMainActivity.getStringPreference("battery_level", "15");
                        try {
                            Integer.parseInt(min_battery_level);
                        } catch (Exception e) {
                            min_battery_level = "15";
                        }
                        if (Integer.parseInt(BatteryPercent) < Integer.parseInt(min_battery_level) && Integer.parseInt(BatteryPercent) != 0) {

                            mMainActivity.showBatteryLowDialog(); //show battery dialog and notification

                        } else {
                            if (mMainActivity.batteryLowDialog.isShowing()) {
                                mMainActivity.batteryLowDialog.dismiss(); //remove battery dialog
                                mMainActivity.batteryDialogShown = false;
                                nMgr.cancel(777); //cancel battery notification
                            }
                        }

                        /** CHECK IF DEVICE DISCONNECTED **/
                        if (Integer.parseInt(BatteryPercent) == 0 && recordMeasurement) {

                            mMainActivity.showDeviceDisconnectedDialog(); // show device disconnected dialog and notification

                        } else if (mMainActivity.deviceDisconnectedDialog.isShowing()) {
                            mMainActivity.deviceDisconnectedDialog.dismiss(); //remove disconnected dialog
                            mMainActivity.deviceDisconnectedDialogShown = false;
                            nMgr.cancel(666); //cancel disconnected notification
                        }
                    }
                    break;

                case HEART_BEAT_TIMESTAMPS:
                    int[] HeartBeatTimeStamps = msg.getData().getIntArray("HeartbeatTimeStamps");
                    /*  The Timestamp rolls over after 65535 ms */

                    int HeartBeatNumber = msg.getData().getInt("HeartbeatNumber");
                    /* The count rolls over after 255 */

                    if (previous_hr_num == 0) {
                        previous_hr_num = HeartBeatNumber;
                    }

                    /*unsigned int */
                    int new_HR_beats = HeartBeatNumber - previous_hr_num;
                    if (new_HR_beats < 0) {
                        //overflow occured
                        new_HR_beats = 256 + new_HR_beats;
                    }
                    //Log.d("new heart beats", String.valueOf(new_HR_beats));

                    if ( new_HR_beats != 0 && new_HR_beats < 14) {
                        if (recordMeasurement) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String currentTimeStamp = dateFormat.format(new Date());

                            for (int i = 0; i < new_HR_beats; i++) {
                                int RR = HeartBeatTimeStamps[i] - HeartBeatTimeStamps[i + 1];
                                if (RR < 0) {
                                    //overflow occured
                                    RR = 65536 + RR;
                                }
                                Log.d("Latest R-R interval", "         " + String.valueOf(RR));
                                if (RR > 0) {
                                    double RMSSD = 0;
                                    last_two_RR_intervals.add(new DataGraph.rrInterval(currentTimeStamp, RR, mMainActivity.mCurrentRecord, 0));

                                    RMSSD = computeRMSSD();
                                    Log.d("RMSSD", String.valueOf(RMSSD));
                                    total_RR_intervals+=1;

                                    if (RMSSD > 0) {
                                        mMainActivity.myDBHelper.insertRRInterval(currentTimeStamp, String.valueOf(RR), mMainActivity.mCurrentRecord, String.valueOf(RMSSD));

                                        if (DataGraph.current_graph.equals("ECG") && DataGraph.mChart.isShown()) {
                                            DataGraph.updateLiveECG(currentTimeStamp, String.valueOf(RR), mMainActivity.mCurrentRecord, String.valueOf(RMSSD));
                                        }

                                        if (DataGraph.current_graph.equals("HRV") && DataGraph.mChart.isShown()) {
                                            DataGraph.updateLiveHRV(currentTimeStamp, String.valueOf(RR), mMainActivity.mCurrentRecord, String.valueOf(RMSSD));
                                        }

                                        /** Check if MAX HRV Score reached **/
                                        if (checkSettings()) {
                                            if (RMSSD >= MAX_HRV_SCORE) {
                                                problem_HRVScores.add(new DataGraph.rrInterval(currentTimeStamp, RR, mMainActivity.mCurrentRecord, RMSSD));
                                                time_max_hrv += 1;

                                                if (time_max_hrv >= HRV_SCORE_DURATION) {
                                                    /* problem */
                                                    //mMainActivity.showToast("MAX HRV REACHED FOR TOO LONG!");

                                                    mMainActivity.showAlertContactsDialog("HRV over: " + String.valueOf((float)RMSSD) + " ms for " + String.valueOf(HRV_SCORE_DURATION) + " beats");
                                                    //initiate countdown for alerting contacts

                                                    time_max_hrv = 0;
                                                    problem_HRVScores = new ArrayList<>();

                                                    danger_cause = "max_hrv";
                                                }

                                            } else {
                                                time_max_hrv = 0;
                                                problem_HRVScores = new ArrayList<>();

                                                if (mMainActivity.alertContactsDialog.isShowing() && danger_cause.equals("max_hrv")) {
                                                    mMainActivity.alertContactsDialogShown = false;
                                                    mMainActivity.alertContactsDialog.dismiss();
                                                    mMainActivity.alertContactsCounter.cancel();
                                                    nMgr.cancel(888);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    previous_hr_num = HeartBeatNumber;
                    break;
            }
        }

    };

    double computeRMSSD() {
        double RMSSD = 0;
        double SUM_SSD = 0;
        if ( last_two_RR_intervals.size() >= 2 ) {

            if (old_RMSSD == 0) {
                RMSSD = MAX_HRV_SCORE / 2;
                /*
                for (int i = 0; i < last_two_RR_intervals.size() - 1; i++) {
                    double SD = last_two_RR_intervals.get(i + 1).mRRInterval - last_two_RR_intervals.get(i).mRRInterval;
                    double SSD = Math.pow(SD, 2);
                    SUM_SSD += SSD;
                }
                RMSSD = Math.sqrt(SUM_SSD / (last_two_RR_intervals.size() - 1));*/

            } else {

                int i = last_two_RR_intervals.size() - 1;
                int SD = last_two_RR_intervals.get(i).mRRInterval - last_two_RR_intervals.get(i - 1).mRRInterval;

                if (Math.abs(SD) < 300) {
                    RMSSD = Math.sqrt((Math.pow(old_RMSSD, 2) * (total_RR_intervals - 1) + Math.pow(SD, 2)) / (total_RR_intervals));
                }
                last_two_RR_intervals.remove(i);
                last_two_RR_intervals.remove(i - 1);

                /** Make it rise slower **/
                if (RMSSD > old_RMSSD) {
                    RMSSD = old_RMSSD + ((RMSSD - old_RMSSD) / 2);
                }
            }
        }

        if (RMSSD == 0) {
            return old_RMSSD;
        }

        old_RMSSD = RMSSD;
        return RMSSD;
    }

    private void showSaveDeviceDialog(final BluetoothDevice device){
        new AlertDialog.Builder(mMainPageContext)
                .setTitle("Is this your device?")
                .setMessage(device.getName() + " - " + device.getAddress())
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mZephyr = device;
                        if (mZephyr.getBondState() != BluetoothDevice.BOND_BONDED) {
                            pairDevice(mZephyr);
                        }
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mMainPageContext);
                        SharedPreferences.Editor prefEditor = sp.edit();
                        prefEditor.putString("zephyr_mac_address", mZephyr.getAddress());
                        prefEditor.putString("zephyr_name", mZephyr.getName());
                        prefEditor.apply();
                        mMainActivity.onResume();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    protected void enableBluetooth(){

        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        mMainActivity.startActivityForResult(enableBtIntent, 1);
    }

    protected void setBluetoothListener() {
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        mMainActivity.registerReceiver(mReceiver, filter);
    }

    protected final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON) {

                    mMainActivity.showToast("Bluetooth - Enabled");

                } else if (state == BluetoothAdapter.STATE_OFF) {

                    mMainActivity.showToast("Bluetooth - Disabled");
                    mMainActivity.onResume();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {

                mDeviceList = new ArrayList<BluetoothDevice>();
                mMainActivity.showToast("Scanning...");
                scanningDialog.show();


            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                String previous_mac = mMainActivity.getStringPreference("zephyr_mac_address", "");
                String previous_name = mMainActivity.getStringPreference("zephyr_name", "HXM");

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDeviceList.add(device);
                if (device.getName() != null && (device.getAddress().equals(previous_mac) || device.getName().startsWith("HXM") || device.getName().equals(previous_name))) {
                    mBluetoothAdapter.cancelDiscovery();
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

                if (scanningDialog.isShowing()) {
                    scanningDialog.dismiss();
                }

                if (mZephyr == null) {
                    mMainActivity.showToast("Discovery finished");
                    if (!mDeviceList.isEmpty()) {
                        connectWithZephyr();
                        if (mZephyr != null) {
                            mMainActivity.showToast("ZEPHYR found");
                        } else {
                            mMainActivity.showToast("Is your ZEPHYR clipped on?");
                            mMainActivity.onResume();
                        }
                    } else {
                        mMainActivity.showToast("No devices found");
                        mMainActivity.onResume();
                    }
                }
            }
        }
    };

    private void updateSensorBatteryIcon(int level) {

        int id = Resources.getSystem().getIdentifier("stat_sys_battery_100", "drawable", "android");

        if (level < 100 && level >= 85) {
            id = Resources.getSystem().getIdentifier("stat_sys_battery_85", "drawable", "android");
        }
        else if (level < 85 && level >= 60) {
            id = Resources.getSystem().getIdentifier("stat_sys_battery_71", "drawable", "android");
        }
        else if (level < 60 && level >= 50) {
            id = Resources.getSystem().getIdentifier("stat_sys_battery_57", "drawable", "android");
        }
        else if (level < 50 && level >= 28) {
            id = Resources.getSystem().getIdentifier("stat_sys_battery_43", "drawable", "android");
        }
        else if (level < 28 && level >= 16) {
            id = Resources.getSystem().getIdentifier("stat_sys_battery_28", "drawable", "android");
        }
        else if (level < 16 && level >= 5) {
            id = Resources.getSystem().getIdentifier("stat_sys_battery_15", "drawable", "android");
        }
        else if (level < 5 && level >= 0) {
            id = Resources.getSystem().getIdentifier("stat_sys_battery_0", "drawable", "android");
        }

        Drawable right = mMainActivity.getResources().getDrawable(id);
        mMainActivity.sensor_battery_tv.setCompoundDrawablesWithIntrinsicBounds(null, null, right, null);
    }

    protected Boolean getBooleanPreference(String pref_string, boolean default_val) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mMainPageContext);
        return sp.getBoolean(pref_string, default_val);
    }

    protected void reset_problem_detection() {
        problem_heartRates = new ArrayList<DataGraph.heartRate>();
        problem_HRVScores = new ArrayList<DataGraph.rrInterval>();
        time_max_hr = 0;
        time_min_hr = 0;
        time_max_hrv = 0;
        total_RR_intervals = 0;
        old_RMSSD = 0;
    }

    protected boolean checkSettings() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mMainPageContext);
        String setting_string = "";
        try {

            if (getBooleanPreference("alert_contacts_switch", true)) {
                setting_string = "Max HRV Score";
                MAX_HRV_SCORE = Integer.parseInt(sp.getString("max_hrv_score", ""));

                setting_string = "HRV Score Duration";
                HRV_SCORE_DURATION = Integer.parseInt(sp.getString("hrv_score_duration", ""));

                setting_string = "Min Heart Rate";
                MIN_HR_LIMIT = Integer.parseInt(sp.getString("min_heart_rate", ""));

                setting_string = "Min Heart Rate Duration";
                MIN_HR_DURATION = Integer.parseInt(sp.getString("min_heart_rate_duration", ""));

                setting_string = "Max Heart Rate";
                MAX_HR_LIMIT = Integer.parseInt(sp.getString("max_heart_rate", ""));

                setting_string = "Max Heart Rate Duration";
                MAX_HR_DURATION = Integer.parseInt(sp.getString("max_heart_rate_duration", ""));
            }

        }catch (Exception e) {
            mMainActivity.showToast(setting_string + " setting needed for alerting contacts!");
            mMainActivity.stop_measure_btn.callOnClick();
            return false;
        }
        return true;
    }
}
