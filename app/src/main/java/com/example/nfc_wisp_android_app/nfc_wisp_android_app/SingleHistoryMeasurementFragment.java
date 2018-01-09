package com.example.nfc_wisp_android_app.nfc_wisp_android_app;


import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.vision.text.Text;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;


/**
 * A simple {@link Fragment} subclass.
 */
public class SingleHistoryMeasurementFragment extends Fragment implements OnClickListener {

    private static final String TIME = "TIME";
    private static final float AXIS_LABEL_FONT_SIZE = 12f;


    private LineChart infraredLineChart;
    private LineChart redLineChart;
    private static final int WINDOW_SIZE = 200;
    private static final float Y_INITIAL = 1000000f;
    private static final String IR_CHART_TAG = "Infrared Light Data";
    private static final String RD_CHART_TAG = "Red Light Data";
    private static final String IR_DATA_TAG = "IR";
    private static final String RD_DATA_TAG = "RD";



    private static int CONVOLVE_SIZE = 10;
    private float IR_max;
    private float IR_min;
    private double IR_mean;
    private double IR_stddev;
    private float RED_max;
    private float RED_min;

    private ArrayList<Float> IR_max_arr;
    private ArrayList<Float> IR_min_arr;
    private ArrayList<Float> RED_max_arr;
    private ArrayList<Float> RED_min_arr;
    private ArrayList<Integer> IR_max_index_arr;
    private ArrayList<Integer> IR_min_index_arr;
    private ArrayList<Integer> RED_max_index_arr;
    private ArrayList<Integer> RED_min_index_arr;


    private static double[] IR_energy_vec;
    private static double[] RED_energy_vec;

    private static float[] IR_raw;
    private static float[] RD_raw;


    public enum LightType {
        RED,
        IR
    }

    public SingleHistoryMeasurementFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_single_history_measurement, container, false);
        Button backButton = (Button) view.findViewById(R.id.back_to_measurement_list);
        backButton.setOnClickListener(this);

        Button saveButton = (Button) view.findViewById(R.id.save_data);
        saveButton.setOnClickListener(this);
        return view;
    }


    @Override
    public void onStart() {
        super.onStart();




        Bundle args = getArguments();
        String time = args.getString(TIME);

        MainActivity mMain = (MainActivity) getActivity();
        Cursor cursor = mMain.dbManager.getSingleMeasurementHistory(time);

        cursor.moveToFirst();
        byte[] m_ir = cursor.getBlob(0);
        byte[] m_rd = cursor.getBlob(1);

        int ir_start = /*m_ir.length / 4;*/ 128;
        int rd_start = /*m_rd.length / 4;*/ 128;
        byte[] m_ir_data = new byte[m_ir.length - ir_start];
        byte[] m_rd_data = new byte[m_rd.length - rd_start];
        for(int i = ir_start; i < m_ir.length; i++) {
            m_ir_data[i - ir_start] = m_ir[i];
        }
        for(int i = rd_start; i < m_rd.length; i++) {
            m_rd_data[i - rd_start] = m_rd[i];
        }

        infraredLineChart = (LineChart) getActivity().findViewById(R.id.history_ir);
        redLineChart = (LineChart) getActivity().findViewById(R.id.history_rd);
        // Line chart general configuration
        chartConfig(infraredLineChart, IR_CHART_TAG);
        chartConfig(redLineChart, RD_CHART_TAG);

        float[] dst_ir = testNFCTag(m_ir_data, LightType.IR);
        float[] dst_red = testNFCTag(m_rd_data, LightType.RED);

        levelUpArray(dst_ir);
        levelUpArray(dst_red);

        // Actual Data Flow
        LineDataSet ir = setStaticData(dst_ir, IR_DATA_TAG, Color.BLUE, LightType.IR);
        LineDataSet rd = setStaticData(dst_red, RD_DATA_TAG, Color.RED, LightType.RED);


        TextView oxygen_level = (TextView) getActivity().findViewById(R.id.oxygen_level_percentage);
        double SO2 = getOxygenLevel(dst_ir, dst_red);

        Log.d("Debug", String.format( "%.2f", SO2 ) + "%");
        oxygen_level.setText("Oxygen Level: " + String.format( "%.2f", SO2 ) + "%");
        TextView heart_rate = (TextView) getActivity().findViewById(R.id.heartrate_percentage);

        double BPM = calculateBPM(dst_ir);

        heart_rate.setText("BPM: " + String.format("%.2f", BPM));

        if(ir != null) {
            infraredLineChart.setData(new LineData(ir));
        } else {
            infraredLineChart.setData(new LineData(setInitialData(IR_CHART_TAG, Color.BLUE)));
        }
        if(rd != null) {
            redLineChart.setData(new LineData(rd));
        } else {
            redLineChart.setData(new LineData(setInitialData(RD_DATA_TAG, Color.RED)));
        }

        infraredLineChart.invalidate();
        redLineChart.invalidate();
    }

    private void levelUpArray(float[] filtered) {
        if(IR_min < 0 || RED_min < 0) {
            float min = Math.min(IR_min, RED_min);
            for (int i = 0; i < filtered.length; i++) {
                filtered[i] -= (min - 1);
            }
        }
    }

    private void processData(float[] filtered, LightType ir_red) {

        AMPD ampd = new AMPD(filtered);

        int[] max_indices = ampd.getMaxima();
        Log.d("Debug", "max_indices = " + Arrays.toString(max_indices));
        int[] min_indices = ampd.getMinima();
        Log.d("Debug", "min_indices = " + Arrays.toString(min_indices));


        ArrayList<Float> max_arr = new ArrayList<>();
        ArrayList<Float> min_arr = new ArrayList<>();
        ArrayList<Integer> max_index_arr = new ArrayList<>();
        ArrayList<Integer> min_index_arr = new ArrayList<>();


        for(int i = 0; i < max_indices.length; i++) {
            int index = max_indices[i];
            max_arr.add(filtered[index]);
            max_index_arr.add(index);
        }

        for(int i = 0; i < min_indices.length; i++) {
            int index = min_indices[i];
            min_arr.add(filtered[index]);
            min_index_arr.add(index);
        }

        double[] energy_vec = new double[filtered.length];
        for(int i = 1; i < filtered.length - 1; i++) {
            energy_vec[i] = (double) (filtered[i] * filtered[i]) - (double) (filtered[i - 1] * filtered[i + 1]);
        }

        switch (ir_red) {
            case IR:
                IR_max_arr = max_arr;
                IR_max_index_arr = max_index_arr;
                IR_min_arr = min_arr;
                IR_min_index_arr = min_index_arr;
                IR_energy_vec = energy_vec;
                break;
            case RED:
                RED_max_arr = max_arr;
                RED_max_index_arr = max_index_arr;
                RED_min_arr = min_arr;
                RED_min_index_arr = min_index_arr;
                RED_energy_vec = energy_vec;
                break;
        }
    }


    private double getOxygenLevel(float[] dst_ir, float[] dst_rd) {
        processData(dst_ir, LightType.IR);
        processData(dst_rd, LightType.RED);
        double ir_max = 0, ir_min = 0, red_max = 0, red_min = 0;
        //int ir_max_len = 0, ir_min_len = 0, red_max_len = 0, red_min_len = 0;


        double[] ir_max_arr = new double[IR_max_arr.size()];
        for(int i = 0; i < IR_max_arr.size(); i++) {
            ir_max_arr[i] = IR_max_arr.get(i);
        }
        Arrays.sort(ir_max_arr);
        ir_max = ir_max_arr[ir_max_arr.length / 2];

        double[] ir_min_arr = new double[IR_min_arr.size()];
        for(int i = 0; i < IR_min_arr.size(); i++) {
            ir_min_arr[i] = IR_min_arr.get(i);
        }
        Arrays.sort(ir_min_arr);
        ir_min = ir_min_arr[ir_min_arr.length / 2];

        double[] red_max_arr = new double[RED_max_arr.size()];
        for(int i = 0; i < RED_max_arr.size(); i++) {
            red_max_arr[i] = RED_max_arr.get(i);
        }
        Arrays.sort(red_max_arr);
        red_max = red_max_arr[red_max_arr.length / 2];

        double[] red_min_arr = new double[RED_min_arr.size()];
        for(int i = 0; i < RED_min_arr.size(); i++) {
            red_min_arr[i] = RED_min_arr.get(i);
        }
        Arrays.sort(red_min_arr);
        red_min = red_min_arr[red_min_arr.length / 2];



        /*double[] sorted_IR_energy_vec = new double[IR_energy_vec.length];
        double[] sorted_RED_energy_vec = new double[RED_energy_vec.length];
        for(int i = 0; i < IR_energy_vec.length; i++)
            sorted_IR_energy_vec[i] = IR_energy_vec[i];
        for(int i = 0; i < RED_energy_vec.length; i++)
            sorted_RED_energy_vec[i] = RED_energy_vec[i];
        Arrays.sort(sorted_IR_energy_vec);
        Arrays.sort(sorted_RED_energy_vec);

        double IR_threshold = sorted_IR_energy_vec[((sorted_IR_energy_vec.length - 1) * 96 / 100)];
        double RED_threshold = sorted_RED_energy_vec[((sorted_RED_energy_vec.length - 1) * 96 / 100)];

        for(int i = 3; i < IR_max_arr.size() - 1; i++) {
            double energy_level = IR_energy_vec[IR_max_index_arr.get(i)];
            if(energy_level < IR_threshold) {
            //if(true){
                ir_max += IR_max_arr.get(i);
                ir_max_len++;
            }
        }
        Log.d("Debug", "IR_max_arr.size() = " + Integer.toString(IR_max_arr.size()));
        Log.d("Debug", "ir_max_len = " + Integer.toString(ir_max_len));
        Log.d("Debug", "ir_max sum = " + String.format("%.2f", ir_max));
        Log.d("Debug", "ir_max = " + String.format("%.2f", ir_max / ir_max_len));
        ir_max /= ir_max_len;
        for(int i = 3; i < IR_min_arr.size() - 1; i++) {
            double energy_level = IR_energy_vec[IR_min_index_arr.get(i)];
            if(energy_level < IR_threshold) {
            //if(true) {
                ir_min += IR_min_arr.get(i);
                ir_min_len++;
            }
        }
        Log.d("Debug", "IR_min_arr.size() = " + Integer.toString(IR_min_arr.size()));
        Log.d("Debug", "ir_min_len = " + Integer.toString(ir_min_len));
        Log.d("Debug", "ir_min sum = " + String.format("%.2f", ir_min));
        Log.d("Debug", "ir_min = " + String.format("%.2f", ir_min / ir_min_len));
        ir_min /= ir_min_len;
        for(int i = 1; i < RED_max_arr.size() - 1; i++) {
            double energy_level = RED_energy_vec[RED_max_index_arr.get(i)];
            if(energy_level < RED_threshold) {
                red_max += RED_max_arr.get(i);
                red_max_len++;
            }
        }
        Log.d("Debug", "RED_max_arr.size() = " + Integer.toString(RED_max_arr.size()));
        Log.d("Debug", "red_max_len = " + Integer.toString(red_max_len));
        Log.d("Debug", "red_max sum = " + String.format("%.2f", red_max));
        Log.d("Debug", "red_max = " + String.format("%.2f", red_max / red_max_len));
        red_max /= red_max_len;
        for(int i = 1; i < RED_min_arr.size() - 1; i++) {
            double energy_level = RED_energy_vec[RED_min_index_arr.get(i)];
            if(energy_level < RED_threshold) {
                red_min += RED_min_arr.get(i);
                red_min_len++;
            }
        }
        Log.d("Debug", "RED_min_arr.size() = " + Integer.toString(IR_min_arr.size()));
        Log.d("Debug", "red_min_len = " + Integer.toString(red_min_len));
        Log.d("Debug", "red_min sum = " + String.format("%.2f", red_min));
        Log.d("Debug", "red_min = " + String.format("%.2f", red_min / red_min_len));
        red_min /= red_min_len;*/


        return getSO2((float) ir_max, (float) ir_min, (float) red_max, (float) red_min);
    }

    private double getSO2(float ir_max, float ir_min, float red_max, float red_min) {
        double Ros = Math.log((double) ir_max / (double) ir_min) / Math.log((double) red_max / (double) red_min);
        Log.d("Debug", "ir_max = " + String.format("%.2f", ir_max));
        Log.d("Debug", "ir_min = " + String.format("%.2f", ir_min));
        Log.d("Debug", "red_max = " + String.format("%.2f", red_max));
        Log.d("Debug", "red_min = " + String.format("%.2f", red_min));
        Log.d("Debug", "Ros = " + String.format("%.2f", Ros));
        double SO2 = 110 - 12 * Ros;
        return SO2;
    }

    private double calculateBPM(float[] dst) {
        double T = 0.015;
        double N = (double) (dst.length);
        double Total_time = T * N;

        return (60 * (double) IR_min_arr.size()) / Total_time;
    }





    private LineDataSet setStaticData(float[] dst, String tag, int color, LightType ir_red) {
        if(dst == null || dst.length == 0) return null;

        float fakeTime = 0f;

        //float[] dst = testFixedData(measurement);


        List<Entry> dataList = new ArrayList<Entry>();


        for(int i = 0; i < dst.length; i++) {
            dataList.add(new Entry(fakeTime, dst[i]));
            fakeTime++;
        }
        LineDataSet dataSet = new LineDataSet(dataList, tag);
        dataSetConfig(dataSet, color);
        return dataSet;
    }

    private float[] testNFCTag(byte[] measurement, LightType ir_red) {
        float[] dst = new float[measurement.length / 2];
        for(int i = 0; i < measurement.length; i += 2) {
            int d = 0;
            d |= (measurement[i + 1] & 0xFF);
            d = d << 8;
            d |= (measurement[i] & 0xFF);
            dst[i / 2] = (float) d;
        }
        switch(ir_red) {
            case IR:
                IR_raw = dst;
                break;
            case RED:
                RD_raw = dst;
                break;
        }

        float[] convolved = filterFunction_3(dst, ir_red);

        return convolved;
    }

    private float[] filterFunction_1(float[] dst) {
        float[] convolved = new float[dst.length - (CONVOLVE_SIZE - 1)];
        Queue<Float> q = new LinkedList<Float>();
        float sum = 0;
        float max = Float.MIN_VALUE, min = Float.MAX_VALUE;
        for(int i = 0; i < CONVOLVE_SIZE; i++) {
            q.add(dst[i]);
            sum += dst[i];
        }
        for(int i = 0; i < convolved.length; i++) {
            convolved[i] = sum / ((float) CONVOLVE_SIZE);
            max = Math.max(max, convolved[i]);
            min = Math.min(min, convolved[i]);
            if (i + CONVOLVE_SIZE < dst.length) {
                sum -= q.poll();
                sum += dst[i + CONVOLVE_SIZE];
                q.add(dst[i + CONVOLVE_SIZE]);
            }
        }
        return convolved;
    }

    private float[] filterFunction_2(float[] dst, float smoothing) {
        float[] filtered = new float[dst.length];
        filtered[0] = dst[0];
        float value = dst[0];
        float max = Float.MIN_VALUE, min = Float.MAX_VALUE;
        for(int i = 1; i < dst.length; i++) {
            float cur = dst[i];
            value += (cur - value) / smoothing;
            filtered[i] = value;
            max = Math.max(max, value);
            min = Math.min(min, value);
        }
        return filtered;
    }

    private float[] filterFunction_3(float[] dst, LightType ir_red) {
        float[] filtered = new float[dst.length];
        Filter filter = new Filter(/*1000*/ 150 , dst.length, Filter.PassType.Lowpass, (float) 1);
        float max = Float.MIN_VALUE, min = Float.MAX_VALUE;
        for(int i = 0; i < dst.length; i++) {
            filter.Update(dst[i]);
            filtered[i] = filter.getValue();
            max = Math.max(max, filtered[i]);
            min = Math.min(min, filtered[i]);
        }

        filter = new Filter(40, dst.length, Filter.PassType.Highpass, (float) 1);
        for(int i = 0; i < filtered.length; i++) {
            filter.Update(filtered[i]);
            filtered[i] = filter.getValue();
            max = Math.max(max, filtered[i]);
            min = Math.min(min, filtered[i]);
        }
        switch (ir_red) {
            case IR:
                IR_min = min;
                break;
            case RED:
                RED_min = min;
                break;
        }
        Log.d("Debug", "min = " + Float.toString(min));
        //filtered = filterFunction_2(filtered, 10);
        return filtered;
    }

    private float[] testFixedData(byte[] measurement) {
        float[] dst = new float[measurement.length / 4];
        for(int i = 0; i < measurement.length; i += 4) {
            int d = 0;
            d |= (measurement[i] & 0xFF);
            d |= ((measurement[i + 1] & 0xFF) << 8);
            d |= ((measurement[i + 2] & 0xFF) << 16);
            d |= ((measurement[i + 3] & 0xFF) << 24);
            dst[i / 4] = Float.intBitsToFloat(d);
        }
        return dst;
    }


    private LineDataSet setInitialData(String name, int color) {
        float fakeTime = 0f;
        int count = 0;
        List<Entry> dataList = new ArrayList<Entry>();

        while(count < WINDOW_SIZE) {
            float data = Y_INITIAL;
            dataList.add(new Entry(fakeTime, data));
            fakeTime++;
            count++;
        }
        LineDataSet dataSet = new LineDataSet(dataList, name);
        dataSetConfig(dataSet, color);
        return dataSet;
    }

    private void dataSetConfig(LineDataSet dataSet, int color) {
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setCircleRadius(1f);
        dataSet.setDrawValues(false);
        dataSet.setCircleColor(color);
        dataSet.setColor(color);
    }

    private void chartConfig(LineChart chart, String desc) {
        // General Setting
        Description description = new Description();
        description.setText(desc);
        description.setTextColor(Color.BLACK);
        description.setTextSize(10f);

        chart.setDescription(description);
        chart.setNoDataText(desc);
        chart.setAutoScaleMinMaxEnabled(true);
        chart.setDrawBorders(true);

        chart.setTouchEnabled(true); // disable touch interaction

        chart.getAxisRight().setEnabled(false); // disable the right axis
        YAxis yAxis = chart.getAxisLeft();
        yAxis.setEnabled(true);
        yAxis.setTextColor(Color.BLACK);
        yAxis.setTextSize(AXIS_LABEL_FONT_SIZE);
        yAxis.setLabelCount(7, true);
        yAxis.setDrawGridLines(true); // disable background grid line


        XAxis xAxis = chart.getXAxis();
        xAxis.setEnabled(true);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setTextSize(AXIS_LABEL_FONT_SIZE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false); // disable background grid line
        chart.invalidate();
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.back_to_measurement_list) {
            toMeasurementHistory();
        } else if(id == R.id.save_data) {
            saveData();
        }
    }


    private void saveData() {
        MainActivity mMain = (MainActivity) getActivity();
        Bundle args = getArguments();
        String time = args.getString(TIME);
        //time.replace(' ', '_');
        for(int i = 0; i < time.length(); i++) {
            if(time.charAt(i) == ' ' || time.charAt(i) == ':') {
                time = time.substring(0, i - 1) + "_" + time.substring(i + 1);
            }
        }
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File dir = new File(Environment.getExternalStorageDirectory(), "root");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File ir_file = new File(dir, time + "_IR.csv");
            if(!ir_file.exists()) {
                try {
                    ir_file.createNewFile();
                } catch (IOException e) {
                    Log.d("Debug", "ir_file cannot be created");
                }
                try {
                    FileWriter ir_writer = new FileWriter(ir_file);

                    for (int i = 0; i < IR_raw.length; i++) {
                        ir_writer.append(Float.toString(IR_raw[i]) + "\n");
                        ir_writer.flush();
                    }
                    ir_writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }



            File rd_file = new File(dir, time + "_RD.csv");
            if(!rd_file.exists()) {
                try {
                    rd_file.createNewFile();
                } catch (IOException e) {
                    Log.d("Debug", "rd_file cannot be created");
                }
                try {
                    FileWriter rd_writer = new FileWriter(rd_file);

                    for (int i = 0; i < RD_raw.length; i++) {
                        rd_writer.append(Float.toString(RD_raw[i]) + "\n");
                        rd_writer.flush();
                    }
                    rd_writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Snackbar.make(mMain.findViewById(android.R.id.content), "Data Has Been Save to " + Environment.getExternalStorageDirectory(), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else {
            Snackbar.make(mMain.findViewById(android.R.id.content), "External Memory Not Available.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }


    private void toMeasurementHistory() {
        MeasurementFragment fragment = new MeasurementFragment(); // create new fragment
        // set-up arguments
        Bundle args = new Bundle();
        fragment.setArguments(args);

        // begin fragment trasaction
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.fragment_container, fragment); // replace the transaction with current transaction
        transaction.addToBackStack(null); // add the transaction to the back stack so the user can navigate back

        transaction.commit();
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
