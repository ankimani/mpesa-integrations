package com.mpesa.integration.util;

public final class AppConstants {

    private AppConstants() {
    }

    public static final String SERVICE_NAME = "mpesa-integration";
    public static final String PROCESS_STK_PUSH = "stk-push-request";
    public static final String PROCESS_MPESA_CALLBACK = "mpesa-callback-handler";
    public static final String PROCESS_FETCH_TRANSACTIONS = "transactions-list-request";

    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    public static final String LOG_ERROR_NONE = "none";

    /** Default copy when the gateway rejects input (validation, bad request). */
    public static final String MSG_BAD_REQUEST_CUSTOMER = "We could not process your request.";
    public static final String MSG_BAD_REQUEST_TECHNICAL = "Validation failed";

    public static final String MSG_MPESA_UNAVAILABLE_CUSTOMER = "The payment service is temporarily unavailable. Please try again.";

    /** Daraja {@code 400.002.02} — invalid payload versus API specification. */
    public static final String MSG_DARAJA_BAD_REQUEST_CUSTOMER =
            "M-PESA rejected this payment request. Please check the phone number, amount, and reference, then try again.";

    /** Daraja {@code 500.003.03} quota / rate limiting. */
    public static final String MSG_DARAJA_QUOTA_CUSTOMER =
            "Too many payment requests were sent. Please wait a moment and try again.";

    /** Daraja {@code 500.001.1001} merchant / short code mismatch. */
    public static final String MSG_DARAJA_MERCHANT_CONFIG_CUSTOMER =
            "The payment service is not configured correctly for this business. Please contact support.";

    /** Daraja {@code 500.001.1001} password / passkey / BusinessShortCode mismatch. */
    public static final String MSG_DARAJA_CREDENTIALS_CUSTOMER =
            "The payment service could not authenticate with M-PESA. Please contact support.";

    public static final String MSG_INTERNAL_ERROR_CUSTOMER = "Something went wrong. Please try again later.";
    public static final String MSG_INTERNAL_ERROR_TECHNICAL = "Internal Server Error";

    public static final String MSG_CONFLICT_CUSTOMER = "This request conflicts with an existing payment.";
    public static final String MSG_IDEMPOTENCY_KEY_EXISTS_CUSTOMER =
            "This Idempotency-Key has already been used for another transaction.";
    public static final String MSG_IDEMPOTENCY_KEY_EXISTS_TECHNICAL =
            "Idempotency-Key already exists with different payment details";
    public static final String MSG_STK_INITIATED_CUSTOMER = "The payment request has been initiated successfully.";
    public static final String MSG_STK_INITIATED_TECHNICAL = "STK Push accepted for processing";
    public static final String MSG_STK_DECLINED_CUSTOMER = "The payment request was not accepted by M-PESA. Please try again.";
    public static final String MSG_STK_DECLINED_TECHNICAL = "Daraja rejected STK initiate";
}
