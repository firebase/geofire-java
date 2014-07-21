package com.firebase.geofire;

import com.firebase.client.FirebaseError;

/**
 * GeoFireResult is a helper class to return if a GeoFire write was successful or an error occurred.
 */
public class GeoFireResult {

    private final FirebaseError error;

    GeoFireResult() {
        this.error = null;
    }

    GeoFireResult(FirebaseError error) {
        this.error = error;
    }

    /**
     * @return Returns true if the write was successful, false if an error occurred
     */
    public boolean wasSuccessful() {
        return this.error == null;
    }

    /**
     * @return Returns the error if an error occurred, null if no error occurred
     */
    public FirebaseError getError() {
        return this.error;
    }
}
