package com.example.nfc_wisp_android_app.nfc_wisp_android_app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.w3c.dom.Text;


public class MeasurementHistoryAdapter extends CursorAdapter {
    // Default constructor
    public MeasurementHistoryAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    public void bindView(View view, Context context, Cursor cursor) {
        TextView timeView = (TextView) view.findViewById(R.id.time);
        String time = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
        timeView.setText(time);
    }

    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // R.layout.list_row is your xml layout for each row
        return LayoutInflater.from(context).inflate(R.layout.measurement_item, parent, false);
    }

}
