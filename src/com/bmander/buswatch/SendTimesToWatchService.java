package com.bmander.buswatch;

import android.app.Service;
import android.os.IBinder;
import android.content.Intent;
import android.util.Log;
import android.os.Binder;
import java.util.*;

public class SendTimesToWatchService extends Service {
    String TAG = "SendTimesToWatchService";
    OneBusAway oneBusAway;
    
    SendTimesToWatchService parentThis = this;
    
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
                Log.e( TAG, e.getMessage() );
            }
            
            // set the state of he thread to not running
            this.running = false;
            
            // if stopped, kill the parent service
            parentThis.stopSelf();
        }
    }
    
    SendTimesToWatchThread threadSoul = null;
    
    // return an empty binder, such that the bound application can see when the service starts and stops
    public IBinder onBind(Intent intent) {
        return new Binder();
    }
    
    public int onStartCommand( Intent intent, int flags, int startId ) {
        Log.i( TAG, "You started a service" );
        
        String apiKey = intent.getExtras().getString( "apiKey" );
        String obaApiDomain = intent.getExtras().getString( "obaApiDomain" );
        String stopid = intent.getExtras().getString( "stopId" );
        int period = intent.getExtras().getInt( "period" );
        int duration = intent.getExtras().getInt( "duration" );
        
        Log.i( TAG, "stopid: "+stopid );
        Log.i( TAG, "apiKey: "+apiKey );
        Log.i( TAG, "obaApiDomain: "+obaApiDomain );
        Log.i( TAG, "period: "+period );
        Log.i( TAG, "duration: "+duration );
        
        oneBusAway = new OneBusAway(obaApiDomain, apiKey);
        
        threadSoul = new SendTimesToWatchThread(stopid, period, duration);
        threadSoul.start();
        
        return START_STICKY;
    }
    
    public void onDestroy() {
        if( threadSoul != null ) {
            threadSoul.politeStop();
        }
        Log.i( TAG, "You destroyed the service" );
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
}