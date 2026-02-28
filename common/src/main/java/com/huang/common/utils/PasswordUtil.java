package com.huang.common.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {
    }

    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (isBcryptHash(storedPassword)) {
            try {
                return ENCODER.matches(rawPassword, storedPassword);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        // legacy plain-text fallback
        return rawPassword.equals(storedPassword);
    }

    public static boolean isBcryptHash(String value) {
        return value != null && value.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }
}

