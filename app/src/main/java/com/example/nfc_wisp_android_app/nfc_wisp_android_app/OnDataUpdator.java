package com.example.nfc_wisp_android_app.nfc_wisp_android_app;

import android.os.AsyncTask;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;


public class OnDataUpdator extends AsyncTask<float[], Void, Void> {
    private ILineDataSet IR;
    private ILineDataSet RD;
    private DoMeasurementFragment fragment;
    public OnDataUpdator(DoMeasurementFragment fragment, ILineDataSet IR, ILineDataSet RD) {
        this.fragment = fragment;
        this.IR = IR;
        this.RD = RD;
    }

    @Override
    protected Void doInBackground(float[]... entries) {
        try {
            float[] ir = entries[0];
            float[] rd = entries[1];
            float time = entries[2][0];

            for(int i = 0; i < ir.length; i++) {
                Entry ir_en = new Entry(time, ir[i]);
                Entry rd_en = new Entry(time, rd[i]);
                IR.addEntry(ir_en);
                RD.addEntry(rd_en);
                time++;
            }
            publishProgress();
        } catch (Exception e) {
            Log.e("OnDataUpdator", "Fail to Update Chart Entry", e);
        }
        return null;
    }


    @Override
    protected void onProgressUpdate(Void... entries) {
        try {
            //Log.d("Debug", "Post progress in OnDataUpdator");
            fragment.infraredLineChart.notifyDataSetChanged();
            fragment.infraredLineChart.setVisibleXRangeMaximum(200);
            fragment.infraredLineChart.moveViewToX(fragment.infraredLineChart.getLineData().getEntryCount());

            fragment.redLineChart.notifyDataSetChanged();
            fragment.redLineChart.setVisibleXRangeMaximum(200);
            fragment.redLineChart.moveViewToX(fragment.redLineChart.getLineData().getEntryCount());
        } catch (Exception e) {
            Log.e("OnDataUpdator", "Fail to refresh Chart");
        }
    }

}
