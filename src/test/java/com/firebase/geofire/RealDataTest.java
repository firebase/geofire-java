package com.firebase.geofire;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;

import java.util.concurrent.*;

public class RealDataTest {

    Firebase firebase;

    @Before
    public void setup() {
        this.firebase = new Firebase(String.format("https://%s.firebaseio-demo.com",
                TestHelpers.randomAlphaNumericString(16)));
    }

    public GeoFire newTestGeoFire() {
        return new GeoFire(this.firebase.child(TestHelpers.randomAlphaNumericString(16)));
    }

    protected void setLoc(GeoFire geoFire, String key, double latitude, double longitude) {
        setLoc(geoFire, key, latitude, longitude, false);
    }

    protected void removeLoc(GeoFire geoFire, String key) {
        removeLoc(geoFire, key, false);
    }

    protected void setLoc(GeoFire geoFire, String key, double latitude, double longitude, boolean wait) {
        Future<FirebaseError> future = geoFire.setLocation(key, latitude, longitude);
        if (wait) {
            try {
                Assert.assertNull(future.get(4, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                Assert.fail("Timeout occured!");
            }
        }
    }

    protected void removeLoc(GeoFire geoFire, String key, boolean wait) {
        Future<FirebaseError> future = geoFire.removeLocation(key);
        if (wait) {
            try {
                Assert.assertNull(future.get(4, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                Assert.fail("Timeout occured!");
            }
        }
    }

    protected void waitForGeoFireReady(GeoFire geoFire) throws InterruptedException {
        final Semaphore semaphore = new Semaphore(0);
        geoFire.getFirebase().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                semaphore.release();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Assert.fail("Firebase error: " + firebaseError);
            }
        });
        Assert.assertTrue("Timeout occured!", semaphore.tryAcquire(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    @After
    public void teardown() {
        this.firebase.setValue(null);
        this.firebase = null;
    }
}
