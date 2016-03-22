package com.coen390.teamc.cardiograph;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;

import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class DataGraph extends AppCompatActivity {

    static private class heartRate {
        String mHeatRate;
        String mDate;
        String mNote;

        heartRate  (String date, String rate, String note) {
            mDate = date;
            mHeatRate = rate;
            mNote = note;
        }
    }

    private DB_Helper myDBHelper;

    static protected List<heartRate>  mHeartRates = new ArrayList<>();

    static protected LineChart mChart;
    static private ArrayList<Entry> mEntries;
    static private ArrayList<String> labels;
    private LineDataSet mDataSet;
    private boolean limit_lines_add_once = false;

    private Button delete_alL_btn;
    private Button reset_scale_btn;

    /** Drawer **/
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDrawerList = (ListView)findViewById(R.id.navList);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);

        addDrawerItems();
        setupDrawer();

        myDBHelper = new DB_Helper(this);
        mChart = (LineChart) findViewById(R.id.chart);

        delete_alL_btn = (Button) findViewById(R.id.clear_all_instantaneous_heartrates);
        reset_scale_btn = (Button) findViewById(R.id.reset_scale);

        reset_scale_btn.setOnClickListener(new CustomClickLister());
        delete_alL_btn.setOnClickListener(new CustomClickLister());

        graphLiveHeartRate();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    private class CustomClickLister implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.clear_all_instantaneous_heartrates:
                    myDBHelper.deleteAllInstantaneousHeartRates();
                    mChart.clear();
                    onResume();
                    break;

                case R.id.reset_scale:
                    onResume();
                    mChart.fitScreen();
                    break;
            }
        }
    }

    private void graphLiveHeartRate(){

        getAllInstantaneousHeartRates();

        mEntries = new ArrayList<>();

        for (int i = 0; i < mHeartRates.size(); i++ ) {
            mEntries.add(new Entry(Integer.parseInt(mHeartRates.get(i).mHeatRate), i));
        }

        mDataSet = new LineDataSet(mEntries, "Live Heart Rate");
        labels = new ArrayList<>(); //time

        for (int j = 0; j < mHeartRates.size(); j++ ) {
            labels.add(mHeartRates.get(j).mDate);
        }

        mDataSet.setDrawCubic(false); //rounded -> true causes bug!!!
        mDataSet.setDrawFilled(true); //fill under line
        mDataSet.setFillColor(Color.parseColor("#b8005c"));
        mDataSet.setDrawCircles(false); //remove data 'points'

        LineData data = new LineData(labels, mDataSet);
        data.setDrawValues(false);
        ADD_MAX_AND_MIN_LINES();

        mChart.setData(data);
        mChart.setDescription("Average: " + computeAvg());

        //hide grid
        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getAxisRight().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);

        mChart.setAutoScaleMinMaxEnabled(true);

        mChart.notifyDataSetChanged();
        mChart.invalidate();

        BluetoothConnection.updateLiveHeartRateChart = true;
    }

    private void getAllInstantaneousHeartRates() {
        Cursor cursor = myDBHelper.getAllInstantaneousHeartRates();
        mHeartRates = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String heartrateDate = cursor.getString(0);
                String heartrateValue = cursor.getString(1);
                String heartrateNote = cursor.getString(2);
                mHeartRates.add(new heartRate(heartrateDate, heartrateValue, heartrateNote));
            }while (cursor.moveToNext());
            cursor.close();
        }
    }

    static protected void updateLiveHeartRate(String timeStamp, String heartRate, String note){
        if (mChart != null) {

            heartRate new_hr = new heartRate(timeStamp, heartRate, note);
            mHeartRates.add(new_hr);

            mEntries.add(new Entry(Integer.parseInt(new_hr.mHeatRate), mHeartRates.size()));
            labels.add(new_hr.mDate);

            mChart.setDescription("Average: " + computeAvg());

            mChart.setAutoScaleMinMaxEnabled(true);

            mChart.notifyDataSetChanged();
            mChart.invalidate();
            //mChart.fitScreen();
        }
    }

    private void getAllInstantaneousHeartBeats() {

    }

    protected void graphLiveECGChart() {
        getAllInstantaneousHeartBeats();

        BluetoothConnection.updateLiveECGChart = true;
    }

    static void updateLiveECG(String timeStamp) {

    }

    static private int computeAvg() {
        int sum = 0;
        for (int i=0; i < mHeartRates.size(); i++) {
            sum += Integer.parseInt(mHeartRates.get(i).mHeatRate);
        }

        if (mHeartRates.size() == 0) {
            return 0;
        }
        return sum/mHeartRates.size();
    }

    private void ADD_MAX_AND_MIN_LINES() {
        if (!limit_lines_add_once) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String MAX_LIMIT = sp.getString("max_heart_rate", "");
            String MIN_LIMIT = sp.getString("min_heart_rate", "");

            if (!MAX_LIMIT.equals("")) {
                mChart.getAxisLeft().addLimitLine(getLimitLineAt(Integer.parseInt(MAX_LIMIT), "MAX"));
            }

            if (!MIN_LIMIT.equals("")) {
                mChart.getAxisLeft().addLimitLine(getLimitLineAt(Integer.parseInt(MIN_LIMIT), "MIN"));
            }

            limit_lines_add_once = true;
        }
    }

    private LimitLine getLimitLineAt(int xIndex, String text) {
        LimitLine ll = new LimitLine(xIndex); // set where the line should be drawn
        ll.setLineColor(Color.BLACK);
        ll.setLineWidth(1);
        ll.setLabel(text);
        return ll;
    }

    private void addDrawerItems() {

        LayoutInflater inflater = getLayoutInflater();
        View listHeaderView = inflater.inflate(R.layout.nav_header,null, false);
        mDrawerList.addHeaderView(listHeaderView);

        String[] osArray = { "New Record", "Saved Records", "Live Heart Rate", "Live ECG", "Potential Danger" };
        /**      positions:       1              2                   3              4              5            **/
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, osArray);
        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1 /** New Record **/:
                        delete_alL_btn.callOnClick();
                        BluetoothConnection.updateLiveHeartRateChart = false;
                        closeDrawer();
                        break;
                    case 2 /** Saved Records **/:
                        BluetoothConnection.updateLiveHeartRateChart = false;
                        closeDrawer();
                        break;
                    case 3 /** Live Heart Rate **/:
                        graphLiveHeartRate();
                        showToast("Showing Live Heart Rate");
                        closeDrawer();
                        break;
                    case 4 /** Live ECG **/:
                        BluetoothConnection.updateLiveHeartRateChart = false;
                        closeDrawer();
                        break;
                    case 5 /** Potential Danger **/:
                        BluetoothConnection.updateLiveHeartRateChart = false;
                        closeDrawer();
                        break;
                }
            }
        });
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private  void openDrawer()
    {
        mDrawerLayout.openDrawer(Gravity.LEFT);
    }

    private void closeDrawer() {
        mDrawerLayout.closeDrawer(Gravity.LEFT);
    }

    protected void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;

            case R.id.action_trigger_drawer:
                openDrawer();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_graph, menu);
        return true;
    }

}
