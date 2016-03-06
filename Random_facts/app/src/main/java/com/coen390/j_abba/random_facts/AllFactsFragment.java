package com.coen390.j_abba.random_facts;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class AllFactsFragment extends Fragment {

    FactsDBHelper myDBHelper;
    List<String> myFacts;
    ArrayAdapter<String> myFactsAdapter;

    public AllFactsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myDBHelper = new FactsDBHelper(getContext());

        Cursor cursor = myDBHelper.customQuery("SELECT * from Facts");
        myFacts = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String factText = new String(cursor.getString(0));
                String factType = new String(cursor.getString(1));
                myFacts.add(factText + " - " + factType);
            }while (cursor.moveToNext());
            cursor.close();
        }

        myFactsAdapter = new ArrayAdapter<String>
                (getActivity(), R.layout.list_item_fact,R.id.list_item_fact_textview, myFacts);

        View rootView = inflater.inflate(R.layout.fragment_all_facts, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_facts);
        listView.setAdapter(myFactsAdapter);
        return rootView;
    }
}
