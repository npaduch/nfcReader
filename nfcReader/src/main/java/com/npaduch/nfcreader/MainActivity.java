package com.npaduch.nfcreader;

import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends ActionBarActivity {

    private final static String TAG = "NFCReader";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new MainFragment())
                    .commit();
        }
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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check to see if we were called because a ta was read!
        Intent intent = getIntent();
        NdefMessage[] msgs = null;
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Log.d(TAG,"A tag was found!");
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
        }

        // if messages were read, handle them here
        if(msgs != null) {
            // fill up TextView
            TextView output = (TextView)findViewById(R.id.nfcTagDataTextView);
            output.setText(""); // clear text
            for (int i = 0; i < msgs.length; i++) {
                for(int j=0; j <msgs[i].getRecords().length; j++)
                    // append each entry to the text view
                    output.setText(output.getText()+"\n"+parseRecord(msgs[i].getRecords()[j]));

            }
        }
    }

    String parseRecord(NdefRecord rcd) {
        try {
            byte[] payload = rcd.getPayload();

             /*
             *  First byte of payload is status byte encoding
             *  bit7 = encoding, 0 = UTF-8, 1 = UTF-16
             *  bit6 = reserved
             *  bit 5:0 = length of lanuage code
             *
             */

            // Get encoding (bit7)
            String encoding;
            if((payload[0] & 0x80) == 0)
                encoding = "UTF-8";
            else
                encoding = "UTF-16";
            Log.d(TAG,"Encoding: "+encoding);

            // language code length [5:0]
            int langCodeLen = payload[0] & 0x3F;
            // get a substring of the specified length
            String langCode = new String(payload, 1, langCodeLen, "US-ASCII");
            Log.d(TAG,"Language code: "+langCode);

            // The rest of the message is the raw text
            String text = new String(payload, langCodeLen + 1, payload.length - langCodeLen - 1, encoding);
            Log.d(TAG,"Text: "+text);

            outputRecordToFile(payload);

            return text;
        } catch (Exception e) {
            throw new RuntimeException("Record Parsing Failure!!");
        }
    }

    private void outputRecordToFile(byte[] payload){

        Log.d(TAG,"Writing to file.");

        String FILENAME = "nfc_read_data";

        try {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state) ||
                    Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                Log.d(TAG,"Storage found");
            }
            else{
                Log.d(TAG,"Not found");
            }
            //FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_WORLD_READABLE);
            // Get the directory for the app's private pictures directory.
            //File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File path = Environment.getExternalStorageDirectory();
            File file = new File(path, FILENAME);
            FileOutputStream fos  = new FileOutputStream(file);
            if (!file.mkdirs()) {
                Log.e(TAG, "Directory not created");
            }
            Log.d(TAG,"File opened: "+fos.toString());
            fos.write(payload);
            Log.d(TAG, "PAyload written");
            fos.close();
            Log.d(TAG, "File closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
