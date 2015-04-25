package com.example.johanordin.medsonthetablenfc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Class for read a NFC tag
 * Created by johanordin on 12/04/15.
 */
public class TagDispatch extends Activity {

    private static final String TAG = "TagDispatch";

    public static final String BASE_URL = "http://site-medsonthetable.openshift.ida.liu.se/";

    //private TextView mTextView;
    private WebView browser;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mIntentFilters;
    private String[][] mNFCTechLists;

    List<String> medsList = new ArrayList<String>();
    String messageOnTag;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate is called :: -->");

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //set content view AFTER ABOVE sequence (to avoid crash)
        this.setContentView(R.layout.tagdispatch);

        //setContentView(R.layout.tagdispatch);
        // TODO: rename tv in XML-file -->
        browser = (WebView) findViewById(R.id.tv);
        browser.setWebViewClient(new WebBrowser());
        browser.getSettings().setLoadsImagesAutomatically(true);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        // dont show the browser initially
        browser.setVisibility(browser.GONE);

        // Create a NFC
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        /*
        if (mNfcAdapter != null) {
            mTextView.setText("Lägg ett läkemedel mot baksidan av surfplattan.");
        } else {
            mTextView.setText("This phone is not NFC enabled.");
        }
        */

        // create an intent with tag data and deliver to this activity
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // set an intent filter for all MIME data
        IntentFilter ndefIntent = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefIntent.addDataType("*/*");
            mIntentFilters = new IntentFilter[]{ndefIntent};
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        mNFCTechLists = new String[][]{new String[]{NfcF.class.getName()}};
    }

    @Override
    public void onNewIntent(Intent intent) {
        //browser.setVisibility(browser.GONE);
        Log.d(TAG, "onNewIntent is called :: -->");

        String action = intent.getAction();
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        String s = action + "\n\n" + tag.toString();
        System.out.println("onNewIntent: --> " + s);

        // parse through all NDEF messages and their records and pick text type only
        Parcelable[] data = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        String utf_8 = "UTF-8";
        String utf_16 = "UTF-16";

        if (data != null) {
            try {
                for (int i = 0; i < data.length; i++) {
                    NdefRecord[] recs = ((NdefMessage) data[i]).getRecords();
                    for (int j = 0; j < recs.length; j++)
                        if (recs[j].getTnf() == NdefRecord.TNF_WELL_KNOWN &&
                                Arrays.equals(recs[j].getType(), NdefRecord.RTD_TEXT)) {

                            byte[] payload = recs[j].getPayload();
                            String textEncoding = ((payload[0] & 0200) == 0) ? utf_8 : utf_16;
                            int langCodeLen = payload[0] & 0077;

                            s += ("\n\nNdefMessage[" + i + "], NdefRecord[" + j + "]:\n\"" +
                                    new String(payload, langCodeLen + 1,
                                            payload.length - langCodeLen - 1, textEncoding) +
                                    "\"");

                            messageOnTag = new String(payload, langCodeLen + 1, payload.length - langCodeLen - 1, textEncoding);
                            System.out.println("Loggar Meddelande: " + messageOnTag);

                            Log.d(TAG, "String: " + s);
                        }
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }

        }

        //mTextView.setText(s);

        //Visa dialog
        open();

        // TODO: investigate if we should check if it should be possible to compare identical meds
        /*
        if (medsList.contains(messageOnTag)) {
            // Du har redan scannat in Medicinen. VIll du lagga till den iaf??
            // dvs dialog ruta ska visas
        } else {
            // lagg till skicka vidare till webbView
            medsList.add(messageOnTag);
            //Visa dialog
            open();
        }
        */

        // Loop through the list with meds
        Iterator<String> itr = medsList.iterator();
        while (itr.hasNext()) {
            String element = itr.next();
            System.out.printf(element + " " + "%n");
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume is called :: -->");
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mIntentFilters, mNFCTechLists);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause is called :: -->");
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    public void open() {
        Log.d(TAG, "open() is called :: -->");
        browser.setVisibility(browser.GONE);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(R.string.decision);

        // Add medicine
        alertDialogBuilder.setPositiveButton(R.string.add_medicine, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                /*Intent toWebViewActivity = new Intent(TagDispatch.this, WebviewActivity.class);
                startActivity(toWebViewActivity);
                */
                medsList.add(messageOnTag);
                browser.loadUrl(BASE_URL + "med/" + messageOnTag);
                browser.setVisibility(browser.VISIBLE);
            }
        });
        // Rescan
        alertDialogBuilder.setNegativeButton(R.string.rescan_medicine, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Intent toWebViewActivity = new Intent(getApplicationContext(), TagDispatch.class);
                //startActivity(MainActivity);
                //finish();

                // Relunch the current activity for collection a new medicine.
                Intent intent = getIntent();
                finish();
                startActivity(intent);

                // Show a helping text
                Context context = getApplicationContext();
                CharSequence text = getString(R.string.toastmsg_rescan_medicine);
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    private class WebBrowser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

}
