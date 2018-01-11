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

public final class GeoFireTestingRule extends TestWatcher {

    static final long TIMEOUT_SECONDS = 5;

    private static final String ALPHA_NUM_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    private static final String DATABASE_URL = "https://geofiretest-8d811.firebaseio.com/";
    private static final String SERVICE_ACCOUNT_CREDENTIALS = "service-account.json";

    private DatabaseReference databaseReference;

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
                    .setDatabaseUrl(DATABASE_URL)
                    .setCredentials(credentials)
                    .build();
            FirebaseApp.initializeApp(firebaseOptions);

            System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
        }
        this.databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl(DATABASE_URL);
    }

    public GeoFire newTestGeoFire() {
        return new GeoFire(databaseReference.child(randomAlphaNumericString(16)));
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
            assertNull(futureError.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            fail("Timeout occured!");
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
                assertNull(futureError.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                fail("Timeout occured!");
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
                assertNull(futureError.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                fail("Timeout occured!");
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
                fail("Firebase error: " + databaseError);
            }
        });

        assertTrue("Timeout occured!", semaphore.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS));
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
