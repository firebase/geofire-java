package com.firebase.geofire.testing;

import static java.util.Locale.US;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.LocationCallback;
import com.google.firebase.database.DatabaseError;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;

public class TestCallback implements LocationCallback {

    private final SimpleFuture<String> future = new SimpleFuture<>();

    public static String location(String key, double latitude, double longitude) {
        return String.format(US, "LOCATION(%s,%f,%f)", key, latitude, longitude);
    }

    public static String noLocation(String key) {
        return String.format("NO_LOCATION(%s)", key);
    }

    public String getCallbackValue() throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(GeoFireTestingRule.TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void onLocationResult(String key, GeoLocation location) {
        if (future.isDone()) {
            throw new IllegalStateException("Already received callback");
        }
        if (location != null) {
            future.put(location(key, location.latitude, location.longitude));
        } else {
            future.put(noLocation(key));
        }
    }

    @Override
    public void onCancelled(DatabaseError firebaseError) {
        Assert.fail("Firebase synchronization failed: " + firebaseError);
    }
}
