# M-PESA Integration (STK Push)

Spring Boot M-PESA integration for STK Push (Daraja), with callback handling, request deduplication, and paginated transaction listing.

## Features
- STK push initiation endpoint
- Callback endpoint with optional secret token path
- Idempotency conflict protection
- Transaction list endpoint with pagination and optional status filter
- Structured logs for STK, callback, and transactions list

## Tech Stack
- Java 17
- Spring Boot 3
- Spring Data JPA
- PostgreSQL
- Flyway migrations

## Quick Start
1. Configure required environment variables in your IDE run configuration:
   - `DATABASE_URL`
   - `DATABASE_USERNAME`
   - `DATABASE_PASSWORD`
   - `MPESA_BASE_URL`
   - `MPESA_CONSUMER_KEY`
   - `MPESA_CONSUMER_SECRET`
   - `MPESA_SHORT_CODE`
   - `MPESA_PASSKEY`
   - `MPESA_STK_CALLBACK_URL`
   - `MPESA_CALLBACK_SECRET_TOKEN` (optional)
   - `MPESA_MAX_ATTEMPTS` (optional)
   - `MPESA_RETRY_DELAY_MS` (optional)
2. Start PostgreSQL and create database `payment_gateway`.
3. Run the app:
   - `mvn spring-boot:run`

## Main Endpoints
- `POST /api/v1/payments/stk-push`
  - Required header: `Idempotency-Key`
- `POST /api/v1/payments/callback`
- `POST /api/v1/payments/callback/{callbackToken}` (when callback token is enabled)
- `GET /api/v1/payments/transactions?page=1&size=20`
- `GET /api/v1/payments/transactions?page=1&size=20&status=COMPLETED`

## Notes
- First page is `page=1`.
- Transaction list masks phone numbers in response.
- Transaction list logs only summary metadata, not full item payload.
