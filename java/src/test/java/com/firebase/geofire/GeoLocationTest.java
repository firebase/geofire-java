package com.firebase.geofire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GeoLocationTest {

    private static final double EPSILON = 0.0000001;

    @Test
    public void geoLocationHasCorrectValues() {
        assertEquals(new GeoLocation(1, 2).latitude, 1.0, EPSILON);
        assertEquals(new GeoLocation(1, 2).longitude, 2.0, EPSILON);
        assertEquals(new GeoLocation(0.000001, 2).latitude, 0.000001, EPSILON);
        assertEquals(new GeoLocation(0, 0.000001).longitude, 0.000001, EPSILON);
    }

    @Test
    public void invalidCoordinatesThrowException() {
        try {
            new GeoLocation(-90.1, 90);
            fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException expected) {
        }

        try {
            new GeoLocation(0, -180.1);
            fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException expected) {
        }

        try {
            new GeoLocation(0, 180.1);
            fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException expected) {
        }

        try {
            new GeoLocation(Double.NaN, 0);
            fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException expected) {
        }

        try {
            new GeoLocation(0, Double.NaN);
            fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException expected) {
        }
    }
}
