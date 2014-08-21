package com.firebase.geofire;

import junit.framework.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GeoLocationTest {
    @Rule
    public org.junit.rules.ExpectedException exception = ExpectedException.none();

    @Test
    public void geoLocationHasCorrectValues() {
        Assert.assertEquals(new GeoLocation(1,2).latitude, 1.0);
        Assert.assertEquals(new GeoLocation(1,2).longitude, 2.0);
        Assert.assertEquals(new GeoLocation(0.000001,2).latitude, 0.000001);
        Assert.assertEquals(new GeoLocation(0,0.000001).longitude, 0.000001);
    }

    @Test
    public void invalidCoordinatesThrowException() {
        try {
            new GeoLocation(-90.1, 90);
            Assert.fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException e) {
        }
        try {
            new GeoLocation(0, -180.1);
            Assert.fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException e) {
        }
        try {
            new GeoLocation(0, 180.1);
            Assert.fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException e) {
        }
        try {
            new GeoLocation(Double.NaN, 0);
            Assert.fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException e) {
        }
        try {
            new GeoLocation(0, Double.NaN);
            Assert.fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException e) {
        }
    }
}
