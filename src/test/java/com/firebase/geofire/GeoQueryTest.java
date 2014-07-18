package com.firebase.geofire;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

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

        GeoQuery query = geoFire.queryAtLocation(37, -122, 500);

        GeoQueryEventTestListener testListener = new GeoQueryEventTestListener();
        query.addGeoQueryEventListener(testListener);

        waitForGeoFireReady(geoFire);

        Set<String> events = new HashSet<String>();
        events.add(GeoQueryEventTestListener.keyEntered("1", 37, -122));
        events.add(GeoQueryEventTestListener.keyEntered("2", 37.0001, -122.0001));
        events.add(GeoQueryEventTestListener.keyEntered("4", 37.0002, -121.9998));

        testListener.expectEvents(events);

        query.removeAllEventListeners();
    }

    @Test
    public void keyExited() throws InterruptedException {
        GeoFire geoFire = newTestGeoFire();
        setLoc(geoFire, "0", 0, 0);
        setLoc(geoFire, "1", 37.0000, -122.0000);
        setLoc(geoFire, "2", 37.0001, -122.0001);
        setLoc(geoFire, "3", 37.1000, -122.0000);
        setLoc(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(37, -122, 500);
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

        GeoQuery query = geoFire.queryAtLocation(37, -122, 500);

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

        GeoQuery query = geoFire.queryAtLocation(0, 0, 1e6);
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
    public void testRemoveSingleObserver() throws InterruptedException {
        GeoFire geoFire = newTestGeoFire();
        setLoc(geoFire, "0", 0, 0);
        setLoc(geoFire, "1", 37.0000, -122.0000);
        setLoc(geoFire, "2", 37.0001, -122.0001);
        setLoc(geoFire, "3", 37.1000, -122.0000);
        setLoc(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(37.0, -122, 1000);

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

        query.removeEventListener(testListenerRemoved);

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
    public void testRemoveAllObservers() throws InterruptedException {
        GeoFire geoFire = newTestGeoFire();
        setLoc(geoFire, "0", 0, 0);
        setLoc(geoFire, "1", 37.0000, -122.0000);
        setLoc(geoFire, "2", 37.0001, -122.0001);
        setLoc(geoFire, "3", 37.1000, -122.0000);
        setLoc(geoFire, "4", 37.0002, -121.9998, true);

        GeoQuery query = geoFire.queryAtLocation(37.0, -122, 1000);

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

        query.removeEventListener(testListenerRemoved);
        query.removeAllEventListeners();

        setLoc(geoFire, "0", 37, -122); // entered
        setLoc(geoFire, "1", 0, 0); // exited
        setLoc(geoFire, "2", 37, -122.0001, true); // moved

        testListenerRemained.expectEvents(addedEvents);
        testListenerRemoved.expectEvents(addedEvents);
    }
}
