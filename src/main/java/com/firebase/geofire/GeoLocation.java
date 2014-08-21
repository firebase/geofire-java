/*
 * Firebase GeoFire Java Library
 *
 * Copyright © 2014 Firebase - All Rights Reserved
 * https://www.firebase.com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binaryform must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY FIREBASE AS IS AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL FIREBASE BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.firebase.geofire;

/**
 * A wrapper class for location coordinates.
 */
public final class GeoLocation {

    /** The latitude of this location in the range of [-90, 90] */
    public final double latitude;

    /** The longitude of this location in the range of [-180, 180] */
    public final double longitude;

    /**
     * Creates a new GeoLocation with the given latitude and longitude.
     *
     * @throws java.lang.IllegalArgumentException If the coordinates are not valid geo coordinates
     * @param latitude The latitude in the range of [-90, 90]
     * @param longitude The longitude in the range of [-180, 180]
     */
    public GeoLocation(double latitude, double longitude) {
        if (!GeoLocation.coordinatesValid(latitude, longitude)) {
            throw new IllegalArgumentException("Not a valid geo location: " + latitude + ", " + longitude);
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Checks if these coordinates are valid geo coordinates.
     * @param latitude The latitude must be in the range [-90, 90]
     * @param longitude The longitude must be in the range [-180, 180]
     * @return True if these are valid geo coordinates
     */
    public static boolean coordinatesValid(double latitude, double longitude) {
        return (latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoLocation that = (GeoLocation) o;

        if (Double.compare(that.latitude, latitude) != 0) return false;
        if (Double.compare(that.longitude, longitude) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(latitude);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "GeoLocation(" + latitude + ", " + longitude + ")";
    }
}
