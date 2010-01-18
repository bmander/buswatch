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
                JSONArray bustimes = get_bustimes( entryEditText.getText().toString(), apikey );
                
                
                for(int i=0; i<bustimes.length(); i++) {
                    JSONObject bustime = bustimes.getJSONObject(i);
                    String short_name = bustime.getString("routeShortName");
                    String headsign = bustime.getString("tripHeadsign");
                    long predictedDeparture = bustime.getLong("predictedDepartureTime");
                    long scheduledArrival = bustime.getLong("scheduledArrivalTime");
                    long eta;
                    
                    if(predictedDeparture != 0) {
                        eta = predictedDeparture - System.currentTimeMillis();
                    } else {
                        eta = scheduledArrival - System.currentTimeMillis();
                    }
                    
                    print( short_name+" "+headsign+" "+eta );
                }
            } catch( JSONException e ) {
                print( e.getMessage() );
            }
        }
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        okButton = (Button) findViewById(R.id.ok);
        contentTextView = (TextView) findViewById(R.id.content);
        entryEditText = (EditText) findViewById(R.id.entry);
        
        apikey = this.getString(R.string.apikey);
        
        // add a click listener to the button
        okButton.setOnClickListener( new OkButtonClickListener() );
        
        print( apikey );

    }
    
    private void print(String str) {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }
    
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
    
    private JSONArray get_bustimes(String stop_id, String api_key) throws JSONException {
        String url_string = "http://"+API_DOMAIN+ARRIVALS_DEPARTURES_PATH+"/1_"+stop_id+".json?key="+api_key;
        String json_response = get_http_content(url_string);

        return (new JSONObject(json_response)).getJSONObject("data").getJSONArray("arrivalsAndDepartures");
    }
}
