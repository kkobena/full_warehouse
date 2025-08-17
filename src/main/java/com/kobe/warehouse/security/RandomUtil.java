package com.kobe.warehouse.security;

import java.security.SecureRandom;
import org.apache.commons.lang3.RandomStringUtils;

public class RandomUtil {

    private static final int DEF_COUNT = 20;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static {
        SECURE_RANDOM.nextBytes(new byte[64]);
    }

    private RandomUtil() {}

    public static String generateRandomAlphanumericString() {
        return RandomStringUtils.random(20, 0, 0, true, true, null, SECURE_RANDOM);
    }

    public static String generatePassword() {
        return generateRandomAlphanumericString();
    }

    public static String generateActivationKey() {
        return generateRandomAlphanumericString();
    }

    public static String generateResetKey() {
        return generateRandomAlphanumericString();
    }
}
