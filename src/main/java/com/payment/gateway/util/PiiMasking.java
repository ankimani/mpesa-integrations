package com.mpesa.integration.util;

public final class PiiMasking {

    private PiiMasking() {
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return "***";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 8) {
            return "***";
        }
        String start = digits.substring(0, Math.min(4, digits.length()));
        String end = digits.substring(digits.length() - 4);
        return start + "****" + end;
    }
}
