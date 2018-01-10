package com.firebase.geofire;

import com.firebase.geofire.util.SimpleFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredential;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.*;
import java.io.FileNotFoundException;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.Description;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class GeoFireTestingRule extends TestWatcher {
    private static final String DATABASE_URL = "https://geofiretest-8d811.firebaseio.com/";
    private static final String SERVICE_ACCOUNT_CREDENTIALS = "service-account.json";

    private DatabaseReference databaseReference;

    @Override public void starting(Description description) {
        if (FirebaseApp.getApps().isEmpty()) {
            final FirebaseCredential credentials;

            try {
                credentials = FirebaseCredentials.fromCertificate(new FileInputStream(SERVICE_ACCOUNT_CREDENTIALS));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            FirebaseOptions firebaseOptions = new FirebaseOptions.Builder()
                    .setDatabaseUrl(DATABASE_URL)
                    .setCredential(credentials)
                    .build();
            FirebaseApp.initializeApp(firebaseOptions);
            FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);
        }
        this.databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl(DATABASE_URL);
    }

    public GeoFire newTestGeoFire() {
        return new GeoFire(databaseReference.child(TestHelpers.randomAlphaNumericString(16)));
    }

    public void setLoc(GeoFire geoFire, String key, double latitude, double longitude) {
        setLoc(geoFire, key, latitude, longitude, false);
    }

    public void removeLoc(GeoFire geoFire, String key) {
        removeLoc(geoFire, key, false);
    }

    public void setValueAndWait(DatabaseReference databaseReference, Object value) {
        final SimpleFuture<DatabaseError> futureError = new SimpleFuture<DatabaseError>();
        databaseReference.setValue(value, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                futureError.put(databaseError);
            }
        });
        try {
            Assert.assertNull(futureError.get(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            Assert.fail("Timeout occured!");
        }
    }

    public void setLoc(GeoFire geoFire, String key, double latitude, double longitude, boolean wait) {
        final SimpleFuture<DatabaseError> futureError = new SimpleFuture<DatabaseError>();
        geoFire.setLocation(key, new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                futureError.put(error);
            }
        });
        if (wait) {
            try {
                Assert.assertNull(futureError.get(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                Assert.fail("Timeout occured!");
            }
        }
    }

    public void removeLoc(GeoFire geoFire, String key, boolean wait) {
        final SimpleFuture<DatabaseError> futureError = new SimpleFuture<DatabaseError>();
        geoFire.removeLocation(key, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                futureError.put(error);
            }
        });
        if (wait) {
            try {
                Assert.assertNull(futureError.get(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                Assert.fail("Timeout occured!");
            }
        }
    }

    public void waitForGeoFireReady(GeoFire geoFire) throws InterruptedException {
        final Semaphore semaphore = new Semaphore(0);
        geoFire.getDatabaseReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                semaphore.release();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Assert.fail("Firebase error: " + databaseError);
            }
        });
        Assert.assertTrue("Timeout occured!", semaphore.tryAcquire(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    @Override
    public void finished(Description description) {
        this.databaseReference.setValue(null);
        this.databaseReference = null;
    }
}
