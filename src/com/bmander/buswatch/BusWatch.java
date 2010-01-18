package com.bmander.buswatch;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.net.*;
import java.net.*;
import java.io.*;
import java.lang.*;
import android.view.*;
import org.json.*;
import android.content.res.Resources;
import android.content.Intent;
import android.os.SystemClock;

public class BusWatch extends Activity
{
    String API_DOMAIN = "api.onebusaway.org";
    String ARRIVALS_DEPARTURES_PATH = "/api/where/arrivals-and-departures-for-stop/";
    
    Button okButton;
    TextView contentTextView;
    EditText entryEditText;
    
    String apikey;
    
    class OkButtonClickListener implements View.OnClickListener {
        public void onClick(View v) {
            try {
                // get stop id from form input
                String stopid = entryEditText.getText().toString();
                
                // get bustimes from OneBusAway API
                JSONArray bustimes = get_bustimes( stopid, apikey );
                
                // for each departure/arrival prediction
                for(int i=0; i<bustimes.length(); i++) {
                    // grab the bus description
                    JSONObject bustime = bustimes.getJSONObject(i);
                    String short_name = bustime.getString("routeShortName");
                    String headsign = bustime.getString("tripHeadsign");
                    long predictedDeparture = bustime.getLong("predictedDepartureTime");
                    long scheduledArrival = bustime.getLong("scheduledArrivalTime");
                    long eta;
                    
                    // figure out the arrival time
                    if(predictedDeparture != 0) {
                        eta = predictedDeparture - System.currentTimeMillis();
                    } else {
                        eta = scheduledArrival - System.currentTimeMillis();
                    }
                    
                    // make it human-readable
                    long minutes = eta/60000;
                    long seconds = (eta%60000)/1000;
                    String str_eta = minutes+" min "+seconds+" sec";
                    
                    // text the watch
                    textWatch( short_name+" "+headsign, str_eta );
                    
                    // wait a second to print the next one
                    SystemClock.sleep(4000);
                }
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
        
        // fetch the OneBusAway API key from an un-versioned XML file.
        apikey = this.getString(R.string.apikey);
        
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
    
    
    /*
     * Fetch content for a URL
     */
    private String get_http_content(String url_string) {
        try {
            // dear lord
            
            // get url object from url string
            URL url = new URL(url_string);
            // get connection from url
            HttpURLConnection huc = (HttpURLConnection)url.openConnection();
            // grt stream from connection
            InputStream is = huc.getInputStream();
            // get reader from stream
            InputStreamReader isr = new InputStreamReader( is );
            // get buffered reader from reader
            BufferedReader br = new BufferedReader( isr );
            
            StringBuilder ret = new StringBuilder();
            String line;
            while( (line = br.readLine())!=null ) {
                ret.append( line );
            }
            return ret.toString();
            
        } catch (MalformedURLException e) {
            print( e.getMessage() );
            return null;
        } catch (IOException e) {
            print( e.getMessage() );
            return null;
        }
    }
    
    /*
     * Get a JSONArray of bustimes for a given stop_id and api_key
     */
    private JSONArray get_bustimes(String stop_id, String api_key) throws JSONException {
        String url_string = "http://"+API_DOMAIN+ARRIVALS_DEPARTURES_PATH+"/1_"+stop_id+".json?key="+api_key;
        String json_response = get_http_content(url_string);

        return (new JSONObject(json_response)).getJSONObject("data").getJSONArray("arrivalsAndDepartures");
    }
}
