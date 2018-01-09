package com.example.nfc_wisp_android_app.nfc_wisp_android_app;

import android.content.IntentFilter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private String CURRENT_TIME = DateFormat.getDateTimeInstance().format(new Date());
    private static final String DISPLAY_MESSAGE = "DISPLAY_MESSAGE";

    public DBManager dbManager;

    private float fakeTimeStamp;
    public ChartUpdator chartUpdator;

    private IntentFilter[] intentFiltersArray;
    private String[][] techListsArray;
    NfcAdapter mNfcAdapter;
    PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize database helper
        dbManager = new DBManager(this);

        // Initialization configuration currently not in use
        /* NFC Technology initialization
         * (Comments is cited from https://developer.android.com/guide/topics/connectivity/nfc/advanced-nfc.html)*/


        /* Create a PendingIntent object so the Android system can populate it with the details of
         * the tag when it is scanned.*/
        /*pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*//*");    // Handles all MIME based dispatches. You should specify only the ones that you need.
        }
        catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }*/

        /*
        *
        * Declare intent filters to handle the intents that you want to intercept. The foreground
        * dispatch system checks the specified intent filters with the intent that is received when
        * the device scans a tag. If it matches, then your application handles the intent. If it
        * does not match, the foreground dispatch system falls back to the intent dispatch system.
        * Specifying a null array of intent filters and technology filters, specifies that you want
        * to filter for all tags that fallback to the TAG_DISCOVERED intent. The code snippet below
        * handles all MIME types for NDEF_DISCOVERED. You should only handle the ones that you need.
        *
        * */
        //intentFiltersArray = new IntentFilter[] {ndef, };

        /*
        *
        * Set up an array of tag technologies that your application wants to handle. Call the Object.
        * class.getName() method to obtain the class of the technology that you want to support.
        *
        * */
        //techListsArray = new String[][] { new String[] { NfcF.class.getName() } };

        //mNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);

        toDoMeasurementFragment();
        fakeTimeStamp = 0f;
    }


    @Override
    public void onPause() {
        super.onPause();
        mNfcAdapter.getDefaultAdapter(this).disableForegroundDispatch(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        mNfcAdapter.getDefaultAdapter(this).enableForegroundDispatch(this, intent, null, null);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        //Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG); // for write to NFC
        setIntent(intent);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if((fragment instanceof DoMeasurementFragment)
                && (NfcAdapter.getDefaultAdapter(this) == null)) {
            ((DoMeasurementFragment) fragment).OnTagDetected("NFC is not available");
        } else if ((fragment instanceof DoMeasurementFragment)
                && NfcAdapter.ACTION_TAG_DISCOVERED.equals(getIntent().getAction())) {
            ((DoMeasurementFragment) fragment).OnTagDetected("Tag Detected");
            sendTestData((Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
        }
    }

    private void sendTestData(Tag tag) {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String curTime = sdf.format(c.getTime());

        chartUpdator = new ChartUpdator(this, curTime, tag);
        chartUpdator.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fakeTimeStamp);
    }

    public void timerCallBack(float time) {
        this.fakeTimeStamp = time;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.measurement_history) {
            toMeasurementHistory();
        } else if(id == R.id.do_measurement) {
            toDoMeasurementFragment();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void toMeasurementHistory() {
        MeasurementFragment fragment = new MeasurementFragment(); // create new fragment

        // set-up arguments
        Bundle args = new Bundle();
        fragment.setArguments(args);

        // begin fragment trasaction
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.fragment_container, fragment); // replace the transaction with current transaction
        transaction.addToBackStack(null); // add the transaction to the back stack so the user can navigate back

        transaction.commit();
    }

    private void toDoMeasurementFragment() {
        DoMeasurementFragment fragment = new DoMeasurementFragment(); // create new fragment

        // set-up arguments
        Bundle args = new Bundle();
        args.putString(DISPLAY_MESSAGE, "Tap tag!");
        fragment.setArguments(args);

        // begin fragment trasaction
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.fragment_container, fragment); // replace the transaction with current transaction
        transaction.addToBackStack(null); // add the transaction to the back stack so the user can navigate back

        transaction.commit();
    }
}


