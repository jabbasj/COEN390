package com.coen390.teamc.cardiograph;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;

import java.lang.reflect.Method;
import java.util.ArrayList;

import zephyr.android.HxMBT.BTClient;
import zephyr.android.HxMBT.ZephyrProtocol;


public class MainActivity extends AppCompatActivity {

    /** Error handling **/
    private String BLUETOOTH_NOT_ENABLED = "BLUETOOTH NOT ENABLED!";
    private String LOCATION_NOT_ENABLED = "LOCATION NOT ENABLED!";
    private String NO_DEVICES_FOUND = "NO DEVICES FOUND!";
    private String ZEPHYR_NOT_CONNECTED = "ZEPHYR NOT CONNECTED!";
    public ArrayList<String> PROBLEMS_DETECTED;

    /** UI **/
    private ToolTipView mErrorWarningToolTipView;
    private FloatingActionButton warning_fab;
    private TextView warning_tv;
    private Button measure_btn;
    private Button stop_measure_btn;

    /** Backend **/
    private DB_Helper myDBHelper;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    private BluetoothDevice mZephyr;

    BTClient _bt;
    ZephyrProtocol _protocol;
    NewConnectedListener _NConnListener;
    private final int HEART_RATE = 0x100;
    private final int INSTANT_SPEED = 0x101;


    /******                 ENTRY                 ******/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setLogo(R.mipmap.ic_launcher);
        setSupportActionBar(toolbar);

        ask_user_permission_for_location();

        myDBHelper = new DB_Helper(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        measure_btn = (Button) findViewById(R.id.measure);
        stop_measure_btn = (Button) findViewById(R.id.stop_measure);
        warning_fab = (FloatingActionButton) findViewById(R.id.warning_fab);
        warning_tv = (TextView) findViewById(R.id.warning_tv);


        warning_fab.setOnClickListener(new CustomClickLister());
        measure_btn.setOnClickListener(new CustomClickLister());
        stop_measure_btn.setOnClickListener(new CustomClickLister());
    }




    /******                 FLOW CONTROL                  ******/

    @Override
    public void onResume(){
        super.onResume();
        detectProblems();
        setBluetoothListener();

        if (!PROBLEMS_DETECTED.isEmpty()){
            showWarning();
        } else {
            hideWarning();
            showMeasureBtn();
            showStopMeasureBtn();
        }
    }

    public void detectProblems(){
        PROBLEMS_DETECTED = new ArrayList<>();

        if (!isBluetoothAvailable()){
            PROBLEMS_DETECTED.add(BLUETOOTH_NOT_ENABLED);
        }

        if (!isLocationAvailable()){
            PROBLEMS_DETECTED.add(LOCATION_NOT_ENABLED);
        }

        if (mZephyr == null) {
            PROBLEMS_DETECTED.add(ZEPHYR_NOT_CONNECTED);
        }

        if (mDeviceList.isEmpty()) {
            PROBLEMS_DETECTED.add(NO_DEVICES_FOUND);
        }
    }

    private class CustomClickLister implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.measure:
                    connectListener();
                    _bt.start();
                    break;

                case R.id.stop_measure:
                    disconnectListener();
                    break;

                case R.id.warning_fab:
                    if (BLUETOOTH_NOT_ENABLED.equals(PROBLEMS_DETECTED.get(0))) {

                        enableBluetooth();

                    } else if (LOCATION_NOT_ENABLED.equals(PROBLEMS_DETECTED.get(0))) {

                        enableLocation();
                        while (!isLocationAvailable()) ;
                        showToast("Location - Enabled");

                    } else if (NO_DEVICES_FOUND.equals(PROBLEMS_DETECTED.get(0))) {

                        mBluetoothAdapter.startDiscovery();

                    } else if (ZEPHYR_NOT_CONNECTED.equals(PROBLEMS_DETECTED.get(0))) {

                        mBluetoothAdapter.startDiscovery();
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
    public void onDestroy(){
        disconnectListener();
        unregisterReceiver(mReceiver);
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

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showLocationNotEnabledDialog(View v){
        new AlertDialog.Builder(v.getContext())
                .setTitle("Location not enabled")
                .setMessage("Enable Location?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //ask_user_permission_for_location();
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

    private void showMeasureBtn(){
        measure_btn.setVisibility(View.VISIBLE);
    }

    private void hideMeasureBtn() {
        measure_btn.setVisibility(View.GONE);
    }

    private void showStopMeasureBtn() {
        stop_measure_btn.setVisibility(View.VISIBLE);
    }

    private void hideStopMeasureBtn() {
        stop_measure_btn.setVisibility(View.GONE);
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
                .withText(" Warning ! ")
                .withColor(Color.parseColor("#ffdada"))
                .withTextColor(Color.BLACK)
                .withShadow()
                .withAnimationType(ToolTip.AnimationType.FROM_TOP);

        mErrorWarningToolTipView = mToolTipFrameLayout.showToolTipForView(toolTip, findViewById(R.id.warning_fab));
    }





    /******              LOCATION               ******/

    @TargetApi(23)
    private void ask_user_permission_for_location() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        if (!isLocationAvailable()) {
            if (requestCode == 1) {
                //permission granted
            }
        }
    }

    private void enableLocation(){
        Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(enableLocationIntent, 1);
    }

    private boolean isLocationAvailable(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
    }




    /******              BLUETOOTH               ******/

    private void setBluetoothListener() {
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter);
    }

    private void enableBluetooth(){

        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 1);
    }

    private boolean isBluetoothAvailable() {
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

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON) {

                    showToast("Bluetooth - Enabled");

                } else if (state == BluetoothAdapter.STATE_OFF) {

                    showToast("Bluetooth - Disabled");
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {

                mDeviceList = new ArrayList<BluetoothDevice>();
                showToast("Scanning...");

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDeviceList.add(device);
                if (device.getName() != null && device.getName().startsWith("HXM")) {
                    mBluetoothAdapter.cancelDiscovery();
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                showToast("Discovery finished");
                onResume();
                if (!mDeviceList.isEmpty()) {
                    connectWithZephyr();
                }
            }
        }
    };






    /******               ZEPHYR                ******/

    private void connectWithZephyr() {
        for (int i = 0; i < mDeviceList.size(); i++) {

            if (mDeviceList.get(i).getName() != null && mDeviceList.get(i).getName().startsWith("HXM")) {

                mZephyr = mDeviceList.get(i);
                if (mZephyr.getBondState() != BluetoothDevice.BOND_BONDED) {
                    pairDevice(mZephyr);
                }
                showToast("ZEPHYR Found");
                break;
            }
        }
        onResume();
    }

    private void connectListener() {
        _bt = new BTClient(mBluetoothAdapter, mZephyr.getAddress());
        _NConnListener = new NewConnectedListener(Newhandler, Newhandler);
        _bt.addConnectedEventListener(_NConnListener);
    }

    private void disconnectListener() {
        /*This disconnects listener from acting on received messages*/
        if (_bt != null) {
            _bt.removeConnectedEventListener(_NConnListener);
        /*Close the communication with the device & throw an exception if failure*/
            _bt.Close();
        }
    }

    final Handler Newhandler = new Handler(){
        public void handleMessage(Message msg) {
            TextView tv = (TextView)findViewById(R.id.live_pulse);
            switch (msg.what)
            {
                case HEART_RATE:
                    String HeartRatetext = msg.getData().getString("HeartRate");
                    if (tv != null) tv.setText(HeartRatetext + " bpm");
                    break;

                case INSTANT_SPEED:
                    String InstantSpeedtext = msg.getData().getString("InstantSpeed");
                    //if (tv != null)tv.setText(InstantSpeedtext);
                    break;

            }
        }

    };




}
