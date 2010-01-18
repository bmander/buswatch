package com.bmander.buswatch;
import java.net.*;
import org.json.*;
import java.io.*;

/*
 * Provides an interface to the OneBusAway API
 */ 
public class OneBusAway {
    
    /*
     * Thin wrapper around the 'arrivalsAndDepartures' objects returned by the 'arrivals-and-departures-for-stop' API call
     */
    class ArrivalPrediction{
        
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
    public JSONArray get_bustimes(String stop_id) throws JSONException, MalformedURLException, IOException {
        String url_string = "http://"+api_domain+ARRIVALS_DEPARTURES_PATH+"/1_"+stop_id+".json?key="+api_key;
        String json_response = get_http_content(url_string);

        return (new JSONObject(json_response)).getJSONObject("data").getJSONArray("arrivalsAndDepartures");
    }
}