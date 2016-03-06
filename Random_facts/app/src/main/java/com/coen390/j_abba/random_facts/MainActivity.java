package com.coen390.j_abba.random_facts;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {

    FactsDBHelper myDBHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myDBHelper = new FactsDBHelper(this);

        setContentView(R.layout.activity_main);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayShowHomeEnabled(true);
        ab.setIcon(R.drawable.ic_action_gamble_dice_icon);

        Button selectedfactsbutton = (Button) findViewById(R.id.SELECTED_FACTS_BUTTON);
        Button insertbutton = (Button) findViewById(R.id.INSERT_BUTTON);
        Button googlebutton = (Button) findViewById(R.id.GOOGLE_BUTTON);
        Button quitbutton = (Button) findViewById(R.id.DELETE_BUTTON);
        Button viewallfactsbutton = (Button) findViewById(R.id.ALL_FACTS_BUTTON);

        selectedfactsbutton.setOnClickListener(new CustomClickListener());
        googlebutton.setOnClickListener(new CustomClickListener());
        quitbutton.setOnClickListener(new CustomClickListener());
        insertbutton.setOnClickListener(new CustomClickListener());
        viewallfactsbutton.setOnClickListener(new CustomClickListener());
    };

    public class CustomClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v){
            switch (v.getId()){
                case R.id.SELECTED_FACTS_BUTTON:
                    Intent facts_intent = new Intent(MainActivity.this, FactsActivity.class);
                    startActivity(facts_intent);
                    break;

                case R.id.GOOGLE_BUTTON:
                    Intent google_intent = new Intent(Intent.ACTION_VIEW,Uri.parse("http://www.google.ca/search?q=random+facts"));
                    startActivity(google_intent);
                    break;

                case R.id.ALL_FACTS_BUTTON:
                    Intent all_facts_intent = new Intent(MainActivity.this, AllFacts.class);
                    startActivity(all_facts_intent);
                    break;

                case R.id.INSERT_BUTTON:
                    final EditText factText = (EditText) findViewById(R.id.new_fact_text);
                    final RadioGroup radioGroup = (RadioGroup) findViewById(R.id.my_radio_group);
                    TextView textChecked = (TextView) findViewById(radioGroup.getCheckedRadioButtonId());
                    String fact = factText.getText().toString();
                    String fact_type = textChecked.getText().toString();
                    if (!fact.isEmpty() && !fact_type.isEmpty() && !fact.equals("Enter new fact...") && !fact.equals("Success!")) {
                        myDBHelper.insert(fact, fact_type);
                        factText.setText("Success!");
                    }
                    break;

                case R.id.DELETE_BUTTON:
                    myDBHelper.clear_database();
                    final EditText displayText = (EditText) findViewById(R.id.new_fact_text);
                    displayText.setText("Enter new fact...");
                    break;
            }
        }
    };

    @Override
    public void onResume(){
        super.onResume();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String background_color = sp.getString("background_color_list", "-1");
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.parent_activity);
        if (background_color.equals("-1")) {
            rl.setBackgroundColor(getResources().getColor(R.color.background_material_dark));
        } else if (background_color.equals("1")) {
            rl.setBackgroundColor(Color.GRAY);
        } else if (background_color.equals("0")) {
            rl.setBackgroundColor(Color.WHITE);
        }

        String fact_type = sp.getString("display_fact_type", "-1");
        if (fact_type.equals("-1")){
            myDBHelper.type_of_fact = "Crazy";
            RadioButton wild_radio = (RadioButton) findViewById(R.id.wild_radio);
            wild_radio.toggle();
        } else if (fact_type.equals("1")) {
            myDBHelper.type_of_fact = "Sport";
            RadioButton sport_radio = (RadioButton) findViewById(R.id.sport_radio);
            sport_radio.toggle();
        } else if (fact_type.equals("2")) {
            myDBHelper.type_of_fact = "Nature";
            RadioButton nature_radio = (RadioButton) findViewById(R.id.nature_radio);
            nature_radio.toggle();
        } else if (fact_type.equals("3")) {
            myDBHelper.type_of_fact = "Science";
            RadioButton science_radio = (RadioButton) findViewById(R.id.science_radio);
            science_radio.toggle();
        }
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
}
