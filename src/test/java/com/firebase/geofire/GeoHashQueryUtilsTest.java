package com.firebase.geofire;

import com.firebase.geofire.core.GeoHashQuery;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GeoHashQueryUtilsTest {

    @Test
    public void boundingBoxBits() {
        Assert.assertEquals(28, GeoHashQuery.Utils.bitsForBoundingBox(new GeoLocation(35, 0), 1000));
        Assert.assertEquals(27, GeoHashQuery.Utils.bitsForBoundingBox(new GeoLocation(35.645, 0), 1000));
        Assert.assertEquals(27, GeoHashQuery.Utils.bitsForBoundingBox(new GeoLocation(36, 0), 1000));
        Assert.assertEquals(28, GeoHashQuery.Utils.bitsForBoundingBox(new GeoLocation(0, 0), 1000));
        Assert.assertEquals(28, GeoHashQuery.Utils.bitsForBoundingBox(new GeoLocation(0, -180), 1000));
        Assert.assertEquals(28, GeoHashQuery.Utils.bitsForBoundingBox(new GeoLocation(0, 180), 1000));
        Assert.assertEquals(22, GeoHashQuery.Utils.bitsForBoundingBox(new GeoLocation(0, 0), 8000));
        Assert.assertEquals(27, GeoHashQuery.Utils.bitsForBoundingBox(new GeoLocation(45, 0), 1000));
        Assert.assertEquals(25, GeoHashQuery.Utils.bitsForBoundingBox(new GeoLocation(75, 0), 1000));
        Assert.assertEquals(23, GeoHashQuery.Utils.bitsForBoundingBox(new GeoLocation(75, 0), 2000));
        Assert.assertEquals(1, GeoHashQuery.Utils.bitsForBoundingBox(new GeoLocation(90, 0), 1000));
        Assert.assertEquals(1, GeoHashQuery.Utils.bitsForBoundingBox(new GeoLocation(90, 0), 2000));
    }
}
