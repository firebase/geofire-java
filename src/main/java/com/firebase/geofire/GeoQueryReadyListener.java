package com.firebase.geofire;

import com.firebase.client.FirebaseError;

/**
 * GeoQuery notifies GeoQueryReadyListeners once a query has triggered all initial child added events
 */
public interface GeoQueryReadyListener {

    /**
     * This method is called after all initial key entered events have been fired for a query.
     * It is called immediately if all events have been fired before the listener was added.
     * After any search criteria is updated, it is called again once the new data was loaded from the server
     * and all corresponding events have been fired.
     */
    public void onReady();

    /**
     * Called in case an error occurred while retrieving locations for a query, e.g. violating security rules.
     * @param error The error that occurred while retrieving the query
     */
    public void onCancelled(FirebaseError error);
}
