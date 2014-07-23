package com.example.meme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class TimersDAO {
    private SQLiteDatabase db;
    private DBHelper dbHelper;
    private String[] allColumns = { DBHelper.COLUMN_TIME_INSTANT,
            DBHelper.COLUMN_MT, DBHelper.COLUMN_RT, DBHelper.COLUMN_DELTA_T };
    private static boolean createNewDB = false;

    public TimersDAO(Context context) {
        dbHelper = new DBHelper(context);
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public TimersModel createTimers(int timeInstant,
            Map<String, Double> mtArray, Map<String, Double> rtArray) {
        if (!createNewDB) {
            dbHelper.reCreateTable(db);
            createNewDB = !createNewDB;
        }
        Map<String, Double> deltaTArray = new HashMap<String, Double>();
        for (String device : mtArray.keySet()) {
            deltaTArray.put(device, mtArray.get(device) - rtArray.get(device));
        }

        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_TIME_INSTANT, timeInstant);
        values.put(DBHelper.COLUMN_MT, mtArray.toString());
        values.put(DBHelper.COLUMN_RT, rtArray.toString());
        values.put(DBHelper.COLUMN_DELTA_T, deltaTArray.toString());
        @SuppressWarnings("unused")
        long insertId = db.insert(DBHelper.TABLE_TIMERS, null, values);

        // Cursor cursor = db.query(DBHelper.TABLE_TIMERS, allColumns,
        // DBHelper.COLUMN_TIME_INSTANT + " = " + timeInstant, null, null,
        // null, null);
        // cursor.moveToFirst();
        TimersModel newTimers = null;
        return newTimers;

    }

    public List<TimersModel> getAllEntries() {
        List<TimersModel> timers = new ArrayList<TimersModel>();
        Cursor cursor = db.query(DBHelper.TABLE_TIMERS, allColumns, null, null,
                null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            TimersModel timersModel = cursorToTimers(cursor);
            timers.add(timersModel);
            cursor.moveToNext();
        }
        cursor.close();
        return timers;
    }

    public TimersModel getLatestEntry() {
        Cursor cursor = db.query(DBHelper.TABLE_TIMERS, allColumns, null, null,
                null, null, DBHelper.COLUMN_TIME_INSTANT);
        cursor.moveToLast();
        TimersModel timersModel = cursorToTimers(cursor);
        cursor.close();
        return timersModel;
    }

    private TimersModel cursorToTimers(Cursor cursor) {
        TimersModel timersModel = new TimersModel();
        timersModel.setTimeInstant(cursor.getInt(0));
        timersModel.setMtArray(cursor.getString(1));
        timersModel.setRtArray(cursor.getString(2));
        timersModel.setDeltatTArray(cursor.getString(3));
        return timersModel;
    }
}