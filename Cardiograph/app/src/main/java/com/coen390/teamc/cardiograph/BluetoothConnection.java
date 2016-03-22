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

    protected boolean recordMeasurement = false;
    static protected boolean updateLiveHeartRateChart = false;
    static protected boolean updateLiveECGChart = false;
    private final int HEART_RATE = 0x100;
    private final int BATTERY_PERCENT = 0x102;
    private final int HEART_BEAT_TIMESTAMPS = 0x103;
    private final int HEART_BEAT_NUMBER = 0x104;

    protected int previous_hr_num = 0;

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
                            mMainActivity.myDBHelper.insertInstantaneousHeartRate(currentTimeStamp, HeartRatetext, "testRecord");

                            if (updateLiveHeartRateChart) {
                                /** UPDATE LIVE GRAPH **/
                                DataGraph.updateLiveHeartRate(currentTimeStamp, HeartRatetext, "testRecord");
                            }
                        }

                        /** CHECK IF BAD CONNECTION
                        if (Integer.parseInt(HeartRatetext) == 0){
                            mMainActivity.showBadConnectionDialog(); //not sure about this one...
                        } else {
                            mMainActivity.badConnectionDialogShown = false;
                            nMgr.cancel(888);
                        }
                         **/
                    }
                    break;

                case BATTERY_PERCENT:
                    String BatteryPercent = msg.getData().getString("BatteryPercent");
                    mMainActivity.sensor_battery_tv.setText("Sensor Battery: " + BatteryPercent + " % ");
                    if (BatteryPercent != null) {
                        updateSensorBatteryIcon(Integer.parseInt(BatteryPercent));
                    }


                    /** CHECK IF BATTERY LOW **/
                    String min_battery_level = mMainActivity.getStringPreference("battery_level", "15");
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
                        new_HR_beats = -1 * new_HR_beats;
                    }
                    if ( new_HR_beats != 0 ) {
                        for (int i = 0; i < new_HR_beats ; i++ ) {
                            int RR = HeartBeatTimeStamps[i] - HeartBeatTimeStamps[i+1];
                            if (RR < 0) {
                                //overflow occured
                                RR = 65535 + RR;
                            }
                            Log.d("Latest R-R interval", "         " + String.valueOf(RR));
                        }
                        previous_hr_num = HeartBeatNumber;
                    }


                    /*
                    Log.d("heart_beat_num", String.valueOf(HeartBeatNumber));

                    for (int i = 0; i < HeartBeatTimeStamps.length; i++) {
                        Log.d("confirmed", String.valueOf(HeartBeatTimeStamps[i]));
                    }

                    String currentYear = new SimpleDateFormat("yyyy").format(new Date());
                    String currentMonth = new SimpleDateFormat("MM").format(new Date());
                    String currentDay = new SimpleDateFormat("dd").format(new Date());
                    String currentHour = new SimpleDateFormat("HH").format(new Date());
                    String currentMinute = new SimpleDateFormat("mm").format(new Date());
                    String currentSecond = new SimpleDateFormat("ss").format(new Date());
                    */

                    break;

            }
        }

    };

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
                        prefEditor.commit();
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
        mMainActivity.sensor_battery_tv.setCompoundDrawablesWithIntrinsicBounds(null,null,right,null);
    }
}
