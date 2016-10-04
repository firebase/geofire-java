package com.firebase.geofire.core;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.util.Base32Utils;
import com.firebase.geofire.util.Constants;
import com.firebase.geofire.util.GeoUtils;

import java.util.HashSet;
import java.util.Set;

public class GeoHashQuery {

    public static class Utils {

        private Utils() {}

        public static double bitsLatitude(double resolution) {
            return Math.min(Math.log(Constants.EARTH_MERIDIONAL_CIRCUMFERENCE/2/resolution)/Math.log(2),
                    GeoHash.MAX_PRECISION_BITS);
        }

        public static double bitsLongitude(double resolution, double latitude) {
            double degrees = GeoUtils.distanceToLongitudeDegrees(resolution, latitude);
            return (Math.abs(degrees) > 0) ? Math.max(1, Math.log(360/degrees)/Math.log(2)) : 1;
        }

        public static int bitsForBoundingBox(GeoLocation location, double size) {
            double latitudeDegreesDelta = GeoUtils.distanceToLatitudeDegrees(size);
            double latitudeNorth = Math.min(90, location.latitude + latitudeDegreesDelta);
            double latitudeSouth = Math.max(-90, location.latitude - latitudeDegreesDelta);
            int bitsLatitude = ((int)Math.floor(Utils.bitsLatitude(size)))*2;
            int bitsLongitudeNorth = ((int)Math.floor(Utils.bitsLongitude(size, latitudeNorth)))*2 - 1;
            int bitsLongitudeSouth = ((int)Math.floor(Utils.bitsLongitude(size, latitudeSouth)))*2 - 1;
            return Math.min(bitsLatitude, Math.min(bitsLongitudeNorth, bitsLongitudeSouth));
        }
    }

    private final String startValue;
    private final String endValue;

    public GeoHashQuery(String startValue, String endValue) {
        this.startValue = startValue;
        this.endValue = endValue;
    }

    public static GeoHashQuery queryForGeoHash(GeoHash geohash, int bits) {
        String hash = geohash.getGeoHashString();
        int precision = (int)Math.ceil((double)bits/Base32Utils.BITS_PER_BASE32_CHAR);
        if (hash.length() < precision) {
            return new GeoHashQuery(hash, hash+"~");
        }
        hash = hash.substring(0, precision);
        String base = hash.substring(0, hash.length() - 1);
        int lastValue = Base32Utils.base32CharToValue(hash.charAt(hash.length() - 1));
        int significantBits = bits - (base.length() * Base32Utils.BITS_PER_BASE32_CHAR);
        int unusedBits = (Base32Utils.BITS_PER_BASE32_CHAR - significantBits);
        // delete unused bits
        int startValue = (lastValue >> unusedBits) << unusedBits;
        int endValue = startValue + (1 << unusedBits);
        String startHash = base + Base32Utils.valueToBase32Char(startValue);
        String endHash;
        if (endValue > 31) {
            endHash = base + "~";
        } else {
            endHash = base + Base32Utils.valueToBase32Char(endValue);
        }
        return new GeoHashQuery(startHash, endHash);
    }

    public static Set<GeoHashQuery> queriesAtLocation(GeoLocation location, double radius) {
        int queryBits = Math.max(1, Utils.bitsForBoundingBox(location, radius));
        int geoHashPrecision = (int)(Math.ceil(((float)queryBits)/Base32Utils.BITS_PER_BASE32_CHAR));

        double latitude = location.latitude;
        double longitude = location.longitude;
        double latitudeDegrees = radius/Constants.METERS_PER_DEGREE_LATITUDE;
        double latitudeNorth = Math.min(90, latitude + latitudeDegrees);
        double latitudeSouth = Math.max(-90, latitude - latitudeDegrees);
        double longitudeDeltaNorth = GeoUtils.distanceToLongitudeDegrees(radius, latitudeNorth);
        double longitudeDeltaSouth = GeoUtils.distanceToLongitudeDegrees(radius, latitudeSouth);
        double longitudeDelta = Math.max(longitudeDeltaNorth, longitudeDeltaSouth);

        Set<GeoHashQuery> queries = new HashSet<GeoHashQuery>();

        GeoHash geoHash = new GeoHash(latitude, longitude, geoHashPrecision);
        GeoHash geoHashW = new GeoHash(latitude, GeoUtils.wrapLongitude(longitude - longitudeDelta), geoHashPrecision);
        GeoHash geoHashE = new GeoHash(latitude, GeoUtils.wrapLongitude(longitude + longitudeDelta), geoHashPrecision);

        GeoHash geoHashN = new GeoHash(latitudeNorth, longitude, geoHashPrecision);
        GeoHash geoHashNW = new GeoHash(latitudeNorth, GeoUtils.wrapLongitude(longitude - longitudeDelta), geoHashPrecision);
        GeoHash geoHashNE = new GeoHash(latitudeNorth, GeoUtils.wrapLongitude(longitude + longitudeDelta), geoHashPrecision);

        GeoHash geoHashS = new GeoHash(latitudeSouth, longitude, geoHashPrecision);
        GeoHash geoHashSW = new GeoHash(latitudeSouth, GeoUtils.wrapLongitude(longitude - longitudeDelta), geoHashPrecision);
        GeoHash geoHashSE = new GeoHash(latitudeSouth, GeoUtils.wrapLongitude(longitude + longitudeDelta), geoHashPrecision);

        queries.add(queryForGeoHash(geoHash, queryBits));
        queries.add(queryForGeoHash(geoHashE, queryBits));
        queries.add(queryForGeoHash(geoHashW, queryBits));
        queries.add(queryForGeoHash(geoHashN, queryBits));
        queries.add(queryForGeoHash(geoHashNE, queryBits));
        queries.add(queryForGeoHash(geoHashNW, queryBits));
        queries.add(queryForGeoHash(geoHashS, queryBits));
        queries.add(queryForGeoHash(geoHashSE, queryBits));
        queries.add(queryForGeoHash(geoHashSW, queryBits));

        // Join queries
        boolean didJoin;
        do {
            GeoHashQuery query1 = null;
            GeoHashQuery query2 = null;
            for (GeoHashQuery query: queries) {
                for (GeoHashQuery other: queries) {
                    if (query != other && query.canJoinWith(other)) {
                        query1 = query;
                        query2 = other;
                        break;
                    }
                }
            }
            if (query1 != null && query2 != null) {
                queries.remove(query1);
                queries.remove(query2);
                queries.add(query1.joinWith(query2));
                didJoin = true;
            } else {
                didJoin = false;
            }
        } while (didJoin);

        return queries;
    }

    private boolean isPrefix(GeoHashQuery other) {
         return (other.endValue.compareTo(this.startValue) >= 0) &&
                (other.startValue.compareTo(this.startValue) < 0) &&
                (other.endValue.compareTo(this.endValue) < 0);
    }

    private boolean isSuperQuery(GeoHashQuery other) {
        int startCompare = other.startValue.compareTo(this.startValue);
        if (startCompare <= 0) {
            return other.endValue.compareTo(this.endValue) >= 0;
        } else {
            return false;
        }
    }

    public boolean canJoinWith(GeoHashQuery other) {
        return this.isPrefix(other) || other.isPrefix(this) || this.isSuperQuery(other) || other.isSuperQuery(this);
    }

    public GeoHashQuery joinWith(GeoHashQuery other) {
        if (other.isPrefix(this)) {
            return new GeoHashQuery(this.startValue, other.endValue);
        } else if (this.isPrefix(other)) {
            return new GeoHashQuery(other.startValue, this.endValue);
        } else if (this.isSuperQuery(other)) {
            return other;
        } else if (other.isSuperQuery(this)) {
            return this;
        } else {
            throw new IllegalArgumentException("Can't join these 2 queries: " + this + ", " + other);
        }
    }

    public boolean containsGeoHash(GeoHash hash) {
        String hashStr = hash.getGeoHashString();
        return this.startValue.compareTo(hashStr) <= 0 && this.endValue.compareTo(hashStr) > 0;
    }

    public String getStartValue() {
        return this.startValue;
    }

    public String getEndValue() {
        return this.endValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoHashQuery that = (GeoHashQuery) o;

        if (!endValue.equals(that.endValue)) return false;
        if (!startValue.equals(that.startValue)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = startValue.hashCode();
        result = 31 * result + endValue.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "GeoHashQuery{" +
                "startValue='" + startValue + '\'' +
                ", endValue='" + endValue + '\'' +
                '}';
    }

}
