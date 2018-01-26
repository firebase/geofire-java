package com.firebase.geofire.testing;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.database.DatabaseError;

import static java.util.Locale.US;

/**
 * This listener can be used for testing your Geofire instance and asserting that certain events were sent.
 */
public final class GeoQueryEventTestListener extends TestListener implements GeoQueryEventListener {
    public static String keyEntered(String key, double latitude, double longitude) {
        return String.format(US, "KEY_ENTERED(%s,%f,%f)", key, latitude, longitude);
    }

    public static String keyMoved(String key, double latitude, double longitude) {
        return String.format(US, "KEY_MOVED(%s,%f,%f)", key, latitude, longitude);
    }

    public static String keyExited(String key) {
        return String.format("KEY_EXITED(%s)", key);
    }

    private final boolean recordEntered;
    private final boolean recordMoved;
    private final boolean recordExited;

    /** This will by default record all of the events. */
    public GeoQueryEventTestListener() {
        this(true, true, true);
    }

    /** Allows you to specify exactly which of the events you want to record. */
    public GeoQueryEventTestListener(boolean recordEntered, boolean recordMoved, boolean recordExited) {
        this.recordEntered = recordEntered;
        this.recordMoved = recordMoved;
        this.recordExited = recordExited;
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        if (recordEntered) {
            addEvent(keyEntered(key, location.latitude, location.longitude));
        }
    }

    @Override
    public void onKeyExited(String key) {
        if (recordExited) {
            addEvent(keyExited(key));
        }
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        if (recordMoved) {
            addEvent(keyMoved(key, location.latitude, location.longitude));
        }
    }

    @Override
    public void onGeoQueryReady() {
        // No-op.
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        throw error.toException();
    }
}
