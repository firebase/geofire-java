package com.firebase.geofire;

import com.firebase.client.FirebaseError;
import org.junit.Assert;

public class LocationEventTestListener extends TestListener implements LocationEventListener {

    public static String locationChanged(String key, double latitude, double longitude) {
        return String.format("CHANGED(%s,%f,%f)", key, latitude, longitude);
    }

    public static String locationRemoved(String key) {
        return String.format("REMOVED(%s)", key);
    }

    @Override
    public void onLocationChanged(String key, double latitude, double longitude) {
        this.addEvent(locationChanged(key, latitude, longitude));
    }

    @Override
    public void onKeyRemoved(String key) {
        this.addEvent(locationRemoved(key));
    }

    @Override
    public void onCancelled(FirebaseError firebaseError) {
        Assert.fail("Firebase synchronization failed: " + firebaseError);
    }
}
