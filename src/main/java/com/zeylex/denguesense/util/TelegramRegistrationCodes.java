package com.zeylex.denguesense.util;

import java.util.UUID;

public final class TelegramRegistrationCodes {

    private TelegramRegistrationCodes() {
    }

    public static String generate() {
        return "PHI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
