package uk.ac.warwick.my.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "events.db";
    private static final int DATABASE_VERSION = 2;

    private static final String SQL_CREATE_EVENTS = "CREATE TABLE " + EventTable.TABLE_NAME
            + " (" + EventTable._ID + " INTEGER PRIMARY KEY"
            + ", " + EventTable.COLUMN_NAME_SERVER_ID + " VARCHAR(255)"
            + ", " + EventTable.COLUMN_NAME_TYPE + " TEXT"
            + ", " + EventTable.COLUMN_NAME_TITLE + " TEXT"
            + ", " + EventTable.COLUMN_NAME_LOCATION + " TEXT"
            + ", " + EventTable.COLUMN_NAME_START + " DATETIME"
            + ", " + EventTable.COLUMN_NAME_END + " DATETIME"
            + ", " + EventTable.COLUMN_NAME_PARENT_SHORT_NAME + " TEXT"
            + ", " + EventTable.COLUMN_NAME_PARENT_FULL_NAME + " TEXT"
            + ")";

    private static final String SQL_DELETE_EVENTS = "DROP TABLE IF EXISTS " + EventTable.TABLE_NAME;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_EVENTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is effectively a cache - it's fine to delete and recreate
        // if the schema changes
        deleteAndRecreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        deleteAndRecreate(db);
    }

    private void deleteAndRecreate(SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_EVENTS);
        onCreate(db);
    }
}
