package com.coen390.teamc.cardiograph;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ToolTipView mErrorWarningToolTipView;
    private boolean errorWarningToolTipDrawn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setLogo(R.mipmap.ic_launcher);
        setSupportActionBar(toolbar);

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume(){
        super.onResume();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new CustomClickLister());

        if (!detectProblems().isEmpty()){
            fab.show();
            showErrorWarningTooltip();
        } else {
            fab.hide();
            hideErrorWarningTooltip();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public class CustomClickLister implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.fab:
                    hideErrorWarningTooltip();

                    if (!isBluetoothAvailable()){
                        showBluetoothNotEnabledDialog(v);
                    }

            }
        }
    }

    private void showBluetoothNotEnabledDialog(View v){
        new AlertDialog.Builder(v.getContext())
                .setTitle("Bluetooth not enabled")
                .setMessage("Enable Bluetooth?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, 1);
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

    public static boolean isBluetoothAvailable() {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return (bluetoothAdapter != null && bluetoothAdapter.isEnabled());
    }

    private void hideErrorWarningTooltip() {
        if (errorWarningToolTipDrawn) {
            mErrorWarningToolTipView.remove();
            errorWarningToolTipDrawn = false;
        }
    }

    private void showErrorWarningTooltip() {

        if (!errorWarningToolTipDrawn) {
            ToolTipRelativeLayout mToolTipFrameLayout = (ToolTipRelativeLayout) findViewById(R.id.tooltipRelativeLayout);

            ToolTip toolTip = new ToolTip()
                    .withText(" Warning ! ")
                    .withColor(Color.parseColor("#ffdada"))
                    .withTextColor(Color.BLACK)
                    .withShadow()
                    .withAnimationType(ToolTip.AnimationType.FROM_TOP);

            errorWarningToolTipDrawn = true;

            mErrorWarningToolTipView = mToolTipFrameLayout.showToolTipForView(toolTip, findViewById(R.id.fab));
        }
    }

    private ArrayList<String> detectProblems(){
        ArrayList<String> problems = new ArrayList<>();
        if (!isBluetoothAvailable()){
            problems.add("bluetooth_not_enabled");
        }
        return problems;
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
