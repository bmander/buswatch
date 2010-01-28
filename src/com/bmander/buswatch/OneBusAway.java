package com.bmander.buswatch;
import java.net.*;
import org.json.*;
import java.io.*;
import java.util.ArrayList;
import java.lang.Math;

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
        
        ArrivalPrediction(JSONObject content) {
            this.content = content;
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
        
        long getETA(long wrtTime) {
            
            // figure out the arrival time
            if(getPredictedDepartureTime() != NO_PREDICTION_TIME) {
                return getPredictedDepartureTime() - wrtTime;
            } else {
                return getScheduledArrivalTime() - wrtTime;
            }
        }
        
        String getRouteId() {
            try{
                return content.getString("routeId");
            } catch(JSONException e) {
                return "";
            }
        }
        
        String getETAString(long wrtTime) {
            long eta = getETA(wrtTime);
            
            // make it human-readable
            long minutes = eta/60000;
            long seconds = Math.abs( (eta%60000)/1000 );
            return minutes+" min "+seconds+" sec";
        }
        
        String getShortETAString(long wrtTime, boolean forceLongish) {
            long eta = getETA(wrtTime);
            
            // make it human-readable
            long minutes = eta/60000;
            long seconds = Math.abs( (eta%60000)/1000 );
            
            if( forceLongish || Math.abs(minutes) < 2 ) {
                return minutes+"m "+seconds+"s";
            } else {
                return minutes+"m";
            }
        }
        
        String getShortETAString(long wrtTime) {
            return getShortETAString(wrtTime, false);
        }
        
    }
    
    public class Route {
        JSONObject content;
        
        Route( JSONObject content ){
            this.content = content;
        }
        
        String getId() {
            try {
                return content.getString( "id" );
            } catch(JSONException e) {
                return null;
            }
        }
        
        String getDescription() {
            try {
                return content.getString( "description" );
            } catch(JSONException e) {
                return "";
            }
        }
        
        String getLongName() {
            try {
                return content.getString( "longName" );
            } catch(JSONException e) {
                return "";
            }
        }
        
        String getShortName() {
            try {
                return content.getString( "shortName" );
            } catch(JSONException e) {
                return "";
            }
        }
    }
    
    String ARRIVALS_DEPARTURES_PATH = "/api/where/arrivals-and-departures-for-stop/";
    String STOP_PATH = "/api/where/stop/";
    
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
     * Get an ArrayList of bustimes for a given stop_id and api_key
     */
    public ArrayList<ArrivalPrediction> get_bustimes(String stop_id) throws JSONException, MalformedURLException, IOException {
        
        // construct HTTP request and make it
        String url_string = "http://"+api_domain+ARRIVALS_DEPARTURES_PATH+"/1_"+stop_id+".json?key="+api_key;
        String json_response = get_http_content(url_string);
        
        // parse JSON and wrap it a set of ArrivalPrediction objects
        JSONArray json_arrivaldepartures = (new JSONObject(json_response)).getJSONObject("data").getJSONArray("arrivalsAndDepartures");
        
        ArrayList<ArrivalPrediction> ret = new ArrayList<ArrivalPrediction>();
        for(int i=0; i<json_arrivaldepartures.length(); i++) {
            ret.add( new ArrivalPrediction( json_arrivaldepartures.getJSONObject( i ) ) );
        }

        return ret;
    }
    
    /*
     * Get an ArrayList of routes associated with this stop
     */
    public ArrayList<Route> getRoutes(String stop_id) throws JSONException, MalformedURLException, IOException {
        // construct HTTP request and make it
        String url_string = "http://"+api_domain+STOP_PATH+"/1_"+stop_id+".json?key="+api_key;
        String json_response = get_http_content(url_string);
        
        JSONArray jsonRoutes = (new JSONObject(json_response)).getJSONObject("data").getJSONArray("routes");
        
        ArrayList<Route> ret = new ArrayList<Route>();
        for(int i=0; i<jsonRoutes.length(); i++) {
            ret.add( new Route( jsonRoutes.getJSONObject( i ) ) );
        }
        
        return ret;
    }
}