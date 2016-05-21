package com.firebase.geofire;

import com.google.firebase.database.DatabaseError;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RunWith(JUnit4.class)
public class GeoQueryTest extends RealDataTest {

    @Test
    public void keyEntered() throws InterruptedException {
        GeoFire geoFire = newTestGeoFire();
        setLoc(geoFire, "0", 0, 0);
        setLoc(geoFire, "1", 37.0000, -122.0000);
        setLoc(geoFire, "2", 37.0001, -122.0001);
        setLoc(geoFire, "3", 37.1000, -122.0000);
        setLoc(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(37, -122), 0.5);

        GeoQueryEventTestListener testListener = new GeoQueryEventTestListener();
        query.addGeoQueryEventListener(testListener);

        waitForGeoFireReady(geoFire);

        Set<String> events = new HashSet<String>();
        events.add(GeoQueryEventTestListener.keyEntered("1", 37, -122));
        events.add(GeoQueryEventTestListener.keyEntered("2", 37.0001, -122.0001));
        events.add(GeoQueryEventTestListener.keyEntered("4", 37.0002, -121.9998));

        testListener.expectEvents(events);

        query.removeAllListeners();
    }

    @Test
    public void keyExited() throws InterruptedException {
        GeoFire geoFire = newTestGeoFire();
        setLoc(geoFire, "0", 0, 0);
        setLoc(geoFire, "1", 37.0000, -122.0000);
        setLoc(geoFire, "2", 37.0001, -122.0001);
        setLoc(geoFire, "3", 37.1000, -122.0000);
        setLoc(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(37, -122), 0.5);
        GeoQueryEventTestListener testListener = new GeoQueryEventTestListener(false, false, true);
        query.addGeoQueryEventListener(testListener);

        waitForGeoFireReady(geoFire);

        setLoc(geoFire, "0", 0, 0); // not in query
        setLoc(geoFire, "1", 0, 0); // exited
        setLoc(geoFire, "2", 0, 0); // exited
        setLoc(geoFire, "3", 2, 0, true); // not in query
        setLoc(geoFire, "0", 3, 0); // not in query
        setLoc(geoFire, "1", 4, 0); // not in query
        setLoc(geoFire, "2", 5, 0, true); // not in query

        List<String> events = new LinkedList<String>();
        events.add(GeoQueryEventTestListener.keyExited("1"));
        events.add(GeoQueryEventTestListener.keyExited("2"));

        testListener.expectEvents(events);
    }

    @Test
    public void keyMoved() throws InterruptedException {
        GeoFire geoFire = newTestGeoFire();
        setLoc(geoFire, "0", 0, 0);
        setLoc(geoFire, "1", 37.0000, -122.0000);
        setLoc(geoFire, "2", 37.0001, -122.0001);
        setLoc(geoFire, "3", 37.1000, -122.0000);
        setLoc(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(37, -122), 0.5);

        GeoQueryEventTestListener testListener = new GeoQueryEventTestListener(false, true, false);
        query.addGeoQueryEventListener(testListener);

        GeoQueryEventTestListener exitListener = new GeoQueryEventTestListener(false, false, true);
        query.addGeoQueryEventListener(exitListener);

        waitForGeoFireReady(geoFire);

        setLoc(geoFire, "0", 1, 1); // outside of query
        setLoc(geoFire, "1", 37.0001, -122.0000); // moved
        setLoc(geoFire, "2", 37.0001, -122.0001); // location stayed the same
        setLoc(geoFire, "4", 37.0002, -122.0000); // moved
        setLoc(geoFire, "3", 37.0000, -122.0000, true); // entered
        setLoc(geoFire, "3", 37.0003, -122.0003, true); // moved:
        setLoc(geoFire, "2", 0, 0, true); // exited
        // wait for location to exit
        exitListener.expectEvents(Arrays.asList(GeoQueryEventTestListener.keyExited("2")));
        setLoc(geoFire, "2", 37.0000, -122.0000, true); // entered
        setLoc(geoFire, "2", 37.0001, -122.0001, true); // moved

        List<String> events = new LinkedList<String>();
        events.add(GeoQueryEventTestListener.keyMoved("1", 37.0001, -122.0000));
        events.add(GeoQueryEventTestListener.keyMoved("4", 37.0002, -122.0000));
        events.add(GeoQueryEventTestListener.keyMoved("3", 37.0003, -122.0003));
        events.add(GeoQueryEventTestListener.keyMoved("2", 37.0001, -122.0001));

        testListener.expectEvents(events);
    }

    @Test
    public void subQueryTriggersKeyMoved() throws InterruptedException {
        GeoFire geoFire = newTestGeoFire();
        setLoc(geoFire, "0", 1, 1, true);
        setLoc(geoFire, "1", -1, -1, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(0, 0), 1000);
        GeoQueryEventTestListener testListener = new GeoQueryEventTestListener(false, true, true);
        query.addGeoQueryEventListener(testListener);

        waitForGeoFireReady(geoFire);

        setLoc(geoFire, "0", -1, -1);
        setLoc(geoFire, "1", 1, 1);

        Set<String> events = new HashSet<String>();
        events.add(GeoQueryEventTestListener.keyMoved("0", -1, -1));
        events.add(GeoQueryEventTestListener.keyMoved("1", 1, 1));

        testListener.expectEvents(events);
    }

    @Test
    public void removeSingleObserver() throws InterruptedException {
        GeoFire geoFire = newTestGeoFire();
        setLoc(geoFire, "0", 0, 0);
        setLoc(geoFire, "1", 37.0000, -122.0000);
        setLoc(geoFire, "2", 37.0001, -122.0001);
        setLoc(geoFire, "3", 37.1000, -122.0000);
        setLoc(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(37.0, -122), 1);

        GeoQueryEventTestListener testListenerRemoved = new GeoQueryEventTestListener(true, true, true);
        query.addGeoQueryEventListener(testListenerRemoved);

        GeoQueryEventTestListener testListenerRemained = new GeoQueryEventTestListener(true, true, true);
        query.addGeoQueryEventListener(testListenerRemained);

        Set<String> addedEvents = new HashSet<String>();
        addedEvents.add(GeoQueryEventTestListener.keyEntered("1", 37, -122));
        addedEvents.add(GeoQueryEventTestListener.keyEntered("2", 37.0001, -122.0001));
        addedEvents.add(GeoQueryEventTestListener.keyEntered("4", 37.0002, -121.9998));

        testListenerRemained.expectEvents(addedEvents);
        testListenerRemained.expectEvents(addedEvents);

        query.removeGeoQueryEventListener(testListenerRemoved);

        setLoc(geoFire, "0", 37, -122); // entered
        setLoc(geoFire, "1", 0, 0); // exited
        setLoc(geoFire, "2", 37, -122.0001); // moved

        Set<String> furtherEvents = new HashSet<String>(addedEvents);
        furtherEvents.add(GeoQueryEventTestListener.keyEntered("0", 37, -122)); // entered
        furtherEvents.add(GeoQueryEventTestListener.keyExited("1")); // exited
        furtherEvents.add(GeoQueryEventTestListener.keyMoved("2", 37.0000, -122.0001)); // moved

        testListenerRemained.expectEvents(furtherEvents);
        testListenerRemoved.expectEvents(addedEvents);
    }

    @Test
    public void removeAllObservers() throws InterruptedException {
        GeoFire geoFire = newTestGeoFire();
        setLoc(geoFire, "0", 0, 0);
        setLoc(geoFire, "1", 37.0000, -122.0000);
        setLoc(geoFire, "2", 37.0001, -122.0001);
        setLoc(geoFire, "3", 37.1000, -122.0000);
        setLoc(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(37.0, -122), 1);

        GeoQueryEventTestListener testListenerRemoved = new GeoQueryEventTestListener(true, true, true);
        query.addGeoQueryEventListener(testListenerRemoved);

        GeoQueryEventTestListener testListenerRemained = new GeoQueryEventTestListener(true, true, true);
        query.addGeoQueryEventListener(testListenerRemained);

        Set<String> addedEvents = new HashSet<String>();
        addedEvents.add(GeoQueryEventTestListener.keyEntered("1", 37, -122));
        addedEvents.add(GeoQueryEventTestListener.keyEntered("2", 37.0001, -122.0001));
        addedEvents.add(GeoQueryEventTestListener.keyEntered("4", 37.0002, -121.9998));

        testListenerRemained.expectEvents(addedEvents);
        testListenerRemained.expectEvents(addedEvents);

        query.removeGeoQueryEventListener(testListenerRemoved);
        query.removeAllListeners();

        setLoc(geoFire, "0", 37, -122); // entered
        setLoc(geoFire, "1", 0, 0); // exited
        setLoc(geoFire, "2", 37, -122.0001, true); // moved

        testListenerRemained.expectEvents(addedEvents);
        testListenerRemoved.expectEvents(addedEvents);
    }

    @Test
    public void readyListener() throws InterruptedException {
        GeoFire geoFire = newTestGeoFire();
        setLoc(geoFire, "0", 0, 0);
        setLoc(geoFire, "1", 37.0000, -122.0000);
        setLoc(geoFire, "2", 37.0001, -122.0001);
        setLoc(geoFire, "3", 37.1000, -122.0000);
        setLoc(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(37.0, -122), 1);
        final boolean[] done = new boolean[1];
        final boolean[] failed = new boolean[1];
        final Semaphore semaphore = new Semaphore(0);
        query.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (done[0]) {
                    failed[0] = true;
                }
            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            @Override
            public void onGeoQueryReady() {
                semaphore.release();
                done[0] = true;
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });

        Assert.assertTrue(semaphore.tryAcquire(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));
        Assert.assertTrue(done[0]);
        // wait for any further events to fire
        Thread.sleep(250);
        Assert.assertFalse("Key entered after ready event occurred!", failed[0]);
    }

    @Test
    public void readyListenerAfterReady() throws InterruptedException {
        GeoFire geoFire = newTestGeoFire();
        setLoc(geoFire, "0", 0, 0);
        setLoc(geoFire, "1", 37.0000, -122.0000);
        setLoc(geoFire, "2", 37.0001, -122.0001);
        setLoc(geoFire, "3", 37.1000, -122.0000);
        setLoc(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(37.0, -122), 1);

        final Semaphore semaphore = new Semaphore(0);
        query.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            @Override
            public void onGeoQueryReady() {
                semaphore.release();
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });

        Assert.assertTrue(semaphore.tryAcquire(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));

        query.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            @Override
            public void onGeoQueryReady() {
                semaphore.release();
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });
        Assert.assertTrue(semaphore.tryAcquire(10, TimeUnit.MILLISECONDS));
    }

    @Test
    public void readyAfterUpdateCriteria() throws InterruptedException {
        GeoFire geoFire = newTestGeoFire();
        setLoc(geoFire, "0", 0, 0);
        setLoc(geoFire, "1", 37.0000, -122.0000);
        setLoc(geoFire, "2", 37.0001, -122.0001);
        setLoc(geoFire, "3", 37.1000, -122.0000);
        setLoc(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(37.0, -122), 1);
        final boolean[] done = new boolean[1];
        final Semaphore semaphore = new Semaphore(0);
        final int[] readyCount = new int[1];
        query.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (key.equals("0")) {
                    done[0] = true;
                }
            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            @Override
            public void onGeoQueryReady() {
                semaphore.release();
                readyCount[0]++;
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

        Assert.assertTrue(semaphore.tryAcquire(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));
        query.setCenter(new GeoLocation(0,0));
        Assert.assertTrue(semaphore.tryAcquire(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));
        Assert.assertTrue(done[0]);
    }
}
