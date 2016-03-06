package com.coen390.j_abba.random_facts;

import android.provider.BaseColumns;

public class FactContract {
    // Inner class that that defines the table contents of the weather table
    public static final class FactsEntry implements BaseColumns {
        public static final String TABLE_NAME = "Facts";
        public static final String COLUMN_NAME = "Fact";
        public static final String COLUMN_TYPE = "TEXT";
        public static final String COLUMN_FACT_TYPE = "Type";
        public static final String COLUMN_TYPE_TYPE = "TEXT";
    }
}
