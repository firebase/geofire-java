package com.firebase.geofire;

import java.util.*;

import com.firebase.client.*;
import com.firebase.geofire.core.*;
import com.firebase.geofire.util.GeoUtils;

public class GeoQuery implements ChildEventListener {

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

    public static interface GeoQueryEventListener {
        public void onKeyEntered(String key, double latitude, double longitude);
        public void onKeyExited(String key);
        public void onKeyMoved(String key, double latitude, double longitude);
    }

    private final GeoFire geoFire;
    private final Set<GeoQueryEventListener> eventListeners = new HashSet<GeoQueryEventListener>();
    private final Map<GeoHashQuery, Query> firebaseQueries = new HashMap<GeoHashQuery, Query>();
    private final Map<String, LocationInfo> locationInfos = new HashMap<String, LocationInfo>();
    private double centerLatitude;
    private double centerLongitude;
    private double radius;
    private Set<GeoHashQuery> queries;

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
                        listener.onKeyMoved(key, latitude, longitude);
                    }
                });
            }
        }
        LocationInfo newInfo = new LocationInfo(latitude, longitude, this.locationIsInQuery(latitude, longitude));
        this.locationInfos.put(key, newInfo);
    }

    private void setupQueries() {
        Set<GeoHashQuery> oldQueries = (this.queries == null) ? new HashSet<GeoHashQuery>() : this.queries;
        Set<GeoHashQuery> newQueries = GeoHashQuery.queriesAtLocation(centerLatitude, centerLongitude, radius);
        this.queries = newQueries;
        for (GeoHashQuery query: oldQueries) {
            if (!newQueries.contains(query)) {
                firebaseQueries.get(query).removeEventListener(this);
                firebaseQueries.remove(query);
            }
        }
        for (GeoHashQuery query: newQueries) {
            if (!oldQueries.contains(query)) {
                Query firebaseQuery = this.geoFire.getFirebase().child("l");
                firebaseQuery.addChildEventListener(this);
                firebaseQueries.put(query, firebaseQuery);
            }
        }
        for(Map.Entry<String, LocationInfo> info: this.locationInfos.entrySet()) {
            LocationInfo oldLocationInfo = info.getValue();
            this.updateLocationInfo(info.getKey(), oldLocationInfo.latitude, oldLocationInfo.longitude);
        }
        for(Map.Entry<String, LocationInfo> entry: this.locationInfos.entrySet()) {
            boolean inQuery = false;
            for (GeoHashQuery query: newQueries) {
                if (query.containsGeoHash(entry.getValue().geoHash)) {
                    inQuery = true;
                    break;
                }
            }
            if (!inQuery) {
                this.locationInfos.remove(entry.getKey());
            }
        }
    }

    @Override
    public synchronized void onChildAdded(DataSnapshot dataSnapshot, String s) {
        double[] location = GeoFire.getLocationValue(dataSnapshot);
        if (location != null) {
            this.updateLocationInfo(dataSnapshot.getName(), location[0], location[1]);
        } else {
            // throw an error in future?
        }
    }

    @Override
    public synchronized void onChildChanged(DataSnapshot dataSnapshot, String s) {
        double[] location = GeoFire.getLocationValue(dataSnapshot);
        if (location != null) {
            this.updateLocationInfo(dataSnapshot.getName(), location[0], location[1]);
        } else {
            // throw an error in future?
        }
    }

    @Override
    public synchronized void onChildRemoved(DataSnapshot dataSnapshot) {
        final String key = dataSnapshot.getName();
        final LocationInfo info = this.locationInfos.get(key);
        if (info != null) {
            this.locationInfos.remove(key);
            if (info.inGeoQuery) {
                for (final GeoQueryEventListener listener: this.eventListeners) {
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

    @Override
    public synchronized void onChildMoved(DataSnapshot dataSnapshot, String s) {
        // ignore, this should be handle by onChildChanged
    }

    @Override
    public synchronized void onCancelled(FirebaseError firebaseError) {
        // ignore, our API does not support onCancelled
    }

    public synchronized void addGeoQueryEventListener(GeoQueryEventListener listener) {
        if (eventListeners.contains(listener)) {
            throw new IllegalArgumentException("Added the same listener twice to a GeoQuery!");
        }
        eventListeners.add(listener);
        if (this.queries == null) {
            this.setupQueries();
        }
    }

    public synchronized void removeEventListener(GeoQueryEventListener listener) {
        if (!eventListeners.contains(listener)) {
            throw new IllegalArgumentException("Trying to remove listener that was removed or not added!");
        }
        eventListeners.remove(listener);
        // TODO: reset
    }

    public synchronized double getCenterLatitude() {
        return centerLatitude;
    }

    public synchronized double getCenterLongitude() {
        return centerLongitude;
    }

    public synchronized void setCenter(double latitude, double longitude) {
        if (this.centerLatitude != latitude || this.centerLongitude != longitude) {
            this.centerLatitude = latitude;
            this.centerLongitude = longitude;
            this.setupQueries();
        }
    }

    public synchronized double getRadius() {
        return radius;
    }

    public synchronized void setRadius(double radius) {
        if (radius != this.radius) {
            this.radius = radius;
            this.setupQueries();
        }
    }
}
