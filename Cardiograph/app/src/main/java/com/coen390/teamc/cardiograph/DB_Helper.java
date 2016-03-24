package com.coen390.teamc.cardiograph;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DB_Helper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 2;
    static final String DATABASE_NAME = "cardiograph.db";

    private static final String CONTACTS_TABLE_CREATE = "CREATE TABLE " + DB_Contract.ContactsEntry.TABLE_NAME +
            " (" + DB_Contract.ContactsEntry.NAME_COLUMN + " " + DB_Contract.ContactsEntry.NAME_COLUMN_TYPE + "," +
            DB_Contract.ContactsEntry.PHONE_COLUMN + " " + DB_Contract.ContactsEntry.PHONE_COLUMN_TYPE + "," +
            DB_Contract.ContactsEntry.PRIORITY_COLUMN + " " + DB_Contract.ContactsEntry.PRIORITY_COLUMN_TYPE + "," +
            DB_Contract.ContactsEntry.ACTION_COLUMN + " " + DB_Contract.ContactsEntry.ACTION_COLUMN_TYPE
            + ");";

    private static final String INSTANTANEOUS_HEART_RATE_TABLE_CREATE = "CREATE TABLE " + DB_Contract.InstantaneousHeartRateEntry.TABLE_NAME +
            " (" + DB_Contract.InstantaneousHeartRateEntry.DATE_COLUMN + " " + DB_Contract.InstantaneousHeartRateEntry.DATE_COLUMN_TYPE + "," +
            DB_Contract.InstantaneousHeartRateEntry.HEART_RATE_COLUMN + " " + DB_Contract.InstantaneousHeartRateEntry.HEART_RATE_COLUMN_TYPE + "," +
            DB_Contract.InstantaneousHeartRateEntry.NOTE_COLUMN + " " + DB_Contract.InstantaneousHeartRateEntry.NOTE_COLUMN_TYPE
            + ");";

    private static final String AVERAGE_HEART_RATE_TABLE_CREATE = "CREATE TABLE " + DB_Contract.AverageHeartRateEntry.TABLE_NAME +
            " (" + DB_Contract.AverageHeartRateEntry.DATE_COLUMN + " " + DB_Contract.AverageHeartRateEntry.DATE_COLUMN_TYPE + "," +
            DB_Contract.AverageHeartRateEntry.HEART_RATE_COLUMN + " " + DB_Contract.AverageHeartRateEntry.HEART_RATE_COLUMN_TYPE + "," +
            DB_Contract.AverageHeartRateEntry.NOTE_COLUMN + " " + DB_Contract.AverageHeartRateEntry.NOTE_COLUMN_TYPE
            + ");";

    private static final String RR_INTERVALS_TABLE_CREATE = "CREATE TABLE " + DB_Contract.RRIntervals.TABLE_NAME +
            " (" + DB_Contract.RRIntervals.DATE_COLUMN + " " + DB_Contract.RRIntervals.DATE_COLUMN_TYPE + "," +
            DB_Contract.RRIntervals.RR_COLUMN + " " + DB_Contract.RRIntervals.RR_COLUMN_TYPE + "," +
            DB_Contract.RRIntervals.NOTE_COLUMN + " " + DB_Contract.RRIntervals.NOTE_COLUMN_TYPE
            + ");";

    private static final String CONTACTS_TABLE_DROP = "DROP TABLE IF EXISTS " + DB_Contract.ContactsEntry.TABLE_NAME + ";";
    private static final String INSTANTANEOUS_HEART_RATE_TABLE_DROP = "DROP TABLE IF EXISTS " + DB_Contract.InstantaneousHeartRateEntry.TABLE_NAME + ";";
    private static final String AVERAGE_HEART_RATE_TABLE_DROP = "DROP TABLE IF EXISTS " + DB_Contract.AverageHeartRateEntry.TABLE_NAME + ";";
    private static final String RR_INTERVALS_TABLE_DROP = "DROP TABLE IF EXISTS " + DB_Contract.RRIntervals.TABLE_NAME + ";";

    public DB_Helper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CONTACTS_TABLE_CREATE);
        sqLiteDatabase.execSQL(INSTANTANEOUS_HEART_RATE_TABLE_CREATE);
        sqLiteDatabase.execSQL(AVERAGE_HEART_RATE_TABLE_CREATE);
        sqLiteDatabase.execSQL(RR_INTERVALS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL(CONTACTS_TABLE_DROP);
        sqLiteDatabase.execSQL(INSTANTANEOUS_HEART_RATE_TABLE_DROP);
        sqLiteDatabase.execSQL(AVERAGE_HEART_RATE_TABLE_DROP);
        sqLiteDatabase.execSQL(RR_INTERVALS_TABLE_DROP);
        onCreate(sqLiteDatabase);
    }

    protected Cursor customQuery(String query) {
        return getWritableDatabase().rawQuery(query, null, null);
    }


    /********************* Contacts *******************************/
    protected void insertContact(String name, String phone, String priority, String action) {
        getReadableDatabase().execSQL("INSERT INTO " + DB_Contract.ContactsEntry.TABLE_NAME + " VALUES('" + name + "','" +
                phone + "','" + priority + "','" + action + "');");
    }

    protected Cursor getAllContacts() {
        return customQuery("SELECT * FROM " + DB_Contract.ContactsEntry.TABLE_NAME + ";");
    }

    protected void removeContactByPhone(String phone) {
        getReadableDatabase().delete(DB_Contract.ContactsEntry.TABLE_NAME, DB_Contract.ContactsEntry.PHONE_COLUMN + " = ?", new String[]{phone});
    }



    /********************* Instantaneous Heart Rate *******************************/
    protected void insertInstantaneousHeartRate(String timeStamp, String heartRate, String note) {
        getReadableDatabase().execSQL("INSERT INTO " + DB_Contract.InstantaneousHeartRateEntry.TABLE_NAME + " VALUES('" + timeStamp + "','" +
                heartRate + "','" + note + "');");
    }

    protected Cursor getAllInstantaneousHeartRates() {
        return customQuery("SELECT * FROM " + DB_Contract.InstantaneousHeartRateEntry.TABLE_NAME + ";");
    }

    protected void deleteAllInstantaneousHeartRates() {
        getReadableDatabase().delete(DB_Contract.InstantaneousHeartRateEntry.TABLE_NAME, null, null);
    }



    /********************* RR Intervals *******************************/

    protected void insertRRInterval(String timeStamp, String RR_interval, String note) {
        getReadableDatabase().execSQL("INSERT INTO " + DB_Contract.RRIntervals.TABLE_NAME + " VALUES('" + timeStamp + "','" +
                RR_interval + "','" + note + "');");
    }

    protected Cursor getAllRRIntervals() {
        return customQuery("SELECT * FROM " + DB_Contract.RRIntervals.TABLE_NAME + ";");
    }

    protected void deleteAllRRIntervals() {
        getReadableDatabase().delete(DB_Contract.RRIntervals.TABLE_NAME, null, null);
    }


}
