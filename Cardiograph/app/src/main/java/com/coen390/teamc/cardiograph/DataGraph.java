package com.coen390.teamc.cardiograph;

import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class DataGraph extends AppCompatActivity {

    private class heartRate {
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

    private List<String> mHeartRates;
    private List<heartRate>  true_HeartRates;
    private ArrayAdapter<String> mHeartRateAdapter;

    private Button delete_alL_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_graph);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        myDBHelper = new DB_Helper(this);
        delete_alL_btn = (Button) findViewById(R.id.clear_all_instantaneous_heartrates);
        delete_alL_btn.setOnClickListener(new CustomClickLister());
    }

    @Override
    protected void onResume(){
        super.onResume();
        listAllHeartRates();
    }

    private class CustomClickLister implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.clear_all_instantaneous_heartrates:
                    myDBHelper.deleteAllInstantaneousHeartRates();
                    onResume();
                    break;
            }
        }
    }

    private void listAllHeartRates() {
        Cursor cursor = myDBHelper.getAllInstantaneousHeartRates();
        mHeartRates = new ArrayList<>();
        true_HeartRates = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String heartrateDate = new String(cursor.getString(0));
                String heartrateValue = new String(cursor.getString(1));
                String heartrateNote = new String(cursor.getString(2));
                mHeartRates.add(heartrateDate + " (" + heartrateValue + ")" + " - " + heartrateNote);
                true_HeartRates.add(new heartRate(heartrateDate, heartrateValue, heartrateNote));
            }while (cursor.moveToNext());
            cursor.close();
        }

        mHeartRateAdapter = new ArrayAdapter<String>
                (this, R.layout.list_item_instantaneous_heartrate,R.id.list_item_inst_heartrate_textview, mHeartRates);

        ListView listView = (ListView) findViewById(R.id.listview_instantaneous_data);
        listView.setAdapter(mHeartRateAdapter);
        registerForContextMenu(listView);
    }

}
