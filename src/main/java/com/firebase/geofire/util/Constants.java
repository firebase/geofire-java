package com.firebase.geofire.util;

public class Constants {

    // Length of a degree latitude at the equator
    public static final double METERS_PER_DEGREE_LATITUDE = 110574;

    // The equatorial circumference of the earth in meters
    public static final double EARTH_MERIDIONAL_CIRCUMFERENCE = 40007860;

    // The equatorial radius of the earth in meters
    public static final double EARTH_EQ_RADIUS = 6378137;

    // The meridional radius of the earth in meters
    public static final double EARTH_POLAR_RADIUS = 6357852.3;

    /* The following value assumes a polar radius of
     * r_p = 6356752.3
     * and an equatorial radius of
     * r_e = 6378137
     * The value is calculated as e2 == (r_e^2 - r_p^2)/(r_e^2)
     * Use exact value to avoid rounding errors
     */
    public static final double EARTH_E2 =  0.00669447819799;

    // Cutoff for floating point calculations
    public static final double EPSILON = 1e-12;

    private Constants() {}
}
