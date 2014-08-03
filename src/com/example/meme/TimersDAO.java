package com.example.meme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

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

    public long storeTimers(int timeInstant, Map<String, Double> mtArray,
            Map<String, Double> rtArray) {
        if (db == null || !db.isOpen())
            open();
        if (!createNewDB) {
            dbHelper.reCreateTable(db);
            createNewDB = !createNewDB;
        }
        Log.d("MEME", "TimersDAO");
        Log.d("MEME", "MT: " + mtArray);
        Log.d("MEME", "RT: " + rtArray);
        Map<String, Double> deltaTArray = new HashMap<String, Double>();
        for (String device : mtArray.keySet()) {
            deltaTArray.put(device, mtArray.get(device) - rtArray.get(device));
        }

        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_TIME_INSTANT, timeInstant);
        values.put(DBHelper.COLUMN_MT, mtArray.toString());
        values.put(DBHelper.COLUMN_RT, rtArray.toString());
        values.put(DBHelper.COLUMN_DELTA_T, deltaTArray.toString());

        long insertId = db.insert(DBHelper.TABLE_TIMERS, null, values);
        close();
        return insertId;

    }

    public List<TimersModel> getAllEntries() {
        if (db == null || !db.isOpen())
            open();
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
        close();
        return timers;
    }

    public TimersModel getLatestEntry() {
        if (db == null || !db.isOpen())
            open();
        Cursor cursor = db.query(DBHelper.TABLE_TIMERS, allColumns, null, null,
                null, null, DBHelper.COLUMN_TIME_INSTANT);
        if (cursor.getCount() <= 0) {
            close();
            return null;
        } else {
            cursor.moveToLast();
            TimersModel timersModel = cursorToTimers(cursor);
            cursor.close();
            close();
            return timersModel;
        }
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
