package com.firebase.geofire;

import com.firebase.client.FirebaseError;

/**
 * GeoQuery notifies GeoQueryReadyListeners once a query has triggered all initial child added events
 */
public interface GeoQueryReadyListener {

    /**
     * This method is called after all child added events have been triggered for a query.
     * It is called immediately if all events have been triggered before the listener was added.
     * It is called again after all child added events have been triggered after changed the search criteria.
     */
    public void onReady();

    /**
     * Called in case an error occurred while retrieving locations for a query, e.g. violating security rules.
     * @param error The error that occurred while retrieving the query
     */
    public void onCancelled(FirebaseError error);
}
