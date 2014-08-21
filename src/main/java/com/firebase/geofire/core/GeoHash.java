package com.firebase.geofire.core;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.util.Base32Utils;
import com.firebase.geofire.util.GeoUtils;

public class GeoHash {
    private final String geoHash;

    // The default precision of a geohash
    private static final int DEFAULT_PRECISION = 10;

    // The maximal precision of a geohash
    public static final int MAX_PRECISION = 22;

    // The maximal number of bits precision for a geohash
    public static final int MAX_PRECISION_BITS = MAX_PRECISION * Base32Utils.BITS_PER_BASE32_CHAR;

    public GeoHash(double latitude, double longitude) {
        this(latitude, longitude, DEFAULT_PRECISION);
    }

    public GeoHash(GeoLocation location) {
        this(location.latitude, location.longitude, DEFAULT_PRECISION);
    }

    public GeoHash(double latitude, double longitude, int precision) {
        if (precision < 1) {
            throw new IllegalArgumentException("Precision of GeoHash must be larger than zero!");
        }
        if (precision > MAX_PRECISION) {
            throw new IllegalArgumentException("Precision of a GeoHash must be less than " + (MAX_PRECISION + 1) + "!");
        }
        if (!GeoLocation.coordinatesValid(latitude, longitude)) {
            throw new IllegalArgumentException(String.format("Not valid location coordinates: [%f, %f]", latitude, longitude));
        }
        double[] longitudeRange = { -180, 180 };
        double[] latitudeRange = { -90, 90 };

        char[] buffer = new char[precision];

        for (int i = 0; i < precision; i++) {
            int hashValue = 0;
            for (int j = 0; j < Base32Utils.BITS_PER_BASE32_CHAR; j++) {
                boolean even = (((i*Base32Utils.BITS_PER_BASE32_CHAR) + j) % 2) == 0;
                double val = even ? longitude : latitude;
                double[] range = even ? longitudeRange : latitudeRange;
                double mid = (range[0] + range[1])/2;
                if (val > mid) {
                    hashValue = (hashValue << 1) + 1;
                    range[0] = mid;
                } else {
                    hashValue = (hashValue << 1);
                    range[1] = mid;
                }
            }
            buffer[i] = Base32Utils.valueToBase32Char(hashValue);
        }
        this.geoHash = new String(buffer);
    }

    public GeoHash(String hash) {
        if (hash.length() == 0 || !Base32Utils.isValidBase32String(hash)) {
            throw new IllegalArgumentException("Not a valid geoHash: " + hash);
        }
        this.geoHash = hash;
    }

    public String getGeoHashString() {
        return this.geoHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoHash other = (GeoHash) o;

        return this.geoHash.equals(other.geoHash);
    }

    @Override
    public String toString() {
        return "GeoHash{" +
                "geoHash='" + geoHash + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return this.geoHash.hashCode();
    }
}
