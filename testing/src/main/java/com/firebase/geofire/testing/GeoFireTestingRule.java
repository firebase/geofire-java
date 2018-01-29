package com.firebase.geofire.testing;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.impl.SimpleLogger;

/**
 * This is a JUnit rule that can be used for hooking up Geofire with a real database instance.
 */
public final class GeoFireTestingRule extends TestWatcher {

    static final long DEFAULT_TIMEOUT_SECONDS = 5;

    private static final String ALPHA_NUM_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    private static final String SERVICE_ACCOUNT_CREDENTIALS = "service-account.json";

    private DatabaseReference databaseReference;

    public final String databaseUrl;

    /** Timeout in seconds. */
    public final long timeout;

    public GeoFireTestingRule(final String databaseUrl) {
        this(databaseUrl, DEFAULT_TIMEOUT_SECONDS);
    }

    public GeoFireTestingRule(final String databaseUrl, final long timeout) {
        this.databaseUrl = databaseUrl;
        this.timeout = timeout;
    }

    @Override
    public void starting(Description description) {
        if (FirebaseApp.getApps().isEmpty()) {
            final GoogleCredentials credentials;

            try {
                credentials = GoogleCredentials.fromStream(new FileInputStream(SERVICE_ACCOUNT_CREDENTIALS));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            FirebaseOptions firebaseOptions = new FirebaseOptions.Builder()
                    .setDatabaseUrl(databaseUrl)
                    .setCredentials(credentials)
                    .build();
            FirebaseApp.initializeApp(firebaseOptions);

            System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
        }
        this.databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl(databaseUrl);
    }

    /** This will return you a new Geofire instance that can be used for testing. */
    public GeoFire newTestGeoFire() {
        return new GeoFire(databaseReference.child(randomAlphaNumericString(16)));
    }

    /**
     * Sets a given location key from the latitude and longitude on the provided Geofire instance.
     * This operation will run asychronously.
     */
    public void setLocation(GeoFire geoFire, String key, double latitude, double longitude) {
        setLocation(geoFire, key, latitude, longitude, false);
    }

    /**
     * Removes a location on the provided Geofire instance.
     * This operation will run asychronously.
     */
    public void removeLocation(GeoFire geoFire, String key) {
        removeLocation(geoFire, key, false);
    }

    /** Sets the value on the given databaseReference and waits until the operation has successfully finished. */
    public void setValueAndWait(DatabaseReference databaseReference, Object value) {
        final SimpleFuture<DatabaseError> futureError = new SimpleFuture<DatabaseError>();
        databaseReference.setValue(value, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                futureError.put(databaseError);
            }
        });
        try {
            assertNull(futureError.get(timeout, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            fail("Timeout occured!");
        }
    }

    /**
     * Sets a given location key from the latitude and longitude on the provided Geofire instance.
     * This operation will run asychronously or synchronously depending on the wait boolean.
     */
    public void setLocation(GeoFire geoFire, String key, double latitude, double longitude, boolean wait) {
        final SimpleFuture<DatabaseError> futureError = new SimpleFuture<DatabaseError>();
        geoFire.setLocation(key, new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                futureError.put(error);
            }
        });
        if (wait) {
            try {
                assertNull(futureError.get(timeout, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                fail("Timeout occured!");
            }
        }
    }

    /**
     * Removes a location on the provided Geofire instance.
     * This operation will run asychronously or synchronously depending on the wait boolean.
     */
    public void removeLocation(GeoFire geoFire, String key, boolean wait) {
        final SimpleFuture<DatabaseError> futureError = new SimpleFuture<DatabaseError>();
        geoFire.removeLocation(key, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                futureError.put(error);
            }
        });
        if (wait) {
            try {
                assertNull(futureError.get(timeout, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                fail("Timeout occured!");
            }
        }
    }

    /** This lets you blockingly wait until the onGeoFireReady was fired on the provided Geofire instance. */
    public void waitForGeoFireReady(GeoFire geoFire) throws InterruptedException {
        final Semaphore semaphore = new Semaphore(0);
        geoFire.getDatabaseReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                semaphore.release();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                fail("Firebase error: " + databaseError);
            }
        });

        assertTrue("Timeout occured!", semaphore.tryAcquire(timeout, TimeUnit.SECONDS));
    }

    @Override
    public void finished(Description description) {
        this.databaseReference.setValueAsync(null);
        this.databaseReference = null;
    }

    private static String randomAlphaNumericString(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++ ) {
            sb.append(ALPHA_NUM_CHARS.charAt(random.nextInt(ALPHA_NUM_CHARS.length())));
        }
        return sb.toString();
    }
}
