package com.coen390.teamc.cardiograph;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import com.github.mikephil.charting.charts.LineChart;

import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_graph);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        myDBHelper = new DB_Helper(this);
        mChart = (LineChart) findViewById(R.id.chart);

        delete_alL_btn = (Button) findViewById(R.id.clear_all_instantaneous_heartrates);
        reset_scale_btn = (Button) findViewById(R.id.reset_scale);

        reset_scale_btn.setOnClickListener(new CustomClickLister());
        delete_alL_btn.setOnClickListener(new CustomClickLister());
    }

    @Override
    protected void onResume(){
        super.onResume();

        getAllInstantaneousHeartRates();
        fillEntriesAndSetDataSet();
        mChart.notifyDataSetChanged();
        mChart.invalidate();
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

    private void fillEntriesAndSetDataSet(){

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
    }

    static protected void update(String timeStamp, String heartRate, String note){
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
