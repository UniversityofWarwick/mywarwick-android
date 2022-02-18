package uk.ac.warwick.my.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class EventDao implements Closeable {
    private final SQLiteDatabase db;

    public EventDao(Context context) {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        this.db = databaseHelper.getWritableDatabase();
    }

    public EventDao(SQLiteDatabase db) {
        this.db = db;
    }

    public void replaceAll(Collection<Event> events) {
        db.beginTransaction();
        try {
            deleteAll();

            for (Event event : events)
                put(event);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void put(Event event) {
        ContentValues values = buildContentValues(event);

        db.insert(EventTable.TABLE_NAME, null, values);
    }

    public List<Event> findAllByStart(Date start) {
        Cursor cursor = db.query(
                EventTable.TABLE_NAME,
                null,
                EventTable.COLUMN_NAME_START + " = ?",
                new String[]{String.valueOf(start.getTime())},
                null,
                null,
                null
        );

        try {
            List<Event> events = new ArrayList<>();

            while (cursor.moveToNext()) {
                events.add(buildEvent(cursor));
            }

            return events;
        } finally {
            cursor.close();
        }
    }

    public Event findByServerId(String serverId) {
        Cursor cursor = db.query(
                EventTable.TABLE_NAME,
                null,
                EventTable.COLUMN_NAME_SERVER_ID + " = ?",
                new String[]{serverId},
                null,
                null,
                null
        );

        try {
            if (cursor.moveToNext()) {
                return buildEvent(cursor);
            }

            return null;
        } finally {
            cursor.close();
        }
    }

    public Event find(String id) {
        Cursor cursor = db.query(
                EventTable.TABLE_NAME,
                null,
                EventTable._ID + " = ?",
                new String[]{id},
                null,
                null,
                null
        );

        try {
            if (cursor.moveToNext()) {
                return buildEvent(cursor);
            }

            return null;
        } finally {
            cursor.close();
        }
    }

    public Event getFirstEventAfterDate(Date date) {
        Cursor cursor = db.query(
                EventTable.TABLE_NAME,
                null,
                EventTable.COLUMN_NAME_START + " > ?",
                new String[]{String.valueOf(date.getTime())},
                null,
                null,
                EventTable.COLUMN_NAME_START + " ASC",
                "1"
        );

        try {
            if (cursor.moveToNext()) {
                return buildEvent(cursor);
            }

            return null;
        } finally {
            cursor.close();
        }
    }

    public void deleteAll() {
        db.execSQL("DELETE FROM " + EventTable.TABLE_NAME);
    }

    private ContentValues buildContentValues(Event event) {
        ContentValues values = new ContentValues();
        if (event.getId() != null) {
            values.put(EventTable._ID, event.getId());
        }
        values.put(EventTable.COLUMN_NAME_SERVER_ID, event.getServerId());
        values.put(EventTable.COLUMN_NAME_TYPE, event.getType());
        values.put(EventTable.COLUMN_NAME_TITLE, event.getTitle());
        values.put(EventTable.COLUMN_NAME_LOCATION, event.getLocation());
        values.put(EventTable.COLUMN_NAME_PARENT_FULL_NAME, event.getParentFullName());
        values.put(EventTable.COLUMN_NAME_PARENT_SHORT_NAME, event.getParentShortName());
        values.put(EventTable.COLUMN_NAME_START, event.getStart().getTime());
        values.put(EventTable.COLUMN_NAME_END, event.getEnd().getTime());
        return values;
    }

    private Event buildEvent(Cursor cursor) {
        Event event = new Event();
        event.setId(cursor.getInt(cursor.getColumnIndexOrThrow(EventTable._ID)));
        event.setServerId(cursor.getString(cursor.getColumnIndexOrThrow(EventTable.COLUMN_NAME_SERVER_ID)));
        event.setType(cursor.getString(cursor.getColumnIndexOrThrow(EventTable.COLUMN_NAME_TYPE)));
        event.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(EventTable.COLUMN_NAME_TITLE)));
        event.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(EventTable.COLUMN_NAME_LOCATION)));
        event.setParentFullName(cursor.getString(cursor.getColumnIndexOrThrow(EventTable.COLUMN_NAME_PARENT_FULL_NAME)));
        event.setParentShortName(cursor.getString(cursor.getColumnIndexOrThrow(EventTable.COLUMN_NAME_PARENT_SHORT_NAME)));
        event.setStart(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(EventTable.COLUMN_NAME_START))));
        event.setEnd(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(EventTable.COLUMN_NAME_END))));
        return event;
    }

    @Override
    public void close() {
        db.close();
    }
}
