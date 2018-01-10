package com.firebase.geofire;

import com.google.firebase.database.DatabaseError;

import static java.util.Locale.US;

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

    public GeoQueryEventTestListener() {
        this(true, true, true);
    }

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
