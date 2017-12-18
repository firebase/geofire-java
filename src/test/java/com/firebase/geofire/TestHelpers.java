package com.firebase.geofire;

import java.util.Random;

public final class TestHelpers {
    public static final long TIMEOUT_SECONDS = 5;

    private static final String ALPHA_NUM_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    public static String randomAlphaNumericString(int length) {
        StringBuffer buffer = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i < length; i++ ) {
            buffer.append(ALPHA_NUM_CHARS.charAt(random.nextInt(ALPHA_NUM_CHARS.length())));
        }
        return buffer.toString();
    }

    private TestHelpers() {
        throw new AssertionError("No instances.");
    }
}
