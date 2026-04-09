package com.mpesa.integration.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Logs one line per payment operation in the required format:
 * timestamp | service=... | process=... | traceId=... | endpoint=... | duration=...ms | responseCode=... | responseMessage=... | errorMessage=... | request=... | response=...
 */
@Component
public class StructuredPaymentLogger {

    private static final Logger log = LoggerFactory.getLogger(StructuredPaymentLogger.class);

    public void logPaymentEvent(
            String serviceName,
            String processName,
            String traceId,
            String endpointPath,
            long durationMillis,
            int httpStatusCode,
            String outcomeMessage,
            String errorDetail,
            String requestPayloadJson,
            String responsePayloadJson
    ) {
        String errorDetailEscaped = (errorDetail == null || errorDetail.isBlank())
                ? AppConstants.LOG_ERROR_NONE
                : errorDetail.replace("|", "\\|");
        String outcomeEscaped = outcomeMessage == null ? "" : outcomeMessage.replace("|", "\\|");
        String line = String.format(
                "%s | service=%s | process=%s | traceId=%s | endpoint=%s | duration=%dms | responseCode=%d | responseMessage=%s | errorMessage=%s | request=%s | response=%s",
                Instant.now(),
                serviceName,
                processName,
                traceId,
                endpointPath,
                durationMillis,
                httpStatusCode,
                outcomeEscaped,
                errorDetailEscaped,
                sanitizeJson(requestPayloadJson),
                sanitizeJson(responsePayloadJson)
        );
        if (httpStatusCode >= 500) {
            log.error(line);
        } else if (httpStatusCode >= 400) {
            log.warn(line);
        } else {
            log.info(line);
        }
    }

    private static String sanitizeJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        return raw.replace("\n", " ").replace("\r", "").replace("|", "\\|");
    }
}
