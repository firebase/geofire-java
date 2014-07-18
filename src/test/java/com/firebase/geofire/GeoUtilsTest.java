package com.firebase.geofire;

import com.firebase.geofire.util.GeoUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GeoUtilsTest {

    @Test
    public void wrapLongitude() {
        Assert.assertEquals(1, GeoUtils.wrapLongitude(1), 1e-6);
        Assert.assertEquals(0, GeoUtils.wrapLongitude(0), 1e-6);
        Assert.assertEquals(180, GeoUtils.wrapLongitude(180), 1e-6);
        Assert.assertEquals(-180, GeoUtils.wrapLongitude(-180), 1e-6);
        Assert.assertEquals(-178, GeoUtils.wrapLongitude(182), 1e-6);
        Assert.assertEquals(-90, GeoUtils.wrapLongitude(270), 1e-6);
        Assert.assertEquals(0, GeoUtils.wrapLongitude(360), 1e-6);
        Assert.assertEquals(-180, GeoUtils.wrapLongitude(540), 1e-6);
        Assert.assertEquals(-90, GeoUtils.wrapLongitude(630), 1e-6);
        Assert.assertEquals(0, GeoUtils.wrapLongitude(720), 1e-6);
        Assert.assertEquals(90, GeoUtils.wrapLongitude(810), 1e-6);
        Assert.assertEquals(0, GeoUtils.wrapLongitude(-360), 1e-6);
        Assert.assertEquals(178, GeoUtils.wrapLongitude(-182), 1e-6);
        Assert.assertEquals(90, GeoUtils.wrapLongitude(-270), 1e-6);
        Assert.assertEquals(0, GeoUtils.wrapLongitude(-360), 1e-6);
        Assert.assertEquals(-90, GeoUtils.wrapLongitude(-450), 1e-6);
        Assert.assertEquals(180, GeoUtils.wrapLongitude(-540), 1e-6);
        Assert.assertEquals(90, GeoUtils.wrapLongitude(-630), 1e-6);
        Assert.assertEquals(0, GeoUtils.wrapLongitude(1080), 1e-6);
        Assert.assertEquals(0, GeoUtils.wrapLongitude(-1080), 1e-6);
    }

    @Test
    public void distanceToLongitudeDegrees() {
        Assert.assertEquals(0.008983, GeoUtils.distanceToLongitudeDegrees(1000, 0), 1e-5);
        Assert.assertEquals(1, GeoUtils.distanceToLongitudeDegrees(111320, 0), 1e-5);
        Assert.assertEquals(1, GeoUtils.distanceToLongitudeDegrees(107550, 15), 1e-5);
        Assert.assertEquals(1, GeoUtils.distanceToLongitudeDegrees(96486, 30), 1e-5);
        Assert.assertEquals(1, GeoUtils.distanceToLongitudeDegrees(78847, 45), 1e-5);
        Assert.assertEquals(1, GeoUtils.distanceToLongitudeDegrees(55800, 60), 1e-5);
        Assert.assertEquals(1, GeoUtils.distanceToLongitudeDegrees(28902, 75), 1e-5);
        Assert.assertEquals(0, GeoUtils.distanceToLongitudeDegrees(0, 90), 1e-5);
        Assert.assertEquals(360, GeoUtils.distanceToLongitudeDegrees(1000, 90), 1e-5);
        Assert.assertEquals(360, GeoUtils.distanceToLongitudeDegrees(1000, 89.9999), 1e-5);
        Assert.assertEquals(102.594208, GeoUtils.distanceToLongitudeDegrees(1000, 89.995), 1e-5);
    }
}
