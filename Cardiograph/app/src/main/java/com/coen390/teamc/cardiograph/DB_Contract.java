package com.coen390.teamc.cardiograph;


import android.provider.BaseColumns;

public class DB_Contract {

    protected static final class ContactsEntry implements BaseColumns {
        protected static final String TABLE_NAME = "Contacts";

        protected static final String NAME_COLUMN = "Name";
        protected static final String NAME_COLUMN_TYPE = "TEXT";

        protected static final String PHONE_COLUMN = "Phone";
        protected static final String PHONE_COLUMN_TYPE = "TEXT";

        protected static final String PRIORITY_COLUMN = "Priority";
        protected static final String PRIORITY_COLUMN_TYPE = "TEXT";

        protected static final String ACTION_COLUMN = "Action";
        protected static final String ACTION_COLUMN_TYPE = "TEXT";
    }


    protected static final class InstantaneousHeartRateEntry implements BaseColumns {
        protected static final String TABLE_NAME = "InstantaneousHeartRate";

        protected static final String DATE_COLUMN = "Date";
        protected static final String DATE_COLUMN_TYPE = "TEXT";

        protected static final String HEART_RATE_COLUMN = "HeartRate";
        protected static final String HEART_RATE_COLUMN_TYPE = "REAL";

        protected static final String NOTE_COLUMN = "Note";
        protected static final String NOTE_COLUMN_TYPE = "TEXT";
    }

    protected static final class AverageHeartRateEntry implements BaseColumns {
        protected static final String TABLE_NAME = "AverageHeartRate";

        protected static final String DATE_COLUMN = "Date";
        protected static final String DATE_COLUMN_TYPE = "TEXT";

        protected static final String HEART_RATE_COLUMN = "HeartRate";
        protected static final String HEART_RATE_COLUMN_TYPE = "REAL";

        protected static final String NOTE_COLUMN = "Note";
        protected static final String NOTE_COLUMN_TYPE = "TEXT";
    }

    protected static final class RRIntervals implements BaseColumns {
        protected static final String TABLE_NAME = "RRIntervals";

        protected static final String DATE_COLUMN = "Date";
        protected static final String DATE_COLUMN_TYPE = "TEXT";

        protected static final String RR_COLUMN = "RR";
        protected static final String RR_COLUMN_TYPE = "REAL";

        protected static final String NOTE_COLUMN = "Note";
        protected static final String NOTE_COLUMN_TYPE = "TEXT";
    }
}