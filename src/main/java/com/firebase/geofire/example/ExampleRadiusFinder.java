package com.firebase.geofire.example;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoFireRadiusFinder;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;

public class ExampleRadiusFinder {

    public static void main(String[] args) throws InterruptedException {
        Firebase firebase = new Firebase("https://geofire-v3.firebaseio.com/geofire");
        GeoFire geoFire = new GeoFire(firebase);
        GeoFireRadiusFinder radiusFinder=new GeoFireRadiusFinder(geoFire);
        
        GeoLocation center = new GeoLocation(52.7, -122.4);
        
		int minResults = 20;
		Integer radius=radiusFinder.radiusWithResults(minResults , 1, center );
		System.out.println("radius with at least " + minResults + " is " + radius + " (or max radius reached)");
				
        GeoQuery query = geoFire.queryAtLocation(center, radius);
        query.addGeoQueryEventListener(new GeoQueryEventListener() {
        	int counter=0;
        	
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                System.out.println(String.format( "%d:%s entered at [%f, %f]", counter++,key, location.latitude, location.longitude));
            }

            @Override
            public void onKeyExited(String key) {
                System.out.println(String.format("%s exited", key));
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                System.out.println(String.format("%s moved to [%f, %f]", key, location.latitude, location.longitude));
            }

            @Override
            public void onGeoQueryReady() {
                System.out.println("All initial key entered events have been fired!");
            }

            @Override
            public void onGeoQueryError(FirebaseError error) {
                System.err.println("There was an error querying locations: " + error.getMessage());
            }
        });
        // run for another 60 seconds
        
        try {
        	Integer radius2=radiusFinder.radiusWithResults(minResults , 1, 1.5, center );
        } catch (Exception e) {
        	System.out.println("Exception " + e.getMessage() );
        }
        
        Integer radius3=radiusFinder.radiusWithResults(minResults , 2, 1.5, center );
        
        Thread.sleep(60000);
    }
}
