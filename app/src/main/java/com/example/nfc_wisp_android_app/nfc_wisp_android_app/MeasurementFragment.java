package com.example.nfc_wisp_android_app.nfc_wisp_android_app;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;


public class MeasurementFragment extends ListFragment implements OnItemClickListener {
    private DBManager dbManager;

    public MeasurementFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_measurement, container, false);
        dbManager = ((MainActivity) getActivity()).dbManager;

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        Cursor history = dbManager.getMeasurementHistory();
        Log.d("MEASUREMENT_FRAG", String.valueOf(history));
        Log.d("MEASUREMENT_FRAG", String.valueOf(history.getColumnNames().length));
        Log.d("MEASUREMENT_FRAG", history.getColumnName(0));;
        Log.d("MEASUREMENT_FRAG", String.valueOf(history.getColumnCount()));
        if(history.getCount() != 0) {
            view.findViewById(R.id.empty_list_text).setVisibility(View.INVISIBLE);
            MeasurementHistoryAdapter adapter = new MeasurementHistoryAdapter(getActivity(), history);
            setListAdapter(adapter);
            getListView().setOnItemClickListener(this);
        } else {
            view.findViewById(R.id.empty_list_text).setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
        ListView listView = this.getListView();
        String time = ((Cursor) listView.getItemAtPosition(position - listView.getFirstVisiblePosition())).getString(0);

        toSingleHistoryMeasurementFragment(time);
    }


    private void toSingleHistoryMeasurementFragment(String time) {
        SingleHistoryMeasurementFragment fragment = new SingleHistoryMeasurementFragment(); // create new fragment

        // set-up arguments
        Bundle args = new Bundle();
        args.putString("TIME", time);
        fragment.setArguments(args);

        // begin fragment trasaction
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.fragment_container, fragment); // replace the transaction with current transaction
        transaction.addToBackStack(null); // add the transaction to the back stack so the user can navigate back

        transaction.commit();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
