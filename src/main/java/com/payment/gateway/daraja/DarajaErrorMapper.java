package com.mpesa.integration.daraja;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpesa.integration.exception.PaymentGatewayException;
import com.mpesa.integration.util.AppConstants;
import org.springframework.http.HttpStatus;

/**
 * Maps Safaricom Daraja HTTP-layer error codes to {@link PaymentGatewayException}.
 */
public final class DarajaErrorMapper {

    public static final String CODE_400_002_02 = "400.002.02";
    public static final String CODE_404_001_03 = "404.001.03";
    public static final String CODE_404_001_01 = "404.001.01";
    public static final String CODE_405_001 = "405.001";
    public static final String CODE_500_001_1001 = "500.001.1001";
    public static final String CODE_500_003_02 = "500.003.02";
    public static final String CODE_500_003_1001 = "500.003.1001";
    public static final String CODE_500_003_03 = "500.003.03";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DarajaErrorMapper() {
    }

    public static PaymentGatewayException fromHttpError(int httpStatus, String responseBody) {
        ParsedDarajaError error = parse(responseBody);
        String code = effectiveErrorCode(error.code(), httpStatus);
        String technical = technicalSummary(error, httpStatus, code);
        HttpStatus mappedStatus = mapHttpStatus(code, httpStatus);
        String customerMessage = mapCustomerMessage(code, error.message(), mappedStatus);
        String responseCode = code.isBlank() ? String.valueOf(httpStatus) : code;
        return new PaymentGatewayException(responseCode, mappedStatus, customerMessage, technical);
    }

    private static ParsedDarajaError parse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return new ParsedDarajaError("", "", null, "");
        }
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            String code = text(root, "errorCode");
            String message = text(root, "errorMessage");
            String requestId = text(root, "requestId");
            if (!code.isBlank() || !message.isBlank()) {
                return new ParsedDarajaError(code, message, requestId.isBlank() ? null : requestId, responseBody);
            }
        } catch (Exception ignored) {
            // Non-JSON error body; keep as plain message.
        }
        return new ParsedDarajaError("", responseBody.strip(), null, responseBody);
    }

    private static String effectiveErrorCode(String parsedCode, int httpStatus) {
        if (parsedCode != null && !parsedCode.isBlank()) {
            return parsedCode.trim();
        }
        return switch (httpStatus) {
            case 400 -> CODE_400_002_02;
            case 404 -> CODE_404_001_01;
            case 405 -> CODE_405_001;
            default -> "";
        };
    }

    private static HttpStatus mapHttpStatus(String code, int httpStatus) {
        return switch (code) {
            case CODE_400_002_02 -> HttpStatus.BAD_REQUEST;
            case CODE_500_003_03 -> HttpStatus.TOO_MANY_REQUESTS;
            // 404.001.03 Invalid Access Token (upstream auth issue)
            case CODE_404_001_03 -> HttpStatus.BAD_GATEWAY;
            // 404.001.01 Resource not found (upstream Daraja endpoint/resource)
            case CODE_404_001_01 -> HttpStatus.BAD_GATEWAY;
            // 405.001 Method Not Allowed (upstream method mismatch)
            case CODE_405_001 -> HttpStatus.BAD_GATEWAY;
            // 500.001.1001 Merchant/credentials/session lock failures
            case CODE_500_001_1001 -> HttpStatus.BAD_GATEWAY;
            // 500.003.02 System busy / spike arrest pressure
            case CODE_500_003_02 -> HttpStatus.BAD_GATEWAY;
            // 500.003.1001 Internal Server Error from Daraja
            case CODE_500_003_1001 -> HttpStatus.BAD_GATEWAY;
            default -> {
                HttpStatus resolved = HttpStatus.resolve(httpStatus);
                yield resolved != null ? resolved : HttpStatus.BAD_GATEWAY;
            }
        };
    }

    private static String mapCustomerMessage(String code, String errorMessage, HttpStatus mappedStatus) {
        if (CODE_400_002_02.equals(code) || mappedStatus == HttpStatus.BAD_REQUEST) {
            return AppConstants.MSG_DARAJA_BAD_REQUEST_CUSTOMER;
        }
        if (CODE_500_003_03.equals(code) || mappedStatus == HttpStatus.TOO_MANY_REQUESTS) {
            return AppConstants.MSG_DARAJA_QUOTA_CUSTOMER;
        }
        if (CODE_500_001_1001.equals(code)) {
            String msg = errorMessage == null ? "" : errorMessage;
            if (msg.contains("Merchant does not exist")) {
                return AppConstants.MSG_DARAJA_MERCHANT_CONFIG_CUSTOMER;
            }
            if (msg.contains("Wrong credentials") || msg.contains("Password") || msg.contains("encoding")) {
                return AppConstants.MSG_DARAJA_CREDENTIALS_CUSTOMER;
            }
        }
        return AppConstants.MSG_MPESA_UNAVAILABLE_CUSTOMER;
    }

    private static String technicalSummary(ParsedDarajaError error, int httpStatus, String code) {
        String message = (error.message() == null || error.message().isBlank()) ? "(no errorMessage)" : error.message().strip();
        String request = (error.requestId() == null || error.requestId().isBlank()) ? "" : " requestId=" + error.requestId();
        String normalizedCode = code.isBlank() ? String.valueOf(httpStatus) : code;
        String raw = error.rawResponse() == null ? "" : " rawResponse=" + error.rawResponse();
        return "Daraja " + normalizedCode + request + ": " + message + raw;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return "";
        }
        return node.get(field).asText("").strip();
    }

    private record ParsedDarajaError(String code, String message, String requestId, String rawResponse) {
    }
}
