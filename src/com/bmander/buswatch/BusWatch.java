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
import android.view.inputmethod.EditorInfo;
import android.os.Handler;
import android.content.Context;

public class BusWatch extends Activity
{    
    TextView contentTextView;
    EditText entryEditText;
    ProgressBar progressBar;
    LinearLayout routesLinear;
    ArrayList<CheckBox> routeSelectors = new ArrayList<CheckBox>();
    Spinner durationSpinner;
    ImageButton startButton;
    
    OneBusAway oneBusAway;
    
    String TAG = "BusWatch";
    
    int SECS_IN_MINUTE = 60;
    int MILLISECS_IN_SECS = 1000;
    
    int TEXTPERIOD = 4000;
    
    private Handler mHandler = new Handler();

    Context busWatchContext = this;
    
    String stopId = "";
    
    SendTimesToWatchThread currentWatchRunner = null;
    boolean startButtonToggled = true;
    
    class SendTimesToWatchThread extends Thread {
        String stopid;
        int period; // milliseconds
        int duration; // milliseconds
        boolean running;
         
        SendTimesToWatchThread(String stopid, int period, int duration) {
            this.stopid = stopid;
            this.period = period;
            this.duration = duration;
            this.running = false;
        }
        
        public boolean isRunning() {
            return this.running;
        }
        
        public void politeStop() {
            this.running = false;
        }
        
        public void run() {
            // set the state of the thread to running
            this.running = true;
            
            // get and display bustimes on watch
            try {
                // figure out the time at start
                long timeAtStart = System.currentTimeMillis();
                
                // repeat for duration
                while(this.running && System.currentTimeMillis() < timeAtStart+duration) {
                    Log.d( TAG, "current time:"+System.currentTimeMillis()+" end time:"+(timeAtStart+duration) );
                
                    // get arrivaldeparture predictions
                    ArrayList<OneBusAway.ArrivalPrediction> bustimes = oneBusAway.get_bustimes( stopid );
                    
                    // show each prediction on the watch, at a regular interval
                    for(int i=0; i<bustimes.size(); i++) {
                        // if the stop signal has been thrown, exit the loop
                        if(!this.running) {
                            break;
                        }
                        
                        OneBusAway.ArrivalPrediction prediction = bustimes.get(i);
                        
                        // text the watch
                        textWatch( prediction.getShortName()+" "+prediction.getHeadsign(), prediction.getETAString(System.currentTimeMillis()) );
                        
                        // wait a bit to print the next one
                        this.sleep(period);
                    }
                }
            } catch( Exception e ) {
                final Exception fe = e;
                mHandler.post(new Runnable() {
                    public void run() {
                        print( fe.getMessage() );
                    }
                });
            }
            
            // set the state of he thread to not running
            this.running = false;
            
            // toggle the start/stop UI element to show that it's no longer running
            mHandler.post(new Runnable() {
                public void run() {
                    setButtonStart();
                }
            });
        }
    }
    
    class GetRoutesThread extends Thread {
        String routeId;
        
        GetRoutesThread(String routeId) {
            this.routeId = routeId;
        }
        
        public void printInMainThread(final String str) {
            mHandler.post(new Runnable() {
                public void run() {
                    print( str );
                }
            });
        }
        
        public void run() {
            // attempt to get 
            Log.i( TAG, "get routes for "+routeId ); 
            
            // show the indeterminate progress spinner
            mHandler.post(new Runnable() {
                public void run() {
                    routesLinear.removeAllViews();
                    progressBar.setVisibility(View.VISIBLE);
                }
            });
            
            // fetch the routes and fill out the checkboxes for them
            try{
                final ArrayList<OneBusAway.Route> routes = oneBusAway.getRoutes( routeId );
                
                mHandler.post(new Runnable() {
                    public void run() {
                        
                        for(int i=0; i<routes.size(); i++) {
                            OneBusAway.Route route = routes.get(i);
                            
                            // put a checkbox on the right, pre-filled out
                            CheckBox checkBox = new CheckBox(busWatchContext);
                            checkBox.setChecked(true);
                            checkBox.setText( route.getShortName()+" "+route.getDescription() );
                            routesLinear.addView( checkBox );
                            routeSelectors.add( checkBox );
                    
                        }
                    }
                });

            } catch( Exception e ) {
                printInMainThread( "routes fetch failed: "+e.getMessage() );
            }
            
            // hide the indeterminate progress spinner
            mHandler.post(new Runnable() {
                public void run() {
                    progressBar.setVisibility(View.INVISIBLE);
                }
            });
        }
    }
    
    class StartButtonClickListener implements View.OnClickListener {
        public void onClick(View v) {
            // start
            if( startButtonToggled ) {
                // get stop id and duration from form input
                String stopid = entryEditText.getText().toString();
                
                DurationSpinnerItem dsi = (DurationSpinnerItem)durationSpinner.getSelectedItem();
                int duration = dsi.duration*SECS_IN_MINUTE*MILLISECS_IN_SECS;
                
                Log.d( TAG, "run for duration:"+duration );
                
                // stop the current watch runner if it's going
                if( currentWatchRunner != null ) {
                    currentWatchRunner.politeStop();
                }
                
                // start a new watch runner
                Log.i( TAG, "launching thread for stop_id:"+stopid+" textperiod:"+TEXTPERIOD+"duration:"+duration );
                currentWatchRunner = new SendTimesToWatchThread( stopid, TEXTPERIOD, duration );
                currentWatchRunner.start();
                
                setButtonStop();
            // stop
            } else {
                if( currentWatchRunner != null ) {
                    currentWatchRunner.politeStop();
                }
                
                // the watch transmission runner will turn the toggle button off itself
            }
        }
    }
    
    public void setButtonStart() {
        startButton.setImageResource( R.drawable.startsmall );
        startButtonToggled = true;
    }
    
    public void setButtonStop() {
        startButton.setImageResource( R.drawable.stopsmall );
        startButtonToggled = false;
    }
    
    private void getRoutes() {
        // get the route id
        String newStopId = entryEditText.getText().toString();
    
        // if this is a different stop id than we already have all locked in
        if( !stopId.equals(newStopId) ) {
            stopId = newStopId;
            (new GetRoutesThread(stopId)).start();
        }
    }
    
    class StopIdFocusChangeListener implements View.OnFocusChangeListener {
        public void onFocusChange (View v, boolean hasFocus) {
            // if they've progressed to the next dialog box
            if( !hasFocus ) {
                getRoutes();
            }
        }
    }
    
    class DurationSpinnerItem {
        int duration;
        
        DurationSpinnerItem(int duration) {
            this.duration = duration;
        }
        
        public String toString() {
            return this.duration+" minutes";
        }
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // call super, use XML layout 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        contentTextView = (TextView) findViewById(R.id.content);
        entryEditText = (EditText) findViewById(R.id.entry);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        routesLinear = (LinearLayout) findViewById(R.id.routes);
        startButton = (ImageButton) findViewById(R.id.startstopbutton);
        
        // create an object to represent the OneBusAway API
        String apikey = this.getString(R.string.apikey);
        String oba_api_domain = this.getString(R.string.onebusaway_api_domain);
        oneBusAway = new OneBusAway(oba_api_domain, apikey);
        
        // add a click listener on the startstop toggle
        startButton.setOnClickListener( new StartButtonClickListener() );
        
        // add a listener for the enter event on the text entry box
        entryEditText.setOnFocusChangeListener( new StopIdFocusChangeListener() );
        
        // set progress spinner to indeterminate
        progressBar.setIndeterminate(true);
        
        // create and fill out spinner duration
        durationSpinner = (Spinner) findViewById(R.id.durationspinner);
        ArrayAdapter<DurationSpinnerItem> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item);
        int[] durations = this.getResources().getIntArray( R.array.durations );
        for(int i=0; i<durations.length; i++) {
            adapter.add( new DurationSpinnerItem(durations[i]) );
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durationSpinner.setAdapter(adapter);
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
