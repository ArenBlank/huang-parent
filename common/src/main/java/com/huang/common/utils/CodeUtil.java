package com.huang.common.utils;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class CodeUtil {

    private static final char[] ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    public static String getRandomCode(Integer length) {
        StringBuilder builder = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int num = random.nextInt(10);
            builder.append(num);
        }
        return builder.toString();
    }

    public static String getRandomAlphaNumericCode(Integer length) {
        StringBuilder builder = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            builder.append(ALPHA_NUMERIC[random.nextInt(ALPHA_NUMERIC.length)]);
        }
        return builder.toString();
    }
}
