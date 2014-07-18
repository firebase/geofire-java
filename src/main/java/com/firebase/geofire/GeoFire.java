/*
 * Firebase GeoFire Java Library
 *
 * Copyright © 2014 Firebase - All Rights Reserved
 * https://www.firebase.com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binaryform must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY FIREBASE AS IS AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL FIREBASE BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.firebase.geofire;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.geofire.core.GeoHash;
import com.firebase.geofire.core.SimpleFuture;
import com.firebase.geofire.util.GeoUtils;

import java.util.*;
import java.util.concurrent.Future;

/**
 * A GeoFire instance is used to store geo location data in Firebase.
 */
public class GeoFire {

    /**
     * A small wrapper class to forward any events to the LocationEventListener
     */
    private static class LocationValueEventListener implements ValueEventListener {

        private final LocationEventListener eventListener;

        LocationValueEventListener(LocationEventListener eventListener) {
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

    static double[] getLocationValue(DataSnapshot dataSnapshot) {
        try {
            Map data = dataSnapshot.getValue(Map.class);
            List<Double> location = (List<Double>)data.get("l");
            if (location.size() == 2 && GeoUtils.coordinatesValid(location.get(0), location.get(1))) {
                return new double[]{ location.get(0), location.get(1) };
            } else {
                return null;
            }
        } catch (NullPointerException e) {
            return null;
        } catch (ClassCastException e) {
            return null;
        }
    }

    private final Firebase firebase;

    private final Map<LocationEventListener, Map<String, LocationValueEventListener>> eventMapping =
            new IdentityHashMap<LocationEventListener, Map<String, LocationValueEventListener>>();

    /**
     * Creates a new GeoFire instance at the given Firebase reference.
     * @param firebase The Firebase reference this GeoFire instance uses
     */
    public GeoFire(Firebase firebase) {
        this.firebase = firebase;
    }

    /**
     * @return The Firebase reference this GeoFire instance uses
     */
    public Firebase getFirebase() {
        return this.firebase;
    }

    Firebase firebaseRefForKey(String key) {
        return this.firebase.child(key);
    }

    /**
     * Sets the location for a given key
     * @param key The key to save the location for
     * @param latitude The latitude of the location in the range of [-90,90]
     * @param longitude The longitude of the location in the range of [-180,180]
     * @return A Future which will be done once the location was successfully saved on the server or an error occurred.
     * In that case of an error get() will return a FirebaseError. In the case of success get() will return null.
     */
    public Future<FirebaseError> setLocation(String key, double latitude, double longitude) {
        if (key == null) {
            throw new NullPointerException();
        }
        if (!GeoUtils.coordinatesValid(latitude, longitude)) {
            throw new IllegalArgumentException(String.format("Not a valid geo coordinate: [%f,%f]", latitude, longitude));
        }
        Firebase keyRef = this.firebaseRefForKey(key);
        GeoHash geoHash = new GeoHash(latitude, longitude);
        Map<String, Object> updates = new HashMap<String, Object>();
        updates.put("g", geoHash.getGeoHashString());
        updates.put("l", new double[]{latitude, longitude});
        final SimpleFuture<FirebaseError> future = new SimpleFuture<FirebaseError>();
        keyRef.setValue(updates, geoHash.getGeoHashString(), new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                future.put(firebaseError);
            }
        });
        return future;
    }

    /**
     * Removes the location for a key from this GeoFire
     * @param key The key to remove from this GeoFire
     * @return A Future which will be done once the location on the server was successfully removed or an error occurred.
     * In the case of an error get() will return a FirebaseError. In the case of success it get() will return null.
     */
    public Future<FirebaseError> removeLocation(String key) {
        Firebase keyRef = this.firebaseRefForKey(key);
        final SimpleFuture<FirebaseError> future = new SimpleFuture<FirebaseError>();
        keyRef.setValue(null, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                future.put(firebaseError);
            }
        });
        return future;
    }

    /**
     * Adds a location event listener to a key which is triggered once initially and for every further location update
     * of the key.
     *
     * @throws java.lang.IllegalArgumentException If the listener has previously added to the same key.
     *
     * @param key The key to listen on
     * @param listener The listener for the callbacks
     */
    public void addLocationEventListener(String key, LocationEventListener listener) {
        synchronized (this.eventMapping) {
            Firebase keyFirebase = this.firebaseRefForKey(key);
            Map<String, LocationValueEventListener> keysForListener = this.eventMapping.get(key);
            if (keysForListener == null) {
                keysForListener = new HashMap<String, LocationValueEventListener>();
                this.eventMapping.put(listener, keysForListener);
            }
            if (keysForListener.containsKey(key)) {
                throw new IllegalArgumentException("Added the same LocationEventListener for the same key twice!");
            }
            LocationValueEventListener valueListener = new LocationValueEventListener(listener);
            keysForListener.put(key, valueListener);
            keyFirebase.addValueEventListener(valueListener);
        }
    }

    /**
     * Removes a LocationEventListener for a key
     * @param key The key to remove this LocationEventListener from
     * @param listener The LocationEventListener to remove
     */
    public void removeEventListener(String key, LocationEventListener listener) {
        synchronized (this.eventMapping) {
            Map<String, LocationValueEventListener> listeners = this.eventMapping.get(listener);
            if (listeners == null || listeners.containsKey(key)) {
                throw new IllegalArgumentException("Did not previously add or already removed listener");
            }
            LocationValueEventListener valueListener = listeners.get(key);
            this.firebaseRefForKey(key).removeEventListener(valueListener);
            listeners.remove(key);
            if (listeners.size() == 0) {
                this.eventMapping.remove(listener);
            }
        }
    }

    /**
     * Removes LocationEventListener for all keys
     * @param listener The LocationEventListener to remove
     */
    public void removeEventListener(LocationEventListener listener) {
        synchronized (this.eventMapping) {
            Map<String, LocationValueEventListener> listeners = this.eventMapping.get(listener);
            if (listeners == null) {
                throw new IllegalArgumentException("Did not previously add or already removed listener");
            }
            for (Map.Entry<String, LocationValueEventListener> entry: listeners.entrySet()) {
                this.firebaseRefForKey(entry.getKey()).removeEventListener(entry.getValue());
            }
            this.eventMapping.remove(listener);
        }
    }

    /**
     * Removes all LocationEventListener's associated with this GeoFire instance
     */
    public void removeAllEventListeners() {
        synchronized (this.eventMapping) {
            for (LocationEventListener listener : new HashSet<LocationEventListener>(this.eventMapping.keySet())) {
                this.removeEventListener(listener);
            }
        }
    }

    /**
     * Returns a new Query object at the given position and radius
     * @param latitude The latitude of the query center in the range of [-90,90]
     * @param longitude The longitude of the query center in the range of [-180,180]
     * @param radius The radius of the query in meters.
     * @return The new GeoQuery object
     */
    public GeoQuery queryAtLocation(double latitude, double longitude, double radius) {
        return new GeoQuery(this, latitude, longitude, radius);
    }
}
