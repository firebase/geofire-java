package com.firebase.geofire;

import com.firebase.client.Firebase;

public class Example {

    public static void main(String[] args) throws InterruptedException {
        Firebase firebase = new Firebase("https://geofire-ios.firebaseio.com/geofire");
        GeoFire geoFire = new GeoFire(firebase);
        GeoQuery query = geoFire.queryAtLocation(37.7, -122.4, 10000);
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
        });
        Thread.sleep(100000);
    }
}
