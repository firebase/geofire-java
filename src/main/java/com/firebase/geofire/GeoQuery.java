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

import com.firebase.client.*;
import com.firebase.geofire.core.GeoHash;
import com.firebase.geofire.core.GeoHashQuery;
import com.firebase.geofire.util.GeoUtils;

import java.util.*;

/**
 * A GeoQuery object can be used for geo queries in a given cirlce. The GeoQuery class is thread safe.
 */
public class GeoQuery {

    private static class LocationInfo {
        final double latitude;
        final double longitude;
        final boolean inGeoQuery;
        final GeoHash geoHash;

        public LocationInfo(double latitude, double longitude, boolean inGeoQuery) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.inGeoQuery = inGeoQuery;
            this.geoHash = new GeoHash(latitude, longitude);
        }
    }

    private final ChildEventListener childEventLister = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            synchronized (GeoQuery.this) {
                GeoQuery.this.childAdded(dataSnapshot);
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            synchronized (GeoQuery.this) {
                GeoQuery.this.childChanged(dataSnapshot);
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            synchronized (GeoQuery.this) {
                GeoQuery.this.childRemoved(dataSnapshot);
            }
        }

        @Override
        public synchronized void onChildMoved(DataSnapshot dataSnapshot, String s) {
            // ignore, this should be handle by onChildChanged
        }

        @Override
        public synchronized void onCancelled(FirebaseError firebaseError) {
            // ignore, our API does not support onCancelled
        }
    };

    private final GeoFire geoFire;
    private final Set<GeoQueryEventListener> eventListeners = new HashSet<GeoQueryEventListener>();
    private final Map<GeoHashQuery, Query> firebaseQueries = new HashMap<GeoHashQuery, Query>();
    private final Map<String, LocationInfo> locationInfos = new HashMap<String, LocationInfo>();
    private double centerLatitude;
    private double centerLongitude;
    private double radius;
    private Set<GeoHashQuery> queries;

    /**
     * Creates a new GeoQuery with the given GeoFire at the given center and radius
     * @param geoFire The GeoFire object this GeoQuery uses
     * @param latitude The latitude of the center of this query in the range of [-90,90]
     * @param longitude The longitude of the center of this query in the range of [-180,180]
     * @param radius The radius of this query in meters
     */
    public GeoQuery(GeoFire geoFire, double latitude, double longitude, double radius) {
        this.geoFire = geoFire;
        this.centerLatitude = latitude;
        this.centerLongitude = longitude;
        this.radius = radius;
    }

    private boolean locationIsInQuery(double latitude, double longitude) {
        return GeoUtils.distance(latitude, longitude, centerLatitude, centerLongitude) <= this.radius;
    }

    private void postEvent(Runnable r) {
        EventTarget target = Firebase.getDefaultConfig().getEventTarget();
        target.postEvent(r);
    }

    private void updateLocationInfo(final String key, final double latitude, final double longitude) {
        LocationInfo oldInfo = this.locationInfos.get(key);
        boolean isNew = (oldInfo == null);
        boolean changedLocation = (oldInfo != null && (oldInfo.latitude != latitude || oldInfo.longitude != longitude));
        boolean wasInQuery = (oldInfo != null && oldInfo.inGeoQuery);

        boolean isInQuery = this.locationIsInQuery(latitude, longitude);
        if ((isNew || !wasInQuery) && isInQuery) {
            for (final GeoQueryEventListener listener: this.eventListeners) {
                postEvent(new Runnable() {
                    @Override
                    public void run() {
                        listener.onKeyEntered(key, latitude, longitude);
                    }
                });
            }
        } else if (!isNew && changedLocation && isInQuery) {
            for (final GeoQueryEventListener listener: this.eventListeners) {
                postEvent(new Runnable() {
                    @Override
                    public void run() {
                        listener.onKeyMoved(key, latitude, longitude);
                    }
                });
            }
        } else if (wasInQuery && !isInQuery) {
            for (final GeoQueryEventListener listener: this.eventListeners) {
                postEvent(new Runnable() {
                    @Override
                    public void run() {
                        listener.onKeyExited(key);
                    }
                });
            }
        }
        LocationInfo newInfo = new LocationInfo(latitude, longitude, this.locationIsInQuery(latitude, longitude));
        this.locationInfos.put(key, newInfo);
    }

    private boolean geoHashQueriesContainGeoHash(GeoHash geoHash) {
        for (GeoHashQuery query: this.queries) {
            if (query.containsGeoHash(geoHash)) {
                return true;
            }
        }
        return false;
    }

    private void reset() {
        for(Map.Entry<GeoHashQuery, Query> entry: this.firebaseQueries.entrySet()) {
            entry.getValue().removeEventListener(this.childEventLister);
        }
        this.firebaseQueries.clear();
        this.queries = null;
        this.locationInfos.clear();
    }

    private void setupQueries() {
        Set<GeoHashQuery> oldQueries = (this.queries == null) ? new HashSet<GeoHashQuery>() : this.queries;
        Set<GeoHashQuery> newQueries = GeoHashQuery.queriesAtLocation(centerLatitude, centerLongitude, radius);
        this.queries = newQueries;
        for (GeoHashQuery query: oldQueries) {
            if (!newQueries.contains(query)) {
                firebaseQueries.get(query).removeEventListener(this.childEventLister);
                firebaseQueries.remove(query);
            }
        }
        for (GeoHashQuery query: newQueries) {
            if (!oldQueries.contains(query)) {
                Firebase firebase = this.geoFire.getFirebase();
                Query firebaseQuery = firebase.startAt(query.getStartValue()).endAt(query.getEndValue());
                firebaseQuery.addChildEventListener(this.childEventLister);
                firebaseQueries.put(query, firebaseQuery);
            }
        }
        for(Map.Entry<String, LocationInfo> info: this.locationInfos.entrySet()) {
            LocationInfo oldLocationInfo = info.getValue();
            this.updateLocationInfo(info.getKey(), oldLocationInfo.latitude, oldLocationInfo.longitude);
        }
        // remove locations that are not part of the geo query anymore
        Iterator<Map.Entry<String, LocationInfo>> it = this.locationInfos.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, LocationInfo> entry = it.next();
            if (!this.geoHashQueriesContainGeoHash(entry.getValue().geoHash)) {
                it.remove();
            }
        }
    }

    private void childAdded(DataSnapshot dataSnapshot) {
        double[] location = GeoFire.getLocationValue(dataSnapshot);
        if (location != null) {
            this.updateLocationInfo(dataSnapshot.getName(), location[0], location[1]);
        } else {
            // throw an error in future?
        }
    }

    private void childChanged(DataSnapshot dataSnapshot) {
        double[] location = GeoFire.getLocationValue(dataSnapshot);
        if (location != null) {
            this.updateLocationInfo(dataSnapshot.getName(), location[0], location[1]);
        } else {
            // throw an error in future?
        }
    }

    private void childRemoved(DataSnapshot dataSnapshot) {
        final String key = dataSnapshot.getName();
        final LocationInfo info = this.locationInfos.get(key);
        if (info != null) {
            this.geoFire.firebaseRefForKey(key).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    synchronized(GeoQuery.this) {
                        double[] location = GeoFire.getLocationValue(dataSnapshot);
                        GeoHash hash = (location != null) ? new GeoHash(location[0], location[1]) : null;
                        if (hash == null || !GeoQuery.this.geoHashQueriesContainGeoHash(hash)) {
                            final LocationInfo info = GeoQuery.this.locationInfos.get(key);
                            GeoQuery.this.locationInfos.remove(key);
                            if (info != null && info.inGeoQuery) {
                                for (final GeoQueryEventListener listener: GeoQuery.this.eventListeners) {
                                    postEvent(new Runnable() {
                                        @Override
                                        public void run() {
                                            listener.onKeyExited(key);
                                        }
                                    });
                                }
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    // tough luck
                }
            });
        }
    }

    /**
     * Adds a new GeoQueryEventListener to this GeoQuery
     *
     * @throws java.lang.IllegalArgumentException If this listener was already added
     *
     * @param listener The listener to add
     */
    public synchronized void addGeoQueryEventListener(GeoQueryEventListener listener) {
        if (eventListeners.contains(listener)) {
            throw new IllegalArgumentException("Added the same listener twice to a GeoQuery!");
        }
        eventListeners.add(listener);
        if (this.queries == null) {
            this.setupQueries();
        }
    }

    /**
     * Removes an event listener
     *
     * @throws java.lang.IllegalArgumentException If the listener was removed already or never added
     *
     * @param listener The listener to remove
     */
    public synchronized void removeEventListener(GeoQueryEventListener listener) {
        if (!eventListeners.contains(listener)) {
            throw new IllegalArgumentException("Trying to remove listener that was removed or not added!");
        }
        eventListeners.remove(listener);
        if (eventListeners.size() == 0) {
            reset();
        }
    }

    /**
     * Removes all event listeners for this GeoQuery
     */
    public synchronized void removeAllEventListeners() {
        eventListeners.clear();
        reset();
    }

    /**
     * Returns the latitude value of the current center of this query
     * @return The latitude value
     */
    public synchronized double getCenterLatitude() {
        return centerLatitude;
    }

    /**
     * Returns the longitude value of the current center of this query
     * @return The longitude value
     */
    public synchronized double getCenterLongitude() {
        return centerLongitude;
    }

    /**
     * Sets the new center of this query and triggers new events if necessary
     * @param latitude The new latitude value of the center
     * @param longitude The new longitude value of the center
     */
    public synchronized void setCenter(double latitude, double longitude) {
        if (this.centerLatitude != latitude || this.centerLongitude != longitude) {
            this.centerLatitude = latitude;
            this.centerLongitude = longitude;
            if (this.eventListeners.size() > 0) {
                this.setupQueries();
            }
        }
    }

    /**
     * Returns the radius of the query in meters
     * @return The radius of this query in meters
     */
    public synchronized double getRadius() {
        return radius;
    }

    /**
     * Sets the radius of this query in meters and triggers new events if necessary
     * @param radius The new radius value of this query in meters
     */
    public synchronized void setRadius(double radius) {
        if (radius != this.radius) {
            this.radius = radius;
            if (this.eventListeners.size() > 0) {
                this.setupQueries();
            }
        }
    }
}
