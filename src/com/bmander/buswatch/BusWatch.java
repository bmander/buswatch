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
    EditText durationEditText;
    ProgressBar progressBar;
    
    OneBusAway oneBusAway;
    
    String TAG = "BusWatch";
    
    int SECS_IN_MINUTE = 60;
    int MILLISECS_IN_SECS = 1000;
    
    int TEXTPERIOD = 4000;
    
    
    class SendTimesToWatchRunner extends Thread {
        String stopid;
        int period; // milliseconds
        int duration; // milliseconds
         
        SendTimesToWatchRunner(String stopid, int period, int duration) {
            this.stopid = stopid;
            this.period = period;
            this.duration = duration;
        }
        
        public void run() {
            try {
                // figure out the time at start
                long timeAtStart = System.currentTimeMillis();
                
                // repeat for duration
                while(System.currentTimeMillis() < timeAtStart+duration) {
                    Log.d( TAG, "current time:"+System.currentTimeMillis()+" end time:"+(timeAtStart+duration) );
                
                    // get arrivaldeparture predictions
                    ArrayList<OneBusAway.ArrivalPrediction> bustimes = oneBusAway.get_bustimes( stopid );
                    
                    // show each prediction on the watch, at a regular interval
                    for(int i=0; i<bustimes.size(); i++) {
                        OneBusAway.ArrivalPrediction prediction = bustimes.get(i);
                        
                        // text the watch
                        textWatch( prediction.getShortName()+" "+prediction.getHeadsign(), prediction.getETAString() );
                        
                        // wait a bit to print the next one
                        this.sleep(period);
                    }
                }
            } catch( Exception e ) {
                print( e.getMessage() );
            }
        }
    }  
    
    class OkButtonClickListener implements View.OnClickListener {
        public void onClick(View v) {
            // get stop id and duration from form input
            String stopid = entryEditText.getText().toString();
            int duration = Integer.parseInt( durationEditText.getText().toString() )*MILLISECS_IN_SECS;
            
            Log.d( TAG, "run for duration:"+duration );
            
            // start concurrent thread sending predictions to watch at regular intervals
            Log.i( TAG, "launching thread for stop_id:"+stopid+" textperiod:"+TEXTPERIOD+"duration:"+duration );
            SendTimesToWatchRunner worker = new SendTimesToWatchRunner( stopid, TEXTPERIOD, duration );
            worker.start();
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
        Log.i( TAG, "id: "+R.id.ok );
        Log.i( TAG, "object: "+ findViewById(R.id.ok) );
        
        okButton = (Button) findViewById(R.id.ok);
        contentTextView = (TextView) findViewById(R.id.content);
        entryEditText = (EditText) findViewById(R.id.entry);
        durationEditText = (EditText) findViewById(R.id.durationentry);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        
        // create an object to represent the OneBusAway API
        String apikey = this.getString(R.string.apikey);
        String oba_api_domain = this.getString(R.string.onebusaway_api_domain);
        oneBusAway = new OneBusAway(oba_api_domain, apikey);
        
        // add a click listener to the button
        okButton.setOnClickListener( new OkButtonClickListener() );
        
        // set progress spinner to indeterminate and make sure it's off
        progressBar.setIndeterminate(true);
    
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
