package uk.ac.warwick.my.app.data;

import android.provider.BaseColumns;

public class EventTable implements BaseColumns {
    public static final String TABLE_NAME = "event";
    public static final String COLUMN_NAME_TITLE = "title";
    public static final String COLUMN_NAME_TYPE = "type";
    public static final String COLUMN_NAME_SERVER_ID = "server_id";
    public static final String COLUMN_NAME_START = "start";
    public static final String COLUMN_NAME_END = "end";
    public static final String COLUMN_NAME_LOCATION = "location";
    public static final String COLUMN_NAME_PARENT_SHORT_NAME = "parent_short_name";
    public static final String COLUMN_NAME_PARENT_FULL_NAME = "parent_full_name";

    private EventTable() {
    }
}
