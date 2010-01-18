package com.bmander.buswatch;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import java.lang.*;
import android.view.*;
import org.json.*;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import java.util.*;

public class BusWatch extends Activity
{    
    Button okButton;
    TextView contentTextView;
    EditText entryEditText;
    
    OneBusAway oneBusAway;
    
    String TAG = "BusWatch";
    
    class SendTimesToWatchRunner extends Thread {
        ArrayList<OneBusAway.ArrivalPrediction> bustimes;
        int interval;
        
        SendTimesToWatchRunner(ArrayList<OneBusAway.ArrivalPrediction> bustimes, int interval) {
            this.bustimes = bustimes;
            this.interval = interval;
        }
        
        public void run() {
            // show each prediction on the watch, at a regular interval
            for(int i=0; i<bustimes.size(); i++) {
                OneBusAway.ArrivalPrediction prediction = bustimes.get(i);
                
                // text the watch
                textWatch( prediction.getShortName()+" "+prediction.getHeadsign(), prediction.getETAString() );
                
                // wait a bit to print the next one
                try {
                    this.sleep(interval);
                } catch(InterruptedException e) {
                    // interrupted while waiting? do nothing.
                }
            }
        }
    }
    
    class OkButtonClickListener implements View.OnClickListener {
        public void onClick(View v) {
            try {
                // get stop id from form input
                String stopid = entryEditText.getText().toString();
                
                // get arrivaldeparture predictions
                Log.i( TAG, "getting predictions for stop_id "+stopid );
                ArrayList<OneBusAway.ArrivalPrediction> bustimes = oneBusAway.get_bustimes( stopid );
                
                // start concurrent thread sending predictions to watch at regular intervals
                SendTimesToWatchRunner worker = new SendTimesToWatchRunner( bustimes, 4000 );
                worker.start();
                
            } catch( Exception e ) {
                print( e.getMessage() );
            }
        }
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // call super, use XML layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // fetch elements defined in XML layout so we can manipulate them
        okButton = (Button) findViewById(R.id.ok);
        contentTextView = (TextView) findViewById(R.id.content);
        entryEditText = (EditText) findViewById(R.id.entry);
        
        // create an object to represent the OneBusAway API
        String apikey = this.getString(R.string.apikey);
        String oba_api_domain = this.getString(R.string.onebusaway_api_domain);
        oneBusAway = new OneBusAway(oba_api_domain, apikey);
        
        // add a click listener to the button
        okButton.setOnClickListener( new OkButtonClickListener() );
    
    }
    
    /*
     * Convenience method for activating bluetooth watch
     */
    private void textWatch(String line1, String line2) {
        Intent phoneIntent = new Intent("com.smartmadsoft.openwatch.action.TEXT");
        phoneIntent.putExtra( "line1", line1 );
        phoneIntent.putExtra( "line2", line2 );
        this.sendBroadcast( phoneIntent );
    }
    
    /*
     * Convenience method for popping up a toast message
     */
    private void print(String str) {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }
    
}
