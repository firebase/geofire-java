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
import com.firebase.geofire.core.GeoHashQuery;
import com.firebase.geofire.util.GeoUtils;
import com.google.firebase.database.*;

import java.util.*;

/**
 * A GeoQuery object can be used for geo queries in a given circle. The GeoQuery class is thread safe.
 */
public class GeoQuery {

    private static class LocationInfo {
        final GeoLocation location;
        final boolean inGeoQuery;
        final GeoHash geoHash;

        public LocationInfo(GeoLocation location, boolean inGeoQuery) {
            this.location = location;
            this.inGeoQuery = inGeoQuery;
            this.geoHash = new GeoHash(location);
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
            // ignore, this should be handled by onChildChanged
        }

        @Override
        public synchronized void onCancelled(DatabaseError databaseError) {
            // ignore, our API does not support onCancelled
        }
    };

    private final GeoFire geoFire;
    private final Set<GeoQueryEventListener> eventListeners = new HashSet<GeoQueryEventListener>();
    private final Map<GeoHashQuery, Query> firebaseQueries = new HashMap<GeoHashQuery, Query>();
    private final Set<GeoHashQuery> outstandingQueries = new HashSet<GeoHashQuery>();
    private final Map<String, LocationInfo> locationInfos = new HashMap<String, LocationInfo>();
    private GeoLocation center;
    private double radius;
    private Set<GeoHashQuery> queries;

    /**
     * Creates a new GeoQuery object centered at the given location and with the given radius.
     * @param geoFire The GeoFire object this GeoQuery uses
     * @param center The center of this query
     * @param radius The radius of this query, in kilometers
     */
    GeoQuery(GeoFire geoFire, GeoLocation center, double radius) {
        this.geoFire = geoFire;
        this.center = center;
        // convert from kilometers to meters
        this.radius = radius * 1000;
    }

    private boolean locationIsInQuery(GeoLocation location) {
        return GeoUtils.distance(location, center) <= this.radius;
    }

    private void updateLocationInfo(final String key, final GeoLocation location) {
        LocationInfo oldInfo = this.locationInfos.get(key);
        boolean isNew = (oldInfo == null);
        boolean changedLocation = (oldInfo != null && !oldInfo.location.equals(location));
        boolean wasInQuery = (oldInfo != null && oldInfo.inGeoQuery);

        boolean isInQuery = this.locationIsInQuery(location);
        if ((isNew || !wasInQuery) && isInQuery) {
            for (final GeoQueryEventListener listener: this.eventListeners) {
                this.geoFire.raiseEvent(new Runnable() {
                    @Override
                    public void run() {
                        listener.onKeyEntered(key, location);
                    }
                });
            }
        } else if (!isNew && changedLocation && isInQuery) {
            for (final GeoQueryEventListener listener: this.eventListeners) {
                this.geoFire.raiseEvent(new Runnable() {
                    @Override
                    public void run() {
                        listener.onKeyMoved(key, location);
                    }
                });
            }
        } else if (wasInQuery && !isInQuery) {
            for (final GeoQueryEventListener listener: this.eventListeners) {
                this.geoFire.raiseEvent(new Runnable() {
                    @Override
                    public void run() {
                        listener.onKeyExited(key);
                    }
                });
            }
        }
        LocationInfo newInfo = new LocationInfo(location, this.locationIsInQuery(location));
        this.locationInfos.put(key, newInfo);
    }

    private boolean geoHashQueriesContainGeoHash(GeoHash geoHash) {
        if (this.queries == null) {
            return false;
        }
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
        this.outstandingQueries.clear();
        this.firebaseQueries.clear();
        this.queries = null;
        this.locationInfos.clear();
    }

    private boolean hasListeners() {
        return !this.eventListeners.isEmpty();
    }

    private boolean canFireReady() {
        return this.outstandingQueries.isEmpty();
    }

    private void checkAndFireReady() {
        if (canFireReady()) {
            for (final GeoQueryEventListener listener: this.eventListeners) {
                this.geoFire.raiseEvent(new Runnable() {
                    @Override
                    public void run() {
                        listener.onGeoQueryReady();
                    }
                });
            }
        }
    }

    private void addValueToReadyListener(final Query firebase, final GeoHashQuery query) {
        firebase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                synchronized (GeoQuery.this) {
                    GeoQuery.this.outstandingQueries.remove(query);
                    GeoQuery.this.checkAndFireReady();
                }
            }

            @Override
            public void onCancelled(final DatabaseError databaseError) {
                synchronized (GeoQuery.this) {
                    for (final GeoQueryEventListener listener : GeoQuery.this.eventListeners) {
                        GeoQuery.this.geoFire.raiseEvent(new Runnable() {
                            @Override
                            public void run() {
                                listener.onGeoQueryError(databaseError);
                            }
                        });
                    }
                }
            }
        });
    }

    private void setupQueries() {
        Set<GeoHashQuery> oldQueries = (this.queries == null) ? new HashSet<GeoHashQuery>() : this.queries;
        Set<GeoHashQuery> newQueries = GeoHashQuery.queriesAtLocation(center, radius);
        this.queries = newQueries;
        for (GeoHashQuery query: oldQueries) {
            if (!newQueries.contains(query)) {
                firebaseQueries.get(query).removeEventListener(this.childEventLister);
                firebaseQueries.remove(query);
                outstandingQueries.remove(query);
            }
        }
        for (final GeoHashQuery query: newQueries) {
            if (!oldQueries.contains(query)) {
                outstandingQueries.add(query);
                DatabaseReference databaseReference = this.geoFire.getDatabaseReference();
                Query firebaseQuery = databaseReference.orderByChild("g").startAt(query.getStartValue()).endAt(query.getEndValue());
                firebaseQuery.addChildEventListener(this.childEventLister);
                addValueToReadyListener(firebaseQuery, query);
                firebaseQueries.put(query, firebaseQuery);
            }
        }
        for(Map.Entry<String, LocationInfo> info: this.locationInfos.entrySet()) {
            LocationInfo oldLocationInfo = info.getValue();
            this.updateLocationInfo(info.getKey(), oldLocationInfo.location);
        }
        // remove locations that are not part of the geo query anymore
        Iterator<Map.Entry<String, LocationInfo>> it = this.locationInfos.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, LocationInfo> entry = it.next();
            if (!this.geoHashQueriesContainGeoHash(entry.getValue().geoHash)) {
                it.remove();
            }
        }

        checkAndFireReady();
    }

    private void childAdded(DataSnapshot dataSnapshot) {
        GeoLocation location = GeoFire.getLocationValue(dataSnapshot);
        if (location != null) {
            this.updateLocationInfo(dataSnapshot.getKey(), location);
        } else {
            // throw an error in future?
        }
    }

    private void childChanged(DataSnapshot dataSnapshot) {
        GeoLocation location = GeoFire.getLocationValue(dataSnapshot);
        if (location != null) {
            this.updateLocationInfo(dataSnapshot.getKey(), location);
        } else {
            // throw an error in future?
        }
    }

    private void childRemoved(DataSnapshot dataSnapshot) {
        final String key = dataSnapshot.getKey();
        final LocationInfo info = this.locationInfos.get(key);
        if (info != null) {
            this.geoFire.getDatabaseRefForKey(key).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    synchronized(GeoQuery.this) {
                        GeoLocation location = GeoFire.getLocationValue(dataSnapshot);
                        GeoHash hash = (location != null) ? new GeoHash(location) : null;
                        if (hash == null || !GeoQuery.this.geoHashQueriesContainGeoHash(hash)) {
                            final LocationInfo info = GeoQuery.this.locationInfos.get(key);
                            GeoQuery.this.locationInfos.remove(key);
                            if (info != null && info.inGeoQuery) {
                                for (final GeoQueryEventListener listener: GeoQuery.this.eventListeners) {
                                    GeoQuery.this.geoFire.raiseEvent(new Runnable() {
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
                public void onCancelled(DatabaseError databaseError) {
                    // tough luck
                }
            });
        }
    }

    /**
     * Adds a new GeoQueryEventListener to this GeoQuery.
     *
     * @throws java.lang.IllegalArgumentException If this listener was already added
     *
     * @param listener The listener to add
     */
    public synchronized void addGeoQueryEventListener(final GeoQueryEventListener listener) {
        if (eventListeners.contains(listener)) {
            throw new IllegalArgumentException("Added the same listener twice to a GeoQuery!");
        }
        eventListeners.add(listener);
        if (this.queries == null) {
            this.setupQueries();
        } else {
            for (final Map.Entry<String, LocationInfo> entry: this.locationInfos.entrySet()) {
                final String key = entry.getKey();
                final LocationInfo info = entry.getValue();
                if (info.inGeoQuery) {
                    this.geoFire.raiseEvent(new Runnable() {
                        @Override
                        public void run() {
                            listener.onKeyEntered(key, info.location);
                        }
                    });
                }
            }
            if (this.canFireReady()) {
                this.geoFire.raiseEvent(new Runnable() {
                    @Override
                    public void run() {
                        listener.onGeoQueryReady();
                    }
                });
            }
        }
    }

    /**
     * Removes an event listener.
     *
     * @throws java.lang.IllegalArgumentException If the listener was removed already or never added
     *
     * @param listener The listener to remove
     */
    public synchronized void removeGeoQueryEventListener(GeoQueryEventListener listener) {
        if (!eventListeners.contains(listener)) {
            throw new IllegalArgumentException("Trying to remove listener that was removed or not added!");
        }
        eventListeners.remove(listener);
        if (!this.hasListeners()) {
            reset();
        }
    }

    /**
     * Removes all event listeners from this GeoQuery.
     */
    public synchronized void removeAllListeners() {
        eventListeners.clear();
        reset();
    }

    /**
     * Returns the current center of this query.
     * @return The current center
     */
    public synchronized GeoLocation getCenter() {
        return center;
    }

    /**
     * Sets the new center of this query and triggers new events if necessary.
     * @param center The new center
     */
    public synchronized void setCenter(GeoLocation center) {
        this.center = center;
        if (this.hasListeners()) {
            this.setupQueries();
        }
    }

    /**
     * Returns the radius of the query, in kilometers.
     * @return The radius of this query, in kilometers
     */
    public synchronized double getRadius() {
        // convert from meters
        return radius / 1000;
    }

    /**
     * Sets the radius of this query, in kilometers, and triggers new events if necessary.
     * @param radius The new radius value of this query in kilometers
     */
    public synchronized void setRadius(double radius) {
        // convert to meters
        this.radius = radius * 1000;
        if (this.hasListeners()) {
            this.setupQueries();
        }
    }

    /**
     * Sets the center and radius (in kilometers) of this query, and triggers new events if necessary.
     * @param center The new center
     * @param radius The new radius value of this query in kilometers
     */
    public synchronized void setLocation(GeoLocation center, double radius) {
        this.center = center;
        // convert radius to meters
        this.radius = radius * 1000;
        if (this.hasListeners()) {
            this.setupQueries();
        }
    }
}
