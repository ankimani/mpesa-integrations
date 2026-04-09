# Demo Runbook: Terminal + Postman

## Prerequisites
- App running on `http://localhost:8080`
- Public callback URL configured (for example ngrok)
- Valid Daraja sandbox credentials in environment variables
- `MPESA_STK_CALLBACK_URL` set to your active public callback route

## 1) STK Push Request (Postman)
- Method: `POST`
- URL: `http://localhost:8080/api/v1/payments/stk-push`
- Headers:
  - `Content-Type: application/json`
  - `Idempotency-Key: {{$guid}}` (or any UUID)
- Body:
```json
{
  "phoneNumber": "254712345678",
  "amount": 1,
  "clientReference": "INV-1001",
  "transactionDescription": "Demo Payment"
}
```

### Expected On-Screen Talking Points
- Response includes `responseCode`, `requestId`, `customerMessage`, and payment identifiers.
- Initial `paymentStatus` is typically `STK_PENDING` until callback settles it.

## 2) Duplicate Idempotency Key Conflict
- Re-send same request using the same `Idempotency-Key`.
- Explain expected conflict behavior in this branch (`409` for existing key).

## 3) Callback Handling Demo
- Use real callback from Daraja sandbox if available.
- If callback token security is enabled, ensure callback path includes token:
  - `/api/v1/payments/callback/{token}`
- Mention: payloads larger than callback guard limit are rejected (`413`).

## 4) Fetch Transactions (Paginated)
- Method: `GET`
- URL (page 1): `http://localhost:8080/api/v1/payments/transactions?page=1&size=20`
- URL (filter): `http://localhost:8080/api/v1/payments/transactions?page=1&size=20&status=COMPLETED`

### Expected On-Screen Talking Points
- One-based pagination (`page=1` first page).
- `hasNext` indicates additional pages.
- `phoneNumber` is masked in list response.
- Includes callback-derived fields like `mpesaResultCode` and `mpesaResultDescription`.

## 5) Terminal Log Segment to Capture
- Show structured lines for:
  - STK push operation
  - Callback operation
  - Transaction list operation
- Emphasize transaction list logs only summarize response (no large item dump).

## Presenter Checklist (Before Recording)
- Replace sensitive values with placeholders in Postman and terminal.
- Clear old logs to avoid accidental token exposure.
- Keep one successful run and one duplicate-key conflict run for narrative clarity.
