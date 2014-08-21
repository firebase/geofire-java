package com.firebase.geofire;

import com.firebase.geofire.core.GeoHash;
import com.firebase.geofire.core.GeoHashQuery;
import com.firebase.geofire.util.GeoUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Set;

@RunWith(JUnit4.class)
public class GeoHashQueryTest {
    @Rule
    public org.junit.rules.ExpectedException exception = ExpectedException.none();

    @Test
    public void queryForGeoHash() {
        Assert.assertEquals(new GeoHashQuery("60", "6h"), GeoHashQuery.queryForGeoHash(new GeoHash("64m9yn96mx"), 6));
        Assert.assertEquals(new GeoHashQuery("0", "h"), GeoHashQuery.queryForGeoHash(new GeoHash("64m9yn96mx"), 1));
        Assert.assertEquals(new GeoHashQuery("64", "65"), GeoHashQuery.queryForGeoHash(new GeoHash("64m9yn96mx"), 10));
        Assert.assertEquals(new GeoHashQuery("640", "64h"), GeoHashQuery.queryForGeoHash(new GeoHash("6409yn96mx"), 11));
        Assert.assertEquals(new GeoHashQuery("64h", "64~"), GeoHashQuery.queryForGeoHash(new GeoHash("64m9yn96mx"), 11));
        Assert.assertEquals(new GeoHashQuery("6", "6~"), GeoHashQuery.queryForGeoHash(new GeoHash("6"), 10));
        Assert.assertEquals(new GeoHashQuery("64s", "64~"), GeoHashQuery.queryForGeoHash(new GeoHash("64z178"), 12));
        Assert.assertEquals(new GeoHashQuery("64z", "64~"), GeoHashQuery.queryForGeoHash(new GeoHash("64z178"), 15));
    }

    @Test
    public void pointsInGeoHash() {
        for (int i = 0; i < 1000; i++) {
            double centerLat = Math.random()*160 - 80;
            double centerLong = Math.random()*360 - 180;
            double radius = Math.random()*100000;
            double radiusDegrees = GeoUtils.distanceToLatitudeDegrees(radius);
            Set<GeoHashQuery> queries = GeoHashQuery.queriesAtLocation(new GeoLocation(centerLat, centerLong), radius);
            for (int j = 0; j < 1000; j++) {
                double pointLat = Math.max(-89.9, Math.min(89.9, centerLat + Math.random()*radiusDegrees));
                double pointLong = GeoUtils.wrapLongitude(centerLong + Math.random()*radiusDegrees);
                if (GeoUtils.distance(centerLat, centerLong, pointLat, pointLong) < radius) {
                    GeoHash geoHash = new GeoHash(pointLat, pointLong);
                    boolean inQuery = false;
                    for (GeoHashQuery query: queries) {
                        if (query.containsGeoHash(geoHash)) {
                            inQuery = true;
                        }
                    }
                    Assert.assertTrue(inQuery);
                }
            }
        }
    }

    @Test
    public void canJoinWith() {
        Assert.assertTrue(new GeoHashQuery("abcd", "abce").canJoinWith(new GeoHashQuery("abce", "abcf")));
        Assert.assertTrue(new GeoHashQuery("abce", "abcf").canJoinWith(new GeoHashQuery("abcd", "abce")));
        Assert.assertTrue(new GeoHashQuery("abcd", "abcf").canJoinWith(new GeoHashQuery("abcd", "abce")));
        Assert.assertTrue(new GeoHashQuery("abcd", "abcf").canJoinWith(new GeoHashQuery("abce", "abcf")));
        Assert.assertTrue(new GeoHashQuery("abc", "abd").canJoinWith(new GeoHashQuery("abce", "abcf")));
        Assert.assertTrue(new GeoHashQuery("abce", "abcf").canJoinWith(new GeoHashQuery("abc", "abd")));
        Assert.assertTrue(new GeoHashQuery("abcd", "abce~").canJoinWith(new GeoHashQuery("abc", "abd")));
        Assert.assertTrue(new GeoHashQuery("abcd", "abce~").canJoinWith(new GeoHashQuery("abce", "abcf")));
        Assert.assertTrue(new GeoHashQuery("abcd", "abcf").canJoinWith(new GeoHashQuery("abce", "abcg")));

        Assert.assertFalse(new GeoHashQuery("abcd", "abce").canJoinWith(new GeoHashQuery("abcg", "abch")));
        Assert.assertFalse(new GeoHashQuery("abcd", "abce").canJoinWith(new GeoHashQuery("dce", "dcf")));
        Assert.assertFalse(new GeoHashQuery("abc", "abd").canJoinWith(new GeoHashQuery("dce", "dcf")));
    }

    @Test
    public void joinWith() {
        Assert.assertEquals(new GeoHashQuery("abcd", "abcf"), new GeoHashQuery("abcd", "abce").joinWith(new GeoHashQuery("abce", "abcf")));
        Assert.assertEquals(new GeoHashQuery("abcd", "abcf"), new GeoHashQuery("abce", "abcf").joinWith(new GeoHashQuery("abcd", "abce")));
        Assert.assertEquals(new GeoHashQuery("abcd", "abcf"), new GeoHashQuery("abcd", "abcf").joinWith(new GeoHashQuery("abcd", "abce")));
        Assert.assertEquals(new GeoHashQuery("abcd", "abcf"), new GeoHashQuery("abcd", "abcf").joinWith(new GeoHashQuery("abce", "abcf")));
        Assert.assertEquals(new GeoHashQuery("abc", "abd"), new GeoHashQuery("abc", "abd").joinWith(new GeoHashQuery("abce", "abcf")));
        Assert.assertEquals(new GeoHashQuery("abc", "abd"), new GeoHashQuery("abce", "abcf").joinWith(new GeoHashQuery("abc", "abd")));
        Assert.assertEquals(new GeoHashQuery("abc", "abd"), new GeoHashQuery("abcd", "abce~").joinWith(new GeoHashQuery("abc", "abd")));
        Assert.assertEquals(new GeoHashQuery("abcd", "abcf"), new GeoHashQuery("abcd", "abce~").joinWith(new GeoHashQuery("abce", "abcf")));
        Assert.assertEquals(new GeoHashQuery("abcd", "abcg"), new GeoHashQuery("abcd", "abcf").joinWith(new GeoHashQuery("abce", "abcg")));

        try {
            new GeoHashQuery("abcd", "abce").joinWith(new GeoHashQuery("abcg", "abch"));
            Assert.fail("Exception was not thrown!");
        } catch(IllegalArgumentException e) {
        }
        try {
            new GeoHashQuery("abcd", "abce").joinWith(new GeoHashQuery("dce", "dcf"));
            Assert.fail("Exception was not thrown!");
        } catch(IllegalArgumentException e) {
        }
        try {
            new GeoHashQuery("abc", "abd").joinWith(new GeoHashQuery("dce", "dcf"));
            Assert.fail("Exception was not thrown!");
        } catch(IllegalArgumentException e) {
        }
    }
}
