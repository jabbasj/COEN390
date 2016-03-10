package com.coen390.teamc.cardiograph;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
    private String BLUETOOTH_NOT_ENABLED = "BLUETOOTH NOT ENABLED!";
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
    private BluetoothConnection mBluetoothConnection;


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
        mBluetoothConnection = new BluetoothConnection((TextView)findViewById(R.id.live_pulse), this);

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
        mBluetoothConnection.setBluetoothListener();

        if (!PROBLEMS_DETECTED.isEmpty()){
            showWarning();
            hideMeasureBtn();
            hideStopMeasureBtn();
            mBluetoothConnection.disconnectListener();
        } else {
            hideWarning();
            showMeasureBtn();
            showStopMeasureBtn();
        }
    }

    public void detectProblems(){
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
    }

    private class CustomClickLister implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.measure:
                    mBluetoothConnection.connectListener();
                    mBluetoothConnection._bt.start();
                    break;

                case R.id.stop_measure:
                    mBluetoothConnection.disconnectListener();
                    break;

                case R.id.warning_fab:
                    if (BLUETOOTH_NOT_ENABLED.equals(PROBLEMS_DETECTED.get(0))) {

                        mBluetoothConnection.enableBluetooth();

                    } else if (NO_DEVICES_FOUND.equals(PROBLEMS_DETECTED.get(0))) {

                        mBluetoothConnection.mBluetoothAdapter.startDiscovery();

                    } else if (ZEPHYR_NOT_CONNECTED.equals(PROBLEMS_DETECTED.get(0))) {

                        mBluetoothConnection.mBluetoothAdapter.startDiscovery();
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
        mBluetoothConnection.disconnectListener();
        unregisterReceiver(mBluetoothConnection.mReceiver);
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

    public void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
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
        if (requestCode == 1) {
            //permission granted
        }
    }

}
