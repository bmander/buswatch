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
    
    ArrayList<OneBusAway.ArrivalPrediction> bustimes = null;
    
    SendTimesToWatchThread threadSoul = null;
    ServiceKillerTimer serviceKillerTimer = null;
    GetTimesFromApiThread getTimesFromApiThread = null;
    
    /*
     * The ServiceKillerTimer comes alive at the start of the service, waits for a while, and then kills the service off
     */
    class ServiceKillerTimer extends Thread {
        int duration;
        
        ServiceKillerTimer(int duration) {
            this.duration = duration;
        }
        
        public void run() {
            try {
                this.sleep(duration);
            } catch( InterruptedException ex ) {
                Log.i( TAG, ex.getMessage() );
            }
            
            parentThis.stopSelf();
            
            Log.i( TAG, "killer timer shutting everything down now" );
        }
    }
    
    /*
     * A thread for fetching times from the OBA API
     */
    class GetTimesFromApiThread extends Thread {
        String stopid;
        int period; // milliseconds
        boolean running;
         
        GetTimesFromApiThread(String stopid, int period) {
            this.stopid = stopid;
            this.period = period;
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
                
                // repeat until something kills this thread - like the killer timer
                while(this.running) {
                
                    // get arrivaldeparture predictions
                    bustimes = oneBusAway.get_bustimes( stopid );
                    
                    this.sleep( period );
                }
            } catch( Exception e ) {
                Log.e( TAG, e.getMessage() );
            }
            
            // set the state of he thread to not running
            this.running = false;
            
        }
    }
    
    /*
     * A thread for sending times to the watch
     */
    class SendTimesToWatchThread extends Thread {
        String stopid;
        int period; // milliseconds
        boolean running;
         
        SendTimesToWatchThread(String stopid, int period) {
            this.stopid = stopid;
            this.period = period;
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
                
                // repeat until something kills this thread - like the killer timer
                while(this.running) {
                    if( bustimes != null ) {
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
                    this.sleep(500);
                }
            } catch( Exception e ) {
                Log.e( TAG, e.getMessage() );
            }
            
            // set the state of he thread to not running
            this.running = false;
            
        }
    }
    
    // return an empty binder, so that the bound activity can see when the service starts and stops
    public IBinder onBind(Intent intent) {
        return new Binder();
    }
    
    public int onStartCommand( Intent intent, int flags, int startId ) {
        // get service parameters from the intent
        String apiKey = intent.getExtras().getString( "apiKey" );
        String obaApiDomain = intent.getExtras().getString( "obaApiDomain" );
        String stopid = intent.getExtras().getString( "stopId" );
        int period = intent.getExtras().getInt( "period" );
        int duration = intent.getExtras().getInt( "duration" );
        
        // log service parameters
        Log.i( TAG, TAG+" started" );
        Log.i( TAG, "stopid: "+stopid );
        Log.i( TAG, "apiKey: "+apiKey );
        Log.i( TAG, "obaApiDomain: "+obaApiDomain );
        Log.i( TAG, "period: "+period );
        Log.i( TAG, "duration: "+duration );
        
        // create the OneBusAway API
        oneBusAway = new OneBusAway(obaApiDomain, apiKey);
        
        // start the thread that does all the work
        threadSoul = new SendTimesToWatchThread(stopid, period);
        threadSoul.start();
        
        // start the times fetcher thread
        getTimesFromApiThread = new GetTimesFromApiThread( stopid, period );
        getTimesFromApiThread.start();
        
        // start the killer timer
        serviceKillerTimer = new ServiceKillerTimer( duration );
        serviceKillerTimer.start();
        
        // inform the calling Activity that this service should be killed just as soon as possible
        return START_STICKY;
    }
    
    public void onDestroy() {
        if( threadSoul != null ) {
            threadSoul.politeStop();
        }
        if( getTimesFromApiThread != null ) {
            getTimesFromApiThread.politeStop();
        }
        Log.i( TAG, TAG+" stopped" );
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