package com.firebase.geofire;

import static com.firebase.geofire.GeoFireIT.DATABASE_URL;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.firebase.geofire.testing.GeoFireTestingRule;
import com.firebase.geofire.testing.GeoQueryDataEventTestListener;
import com.firebase.geofire.testing.GeoQueryEventTestListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GeoQueryIT {
    @Rule public final GeoFireTestingRule geoFireTestingRule = new GeoFireTestingRule(DATABASE_URL);

    @Test
    public void keyEntered() throws InterruptedException {
        GeoFire geoFire = geoFireTestingRule.newTestGeoFire();
        geoFireTestingRule.setLocation(geoFire, "0", 0, 0);
        geoFireTestingRule.setLocation(geoFire, "1", 37.0000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "2", 37.0001, -122.0001);
        geoFireTestingRule.setLocation(geoFire, "3", 37.1000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(37, -122), 0.5);

        GeoQueryEventTestListener testListener = new GeoQueryEventTestListener();
        query.addGeoQueryEventListener(testListener);

        geoFireTestingRule.waitForGeoFireReady(geoFire);

        Set<String> events = new HashSet<>();
        events.add(GeoQueryEventTestListener.keyEntered("1", 37, -122));
        events.add(GeoQueryEventTestListener.keyEntered("2", 37.0001, -122.0001));
        events.add(GeoQueryEventTestListener.keyEntered("4", 37.0002, -121.9998));

        testListener.expectEvents(events);

        query.removeAllListeners();
    }

    @Test
    public void keyExited() throws InterruptedException {
        GeoFire geoFire = geoFireTestingRule.newTestGeoFire();
        geoFireTestingRule.setLocation(geoFire, "0", 0, 0);
        geoFireTestingRule.setLocation(geoFire, "1", 37.0000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "2", 37.0001, -122.0001);
        geoFireTestingRule.setLocation(geoFire, "3", 37.1000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(37, -122), 0.5);
        GeoQueryEventTestListener testListener = new GeoQueryEventTestListener(false, false, true);
        query.addGeoQueryEventListener(testListener);

        geoFireTestingRule.waitForGeoFireReady(geoFire);

        geoFireTestingRule.setLocation(geoFire, "0", 0, 0); // not in query
        geoFireTestingRule.setLocation(geoFire, "1", 0, 0); // exited
        geoFireTestingRule.setLocation(geoFire, "2", 0, 0); // exited
        geoFireTestingRule.setLocation(geoFire, "3", 2, 0, true); // not in query
        geoFireTestingRule.setLocation(geoFire, "0", 3, 0); // not in query
        geoFireTestingRule.setLocation(geoFire, "1", 4, 0); // not in query
        geoFireTestingRule.setLocation(geoFire, "2", 5, 0, true); // not in query

        List<String> events = new LinkedList<>();
        events.add(GeoQueryEventTestListener.keyExited("1"));
        events.add(GeoQueryEventTestListener.keyExited("2"));

        testListener.expectEvents(events);
    }

    @Test
    public void keyMoved() throws InterruptedException {
        GeoFire geoFire = geoFireTestingRule.newTestGeoFire();
        geoFireTestingRule.setLocation(geoFire, "0", 0, 0);
        geoFireTestingRule.setLocation(geoFire, "1", 37.0000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "2", 37.0001, -122.0001);
        geoFireTestingRule.setLocation(geoFire, "3", 37.1000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(37, -122), 0.5);

        GeoQueryEventTestListener testListener = new GeoQueryEventTestListener(false, true, false);
        query.addGeoQueryEventListener(testListener);

        GeoQueryEventTestListener exitListener = new GeoQueryEventTestListener(false, false, true);
        query.addGeoQueryEventListener(exitListener);

        geoFireTestingRule.waitForGeoFireReady(geoFire);

        geoFireTestingRule.setLocation(geoFire, "0", 1, 1); // outside of query
        geoFireTestingRule.setLocation(geoFire, "1", 37.0001, -122.0000); // moved
        geoFireTestingRule.setLocation(geoFire, "2", 37.0001, -122.0001); // location stayed the same
        geoFireTestingRule.setLocation(geoFire, "4", 37.0002, -122.0000); // moved
        geoFireTestingRule.setLocation(geoFire, "3", 37.0000, -122.0000, true); // entered
        geoFireTestingRule.setLocation(geoFire, "3", 37.0003, -122.0003, true); // moved:
        geoFireTestingRule.setLocation(geoFire, "2", 0, 0, true); // exited
        // wait for location to exit
        exitListener.expectEvents(Collections.singletonList(GeoQueryEventTestListener.keyExited("2")));
        geoFireTestingRule.setLocation(geoFire, "2", 37.0000, -122.0000, true); // entered
        geoFireTestingRule.setLocation(geoFire, "2", 37.0001, -122.0001, true); // moved

        List<String> events = new LinkedList<>();
        events.add(GeoQueryEventTestListener.keyMoved("1", 37.0001, -122.0000));
        events.add(GeoQueryEventTestListener.keyMoved("4", 37.0002, -122.0000));
        events.add(GeoQueryEventTestListener.keyMoved("3", 37.0003, -122.0003));
        events.add(GeoQueryEventTestListener.keyMoved("2", 37.0001, -122.0001));

        testListener.expectEvents(events);
    }

    @Test
    public void dataChanged() throws InterruptedException {
        GeoFire geoFire = geoFireTestingRule.newTestGeoFire();
        geoFireTestingRule.setLocation(geoFire, "0", 0, 0);
        geoFireTestingRule.setLocation(geoFire, "1", 37.0000, -122.0001);
        geoFireTestingRule.setLocation(geoFire, "2", 37.0001, -122.0001);
        geoFireTestingRule.setLocation(geoFire, "3", 37.1000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(37, -122), 0.5);

        GeoQueryDataEventTestListener testListener = new GeoQueryDataEventTestListener(
            false, true, true, false);
        query.addGeoQueryDataEventListener(testListener);

        geoFireTestingRule.waitForGeoFireReady(geoFire);

        geoFireTestingRule.setLocation(geoFire, "0", 1, 1, true); // outside of query
        geoFireTestingRule.setLocation(geoFire, "1", 37.0001, -122.0001, true); // moved
        geoFireTestingRule.setLocation(geoFire, "2", 37.0001, -122.0001, true); // location stayed the same
        geoFireTestingRule.setLocation(geoFire, "4", 37.0002, -121.9999, true); // moved

        DatabaseReference childRef = geoFire.getDatabaseRefForKey("2").child("some_child");
        geoFireTestingRule.setValueAndWait(childRef, "some_value"); // data changed

        List<String> events = new LinkedList<>();
        events.add(GeoQueryDataEventTestListener.dataMoved("1", 37.0001, -122.0001));
        events.add(GeoQueryDataEventTestListener.dataChanged("1", 37.0001, -122.0001));

        events.add(GeoQueryDataEventTestListener.dataMoved("4", 37.0002, -121.9999));
        events.add(GeoQueryDataEventTestListener.dataChanged("4", 37.0002, -121.9999));

        events.add(GeoQueryDataEventTestListener.dataChanged("2", 37.0001, -122.0001));

        testListener.expectEvents(events);
    }

    @Test
    public void subQueryTriggersKeyMoved() throws InterruptedException {
        GeoFire geoFire = geoFireTestingRule.newTestGeoFire();
        geoFireTestingRule.setLocation(geoFire, "0", 1, 1, true);
        geoFireTestingRule.setLocation(geoFire, "1", -1, -1, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(0, 0), 1000);
        GeoQueryEventTestListener testListener = new GeoQueryEventTestListener(false, true, true);
        query.addGeoQueryEventListener(testListener);

        geoFireTestingRule.waitForGeoFireReady(geoFire);

        geoFireTestingRule.setLocation(geoFire, "0", -1, -1);
        geoFireTestingRule.setLocation(geoFire, "1", 1, 1);

        Set<String> events = new HashSet<>();
        events.add(GeoQueryEventTestListener.keyMoved("0", -1, -1));
        events.add(GeoQueryEventTestListener.keyMoved("1", 1, 1));

        testListener.expectEvents(events);
    }

    @Test
    public void removeSingleObserver() throws InterruptedException {
        GeoFire geoFire = geoFireTestingRule.newTestGeoFire();
        geoFireTestingRule.setLocation(geoFire, "0", 0, 0);
        geoFireTestingRule.setLocation(geoFire, "1", 37.0000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "2", 37.0001, -122.0001);
        geoFireTestingRule.setLocation(geoFire, "3", 37.1000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(37.0, -122), 1);

        GeoQueryEventTestListener testListenerRemoved = new GeoQueryEventTestListener(true, true, true);
        query.addGeoQueryEventListener(testListenerRemoved);

        GeoQueryEventTestListener testListenerRemained = new GeoQueryEventTestListener(true, true, true);
        query.addGeoQueryEventListener(testListenerRemained);

        Set<String> addedEvents = new HashSet<>();
        addedEvents.add(GeoQueryEventTestListener.keyEntered("1", 37, -122));
        addedEvents.add(GeoQueryEventTestListener.keyEntered("2", 37.0001, -122.0001));
        addedEvents.add(GeoQueryEventTestListener.keyEntered("4", 37.0002, -121.9998));

        testListenerRemained.expectEvents(addedEvents);
        testListenerRemained.expectEvents(addedEvents);

        query.removeGeoQueryEventListener(testListenerRemoved);

        geoFireTestingRule.setLocation(geoFire, "0", 37, -122); // entered
        geoFireTestingRule.setLocation(geoFire, "1", 0, 0); // exited
        geoFireTestingRule.setLocation(geoFire, "2", 37, -122.0001); // moved

        Set<String> furtherEvents = new HashSet<>(addedEvents);
        furtherEvents.add(GeoQueryEventTestListener.keyEntered("0", 37, -122)); // entered
        furtherEvents.add(GeoQueryEventTestListener.keyExited("1")); // exited
        furtherEvents.add(GeoQueryEventTestListener.keyMoved("2", 37.0000, -122.0001)); // moved

        testListenerRemained.expectEvents(furtherEvents);
        testListenerRemoved.expectEvents(addedEvents);
    }

    @Test
    public void removeAllObservers() throws InterruptedException {
        GeoFire geoFire = geoFireTestingRule.newTestGeoFire();
        geoFireTestingRule.setLocation(geoFire, "0", 0, 0);
        geoFireTestingRule.setLocation(geoFire, "1", 37.0000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "2", 37.0001, -122.0001);
        geoFireTestingRule.setLocation(geoFire, "3", 37.1000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(37.0, -122), 1);

        GeoQueryEventTestListener testListenerRemoved = new GeoQueryEventTestListener(true, true, true);
        query.addGeoQueryEventListener(testListenerRemoved);

        GeoQueryEventTestListener testListenerRemained = new GeoQueryEventTestListener(true, true, true);
        query.addGeoQueryEventListener(testListenerRemained);

        Set<String> addedEvents = new HashSet<>();
        addedEvents.add(GeoQueryEventTestListener.keyEntered("1", 37, -122));
        addedEvents.add(GeoQueryEventTestListener.keyEntered("2", 37.0001, -122.0001));
        addedEvents.add(GeoQueryEventTestListener.keyEntered("4", 37.0002, -121.9998));

        testListenerRemained.expectEvents(addedEvents);
        testListenerRemained.expectEvents(addedEvents);

        query.removeGeoQueryEventListener(testListenerRemoved);
        query.removeAllListeners();

        geoFireTestingRule.setLocation(geoFire, "0", 37, -122); // entered
        geoFireTestingRule.setLocation(geoFire, "1", 0, 0); // exited
        geoFireTestingRule.setLocation(geoFire, "2", 37, -122.0001, true); // moved

        testListenerRemained.expectEvents(addedEvents);
        testListenerRemoved.expectEvents(addedEvents);
    }

    @Test
    public void readyListener() throws InterruptedException {
        GeoFire geoFire = geoFireTestingRule.newTestGeoFire();
        geoFireTestingRule.setLocation(geoFire, "0", 0, 0);
        geoFireTestingRule.setLocation(geoFire, "1", 37.0000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "2", 37.0001, -122.0001);
        geoFireTestingRule.setLocation(geoFire, "3", 37.1000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "4", 37.0002, -121.9998, true);

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
                done[0] = true;
                semaphore.release();
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                fail("onGeoQueryError: " + error.toString());
            }
        });

        assertTrue(semaphore.tryAcquire(geoFireTestingRule.timeout, TimeUnit.SECONDS));
        assertTrue("GeoQuery not ready, test timed out.", done[0]);
        // wait for any further events to fire
        Thread.sleep(250);
        assertFalse("Key entered after ready event occurred!", failed[0]);
    }

    @Test
    public void readyListenerAfterReady() throws InterruptedException {
        GeoFire geoFire = geoFireTestingRule.newTestGeoFire();
        geoFireTestingRule.setLocation(geoFire, "0", 0, 0);
        geoFireTestingRule.setLocation(geoFire, "1", 37.0000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "2", 37.0001, -122.0001);
        geoFireTestingRule.setLocation(geoFire, "3", 37.1000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "4", 37.0002, -121.9998, true);

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

        assertTrue(semaphore.tryAcquire(geoFireTestingRule.timeout, TimeUnit.SECONDS));

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
        assertTrue(semaphore.tryAcquire(10, TimeUnit.MILLISECONDS));
    }

    @Test
    public void readyAfterUpdateCriteria() throws InterruptedException {
        GeoFire geoFire = geoFireTestingRule.newTestGeoFire();
        geoFireTestingRule.setLocation(geoFire, "0", 0, 0);
        geoFireTestingRule.setLocation(geoFire, "1", 37.0000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "2", 37.0001, -122.0001);
        geoFireTestingRule.setLocation(geoFire, "3", 37.1000, -122.0000);
        geoFireTestingRule.setLocation(geoFire, "4", 37.0002, -121.9998, true);

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

        assertTrue(semaphore.tryAcquire(geoFireTestingRule.timeout, TimeUnit.SECONDS));
        query.setCenter(new GeoLocation(0,0));
        assertTrue(semaphore.tryAcquire(geoFireTestingRule.timeout, TimeUnit.SECONDS));
        assertTrue(done[0]);
    }
}
