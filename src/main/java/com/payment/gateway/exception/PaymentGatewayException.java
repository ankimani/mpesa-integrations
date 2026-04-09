package com.mpesa.integration.exception;

import org.springframework.http.HttpStatus;

public class PaymentGatewayException extends RuntimeException {

    private final String responseCode;
    private final HttpStatus httpStatus;
    private final String customerMessage;
    private final String technicalMessage;

    public PaymentGatewayException(
            String responseCode,
            HttpStatus httpStatus,
            String customerMessage,
            String technicalMessage,
            Throwable cause
    ) {
        super(messageForSuper(customerMessage, technicalMessage, responseCode), cause);
        this.responseCode = responseCode;
        this.httpStatus = httpStatus;
        this.customerMessage = customerMessage;
        this.technicalMessage = technicalMessage;
    }

    public PaymentGatewayException(String responseCode, HttpStatus httpStatus, String customerMessage, String technicalMessage) {
        this(responseCode, httpStatus, customerMessage, technicalMessage, null);
    }

    /**
     * Code returned in {@link com.mpesa.integration.response.ApiResponse#responseCode()} (any string you choose).
     */
    public String getResponseCode() {
        return responseCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCustomerMessage() {
        return customerMessage;
    }

    public String getTechnicalMessage() {
        return technicalMessage;
    }

    private static String messageForSuper(String customerMessage, String technicalMessage, String responseCode) {
        if (technicalMessage != null && !technicalMessage.isBlank()) {
            return technicalMessage;
        }
        if (customerMessage != null && !customerMessage.isBlank()) {
            return customerMessage;
        }
        return responseCode != null ? responseCode : "error";
    }
}
