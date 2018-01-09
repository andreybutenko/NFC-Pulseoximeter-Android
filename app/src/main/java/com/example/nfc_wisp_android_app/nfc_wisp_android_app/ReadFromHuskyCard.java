package com.example.nfc_wisp_android_app.nfc_wisp_android_app;


import android.content.res.Resources;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.util.Log;
import android.util.StringBuilderPrinter;

import com.github.mikephil.charting.data.Entry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Scanner;


public class ReadFromHuskyCard {
    private static final String TAG = IsoDep.class.getSimpleName();

    public String readTag(Tag tag) {
        IsoDep isodep = IsoDep.get(tag);
        try {
            isodep.connect();
            // byte[] payload = isodep.getHiLayerResponse(); // Return the higher layer response bytes for NfcB tags.
            //byte[] payload = isodep.getHistoricalBytes(); // Return the ISO-DEP historical bytes for NfcA tags.
            //byte[] message = {0x0};
            //byte[] payload = isodep.transceive(message);
            byte[] payload = tag.getId();
            String huskyID = mToString(payload);
            return huskyID;
            //return new String(payload, Charset.forName("US-ASCII"));
        } catch (IOException e) {
            Log.e(TAG, "IOException while writing MifareUltralight message...", e);
        } finally {
            if (isodep != null) {
                try {
                    isodep.close();
                }
                catch (IOException e) {
                    Log.e(TAG, "Error closing tag...", e);
                }
            }
        }
        return null;
    }

    private String mToString(byte[] payload) {
        StringBuilder sb = new StringBuilder();
        for(byte b : payload) {
            sb.append(b);
        }
        return sb.toString();
    }
}
