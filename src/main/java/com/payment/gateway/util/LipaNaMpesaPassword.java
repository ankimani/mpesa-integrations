package com.mpesa.integration.util;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public final class LipaNaMpesaPassword {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private LipaNaMpesaPassword() {
    }

    /** Daraja requires the STK password timestamp in East Africa Time (UTC+3). */
    public static String timestamp() {
        return ZonedDateTime.now(ZoneOffset.ofHours(3)).format(TIMESTAMP);
    }

    public static String encodePassword(String shortCode, String passkey, String timestamp) {
        String raw = shortCode + passkey + timestamp;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
