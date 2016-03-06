package com.coen390.j_abba.random_facts;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class FactsActivity extends ActionBarActivity {

    FactsDBHelper myDBHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facts);
        myDBHelper = new FactsDBHelper(this);
        update_fact();

        Button another_fact = (Button) findViewById(R.id.another_fact_button);
        another_fact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                update_fact();
            }
        });
    }

    private void update_fact(){
        final TextView fact_display = (TextView) findViewById(R.id.FACT);
        fact_display.setText(myDBHelper.get_random_fact());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_crazy_facts, menu);
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
