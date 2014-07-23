package com.example.meme;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

    static final String TABLE_TIMERS = "timers";
    static final String COLUMN_TIME_INSTANT = "time_instant";
    static final String COLUMN_MT = "mt";
    static final String COLUMN_RT = "rt";
    static final String COLUMN_DELTA_T = "delta_t";
    static final String DATABASE_NAME = "timers.db";
    static int DATABASE_VERSION = 1;

    private static final String DB_CREATE = "create table " + TABLE_TIMERS
            + "(" + COLUMN_TIME_INSTANT + " integer primary key, " + COLUMN_MT
            + " text not null, " + COLUMN_RT + " text not null, "
            + COLUMN_DELTA_T + " text not null);";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DB_CREATE);
    }

    public void reCreateTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIMERS);
        onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(DBHelper.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIMERS);
        onCreate(db);
    }

}
