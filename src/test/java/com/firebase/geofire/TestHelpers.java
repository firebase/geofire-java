package com.firebase.geofire;

import java.util.Random;

public class TestHelpers {

    public static final long TIMEOUT_SECONDS = 5;

    private static final String alphaNumChars = "abcdefghijklmnopqrstuvwxyz0123456789";

    public static String randomAlphaNumericString(int length) {
        StringBuffer buffer = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i < length; i++ ) {
            buffer.append(alphaNumChars.charAt(random.nextInt(alphaNumChars.length())));
        }
        return buffer.toString();
    }
}
