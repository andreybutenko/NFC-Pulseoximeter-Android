package com.example.nfc_wisp_android_app.nfc_wisp_android_app;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.Entry;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.util.Log;
import java.io.InputStream;
import java.io.PipedReader;
import java.lang.reflect.Array;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ChartUpdator extends AsyncTask<Float, float[], Float> {
    private MainActivity mMain;
    private DoMeasurementFragment fragment;
    private byte[] out;
    private String curTime;
    private int OUTPUT_SIZE = 16;
    private int INPUT_SIZE = 16;
    private static final int MAX_WIDTH = 512;
    private static final long MEASURE_TIME = 10 * 1000; // milliseconds
    private Tag tag;


    public ChartUpdator(MainActivity mMain, String curTime, Tag tag) {
        this.mMain = mMain;
        this.fragment = (DoMeasurementFragment) this.mMain.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        this.curTime = curTime;
        this.tag = tag;
        this.out = new byte[OUTPUT_SIZE];
    }

    @Override
    protected Float doInBackground(Float... timeStamp) {
        float time = timeStamp[0];
        float[] red = new float[INPUT_SIZE / 4];
        float[] ir  = new float[INPUT_SIZE / 4];

        IsoDep isoDep = IsoDep.get(tag);
        try {
            isoDep.connect();
            byte[] payload, rawPayload;
            ArrayList<Byte> ir_array = new ArrayList<>();
            ArrayList<Byte> red_array = new ArrayList<>();
            int count = 0;
            long endTime = System.currentTimeMillis() + MEASURE_TIME;

            while(isoDep.isConnected() && count < MAX_WIDTH
                    && System.currentTimeMillis() <= endTime
                    ) {
//
                Log.d("CU", "===");
//                Log.d("CU", "Cont: " + count);
//                Log.d("CU", "Time: " + time);
//                Log.d("CU", "Unix: " + System.currentTimeMillis());
                Log.d("CU", "Remaining: " + String.valueOf((endTime - System.currentTimeMillis()) / 1000f));
                isoDep.setTimeout(50000);
                rawPayload = isoDep.transceive(out);
                payload = decodePayload(rawPayload);

                // The device sends 16 bytes at a time in 4 groups of 4 bytes each
                // This loop performs some bitwise operations to get two values from this packet
                // One is the reflection of red light, the other is the reflection of infrared light

                for(int j = 0; j < INPUT_SIZE; j += 4) {
                    ir_array.add(payload[j]);
                    ir_array.add(payload[j + 1]);

                    red_array.add(payload[j + 2]);
                    red_array.add(payload[j + 3]);

                    int ir_s = 0;
                    ir_s = (payload[j + 1] & 0xFF);
                    ir_s = (ir_s << 8) | (payload[j] & 0xFF);

                    int rd_s = 0;
                    rd_s = (payload[j + 3] & 0xFF);
                    rd_s = (rd_s << 8) | (payload[j + 2] & 0xFF);

                    ir[j / 4] = (float) ir_s;
                    red[j / 4] = (float) rd_s;

                    time += 4;
                }

                float[] times = new float[]{time};
                if(count > 0) publishProgress(ir, red, times);
                count++;
            }

            Log.d("CU", "Complete!");

            byte[] result_ir = new byte[ir_array.size()];
            byte[] result_red = new byte[red_array.size()];
            for(int i = 0; i < ir_array.size(); i++) {
                result_ir[i] = ir_array.get(i);
                result_red[i] = red_array.get(i);
            }

            Log.d("CU", "Complete + Processed!");

            Snackbar.make(mMain.findViewById(android.R.id.content), "All done! :)", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            mMain.dbManager.insertMeasurement(result_ir, result_red, curTime);
            Log.d("CU", "Complete + Processed + Saved!");
            isoDep.close();
            return time;
        } catch (Exception e) {
            Snackbar.make(mMain.findViewById(android.R.id.content), "Tag is lost :(", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            Log.e("Debug", "Fail to send new data", e);
        }
        return null;
    }

    // decode function
    private byte[] decodePayload(byte[] rawPayload) {
        //TODO implement decode function
        return rawPayload;
    }


    @Override
    protected void onProgressUpdate(float[]... entries) {
        try {
            fragment.onDataSetChange(entries[0], entries[1], entries[2][0]);
        } catch (Exception e) {
            Log.e("Debug", "Fail to publish updates", e);
        }
    }

    @Override
    protected void onPostExecute(Float result) {
        try {
            mMain.timerCallBack(result);
            Snackbar.make(mMain.findViewById(android.R.id.content), "Measurement Finished.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } catch (Exception e) {
            Log.e("Debug", "Fail to update new time");
        }
    }
}
