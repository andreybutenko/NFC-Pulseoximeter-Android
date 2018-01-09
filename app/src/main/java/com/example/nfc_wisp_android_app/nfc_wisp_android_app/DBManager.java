package com.example.nfc_wisp_android_app.nfc_wisp_android_app;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;


public class DBManager extends SQLiteOpenHelper {

    public static final String DB_NAME = "USER_MEASUREMENT_DB";
    public static final String MEASUREMENT_TABLE_NAME = "MEASUREMENT";

    public DBManager(Context context) {
        super(context, DB_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + MEASUREMENT_TABLE_NAME + "(" +
                "m_ir blob, " +
                "m_rd blob, " +
                "time varchar(50), " +
                "primary key(time)" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MEASUREMENT_TABLE_NAME);
        onCreate(db);
    }

    public void insertMeasurement(byte[] m_ir, byte[] m_rd, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "INSERT INTO " + MEASUREMENT_TABLE_NAME + " (m_ir, m_rd, time) " +
                "VALUES (?, ?, ?)";
        SQLiteStatement insert = db.compileStatement(sql);
        insert.bindBlob(1, m_ir);
        insert.bindBlob(2, m_rd);
        insert.bindString(3, time);
        insert.executeInsert();
    }

    public Cursor getSingleMeasurementHistory(String time) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor singleMeasurement = db.rawQuery("SELECT m_ir, m_rd FROM " +
                MEASUREMENT_TABLE_NAME + " " +
                "WHERE time = '" + time + "'", null);
        return singleMeasurement;
    }

    public Cursor getMeasurementHistory() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor measurement = db.rawQuery("SELECT time AS _id " +
                "FROM " + MEASUREMENT_TABLE_NAME, null);
        return measurement;
    }
}
