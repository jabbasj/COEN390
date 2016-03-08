package com.coen390.teamc.cardiograph;


import android.provider.BaseColumns;

public class DB_Contract {

    public static final class ContactsEntry implements BaseColumns {
        public static final String TABLE_NAME = "Contacts";

        public static final String NAME_COLUMN = "Name";
        public static final String NAME_COLUMN_TYPE = "TEXT";

        public static final String PHONE_COLUMN = "Phone";
        public static final String PHONE_COLUMN_TYPE = "TEXT";

        public static final String PRIORITY_COLUMN = "Priority";
        public static final String PRIORITY_COLUMN_TYPE = "TEXT";

        public static final String ACTION_COLUMN = "Action";
        public static final String ACTION_COLUMN_TYPE = "TEXT";
    }


    public static final class InstantaneousHeartRateEntry implements BaseColumns {
        public static final String TABLE_NAME = "InstantaneousHeartRate";

        public static final String DATE_COLUMN = "Date";
        public static final String DATE_COLUMN_TYPE = "TEXT";

        public static final String HEART_RATE_COLUMN = "HeartRate";
        public static final String HEART_RATE_COLUMN_TYPE = "REAL";

        public static final String NOTE_COLUMN = "Note";
        public static final String NOTE_COLUMN_TYPE = "TEXT";
    }

    public static final class AverageHeartRateEntry implements BaseColumns {
        public static final String TABLE_NAME = "AverageHeartRate";

        public static final String DATE_COLUMN = "Date";
        public static final String DATE_COLUMN_TYPE = "TEXT";

        public static final String HEART_RATE_COLUMN = "HeartRate";
        public static final String HEART_RATE_COLUMN_TYPE = "REAL";

        public static final String NOTE_COLUMN = "Note";
        public static final String NOTE_COLUMN_TYPE = "TEXT";
    }
}