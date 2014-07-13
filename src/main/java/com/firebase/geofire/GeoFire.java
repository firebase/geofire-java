package com.firebase.geofire;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.geofire.core.GeoHash;
import com.firebase.geofire.util.GeoUtils;

import java.util.HashMap;
import java.util.Map;

public class GeoFire {

    public static interface LocationEventListener {
        public void onLocationChanged(String key, double latitude, double longitude);
        public void onKeyRemoved(String key);
        public void onCancelled(FirebaseError firebaseError);
    }

    static double[] getLocationValue(DataSnapshot dataSnapshot) {
        try {
            //Map data = (Map)dataSnapshot.getValue(Map.class);
            //double[] location = (double[])data.get("l");
            double[] location = dataSnapshot.getValue(double[].class);
            if (location.length == 2 && GeoUtils.coordinatesValid(location[0], location[1])) {
                return location;
            } else {
                return null;
            }
        } catch (NullPointerException e) {
            return null;
        } catch (ClassCastException e) {
            return null;
        }
    }

    private static class LocationValueEventListener implements ValueEventListener {

        private final LocationEventListener eventListener;
        private final String key;

        LocationValueEventListener(String key, LocationEventListener eventListener) {
            this.key = key;
            this.eventListener = eventListener;
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            double[] location = GeoFire.getLocationValue(dataSnapshot);
            if (location != null) {
                this.eventListener.onLocationChanged(dataSnapshot.getName(), location[0], location[1]);
            } else {
                this.eventListener.onKeyRemoved(dataSnapshot.getName());
            }
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {
            this.eventListener.onCancelled(firebaseError);
        }
    }

    private final Firebase firebase;
    private final Map<LocationEventListener, LocationValueEventListener> eventMapping =
            new HashMap<LocationEventListener, LocationValueEventListener>();

    public GeoFire(Firebase firebase) {
        this.firebase = firebase;
    }

    public Firebase getFirebase() {
        return this.firebase;
    }

    private Firebase firebaseForKey(String key) {
        return this.firebase.child("l").child(key);
    }

    public synchronized void setLocation(String key, double latitude, double longitude) {
        this.setLocation(key, latitude, longitude, null);
    }

    public synchronized void setLocation(String key, double latitude, double longitude, Firebase.CompletionListener listener) {
        if (key == null) {
            throw new NullPointerException();
        }
        Firebase keyFirebase = this.firebaseForKey(key);
        GeoHash geoHash = new GeoHash(latitude, longitude);
        Map<String, Object> updates = new HashMap<String, Object>();
        updates.put("g", geoHash.getGeoHashString());
        updates.put("l", new double[]{latitude, longitude});
        keyFirebase.setValue(updates, listener);
    }

    public synchronized void addLocationEventListener(String key, LocationEventListener listener) {
        Firebase keyFirebase = this.firebaseForKey(key);
        if (this.eventMapping.containsKey(listener)) {
            throw new IllegalArgumentException("Added the same LocationEventListener twice!");
        }
        LocationValueEventListener valueListener = new LocationValueEventListener(key, listener);
        this.eventMapping.put(listener, valueListener);
        keyFirebase.addValueEventListener(valueListener);
    }

    public synchronized void removeEventListener(LocationEventListener listener) {
        LocationValueEventListener valueListener = this.eventMapping.get(listener);
        if (valueListener == null) {
            throw new IllegalArgumentException("Did not previously add or already removed listener");
        }
        this.firebaseForKey(valueListener.key).removeEventListener(valueListener);
    }

    public GeoQuery queryAtLocation(double latitude, double longitude, double radius) {
        return new GeoQuery(this, latitude, longitude, radius);
    }
}
