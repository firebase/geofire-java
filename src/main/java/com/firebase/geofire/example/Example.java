package com.firebase.geofire.example;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;

public class Example {

    public static void main(String[] args) throws InterruptedException {
        Firebase firebase = new Firebase("https://geofire-v3.firebaseio.com/geofire");
        GeoFire geoFire = new GeoFire(firebase);
        GeoQuery query = geoFire.queryAtLocation(37.7, -122.4, 10);
        query.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, double latitude, double longitude) {
                System.out.println(String.format("%s entered at [%f, %f]", key, latitude, longitude));
            }

            @Override
            public void onKeyExited(String key) {
                System.out.println(String.format("%s exited", key));
            }

            @Override
            public void onKeyMoved(String key, double latitude, double longitude) {
                System.out.println(String.format("%s moved to [%f, %f]", key, latitude, longitude));
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
        Thread.sleep(60000);
    }
}
