package com.bmander.buswatch;
import java.net.*;
import org.json.*;
import java.io.*;
import java.util.ArrayList;

/*
 * Provides an interface to the OneBusAway API
 */ 
public class OneBusAway {
    
    String TAG = "OneBusAwayAPI";
    
    /*
     * Thin wrapper around the 'arrivalsAndDepartures' objects returned by the 'arrivals-and-departures-for-stop' API call
     */
    public class ArrivalPrediction{
        
        long NO_PREDICTION_TIME=0;
        
        JSONObject content;
        long timeAtFetch;
        
        ArrivalPrediction(JSONObject content, long timeAtFetch) {
            this.content = content;
            this.timeAtFetch = timeAtFetch;
        }
        
        String getShortName() {
            try{
                return content.getString("routeShortName");
            } catch(JSONException e) {
                return "";
            }
        }
        
        String getHeadsign() {
            try{
                return content.getString("tripHeadsign");
            } catch(JSONException e) {
                return "";
            }
        }
        
        long getPredictedDepartureTime() {
            try{
                return content.getLong("predictedDepartureTime");
            } catch(JSONException e) {
                return 0;
            }
        }
        
        long getScheduledArrivalTime() {
            try{
                return content.getLong("scheduledArrivalTime");
            } catch(JSONException e) {
                return 0;
            }
        }
        
        long getETA() {
            
            // figure out the arrival time
            if(getPredictedDepartureTime() != NO_PREDICTION_TIME) {
                return getPredictedDepartureTime() - timeAtFetch;
            } else {
                return getScheduledArrivalTime() - timeAtFetch;
            }
        }
        
        String getETAString() {
            long eta = getETA();
            
            // make it human-readable
            long minutes = eta/60000;
            long seconds = (eta%60000)/1000;
            return minutes+" min "+seconds+" sec";
        }
        
    }
    
    String ARRIVALS_DEPARTURES_PATH = "/api/where/arrivals-and-departures-for-stop/";
    
    String api_domain;
    String api_key;
    
    OneBusAway(String api_domain, String api_key) {
        this.api_domain = api_domain;
        this.api_key = api_key;
    }
    
    /*
     * Fetch content for a URL
     */
    private String get_http_content(String url_string) throws MalformedURLException, IOException {
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
        
        // scoop the response body out of the reader one line at a time
        StringBuilder ret = new StringBuilder();
        String line;
        while( (line = br.readLine())!=null ) {
            ret.append( line );
        }
        return ret.toString();
    }
    
    /*
     * Get a JSONArray of bustimes for a given stop_id and api_key
     */
    public ArrayList<ArrivalPrediction> get_bustimes(String stop_id) throws JSONException, MalformedURLException, IOException {
        // get the time that the request was made - necessary for calculating ETAs
        long timeAtFetch = System.currentTimeMillis();
        
        // construct HTTP request and make it
        String url_string = "http://"+api_domain+ARRIVALS_DEPARTURES_PATH+"/1_"+stop_id+".json?key="+api_key;
        String json_response = get_http_content(url_string);
        
        // parse JSON and wrap it a set of ArrivalPrediction objects
        JSONArray json_arrivaldepartures = (new JSONObject(json_response)).getJSONObject("data").getJSONArray("arrivalsAndDepartures");
        
        ArrayList<ArrivalPrediction> ret = new ArrayList<ArrivalPrediction>();
        for(int i=0; i<json_arrivaldepartures.length(); i++) {
            ret.add( new ArrivalPrediction( json_arrivaldepartures.getJSONObject( i ), timeAtFetch ) );
        }

        return ret;
    }
}