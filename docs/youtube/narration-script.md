# YouTube Narration Script: M-PESA Payment Gateway Walkthrough

## 0:00-1:00 Hook + Outcome
- "In this walkthrough, we initiate M-PESA STK Push, handle async callbacks safely, and expose a fast transaction listing API."
- "By the end, you will understand the architecture, why idempotency matters, and how this code handles production realities like retries, masking, and structured logs."
- Show endpoints quickly:
  - `POST /api/v1/payments/stk-push`
  - `POST /api/v1/payments/callback/{token}`
  - `GET /api/v1/payments/transactions?page=1&size=20`

## 1:00-3:00 Architecture Overview
- "The controller layer defines API contracts and validation boundaries."
  - Reference: `src/main/java/com/payment/gateway/controller/PaymentController.java`
- "The service layer owns business rules and transaction state transitions."
  - Reference: `src/main/java/com/payment/gateway/service/PaymentServiceImpl.java`
- "The Daraja client isolates external API details: OAuth, STK request formatting, retry/error mapping."
  - Reference: `src/main/java/com/payment/gateway/daraja/MpesaDarajaClient.java`
- "Persistence is explicit and query-oriented through entity + repository."
  - References:
    - `src/main/java/com/payment/gateway/entity/PaymentTransaction.java`
    - `src/main/java/com/payment/gateway/repository/PaymentTransactionRepository.java`

## 3:00-8:00 STK Push Request Lifecycle
- "Input validation begins in the request DTO, including Kenyan MSISDN format and amount constraints."
  - Reference: `src/main/java/com/payment/gateway/request/StkPushRequest.java`
- "Controller requires `Idempotency-Key`, then forwards to service."
- "Service writes an `INITIATED` transaction first, then calls Daraja."
- "If Daraja returns `ResponseCode=0`, transaction moves to `STK_PENDING`; otherwise it is marked `FAILED`."
- "In this branch, duplicate idempotency keys return `409 Conflict` with a specific message."
  - Reference: `enforceSamePaymentDetailsForIdempotentKey(...)` in `PaymentServiceImpl.java`

## 8:00-12:00 Callback Processing + Security
- "Callbacks come through `/callback` or `/callback/{callbackToken}`."
- "If `MPESA_CALLBACK_SECRET_TOKEN` is configured, token validation is enforced."
  - Reference: callback check in `PaymentController.java`
- "Service parses callback payload, maps result code, and transitions transaction status to terminal/non-terminal outcomes."
  - Reference: `updateTransactionFromStkCallback(...)` in `PaymentServiceImpl.java`
- "Oversized payloads are rejected early using `MAX_CALLBACK_PAYLOAD_BYTES` to reduce abuse risk."
- "Callback-confirmed phone is masked before storage."
  - Reference: `applyCallbackMetadata(...)` in `PaymentServiceImpl.java`

## 12:00-15:00 Fast Transactions Listing
- "Endpoint is one-based: `page=1` is the first page."
- "It uses `Slice` instead of full `Page` to avoid expensive count queries."
- "Repository uses projection DTOs to reduce hydration overhead."
  - Reference: `findTransactionPage(...)` in `PaymentTransactionRepository.java`
- "Indexes support common sort/filter patterns."
  - Reference: `src/main/resources/db/migration/V1__init.sql`
- "Phone numbers are masked in API response to reduce PII exposure."

## 15:00-18:00 Logging + Error Design
- "A single structured log format is used across STK, callback, and transaction list operations."
  - Reference: `src/main/java/com/payment/gateway/util/StructuredPaymentLogger.java`
- "Operation names are standardized in app constants."
  - Reference: `src/main/java/com/payment/gateway/util/AppConstants.java`
- "For transaction list, response logging is summarized only; full item payload is intentionally excluded to keep logs lean."

## 18:00-22:00 Demo Sequence
- "Trigger STK push using Postman with a valid `Idempotency-Key`."
- "Show immediate API response with request IDs and initial payment status."
- "Receive callback and show the status transition on subsequent fetch."
- "Query paginated transactions with and without status filter."
- "Show duplicate key behavior: `409` with clear conflict messaging."

## Closing (30-45 seconds)
- "This design gives you reliability, security controls, and observability for mobile money flows."
- "If you are building fintech integrations, copy these patterns: idempotency, callback hardening, structured logs, and query-efficient pagination."
