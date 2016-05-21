package com.firebase.geofire;

import com.firebase.geofire.core.SimpleFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import com.google.firebase.database.core.Repo;
import com.oracle.javafx.jmx.json.JSONException;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RealDataTest {

    DatabaseReference databaseReference;

    @Before
    public void setup() throws FileNotFoundException {
        String databaseUrl = String.format("https://%s.firebaseio-demo.com", TestHelpers.randomAlphaNumericString(16));
        FirebaseOptions firebaseOptions = new FirebaseOptions.Builder()
                .setDatabaseUrl(databaseUrl)
                .setServiceAccount(new FileInputStream("firebase.json"))
                .build();
        FirebaseApp.initializeApp(firebaseOptions);
        this.databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl(databaseUrl);
        Repo repo= databaseReference.getRepo();
        if (repo != null) {
            FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);
        }
    }

    public GeoFire newTestGeoFire() {
        return new GeoFire(this.databaseReference.child(TestHelpers.randomAlphaNumericString(16)));
    }

    protected void setLoc(GeoFire geoFire, String key, double latitude, double longitude) {
        setLoc(geoFire, key, latitude, longitude, false);
    }

    protected void removeLoc(GeoFire geoFire, String key) {
        removeLoc(geoFire, key, false);
    }

    protected void setValueAndWait(DatabaseReference databaseReference, Object value) {
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
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            Assert.fail("Timeout occured!");
        }
    }

    protected void setLoc(GeoFire geoFire, String key, double latitude, double longitude, boolean wait) {
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
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                Assert.fail("Timeout occured!");
            }
        }
    }

    protected void removeLoc(GeoFire geoFire, String key, boolean wait) {
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
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                Assert.fail("Timeout occured!");
            }
        }
    }

    protected void waitForGeoFireReady(GeoFire geoFire) throws InterruptedException {
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

    @After
    public void teardown() {
        this.databaseReference.setValue(null);
        this.databaseReference = null;
    }
}
