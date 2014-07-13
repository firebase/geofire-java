package com.firebase.geofire;

import com.firebase.geofire.core.GeoHash;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GeoHashTest {
    @Rule
    public org.junit.rules.ExpectedException exception = ExpectedException.none();

    @Test
    public void hashValues() {
        Assert.assertEquals(new GeoHash("7zzzzzzzzz"), new GeoHash(0, 0));
        Assert.assertEquals(new GeoHash("2pbpbpbpbp"), new GeoHash(0, -180));
        Assert.assertEquals(new GeoHash("rzzzzzzzzz"), new GeoHash(0, 180));
        Assert.assertEquals(new GeoHash("5bpbpbpbpb"), new GeoHash(-90, 0));
        Assert.assertEquals(new GeoHash("0000000000"), new GeoHash(-90, -180));
        Assert.assertEquals(new GeoHash("pbpbpbpbpb"), new GeoHash(-90, 180));
        Assert.assertEquals(new GeoHash("gzzzzzzzzz"), new GeoHash(90, 0));
        Assert.assertEquals(new GeoHash("bpbpbpbpbp"), new GeoHash(90, -180));
        Assert.assertEquals(new GeoHash("zzzzzzzzzz"), new GeoHash(90, 180));

        Assert.assertEquals(new GeoHash("9q8yywe56g"), new GeoHash(37.7853074, -122.4054274));
        Assert.assertEquals(new GeoHash("dqcjf17sy6"), new GeoHash(38.98719, -77.250783));
        Assert.assertEquals(new GeoHash("tj4p5gerfz"), new GeoHash(29.3760648, 47.9818853));
        Assert.assertEquals(new GeoHash("umghcygjj7"), new GeoHash(78.216667, 15.55));
        Assert.assertEquals(new GeoHash("4qpzmren1k"), new GeoHash(-54.933333, -67.616667));
        Assert.assertEquals(new GeoHash("4w2kg3s54y"), new GeoHash(-54, -67));
    }

    @Test
    public void customPrecision() {
        Assert.assertEquals(new GeoHash("000000"), new GeoHash(-90, -180, 6));
        Assert.assertEquals(new GeoHash("zzzzzzzzzzzzzzzzzzzz"), new GeoHash(90, 180, 20));
        Assert.assertEquals(new GeoHash("p"), new GeoHash(-90, 180, 1));
        Assert.assertEquals(new GeoHash("bpbpb"), new GeoHash(90, -180, 5));
        Assert.assertEquals(new GeoHash("9q8yywe5"), new GeoHash(37.7853074, -122.4054274, 8));
        Assert.assertEquals(new GeoHash("dqcjf17sy6cppp8vfn"), new GeoHash(38.98719, -77.250783, 18));
        Assert.assertEquals(new GeoHash("tj4p5gerfzqu"), new GeoHash(29.3760648, 47.9818853, 12));
        Assert.assertEquals(new GeoHash("u"), new GeoHash(78.216667, 15.55, 1));
        Assert.assertEquals(new GeoHash("4qpzmre"), new GeoHash(-54.933333, -67.616667, 7));
        Assert.assertEquals(new GeoHash("4w2kg3s54"), new GeoHash(-54, -67, 9));
    }

    @Test
    public void zeroPrecisionException() {
        exception.expect(IllegalArgumentException.class);
        new GeoHash(1,2,0);
    }

    @Test
    public void largePrecisionException() {
        exception.expect(IllegalArgumentException.class);
        new GeoHash(1,2,23);
    }

    @Test
    public void invalidGeoHashException() {
        exception.expect(IllegalArgumentException.class);
        new GeoHash("abc");
        new GeoHash("");
        new GeoHash("~");
    }
}
