package com.coen390.teamc.cardiograph;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
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

    static private class rrInterval {
        String mRRInterval;
        String mDate;
        String mNote;

        rrInterval  (String date, String rate, String note) {
            mDate = date;
            mRRInterval = rate;
            mNote = note;
        }
    }

    private DB_Helper myDBHelper;

    static protected List<heartRate>  mHeartRates = new ArrayList<>();
    static protected List<rrInterval> mRRIntervals = new ArrayList<>();
    static protected LineChart mChart;
    static protected String MAX_HR_LIMIT = "";
    static protected String MIN_HR_LIMIT = "";
    static protected String MAX_RR_INTERVAL = "";
    static protected String MIN_RR_INTERVAL = "";

    static protected TextView stats_tv;

    /** Heart Rate chart stuff **/
    static private ArrayList<Entry> mHR_Entries;
    static private ArrayList<String> mHR_labels;
    static private LineDataSet mHR_DataSet;
    static private boolean limit_lines_add_once = false;
    private LimitLine MAX_HR_LIMIT_LINE;
    private LimitLine MIN_HR_LIMIT_LINE;

    private LimitLine MAX_RR_LIMIT_LINE;
    private LimitLine MIN_RR_LIMIT_LINE;

    /** RR interval chart stuff **/
    static private ArrayList<Entry> mRR_Entries;
    static private ArrayList<String> mRR_labels;
    static private LineDataSet mRR_DataSet;

    private Button delete_alL_btn;
    private Button reset_scale_btn;

    /** Drawer **/
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;

    static protected String current_graph = "HR";
    protected String selectedRecord = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Heart Rate Graph");

        mDrawerList = (ListView)findViewById(R.id.navList);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);

        addDrawerItems();
        setupDrawer();

        myDBHelper = new DB_Helper(this);
        mChart = (LineChart) findViewById(R.id.chart);

        delete_alL_btn = (Button) findViewById(R.id.clear_record);
        reset_scale_btn = (Button) findViewById(R.id.reset_scale);
        stats_tv = (TextView) findViewById(R.id.stats);

        reset_scale_btn.setOnClickListener(new CustomClickLister());
        delete_alL_btn.setOnClickListener(new CustomClickLister());

        if (!MainActivity.mCurrentRecord.equals("")) {
            if (current_graph.equals("HR")) {
                getSupportActionBar().setTitle("Heart Rate Graph");
                graphLiveHeartRate();
            }
            if (current_graph.equals("ECG")) {
                getSupportActionBar().setTitle("RR Intervals Graph");
                graphLiveECGChart();
            }
        } else {
            openDrawer();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (MainActivity.mCurrentRecord.equals("")) {
            getSupportActionBar().setSubtitle("Please select a record!");
        } else {
            getSupportActionBar().setSubtitle(MainActivity.mCurrentRecord);
        }
        if (current_graph.equals("HR")) {
            graphLiveHeartRate();
        }

        if (current_graph.equals("ECG")) {
            graphLiveECGChart();
        }
    }

    private class CustomClickLister implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.clear_record:
                    myDBHelper.deleteAllInstantaneousHeartRates(selectedRecord);
                    myDBHelper.deleteAllRRIntervals(selectedRecord);
                    mChart.clear();
                    onResume();
                    break;

                case R.id.reset_scale:
                    mChart.fitScreen();
                    onResume();
                    break;
            }
        }
    }

    private void graphLiveHeartRate(){

        current_graph = "HR";

        removeLimitLines();
        getAllInstantaneousHeartRates();
        ADD_MAX_AND_MIN_LINES();

        mHR_Entries = new ArrayList<>();

        for (int i = 0; i < mHeartRates.size(); i++ ) {
            mHR_Entries.add(new Entry(Integer.parseInt(mHeartRates.get(i).mHeatRate), i));
        }

        mHR_DataSet = new LineDataSet(mHR_Entries, "Heart Rate (bpm)");
        mHR_labels = new ArrayList<>(); //time

        for (int j = 0; j < mHeartRates.size(); j++ ) {
            mHR_labels.add(mHeartRates.get(j).mDate);
        }

        mHR_DataSet.setDrawCubic(false); //rounded -> true causes bug!!!
        mHR_DataSet.setDrawFilled(true); //fill under line
        mHR_DataSet.setFillColor(Color.parseColor("#b8005c"));
        mHR_DataSet.setDrawCircles(false); //remove data 'points'

        LineData data = new LineData(mHR_labels, mHR_DataSet);
        data.setDrawValues(false);

        mChart.setData(data);
        mChart.setDescription("");
        stats_tv.setText(" Max: " + computeMaxHeartRate() + " (bpm)\n Average: " + computeAvgHeartRate() +
                " (bpm)\n Min: " + computeMinHeartRate() + " (bpm)" + "\n Your Max: " + MAX_HR_LIMIT + " (bpm)  Your Min: " + MIN_HR_LIMIT + " (bpm)" );

        //hide grid
        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getAxisRight().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);

        mChart.setAutoScaleMinMaxEnabled(true);

        mChart.notifyDataSetChanged();
        mChart.invalidate();
    }

    private void getAllInstantaneousHeartRates() {
        Cursor cursor = myDBHelper.getAllInstantaneousHeartRates(MainActivity.mCurrentRecord);
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

            if (mChart.getData() == null) {
                LineData data = new LineData(mHR_labels, mHR_DataSet);
                data.setDrawValues(false);

                mChart.setData(data);
            }

            heartRate new_hr = new heartRate(timeStamp, heartRate, note);
            mHeartRates.add(new_hr);

            mHR_Entries.add(new Entry(Integer.parseInt(new_hr.mHeatRate), mHeartRates.size()));
            mHR_labels.add(new_hr.mDate);

            stats_tv.setText(" Max: " + computeMaxHeartRate() + " (bpm)\n Average: " + computeAvgHeartRate() +
                    " (bpm)\n Min: " + computeMinHeartRate() + " (bpm)" + "\n Your Max: " + MAX_HR_LIMIT + " (bpm)  Your Min: " + MIN_HR_LIMIT + " (bpm)" );

            mChart.notifyDataSetChanged();

            mChart.setVisibleXRangeMaximum(30);
            mChart.moveViewToX(mHeartRates.size() - 31);
            //mChart.invalidate();
        }
    }

    private void getAllRRIntervals() {
        Cursor cursor = myDBHelper.getAllRRIntervals(MainActivity.mCurrentRecord);

        mRRIntervals = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String rrDate = cursor.getString(0);
                String rrValue = cursor.getString(1);
                String rrNote = cursor.getString(2);
                mRRIntervals.add(new rrInterval(rrDate, rrValue, rrNote));
            }while (cursor.moveToNext());
            cursor.close();
        }

    }

    private void graphLiveECGChart() {

        current_graph = "ECG";

        removeLimitLines();
        getAllRRIntervals();
        ADD_MAX_AND_MIN_LINES();

        mRR_Entries = new ArrayList<>();

        for (int i = 0; i < mRRIntervals.size(); i++ ) {

            mRR_Entries.add(new Entry(Integer.parseInt(mRRIntervals.get(i).mRRInterval), i));
        }

        mRR_DataSet = new LineDataSet(mRR_Entries, "Beat-to-Beat Intervals (ms)");
        mRR_labels = new ArrayList<>();

        for (int j = 0; j < mRRIntervals.size(); j++ ) {

            mRR_labels.add(mRRIntervals.get(j).mDate);
        }

        mRR_DataSet.setDrawCubic(false); //rounded -> true causes bug!!!
        mRR_DataSet.setDrawFilled(false); //don't fill under line
        mRR_DataSet.setDrawCircles(false); //remove data 'points'

        LineData data = new LineData(mRR_labels, mRR_DataSet);
        data.setDrawValues(false);//?

        mChart.setData(data);
        stats_tv.setText(" Max: " + computeMaxRRInterval() + " (ms)\n Average: " + computeAvgRRInterval() + " (ms)\n Min: " + computeMinRRInterval() + " (ms)"
                + "\n Your Max: " + MAX_RR_INTERVAL + " (ms)  Your Min: " + MIN_RR_INTERVAL + " (ms)" );
        mChart.setDescription("");

        //hide grid
        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getAxisRight().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);

        mChart.setAutoScaleMinMaxEnabled(true);

        mChart.notifyDataSetChanged();
        mChart.invalidate();
    }

    static protected void updateLiveECG(String timeStamp, String rrInterval, String note) {

        if (mChart != null ) {

            if (mChart.getData() == null) {
                LineData data = new LineData(mRR_labels, mRR_DataSet);
                data.setDrawValues(false);
                mChart.setData(data);
            }

            rrInterval new_rr = new rrInterval(timeStamp, rrInterval, note);

            mRRIntervals.add(new_rr);

            mRR_Entries.add(new Entry(Integer.parseInt(new_rr.mRRInterval), mRRIntervals.size()));
            mRR_labels.add(new_rr.mDate);

            mChart.notifyDataSetChanged();
            mChart.setVisibleXRangeMaximum(30);
            mChart.moveViewToX(mRRIntervals.size() - 31);

            stats_tv.setText(" Max: " + computeMaxRRInterval() + " (ms)\n Average: " + computeAvgRRInterval() + " (ms)\n Min: " + computeMinRRInterval() + " (ms)"
                    + "\n Your Max: " + MAX_RR_INTERVAL + " (ms)  Your Min: " + MIN_RR_INTERVAL + " (ms)" );
            //mChart.invalidate();

        }
    }

    static private int computeAvgHeartRate() {
        int sum = 0;
        for (int i=0; i < mHeartRates.size(); i++) {
            sum += Integer.parseInt(mHeartRates.get(i).mHeatRate);
        }

        if (mHeartRates.size() == 0) {
            return 0;
        }
        return sum/mHeartRates.size();
    }

    static private int computeMaxHeartRate() {
        int max = 0;

        if (mHeartRates.size() > 0) {

            max = Integer.parseInt(mHeartRates.get(0).mHeatRate);

            for (int i = 1; i < mHeartRates.size(); i++) {
                if (Integer.parseInt(mHeartRates.get(i).mHeatRate) > max) {
                    max = Integer.parseInt(mHeartRates.get(i).mHeatRate);
                }
            }
        }
        return max;
    }

    static private int computeMinHeartRate() {
        int min = 0;
        if (mHeartRates.size() > 0) {
            min = Integer.parseInt(mHeartRates.get(0).mHeatRate);

            for (int i = 1; i < mHeartRates.size(); i++) {
                if (Integer.parseInt(mHeartRates.get(i).mHeatRate) < min) {
                    min = Integer.parseInt(mHeartRates.get(i).mHeatRate);
                }
            }
        }
        return min;
    }

    static private int computeAvgRRInterval() {
        int sum = 0;
        for (int i=0; i < mRRIntervals.size(); i++) {
            sum += Integer.parseInt(mRRIntervals.get(i).mRRInterval);
        }

        if (mRRIntervals.size() == 0) {
            return 0;
        }
        return sum/mRRIntervals.size();
    }

    static private int computeMinRRInterval() {
        int min = 0;
        if (mRRIntervals.size() > 0) {
            min = Integer.parseInt(mRRIntervals.get(0).mRRInterval);

            for (int i = 1; i < mRRIntervals.size(); i++) {
                if (Integer.parseInt(mRRIntervals.get(i).mRRInterval) < min) {
                    min = Integer.parseInt(mRRIntervals.get(i).mRRInterval);
                }
            }
        }
        return min;
    }

    static private int computeMaxRRInterval() {
        int max = 0;

        if (mRRIntervals.size() > 0) {

            max = Integer.parseInt(mRRIntervals.get(0).mRRInterval);

            for (int i = 1; i < mRRIntervals.size(); i++) {
                if (Integer.parseInt(mRRIntervals.get(i).mRRInterval) > max) {
                    max = Integer.parseInt(mRRIntervals.get(i).mRRInterval);
                }
            }
        }
        return max;
    }

    private void ADD_MAX_AND_MIN_LINES() {
        if (limit_lines_add_once) {
            removeLimitLines();
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (checkSettings()) {
            if (current_graph.equals("HR")) {
                createLimitLines(MAX_HR_LIMIT, MIN_HR_LIMIT);
                mChart.getAxisLeft().addLimitLine(MAX_HR_LIMIT_LINE);
                mChart.getAxisLeft().addLimitLine(MIN_HR_LIMIT_LINE);
            }

            if (current_graph.equals("ECG")) {
                createLimitLines(MAX_RR_INTERVAL, MIN_RR_INTERVAL);
                mChart.getAxisLeft().addLimitLine(MAX_RR_LIMIT_LINE);
                mChart.getAxisLeft().addLimitLine(MIN_RR_LIMIT_LINE);
            }
        }
    }

    private void createLimitLines(String MAX_LIMIT, String MIN_LIMIT) {
        if (checkSettings()) {

            if (current_graph.equals("HR")) {
                MAX_HR_LIMIT_LINE = new LimitLine(Integer.parseInt(MAX_LIMIT)); // set where the line should be drawn
                MAX_HR_LIMIT_LINE.setLineColor(Color.BLACK);
                MAX_HR_LIMIT_LINE.setLineWidth(1);
                MAX_HR_LIMIT_LINE.setLabel("MAX");

                MIN_HR_LIMIT_LINE = new LimitLine(Integer.parseInt(MIN_LIMIT));
                MIN_HR_LIMIT_LINE.setLineColor(Color.BLACK);
                MIN_HR_LIMIT_LINE.setLineWidth(1);
                MIN_HR_LIMIT_LINE.setLabel("MIN");
            }

            if (current_graph.equals("ECG")) {
                MAX_RR_LIMIT_LINE = new LimitLine(Integer.parseInt(MAX_LIMIT)); // set where the line should be drawn
                MAX_RR_LIMIT_LINE.setLineColor(Color.BLACK);
                MAX_RR_LIMIT_LINE.setLineWidth(1);
                MAX_RR_LIMIT_LINE.setLabel("MAX");

                MIN_RR_LIMIT_LINE = new LimitLine(Integer.parseInt(MIN_LIMIT));
                MIN_RR_LIMIT_LINE.setLineColor(Color.BLACK);
                MIN_RR_LIMIT_LINE.setLineWidth(1);
                MIN_RR_LIMIT_LINE.setLabel("MIN");
            }

            limit_lines_add_once = true;
        }
    }

    static protected void removeLimitLines() {
        if (mChart != null) {
            mChart.getAxisLeft().removeAllLimitLines();
            limit_lines_add_once = false;
        }
    }

    private void addDrawerItems() {

        LayoutInflater inflater = getLayoutInflater();
        View listHeaderView = inflater.inflate(R.layout.nav_header,null, false);
        mDrawerList.addHeaderView(listHeaderView);

        String[] osArray = { "New Record", "Saved Records", "Heart Rate Graph", "RR Intervals Graph", "Potential Danger", "Information" };
        /**      positions:       1              2                   3                  4                     5              6     **/
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, osArray);
        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1 /** New Record **/:
                        showNewRecordDialog(view);
                        closeDrawer();
                        break;
                    case 2 /** Saved Records **/:
                        showSavedRecordsDialog(view);
                        closeDrawer();
                        break;
                    case 3 /** Live Heart Rate **/:
                        mChart.clear();
                        graphLiveHeartRate();
                        getSupportActionBar().setTitle("Heart Rate Chart");
                        showToast("Showing Heart Rate");
                        closeDrawer();
                        break;
                    case 4 /** Live ECG **/:
                        mChart.clear();
                        graphLiveECGChart();
                        getSupportActionBar().setTitle("RR Intervals Chart");
                        showToast("Showing RR Intervals");
                        closeDrawer();
                        break;
                    case 5 /** Potential Danger **/:
                        closeDrawer();
                        detectDanger(view);
                        break;

                    case 6 /** Info **/:
                        closeDrawer();
                        showInfoPicture(view);
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
                if (MainActivity.mCurrentRecord.equals("")) {
                    getSupportActionBar().setSubtitle("Please select a record!");
                } else {
                    getSupportActionBar().setSubtitle(MainActivity.mCurrentRecord);
                }
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                if (MainActivity.mCurrentRecord.equals("")) {
                    getSupportActionBar().setSubtitle("Please select a record!");
                } else {
                    getSupportActionBar().setSubtitle(MainActivity.mCurrentRecord);
                }
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


    protected void showNewRecordDialog(final View v){
        LayoutInflater inflater = this.getLayoutInflater();
        final View inflator = inflater.inflate(R.layout.new_record_dialog, null);
        new AlertDialog.Builder(v.getContext())
                .setView(inflator)
                .setTitle("New Record")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText tv = (EditText) inflator.findViewById(R.id.record_name);
                        String name = tv.getText().toString();

                        if (!myDBHelper.checkIfRecordExists(name) && !name.equals("")) {

                            MainActivity.mCurrentRecord = name;
                            if (BluetoothConnection.recordMeasurement) {
                                MainActivity.stop_measure_btn.callOnClick();
                            }
                            Intent main_activity = new Intent(DataGraph.this, MainActivity.class);
                            startActivity(main_activity);

                        } else {
                            showToast("Name already used!");
                            showNewRecordDialog(v);
                        }
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

    protected void setRecord(String record) {
        selectedRecord = record;
    }

    private class records {
        String record_name;
        String first_date;
        String last_date;

        records(String name, String first, String last) {
            record_name = name;
            first_date = first;
            last_date = last;
        }
    }

    protected void showSavedRecordsDialog(final View v){
        setRecord("");

        final LayoutInflater inflater = this.getLayoutInflater();
        final View inflator = inflater.inflate(R.layout.saved_records_dialog, null);

        ArrayList<String> mRecords = myDBHelper.getUniqueRecords();
        ArrayList<records> true_records = new ArrayList<>();
        final ArrayList<records> true_records_final = true_records;
        for (int i = 0; i < mRecords.size(); i++) {
            String first_date = myDBHelper.getFirstTimestampFromRecord(mRecords.get(i));
            String last_date = myDBHelper.getLastTimestampFromRecord(mRecords.get(i));
            true_records.add(new records(mRecords.get(i), first_date, last_date));
            mRecords.set(i, mRecords.get(i) + "\nStart: " + first_date + "\nEnd: " + last_date);
        }

        ArrayAdapter<String> mRecordsAdapter = new ArrayAdapter<>
                (this, R.layout.list_item_record,R.id.list_item_record_textview, mRecords);

        ListView listView = (ListView) inflator.findViewById(R.id.saved_records);
        listView.setAdapter(mRecordsAdapter);

        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            public void onItemClick (AdapterView < ? > parent, View view,int position, long id){
                //setRecord(parent.getItemAtPosition(position).toString());
                setRecord(true_records_final.get(position).record_name);
            }
        };
        listView.setOnItemClickListener(listener);

        new AlertDialog.Builder(v.getContext())
                .setView(inflator)
                .setTitle("Saved Records")
                .setPositiveButton("SELECT", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (!selectedRecord.equals("")) {
                            if (BluetoothConnection.recordMeasurement && !selectedRecord.equals(MainActivity.mCurrentRecord))
                            {
                                MainActivity.stop_measure_btn.callOnClick();
                            }
                            MainActivity.mCurrentRecord = selectedRecord;
                            onResume();
                        }
                    }
                })
                .setNegativeButton("DELETE", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        if (!selectedRecord.equals("")) {
                            delete_alL_btn.callOnClick();
                            if (MainActivity.mCurrentRecord.equals(selectedRecord)) {
                                MainActivity.stop_measure_btn.callOnClick();
                                MainActivity.mCurrentRecord = "";
                                mChart.clear();
                                Intent main_activity = new Intent(DataGraph.this, MainActivity.class);
                                startActivity(main_activity);
                            }
                        }
                    }
                })
                .show();
    }

    protected void detectDanger(View v) {
        if (MainActivity.mCurrentRecord.equals("")) {
            showToast("Select a record first!");
        } else {
            if (checkSettings()){
                showDangers(v);
            } else {
                showToast("Settings needed for this feature!");
            }
        }
    }

    protected ArrayList<String> scanHeartRates() {
        ArrayList<String> max_points = new ArrayList<>();
        ArrayList<String> min_points = new ArrayList<>();

        int max = Integer.parseInt(MAX_HR_LIMIT);
        int max_duration = Integer.parseInt(getStringPreference("max_heart_rate_duration", ""));

        int min = Integer.parseInt(MIN_HR_LIMIT);
        int min_duration = Integer.parseInt(getStringPreference("min_heart_rate_duration", ""));

        int time_max_hr = 0;
        int time_min_hr = 0;
        for (int i=0; i < mHeartRates.size(); i++) {
            if (Integer.parseInt(mHeartRates.get(i).mHeatRate) > max) {
                time_max_hr += 1;

                if (time_max_hr > max_duration) {
                    max_points.add("Max HR surpassed for " + max_duration + " seconds starting at " + mHeartRates.get(i-max_duration).mDate);
                    time_max_hr = 0;
                }
            } else {
                time_max_hr = 0;
            }

            if (Integer.parseInt(mHeartRates.get(i).mHeatRate) < min) {
                time_min_hr += 1;

                if (time_min_hr > min_duration) {
                    max_points.add("Min HR surpassed for " + min_duration + " seconds starting at " + mHeartRates.get(i-min_duration).mDate);
                    time_min_hr = 0;
                }
            } else {
                time_min_hr = 0;
            }
        }

        max_points.addAll(min_points);
        return max_points;
    }

    protected ArrayList<String> scanRRIntervals() {
        return new ArrayList<String>();
    }

    protected void showDangers(View v) {
        final LayoutInflater inflater = this.getLayoutInflater();
        final View inflator = inflater.inflate(R.layout.saved_records_dialog, null);

        ArrayList<String> mDangerPoints = new ArrayList<>();

        if (current_graph.equals("HR")) {
            mDangerPoints.addAll(scanHeartRates());
            if (mDangerPoints.size() == 0) {
                mDangerPoints.add("Max/Min Heart Rate not reached!" + "\n\nYour Max Heart Rate is " + getStringPreference("max_heart_rate", "")
                        + " (bpm) with sensitivity: " + getStringPreference("max_heart_rate_duration", "") + " (secs)"
                        + "\n\nYour Min Heart Rate is " + getStringPreference("min_heart_rate", "")+ " (bpm) with sensitivity: " + getStringPreference("min_heart_rate_duration", "") + " (secs)");
            }
        }

        if (current_graph.equals("ECG")) {
            mDangerPoints.addAll(scanRRIntervals());
            if (mDangerPoints.size() == 0) {
                mDangerPoints.add("Dangerous RR Intervals not reached!");
            }
        }

        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            public void onItemClick (AdapterView < ? > parent, View view,int position, long id){
                String danger_point = parent.getItemAtPosition(position).toString();

                if (!danger_point.startsWith("Max/Min Heart Rate not reached!") && current_graph.equals("HR")) {
                    int index_of_danger_point;
                    index_of_danger_point = danger_point.indexOf("at ");
                    String danger_time_point = danger_point.substring(index_of_danger_point + 3);
                    showToast(danger_time_point);

                    for (int i = 0; i < mHeartRates.size(); i++) {
                        if (mHeartRates.get(i).mDate.equals(danger_time_point)) {
                            mChart.fitScreen();
                            mChart.zoomIn();
                            mChart.zoomIn();
                            mChart.moveViewTo(i, Integer.parseInt(mHeartRates.get(i).mHeatRate), mHR_DataSet.getAxisDependency());
                            break;
                        }
                    }
                }

                if (!danger_point.startsWith("Dangerous RR Intervals not reached!") && current_graph.equals("ECG")) {

                }
            }
        };

        ArrayAdapter<String> mDangerPointsAdapter = new ArrayAdapter<>
                (this, R.layout.list_item_record,R.id.list_item_record_textview, mDangerPoints);
        ListView listView = (ListView) inflator.findViewById(R.id.saved_records);
        listView.setAdapter(mDangerPointsAdapter);
        listView.setOnItemClickListener(listener);

        new AlertDialog.Builder(v.getContext())
                .setView(inflator)
                .setTitle("Danger points!")
                .setPositiveButton("VIEW", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    protected void showInfoPicture(View v) {
        ImageView image = new ImageView(this);
        image.setImageResource(R.drawable.rr_intervals_explained);

        new AlertDialog.Builder(v.getContext())
                .setTitle("Typical Electrocardiogram")
                .setMessage("RR Intervals refers to the time in milliseconds (ms) between individual Heart Beats. Heart Rate is the number of beats per minute (bpm).")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setView(image)
                .show();
    }

    protected void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    protected boolean checkSettings() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String setting_string = "";
        try {
            setting_string = "Max RR Interval";
            Integer.parseInt(sp.getString("max_rr_interval", ""));
            MAX_RR_INTERVAL = sp.getString("max_rr_interval", "");

            setting_string = "Min RR Interval";
            Integer.parseInt(sp.getString("min_rr_interval", ""));
            MIN_RR_INTERVAL = sp.getString("min_rr_interval", "");

            setting_string = "Min Heart Rate";
            Integer.parseInt(sp.getString("min_heart_rate", ""));
            MAX_HR_LIMIT = sp.getString("max_heart_rate", "");


            setting_string = "Min Heart Rate Duration";
            Integer.parseInt(sp.getString("min_heart_rate_duration", ""));

            setting_string = "Max Heart Rate";
            Integer.parseInt(sp.getString("max_heart_rate", ""));
            MIN_HR_LIMIT = sp.getString("min_heart_rate", "");

            setting_string = "Max Heart Rate Duration";
            Integer.parseInt(sp.getString("max_heart_rate_duration", ""));

        }catch (Exception e) {
            showToast(setting_string + " setting missing!");
            return false;
        }
        return true;
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

    protected String getStringPreference(String pref_string, String default_string) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getString(pref_string, default_string);
    }

}
