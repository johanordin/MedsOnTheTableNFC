package com.example.johanordin.medsonthetablenfc;

import android.app.Activity;
import android.app.PendingIntent;
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
import android.widget.TextView;

import java.util.Arrays;

/**
 * Class for read a NFC tag
 * Created by johanordin on 12/04/15.
 */
public class TagDispatch extends Activity {

    private static final String TAG = "TagDispatch";


    private TextView mTextView;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mIntentFilters;
    private String[][] mNFCTechLists;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tagdispatch);
        mTextView = (TextView)findViewById(R.id.tv);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter != null) {
            mTextView.setText("Read an NFC tag");
        } else {
            mTextView.setText("This phone is not NFC enabled.");
        }

        // create an intent with tag data and deliver to this activity
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // set an intent filter for all MIME data
        IntentFilter ndefIntent = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefIntent.addDataType("*/*");
            mIntentFilters = new IntentFilter[] { ndefIntent };
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        mNFCTechLists = new String[][] { new String[] { NfcF.class.getName() } };
    }

    @Override
    public void onNewIntent(Intent intent) {
        String action = intent.getAction();
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        String s = action + "\n\n" + tag.toString();
        Log.d(TAG, "onPause this an Log::-->");
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
                        }
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }

        }

        mTextView.setText(s);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mIntentFilters, mNFCTechLists);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause this an Log::-->");
        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }


}
