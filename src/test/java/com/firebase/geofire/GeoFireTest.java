package com.firebase.geofire;

import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.firebase.geofire.util.ReadFuture;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(JUnit4.class)
public class GeoFireTest extends RealDataTest {
    @Rule
    public org.junit.rules.ExpectedException exception = ExpectedException.none();

    @Test
    public void geoFireSetsLocations() throws InterruptedException, ExecutionException, TimeoutException {
        GeoFire geoFire = newTestGeoFire();
        setLoc(geoFire, "loc1", 0, 0);
        setLoc(geoFire, "loc2", 50, 50);
        setLoc(geoFire, "loc3", -90, -90, true);

        Future<Object> future = new ReadFuture(geoFire.getFirebase());
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("loc1", new HashMap<String, Object>() {{
            put("l", Arrays.asList(0.0, 0.0));
            put("g", "7zzzzzzzzz");
        }});
        expected.put("loc2", new HashMap<String, Object>() {{
            put("l", Arrays.asList(50.0, 50.0));
            put("g", "v0gs3y0zh7");
        }});
        expected.put("loc3", new HashMap<String, Object>() {{
            put("l", Arrays.asList(-90.0, -90.0));
            put("g", "1bpbpbpbpb");
        }});
        Object result = future.get(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertEquals(expected, ((DataSnapshot)result).getValue());
    }

    @Test
    public void locationListenerFires() throws InterruptedException, ExecutionException, TimeoutException {
        GeoFire geoFire = newTestGeoFire();
        LocationEventTestListener testListener = new LocationEventTestListener();
        geoFire.addLocationEventListener("loc1", testListener);
        setLoc(geoFire, "loc1", 0, 0, true); // should fire
        setLoc(geoFire, "loc1", 0, 0, true); // should not fire
        setLoc(geoFire, "loc2", 1, 1, true); // should not fire
        setLoc(geoFire, "loc1", 2, 1, true); // should fire
        setLoc(geoFire, "loc1", 0, 0, true); // should fire
        removeLoc(geoFire, "loc1", true); // should fire
        setLoc(geoFire, "loc1", 0, 0, true); // should fire

        List<String> expected = new ArrayList<String>();
        expected.add(LocationEventTestListener.locationChanged("loc1", 0, 0));
        expected.add(LocationEventTestListener.locationChanged("loc1", 2, 1));
        expected.add(LocationEventTestListener.locationChanged("loc1", 0, 0));
        expected.add(LocationEventTestListener.locationRemoved("loc1"));
        expected.add(LocationEventTestListener.locationChanged("loc1", 0, 0));
        testListener.expectEvents(expected);
    }

    @Test
    public void removeSingleListener() throws InterruptedException, ExecutionException, TimeoutException {
        GeoFire geoFire = newTestGeoFire();
        LocationEventTestListener testListener1a = new LocationEventTestListener();
        LocationEventTestListener testListener1b = new LocationEventTestListener();
        LocationEventTestListener testListener2 = new LocationEventTestListener();

        setLoc(geoFire, "loc1", 1, 2); // should not fire
        setLoc(geoFire, "loc2", 2, 1, true); // should not fire

        List<String> initial1 = new ArrayList<String>();
        initial1.add(LocationEventTestListener.locationChanged("loc1", 1, 2));
        List<String> initial2 = new ArrayList<String>();
        initial2.add(LocationEventTestListener.locationChanged("loc2", 2, 1));

        geoFire.addLocationEventListener("loc1", testListener1a);
        geoFire.addLocationEventListener("loc1", testListener1b);
        geoFire.addLocationEventListener("loc2", testListener2);

        // wait for initial events to fire
        testListener1a.expectEvents(initial1);
        testListener1b.expectEvents(initial1);
        testListener2.expectEvents(initial2);

        setLoc(geoFire, "loc1", 0, 0, true); // should not fire
        setLoc(geoFire, "loc2", 1, 1, true); // should not fire
        setLoc(geoFire, "loc1", 2, 1, true); // should fire
        geoFire.removeEventListener(testListener1a);
        geoFire.removeEventListener(testListener2);

        setLoc(geoFire, "loc1", 0, 0, true); // should fire
        setLoc(geoFire, "loc2", 2, 3, true); // should fire
        removeLoc(geoFire, "loc1", true); // should fire
        removeLoc(geoFire, "loc2", true); // should fire

        List<String> expected1a = new ArrayList<String>(initial1);
        expected1a.add(LocationEventTestListener.locationChanged("loc1", 0, 0));
        expected1a.add(LocationEventTestListener.locationChanged("loc1", 2, 1));
        List<String> expected1b = new ArrayList<String>(expected1a);
        expected1b.add(LocationEventTestListener.locationChanged("loc1", 0, 0));
        expected1b.add(LocationEventTestListener.locationRemoved("loc1"));
        List<String> expected2 = new ArrayList<String>(initial2);
        expected2.add(LocationEventTestListener.locationChanged("loc2", 1, 1));

        testListener1a.expectEvents(expected1a);
        testListener1b.expectEvents(expected1b);
        testListener2.expectEvents(expected2);
    }

    @Test
    public void removeAllListeners() throws InterruptedException, ExecutionException, TimeoutException {
        GeoFire geoFire = newTestGeoFire();
        LocationEventTestListener testListener1a = new LocationEventTestListener();
        LocationEventTestListener testListener1b = new LocationEventTestListener();
        LocationEventTestListener testListener2 = new LocationEventTestListener();

        setLoc(geoFire, "loc1", 1, 2); // should not fire
        setLoc(geoFire, "loc2", 2, 1, true); // should not fire

        List<String> initial1 = new ArrayList<String>();
        initial1.add(LocationEventTestListener.locationChanged("loc1", 1, 2));
        List<String> initial2 = new ArrayList<String>();
        initial2.add(LocationEventTestListener.locationChanged("loc2", 2, 1));

        geoFire.addLocationEventListener("loc1", testListener1a);
        geoFire.addLocationEventListener("loc1", testListener1b);
        geoFire.addLocationEventListener("loc2", testListener2);

        // wait for initial events to fire
        testListener1a.expectEvents(initial1);
        testListener1b.expectEvents(initial1);
        testListener2.expectEvents(initial2);

        setLoc(geoFire, "loc1", 0, 0, true); // should not fire
        setLoc(geoFire, "loc2", 1, 1, true); // should not fire
        setLoc(geoFire, "loc1", 2, 1, true); // should fire
        geoFire.removeAllEventListeners();


        setLoc(geoFire, "loc1", 0, 0, true); // should fire
        setLoc(geoFire, "loc2", 2, 3, true); // should fire
        removeLoc(geoFire, "loc1", true); // should fire
        removeLoc(geoFire, "loc2", true); // should fire

        List<String> expected1 = new ArrayList<String>(initial1);
        expected1.add(LocationEventTestListener.locationChanged("loc1", 0, 0));
        expected1.add(LocationEventTestListener.locationChanged("loc1", 2, 1));
        List<String> expected2 = new ArrayList<String>(initial2);
        expected2.add(LocationEventTestListener.locationChanged("loc2", 1, 1));

        testListener1a.expectEvents(expected1);
        testListener1b.expectEvents(expected1);
        testListener2.expectEvents(expected2);
    }

    @Test
    public void invalidCoordinatesThrowException() {
        GeoFire geoFire = newTestGeoFire();
        try {
            geoFire.setLocation("test", -91, 90);
            Assert.fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException e) {
        }
        try {
            geoFire.setLocation("test", 0, -180.1);
            Assert.fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException e) {
        }
        try {
            geoFire.setLocation("test", 0, 181.1);
            Assert.fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException e) {
        }
    }
}
