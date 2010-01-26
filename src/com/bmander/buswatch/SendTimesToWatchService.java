package com.bmander.buswatch;

import android.app.Service;
import android.os.IBinder;
import android.content.Intent;
import android.util.Log;
import android.os.Binder;
import java.util.*;
import android.os.*;
import android.content.Context;

public class SendTimesToWatchService extends Service {
    String TAG = "SendTimesToWatchService";
    OneBusAway oneBusAway;
    
    SendTimesToWatchService parentThis = this;
    
    ArrayList<OneBusAway.ArrivalPrediction> bustimes = null;
    
    SendTimesToWatchCountDown sendTimesToWatchCountDown = null;
    GetTimesFromApiCountDown getTimesFromApiCountDown = null;
    boolean sendTimesToWatchCountDownStarted = false;
    
    /*
     * A thread for fetching times from the OBA API - called repeatedly from a CountDownTimer
     */
    class GetTimesFromApiThread extends Thread {
        String stopid;
         
        GetTimesFromApiThread(String stopid) {
            this.stopid = stopid;
        }
        
        public void run() {
            try{
                bustimes = oneBusAway.get_bustimes( stopid );
                Log.i( TAG, "api getter tick - got times" );
                
                // if the watch sender hasn't been started, kick it off
                if( !sendTimesToWatchCountDownStarted ) {
                    sendTimesToWatchCountDownStarted=true;
                    sendTimesToWatchCountDown.start();
                }
                                
            } catch( Exception ex ) {
                Log.e( TAG, ex.getMessage() );
            }
        }
    }
    
    /*
     * CountDownTimer that repeatedly gets OBA times
     */
    class GetTimesFromApiCountDown extends CountDownTimer {
        GetTimesFromApiThread worker;
        String stopid;
        
        GetTimesFromApiCountDown(long duration, long period, String stopid) {
            super( duration, period );
            this.stopid = stopid;
        }
        
        public void onTick(long millisUntilFinished) {
            Log.i( TAG, "api getter tick - getting times with "+millisUntilFinished+" left");
            (new GetTimesFromApiThread( stopid )).start();
        }

        public void onFinish() {
            Log.i( TAG, "finished fetching OBA times" );
        }
    }
    
    /*
     * CountDownTimer that repeatedly sends times to the watch. When it finishes, it stops the service.
     */
    class SendTimesToWatchCountDown extends CountDownTimer {
        
        SendTimesToWatchCountDown(long duration, long period) {
            super( duration, period );
        }
        
        public void onTick(long millisUntilFinished) {
            Log.i( TAG, "watch sender tick - "+millisUntilFinished+" left");
            if( bustimes != null ) {
                // show each prediction on the watch, at a regular interval
                //for(int i=0; i<bustimes.size(); i++) {
                    OneBusAway.ArrivalPrediction prediction = bustimes.get(0);
                    
                    // text the watch
                    textWatch( prediction.getShortName()+" "+prediction.getHeadsign(), prediction.getETAString(System.currentTimeMillis()) );
                //}
            }
        }

        public void onFinish() {
            Log.i( TAG, "finished sending times to the watch" );
            parentThis.stopSelf();
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
        int watchPeriod = intent.getExtras().getInt( "watchPeriod" );
        int apiPeriod = intent.getExtras().getInt( "apiPeriod" );
        int duration = intent.getExtras().getInt( "duration" );
        
        // log service parameters
        Log.i( TAG, TAG+" started" );
        Log.i( TAG, "stopid: "+stopid );
        Log.i( TAG, "apiKey: "+apiKey );
        Log.i( TAG, "obaApiDomain: "+obaApiDomain );
        Log.i( TAG, "watchPeriod: "+watchPeriod );
        Log.i( TAG, "apiPeriod: "+apiPeriod );
        Log.i( TAG, "duration: "+duration );
        
        // create the OneBusAway API
        oneBusAway = new OneBusAway(obaApiDomain, apiKey);
        
        // start the countdown timer that sends times to the watch
        sendTimesToWatchCountDown = new SendTimesToWatchCountDown( duration, watchPeriod );
        //sendTimesToWatchCountDown.start();
        
        // start the countdown timer that gets times from the OBA API
        getTimesFromApiCountDown = new GetTimesFromApiCountDown( duration, apiPeriod, stopid );
        getTimesFromApiCountDown.start();
        
        // inform the calling Activity that this service should be killed just as soon as possible
        return START_NOT_STICKY;
    }
    
    public void onDestroy() {

        if( sendTimesToWatchCountDown != null ) {
            sendTimesToWatchCountDown.cancel();
        }
        if( getTimesFromApiCountDown != null ) {
            getTimesFromApiCountDown.cancel();
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