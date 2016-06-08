package com.firebase.geofire;

import com.firebase.geofire.util.ReadFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@RunWith(JUnit4.class)
public class GeoFireTest extends RealDataTest {
    @Rule
    public org.junit.rules.ExpectedException exception = ExpectedException.none();

    @Test
    public void geoFireSetsLocations() throws InterruptedException, ExecutionException, TimeoutException {
        GeoFire geoFire = newTestGeoFire();
        setLoc(geoFire, "loc1", 0.1, 0.1);
        setLoc(geoFire, "loc2", 50.1, 50.1);
        setLoc(geoFire, "loc3", -89.1, -89.1, true);

        Future<Object> future = new ReadFuture(geoFire.getDatabaseReference());
        Map<String, Object> expected = new HashMap<>();
        expected.put("loc1", new HashMap<String, Object>() {{
            put("l", Arrays.asList(0.1, 0.1));
            put("g", "s000d60yd1");
        }});
        expected.put("loc2", new HashMap<String, Object>() {{
            put("l", Arrays.asList(50.1, 50.1));
            put("g", "v0gth03tws");
        }});
        expected.put("loc3", new HashMap<String, Object>() {{
            put("l", Arrays.asList(-89.1, -89.1));
            put("g", "400th7z6gs");
        }});
        Object result = future.get(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertEquals(expected, ((DataSnapshot)result).getValue());
    }

    @Test
    public void getLocationReturnsCorrectLocation() throws InterruptedException, ExecutionException, TimeoutException {
        GeoFire geoFire = newTestGeoFire();

        TestCallback testCallback1 = new TestCallback();
        geoFire.getLocation("loc1", testCallback1);
        Assert.assertEquals(TestCallback.noLocation("loc1"), testCallback1.getCallbackValue());

        TestCallback testCallback2 = new TestCallback();
        setLoc(geoFire, "loc1", 0, 0, true);
        geoFire.getLocation("loc1", testCallback2);
        Assert.assertEquals(TestCallback.location("loc1", 0, 0), testCallback2.getCallbackValue());

        TestCallback testCallback3 = new TestCallback();
        setLoc(geoFire, "loc2", 1, 1, true);
        geoFire.getLocation("loc2", testCallback3);
        Assert.assertEquals(TestCallback.location("loc2", 1, 1), testCallback3.getCallbackValue());

        TestCallback testCallback4 = new TestCallback();
        setLoc(geoFire, "loc1", 5, 5, true);
        geoFire.getLocation("loc1", testCallback4);
        Assert.assertEquals(TestCallback.location("loc1", 5, 5), testCallback4.getCallbackValue());

        TestCallback testCallback5 = new TestCallback();
        removeLoc(geoFire, "loc1");
        geoFire.getLocation("loc1", testCallback5);
        Assert.assertEquals(TestCallback.noLocation("loc1"), testCallback5.getCallbackValue());
    }

    @Test
    public void getLocationOnWrongDataReturnsError() throws InterruptedException {
        GeoFire geoFire = newTestGeoFire();
        setValueAndWait(geoFire.getDatabaseRefForKey("loc1"), "NaN");

        final Semaphore semaphore = new Semaphore(0);
        geoFire.getLocation("loc1", new LocationCallback() {
            @Override
            public void onLocationResult(String key, GeoLocation location) {
                Assert.fail("This should not be a valid location!");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                semaphore.release();
            }
        });
        semaphore.tryAcquire(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS);

        setValueAndWait(geoFire.getDatabaseRefForKey("loc2"), new HashMap<String, Object>() {{
           put("l", 10);
           put("g", "abc");
        }});

        geoFire.getLocation("loc2", new LocationCallback() {
            @Override
            public void onLocationResult(String key, GeoLocation location) {
                Assert.fail("This should not be a valid location!");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                semaphore.release();
            }
        });
        semaphore.tryAcquire(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void invalidCoordinatesThrowException() {
        GeoFire geoFire = newTestGeoFire();
        try {
            geoFire.setLocation("test", new GeoLocation(-91, 90));
            Assert.fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException e) {
        }
        try {
            geoFire.setLocation("test", new GeoLocation(0, -180.1));
            Assert.fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException e) {
        }
        try {
            geoFire.setLocation("test", new GeoLocation(0, 181.1));
            Assert.fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void locationWorksWithLongs() throws InterruptedException, ExecutionException, TimeoutException {
        GeoFire geoFire = newTestGeoFire();
        DatabaseReference databaseReference = geoFire.getDatabaseRefForKey("loc");

        final Semaphore semaphore = new Semaphore(0);
        databaseReference.setValue(new HashMap<String, Object>() {{
            put("l", Arrays.asList(1L, 2L));
            put("g", "7zzzzzzzzz"); // this is wrong but we don't care in this test
        }}, "7zzzzzzzzz", new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                semaphore.release();
            }
        });
        semaphore.tryAcquire(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS);

        TestCallback testCallback = new TestCallback();
        geoFire.getLocation("loc", testCallback);
        Assert.assertEquals(TestCallback.location("loc", 1, 2), testCallback.getCallbackValue());
    }
}
