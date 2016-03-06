package com.coen390.j_abba.random_facts;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Random;

public class FactsDBHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static String type_of_fact = "Crazy";
    private static final int DATABASE_VERSION = 5;
    static final String DATABASE_NAME = "facts.db";
    private static final String FACTS_TABLE_CREATE = "CREATE TABLE " + FactContract.FactsEntry.TABLE_NAME +
            " (" + FactContract.FactsEntry.COLUMN_NAME + " " + FactContract.FactsEntry.COLUMN_TYPE + "," +
            FactContract.FactsEntry.COLUMN_FACT_TYPE + " " + FactContract.FactsEntry.COLUMN_TYPE_TYPE + ");";

    private static final String FACTS_TABLE_DROP = "DROP TABLE " + FactContract.FactsEntry.TABLE_NAME + ";";

    public FactsDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(FACTS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion){
        sqLiteDatabase.execSQL(FACTS_TABLE_DROP);
        onCreate(sqLiteDatabase);
    }

    public void insert(String fact, String fact_type) {
        getReadableDatabase().execSQL("INSERT INTO " + FactContract.FactsEntry.TABLE_NAME + " VALUES('" + fact + "','" +
                fact_type + "');");
    }

    public Cursor customQuery(String query) {
        return getReadableDatabase().rawQuery(query, null);
    }

    public void clear_database() {
        onUpgrade(getReadableDatabase(), DATABASE_VERSION, DATABASE_VERSION);
    }

    public String get_random_fact() {
        Cursor cursor = customQuery("SELECT Fact FROM FACTS where Type='" + type_of_fact + "';");
        int max_index = cursor.getCount();
        Random rand = new Random();
        if (max_index > 0) {
            int rand_index = rand.nextInt(max_index);
            cursor.moveToPosition(rand_index);
            return cursor.getString(0);
        } else {
            return "No " + type_of_fact + " fact in DB!";
        }
    }


}
