# Visuals Pack for Video

## 1) System Architecture Diagram

```mermaid
flowchart TD
ClientApp[ClientAppPostman] --> PaymentController
PaymentController --> PaymentServiceImpl
PaymentServiceImpl --> MpesaDarajaClient
PaymentServiceImpl --> PaymentTransactionRepository
MpesaDarajaClient --> DarajaAPI
DarajaAPI -->|"CallbackPOST"| CallbackEndpoint
CallbackEndpoint --> PaymentServiceImpl
```

## 2) STK Push Lifecycle Diagram

```mermaid
flowchart TD
StartRequest[StartSTKRequest] --> ValidateInput
ValidateInput --> CheckIdempotency
CheckIdempotency -->|NewKey| CreateInitiatedTx
CheckIdempotency -->|ExistingKey| Return409Conflict
CreateInitiatedTx --> CallDarajaSTK
CallDarajaSTK -->|"ResponseCode=0"| MarkStkPending
CallDarajaSTK -->|"ResponseCode!=0"| MarkFailed
MarkStkPending --> ReturnAccepted
MarkFailed --> ReturnDeclined
```

## 3) Callback Lifecycle Diagram

```mermaid
flowchart TD
CallbackArrives --> ValidateCallbackToken
ValidateCallbackToken -->|"Invalid"| Return403
ValidateCallbackToken -->|"Valid"| CheckPayloadSize
CheckPayloadSize -->|"TooLarge"| Return413
CheckPayloadSize --> ParseCallbackBody
ParseCallbackBody --> ResolveTransactionByCheckoutId
ResolveTransactionByCheckoutId --> ClassifyResultCode
ClassifyResultCode -->|"Success"| MarkCompletedAndStoreMetadata
ClassifyResultCode -->|"Cancelled"| MarkCancelled
ClassifyResultCode -->|"Failure"| MarkFailed
ClassifyResultCode -->|"Unknown"| MarkUnknown
MarkCompletedAndStoreMetadata --> ReturnAck200
MarkCancelled --> ReturnAck200
MarkFailed --> ReturnAck200
MarkUnknown --> ReturnAck200
```

## 4) Transactions List Query Path

```mermaid
flowchart TD
ListRequest[ListTransactionsRequest] --> NormalizePageSize
NormalizePageSize --> QueryRepositorySlice
QueryRepositorySlice --> MaskPhoneInResponse
MaskPhoneInResponse --> BuildPagePayload
BuildPagePayload --> StructuredSummaryLog
StructuredSummaryLog --> ReturnApiResponse
```

## 5) On-Screen Masking Checklist
- Hide values for `MPESA_CONSUMER_KEY`, `MPESA_CONSUMER_SECRET`, `MPESA_PASSKEY`.
- Hide callback token in URL path where possible.
- Mask full phone numbers in screenshots (`2547****678` style).
- Blur any ngrok domains if they are still active.
- Blur `requestId`/internal identifiers if they map to production logs.

## 6) Lower-Third Captions (Optional)
- "Idempotency prevents duplicate charge attempts"
- "Callback token hardens inbound webhook endpoint"
- "Slice pagination improves read performance at scale"
- "Structured logs enable faster operational debugging"
