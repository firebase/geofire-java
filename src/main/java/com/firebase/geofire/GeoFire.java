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

import com.firebase.geofire.core.GeoHash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.GenericTypeIndicator;

import java.lang.Throwable;
import java.util.*;

/**
 * A GeoFire instance is used to store geo location data in Firebase.
 */
public class GeoFire {

    /**
     * A listener that can be used to be notified about a successful write or an error on writing.
     */
    public static interface CompletionListener {
        /**
         * Called once a location was successfully saved on the server or an error occurred. On success, the parameter
         * error will be null; in case of an error, the error will be passed to this method.
         *
         * @param key   The key whose location was saved
         * @param error The error or null if no error occurred
         */
        public void onComplete(String key, DatabaseError error);
    }

    /**
     * A small wrapper class to forward any events to the LocationEventListener.
     */
    private static class LocationValueEventListener implements ValueEventListener {

        private final LocationCallback callback;

        LocationValueEventListener(LocationCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if (dataSnapshot.getValue() == null) {
                this.callback.onLocationResult(dataSnapshot.getKey(), null);
            } else {
                GeoLocation location = GeoFire.getLocationValue(dataSnapshot);
                if (location != null) {
                    this.callback.onLocationResult(dataSnapshot.getKey(), location);
                } else {
                    String message = "GeoFire data has invalid format: " + dataSnapshot.getValue();
                    this.callback.onCancelled(DatabaseError.fromException(new Throwable(message)));
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            this.callback.onCancelled(databaseError);
        }
    }

    static GeoLocation getLocationValue(DataSnapshot dataSnapshot) {
        try {
            GenericTypeIndicator<Map<String, Object>> typeIndicator = new GenericTypeIndicator<Map<String, Object>>() {};
            Map<String, Object> data = dataSnapshot.getValue(typeIndicator);
            List<?> location = (List<?>) data.get("l");
            Number latitudeObj = (Number) location.get(0);
            Number longitudeObj = (Number) location.get(1);
            double latitude = latitudeObj.doubleValue();
            double longitude = longitudeObj.doubleValue();
            if (location.size() == 2 && GeoLocation.coordinatesValid(latitude, longitude)) {
                return new GeoLocation(latitude, longitude);
            } else {
                return null;
            }
        } catch (NullPointerException e) {
            return null;
        } catch (ClassCastException e) {
            return null;
        }
    }

    private final DatabaseReference databaseReference;
    private final EventRaiser eventRaiser;

    /**
     * Creates a new GeoFire instance at the given Firebase reference.
     *
     * @param databaseReference The Firebase reference this GeoFire instance uses
     */
    public GeoFire(DatabaseReference databaseReference) {
        this.databaseReference = databaseReference;
        EventRaiser eventRaiser;
        try {
            eventRaiser = new AndroidEventRaiser();
        } catch (Throwable e) {
            // We're not on Android, use the ThreadEventRaiser
            eventRaiser = new ThreadEventRaiser();
        }
        this.eventRaiser = eventRaiser;
    }

    /**
     * @return The Firebase reference this GeoFire instance uses
     */
    public DatabaseReference getDatabaseReference() {
        return this.databaseReference;
    }

    DatabaseReference getDatabaseRefForKey(String key) {
        return this.databaseReference.child(key);
    }

    /**
     * Sets the location for a given key.
     *
     * @param key      The key to save the location for
     * @param location The location of this key
     */
    public void setLocation(String key, GeoLocation location) {
        this.setLocation(key, location, null);
    }

    /**
     * Sets the location for a given key.
     *
     * @param key                The key to save the location for
     * @param location           The location of this key
     * @param completionListener A listener that is called once the location was successfully saved on the server or an
     *                           error occurred
     */
    public void setLocation(final String key, final GeoLocation location, final CompletionListener completionListener) {
        if (key == null) {
            throw new NullPointerException();
        }
        DatabaseReference keyRef = this.getDatabaseRefForKey(key);
        GeoHash geoHash = new GeoHash(location);
        Map<String, Object> updates = new HashMap<String, Object>();
        updates.put("g", geoHash.getGeoHashString());
        updates.put("l", Arrays.asList(location.latitude, location.longitude));
        if (completionListener != null) {
            keyRef.setValue(updates, geoHash.getGeoHashString(), new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    completionListener.onComplete(key, databaseError);
                }
            });
        } else {
            keyRef.setValue(updates, geoHash.getGeoHashString());
        }
    }

    /**
     * Removes the location for a key from this GeoFire.
     *
     * @param key The key to remove from this GeoFire
     */
    public void removeLocation(String key) {
        this.removeLocation(key, null);
    }

    /**
     * Removes the location for a key from this GeoFire.
     *
     * @param key                The key to remove from this GeoFire
     * @param completionListener A completion listener that is called once the location is successfully removed
     *                           from the server or an error occurred
     */
    public void removeLocation(final String key, final CompletionListener completionListener) {
        if (key == null) {
            throw new NullPointerException();
        }
        DatabaseReference keyRef = this.getDatabaseRefForKey(key);
        if (completionListener != null) {
            keyRef.setValue(null, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    completionListener.onComplete(key, databaseError);
                }
            });
        } else {
            keyRef.setValue(null);
        }
    }

    /**
     * Gets the current location for a key and calls the callback with the current value.
     *
     * @param key      The key whose location to get
     * @param callback The callback that is called once the location is retrieved
     */
    public void getLocation(String key, LocationCallback callback) {
        DatabaseReference keyRef = this.getDatabaseRefForKey(key);
        LocationValueEventListener valueListener = new LocationValueEventListener(callback);
        keyRef.addListenerForSingleValueEvent(valueListener);
    }

    /**
     * Returns a new Query object centered at the given location and with the given radius.
     *
     * @param center The center of the query
     * @param radius The radius of the query, in kilometers
     * @return The new GeoQuery object
     */
    public GeoQuery queryAtLocation(GeoLocation center, double radius) {
        return new GeoQuery(this, center, radius);
    }

    void raiseEvent(Runnable r) {
        this.eventRaiser.raiseEvent(r);
    }
}
