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
import android.app.Service;
import android.os.IBinder;
import android.content.ServiceConnection;
import android.content.ComponentName;


public class BusWatch extends Activity
{    
    EditText entryEditText;
    ProgressBar progressBar;
    RadioGroup routesLinear;
    ArrayList<RadioButton> routeSelectors = new ArrayList<RadioButton>();
    Spinner durationSpinner;
    ImageButton startButton;
    
    OneBusAway oneBusAway;
    
    String TAG = "BusWatch";
    
    int SECS_IN_MINUTE = 60;
    int MILLISECS_IN_SECS = 1000;
    
    int TEXTPERIOD = 4000;
    int APIPERIOD = 10000;
    
    private Handler mHandler = new Handler();

    Context busWatchContext = this;
    
    String stopId = "";
    
    boolean startButtonToggled = true;
    
    // onebusaway global variables
    String obaApiDomain;
    String apiKey;
    
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
                            RadioButton radioBox = new RadioButton(busWatchContext);
                            radioBox.setText( route.getShortName()+" "+route.getDescription() );
                            routesLinear.addView( radioBox );
                            routeSelectors.add( radioBox );
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
    
    public class SendTimesToWatchServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
            // when the service starts, set the toggle to show a 'stop' symbol
            setButtonStop();
        }
        public void onServiceDisconnected(ComponentName name) {
            // when the service stops, set the toggle to show a 'start' symbol
            setButtonStart();
        }
    }
    
    class StartButtonClickListener implements View.OnClickListener {
        public void onClick(View v) {
            // start
            if( startButtonToggled ) {
                // get stop id from form input
                String stopid = entryEditText.getText().toString();
                
                // get duration from form input
                DurationSpinnerItem dsi = (DurationSpinnerItem)durationSpinner.getSelectedItem();
                int duration = dsi.duration*SECS_IN_MINUTE*MILLISECS_IN_SECS;
                
                // send a log message
                Log.d( TAG, "run for duration:"+duration );
                
                // create an intent for a new watch-transmitter service
                Intent startWatchTimesIntent = new Intent( busWatchContext, SendTimesToWatchService.class );
                startWatchTimesIntent.putExtra( "stopId", stopid );
                startWatchTimesIntent.putExtra( "obaApiDomain", obaApiDomain );
                startWatchTimesIntent.putExtra( "apiKey", apiKey );
                startWatchTimesIntent.putExtra( "watchPeriod", TEXTPERIOD );
                startWatchTimesIntent.putExtra( "apiPeriod", APIPERIOD );
                startWatchTimesIntent.putExtra( "duration", duration );
                
                // bind to the service so we can pop the toggle when it dies
                bindService( startWatchTimesIntent, 
                             new SendTimesToWatchServiceConnection(), 
                             0 );
                             
                // start the service
                startService( startWatchTimesIntent );
                
            // stop
            } else {                
                stopService( new Intent( busWatchContext, SendTimesToWatchService.class ) );
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
    
    class StopIdActionListener implements TextView.OnEditorActionListener {
        public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
            if( actionId == EditorInfo.IME_ACTION_DONE ) {
                getRoutes();
            }
            return false;
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
        
        entryEditText = (EditText) findViewById(R.id.entry);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        routesLinear = (RadioGroup) findViewById(R.id.routes);
        startButton = (ImageButton) findViewById(R.id.startstopbutton);
        
        // create an object to represent the OneBusAway API
        apiKey = this.getString(R.string.apikey);
        obaApiDomain = this.getString(R.string.onebusaway_api_domain);
        oneBusAway = new OneBusAway(obaApiDomain, apiKey);
        
        // add a click listener on the startstop toggle
        startButton.setOnClickListener( new StartButtonClickListener() );
        
        // add a listener for the enter event on the text entry box
        entryEditText.setOnFocusChangeListener( new StopIdFocusChangeListener() );
        entryEditText.setOnEditorActionListener( new StopIdActionListener() );
        
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
