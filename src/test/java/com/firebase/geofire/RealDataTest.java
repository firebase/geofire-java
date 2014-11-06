package com.firebase.geofire;

import com.firebase.client.*;
import com.firebase.geofire.core.SimpleFuture;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RealDataTest {

    Firebase firebase;

    @Before
    public void setup() {
        Config cfg = Firebase.getDefaultConfig();
        if (!cfg.isFrozen()) {
            cfg.setLogLevel(Logger.Level.DEBUG);
        }
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

    protected void setValueAndWait(Firebase firebase, Object value) {
        final SimpleFuture<FirebaseError> futureError = new SimpleFuture<FirebaseError>();
        firebase.setValue(value, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError error, Firebase firebase) {
                futureError.put(error);
            }
        });
        try {
            Assert.assertNull(futureError.get(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            Assert.fail("Timeout occured!");
        }
    }

    protected void setLoc(GeoFire geoFire, String key, double latitude, double longitude, boolean wait) {
        final SimpleFuture<FirebaseError> futureError = new SimpleFuture<FirebaseError>();
        geoFire.setLocation(key, new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, FirebaseError firebaseError) {
                futureError.put(firebaseError);
            }
        });
        if (wait) {
            try {
                Assert.assertNull(futureError.get(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));
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
        final SimpleFuture<FirebaseError> futureError = new SimpleFuture<FirebaseError>();
        geoFire.removeLocation(key, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, FirebaseError firebaseError) {
                futureError.put(firebaseError);
            }
        });
        if (wait) {
            try {
                Assert.assertNull(futureError.get(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));
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
