package com.coen390.teamc.cardiograph;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;
import zephyr.android.HxMBT.BTClient;
import zephyr.android.HxMBT.ZephyrProtocol;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class BluetoothConnection {

    public BTClient _bt;
    public ZephyrProtocol _protocol;
    public NewConnectedListener _NConnListener;

    public BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    public BluetoothDevice mZephyr;
    private TextView live_pulse_tv;
    private Context mMainPageContext;
    private MainActivity mMainActivity;

    private final int HEART_RATE = 0x100;
    private final int INSTANT_SPEED = 0x101;

    public BluetoothConnection(TextView tv, Context ctx) {
        live_pulse_tv = tv;
        mMainPageContext = ctx;
        mMainActivity = (MainActivity)ctx;
    }

    public boolean isBluetoothAvailable() {
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

    public void connectWithZephyr() {
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

    public void connectListener() {
        _bt = new BTClient(mBluetoothAdapter, mZephyr.getAddress());
        _NConnListener = new NewConnectedListener(Newhandler, Newhandler);
        _bt.addConnectedEventListener(_NConnListener);
    }

    public void disconnectListener() {
        /*This disconnects listener from acting on received messages*/
        if (_bt != null) {
            _bt.removeConnectedEventListener(_NConnListener);
        /*Close the communication with the device & throw an exception if failure*/
            _bt.Close();
        }
    }

    final Handler Newhandler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case HEART_RATE:
                    String HeartRatetext = msg.getData().getString("HeartRate");
                    if (live_pulse_tv != null) live_pulse_tv.setText(HeartRatetext + " bpm");
                    break;

                case INSTANT_SPEED:
                    String InstantSpeedtext = msg.getData().getString("InstantSpeed");
                    //if (tv != null)tv.setText(InstantSpeedtext);
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

    public void enableBluetooth(){

        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        mMainActivity.startActivityForResult(enableBtIntent, 1);
    }

    public void setBluetoothListener() {
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        mMainActivity.registerReceiver(mReceiver, filter);
    }

    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON) {

                    mMainActivity.showToast("Bluetooth - Enabled");

                } else if (state == BluetoothAdapter.STATE_OFF) {

                    mMainActivity.showToast("Bluetooth - Disabled");
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {

                mDeviceList = new ArrayList<BluetoothDevice>();
                mMainActivity.showToast("Scanning...");

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDeviceList.add(device);
                if (device.getName() != null && device.getName().startsWith("HXM")) {
                    mBluetoothAdapter.cancelDiscovery();
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mZephyr == null) {
                    mMainActivity.showToast("Discovery finished");
                    if (!mDeviceList.isEmpty()) {
                        connectWithZephyr();
                        if (mZephyr != null) {
                            mMainActivity.showToast("ZEPHYR Found");
                        }
                    }
                }
            }
        }
    };
}
