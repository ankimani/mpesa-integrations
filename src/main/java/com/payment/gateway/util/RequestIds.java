package com.mpesa.integration.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public final class RequestIds {

    private RequestIds() {
    }

    public static UUID resolve(HttpServletRequest request) {
        String header = request.getHeader(AppConstants.HEADER_REQUEST_ID);
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header.trim());
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return UUID.randomUUID();
    }

    public static UUID random() {
        return UUID.randomUUID();
    }
}
