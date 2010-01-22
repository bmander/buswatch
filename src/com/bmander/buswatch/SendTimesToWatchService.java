package com.bmander.buswatch;

import android.app.Service;
import android.os.IBinder;
import android.content.Intent;
import android.util.Log;

public class SendTimesToWatchService extends Service {
    String TAG = "SendTimesToWatchService";
    
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    public int onStartCommand( Intent intent, int flags, int startId ) {
        Log.i( TAG, "You started a service" );
        return START_STICKY;
    }
    
    public void onDestroy() {
        Log.i( TAG, "You destroyed the service" );
    }
}