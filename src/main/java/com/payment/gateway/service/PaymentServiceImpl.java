package com.mpesa.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpesa.integration.daraja.MpesaDarajaClient;
import com.mpesa.integration.daraja.dto.StkCallbackAckResponse;
import com.mpesa.integration.daraja.dto.StkCallbackBody;
import com.mpesa.integration.daraja.dto.StkCallbackDetail;
import com.mpesa.integration.daraja.dto.StkCallbackEnvelope;
import com.mpesa.integration.daraja.dto.StkCallbackMetadata;
import com.mpesa.integration.daraja.dto.StkInitiateResponse;
import com.mpesa.integration.daraja.dto.StkPushCommand;
import com.mpesa.integration.entity.MpesaResultAssessment;
import com.mpesa.integration.entity.MpesaStkResultCode;
import com.mpesa.integration.entity.PaymentTransaction;
import com.mpesa.integration.entity.PaymentStatus;
import com.mpesa.integration.exception.PaymentGatewayException;
import com.mpesa.integration.repository.PaymentTransactionRepository;
import com.mpesa.integration.request.StkPushRequest;
import com.mpesa.integration.response.StkPushData;
import com.mpesa.integration.response.TransactionListItemData;
import com.mpesa.integration.response.TransactionPageData;
import com.mpesa.integration.util.AppConstants;
import com.mpesa.integration.util.PiiMasking;
import com.mpesa.integration.util.RequestIds;
import com.mpesa.integration.util.StructuredPaymentLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final DateTimeFormatter MPESA_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneOffset EAT = ZoneOffset.ofHours(3);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String STK_PUSH_PATH = "/api/v1/payments/stk-push";
    private static final String CALLBACK_PATH = "/api/v1/payments/callback";
    private static final String TRANSACTIONS_PATH = "/api/v1/payments/transactions";

    private final PaymentTransactionRepository transactionRepository;
    private final MpesaDarajaClient darajaClient;
    private final ObjectMapper objectMapper;
    private final StructuredPaymentLogger structuredPaymentLogger;

    public PaymentServiceImpl(
            PaymentTransactionRepository transactionRepository,
            MpesaDarajaClient darajaClient,
            ObjectMapper objectMapper,
            StructuredPaymentLogger structuredPaymentLogger
    ) {
        this.transactionRepository = transactionRepository;
        this.darajaClient = darajaClient;
        this.objectMapper = objectMapper;
        this.structuredPaymentLogger = structuredPaymentLogger;
    }

    // ── STK Push initiation ──────────────────────────────────────────────

    @Override
    public StkPushData initiateStkPayment(StkPushRequest request, String idempotencyKey, String traceId) {
        long startedAtMs = System.currentTimeMillis();
        String maskedRequestJson = buildMaskedStkRequestJsonForLogs(request);
        try {
            StkPushData response = getOrCreatePaymentForIdempotencyKey(request, idempotencyKey);
            logStructuredStkInitiation(traceId, startedAtMs, maskedRequestJson, response,
                    HttpStatus.OK, technicalMessageForStkOutcome(response), AppConstants.LOG_ERROR_NONE);
            return response;
        } catch (DataIntegrityViolationException ex) {
            PaymentTransaction existingRow =
                    transactionRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
            validateSamePaymentDetailsForSameIdempotencyKey(existingRow, request);
            StkPushData response = toStkPushData(existingRow);
            logStructuredStkInitiation(traceId, startedAtMs, maskedRequestJson, response,
                    HttpStatus.OK, technicalMessageForStkOutcome(response), AppConstants.LOG_ERROR_NONE);
            return response;
        } catch (PaymentGatewayException ex) {
            logStructuredStkInitiation(traceId, startedAtMs, maskedRequestJson, null,
                    ex.getHttpStatus(), ex.getCustomerMessage(), ex.getTechnicalMessage());
            throw ex;
        }
    }

    private StkPushData getOrCreatePaymentForIdempotencyKey(StkPushRequest request, String idempotencyKey) {
        PaymentTransaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);

        if (existing != null) {
            return handleExistingPaymentForStk(existing, request);
        }
        return startNewPaymentTransactionAndPushStk(request, idempotencyKey);
    }

    private StkPushData handleExistingPaymentForStk(PaymentTransaction existing, StkPushRequest request) {
        validateSamePaymentDetailsForSameIdempotencyKey(existing, request);
        throw new PaymentGatewayException(
                String.valueOf(HttpStatus.CONFLICT.value()),
                HttpStatus.CONFLICT,
                AppConstants.MSG_IDEMPOTENCY_KEY_EXISTS_CUSTOMER,
                AppConstants.MSG_IDEMPOTENCY_KEY_EXISTS_TECHNICAL
        );
    }

    private void validateSamePaymentDetailsForSameIdempotencyKey(PaymentTransaction existing, StkPushRequest request) {
        if (!normalizePhone(existing.getPhoneNumber()).equals(normalizePhone(request.getPhoneNumber()))
                || existing.getAmount().compareTo(normalizeAmount(request.getAmount())) != 0
                || !existing.getClientReference().equals(request.getClientReference())) {
            throw new PaymentGatewayException(
                    String.valueOf(HttpStatus.CONFLICT.value()),
                    HttpStatus.CONFLICT,
                    AppConstants.MSG_IDEMPOTENCY_KEY_EXISTS_CUSTOMER,
                    AppConstants.MSG_IDEMPOTENCY_KEY_EXISTS_TECHNICAL
            );
        }
    }

    private StkPushData startNewPaymentTransactionAndPushStk(StkPushRequest request, String idempotencyKey) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setId(UUID.randomUUID());
        tx.setIdempotencyKey(idempotencyKey);
        tx.setClientReference(request.getClientReference());
        tx.setPhoneNumber(normalizePhone(request.getPhoneNumber()));
        tx.setAmount(normalizeAmount(request.getAmount()));
        tx.setCurrencyCode("KES");
        tx.setStatus(PaymentStatus.INITIATED);
        PaymentTransaction transaction = transactionRepository.save(tx);
        return callDarajaAndUpdateTransaction(transaction.getId(), request);
    }

    /**
     * Calls Safaricom Daraja outside any database transaction, then applies the
     * response in a short transactional block.
     */
    private StkPushData callDarajaAndUpdateTransaction(UUID transactionId, StkPushRequest request) {
        StkPushCommand stkCommand = new StkPushCommand(
                normalizeAmount(request.getAmount()),
                normalizePhone(request.getPhoneNumber()),
                request.getClientReference(),
                request.getTransactionDescription()
        );

        StkInitiateResponse mpesaResponse;
        try {
            mpesaResponse = darajaClient.initiateStk(stkCommand);
        } catch (PaymentGatewayException ex) {
            PaymentTransaction paymentRecord = transactionRepository.findById(transactionId).orElseThrow();
            paymentRecord.setStatus(PaymentStatus.FAILED);
            paymentRecord.setLastError(ex.getTechnicalMessage());
            transactionRepository.save(paymentRecord);
            throw ex;
        }

        PaymentTransaction paymentRecord = transactionRepository.findById(transactionId).orElseThrow();
        paymentRecord.setStkResponseCode(mpesaResponse.getResponseCode());
        paymentRecord.setStkResponseDescription(mpesaResponse.getResponseDescription());
        paymentRecord.setStkCustomerMessage(mpesaResponse.getCustomerMessage());

        if (!"0".equals(mpesaResponse.getResponseCode())) {
            paymentRecord.setStatus(PaymentStatus.FAILED);
            paymentRecord.setLastError("Daraja declined STK initiate: " + mpesaResponse.getResponseDescription());
            transactionRepository.save(paymentRecord);
            return toStkPushData(paymentRecord);
        }

        paymentRecord.setMerchantRequestId(mpesaResponse.getMerchantRequestId());
        paymentRecord.setCheckoutRequestId(mpesaResponse.getCheckoutRequestId());
        paymentRecord.setStatus(PaymentStatus.STK_PENDING);
        transactionRepository.save(paymentRecord);
        return toStkPushData(paymentRecord);
    }

    // ── Callback processing ──────────────────────────────────────────────

    @Override
    public void processMpesaStkCallback(String rawJsonBody, HttpServletRequest request) {
        String traceId = RequestIds.resolve(request).toString();
        long startedAtMs = System.currentTimeMillis();
        String callbackPath = CALLBACK_PATH;
        int httpStatusCode = HttpStatus.OK.value();
        String callbackProcessingResult = "callback_ack";
        String errorDetail = AppConstants.LOG_ERROR_NONE;

        StkCallbackDetail stkResult = null;
        try {
            StkCallbackEnvelope envelope = objectMapper.readValue(rawJsonBody, StkCallbackEnvelope.class);
            StkCallbackBody callbackBody = envelope != null ? envelope.getBody() : null;
            stkResult = callbackBody != null ? callbackBody.getStkCallback() : null;
        } catch (JsonProcessingException ex) {
            callbackProcessingResult = "invalid_json";
            errorDetail = ex.getOriginalMessage() != null ? ex.getOriginalMessage() : ex.getMessage();
        }

        if (stkResult != null) {
            try {
                final StkCallbackDetail result = stkResult;
                callbackProcessingResult = updateTransactionFromStkCallback(result);
            } catch (Exception ex) {
                callbackProcessingResult = "error";
                errorDetail = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            }
        } else if ("callback_ack".equals(callbackProcessingResult)) {
            callbackProcessingResult = "invalid_body";
            errorDetail = "Missing Body.stkCallback";
        }

        String safeCallbackJson = buildSafeCallbackLogJson(stkResult);
        String ackBody = StkCallbackAckResponse.toJson(StkCallbackAckResponse.accepted(), objectMapper);
        structuredPaymentLogger.logPaymentEvent(
                AppConstants.SERVICE_NAME, AppConstants.PROCESS_MPESA_CALLBACK, traceId,
                callbackPath, System.currentTimeMillis() - startedAtMs, httpStatusCode,
                callbackProcessingResult, errorDetail, safeCallbackJson, ackBody);
    }

    @Override
    public TransactionPageData listTransactions(int page, int size, PaymentStatus status, String traceId) {
        long startedAtMs = System.currentTimeMillis();
        int safePage = Math.max(1, page);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        String requestJson = buildTransactionListRequestJsonForLogs(safePage, safeSize, status);
        try {
            int limit = safeSize + 1;
            long offset = (long) (safePage - 1) * safeSize;
            List<Object[]> rows = transactionRepository.findTransactionPageNative(
                    status != null ? status.name() : null,
                    limit,
                    offset
            );
            boolean hasNext = rows.size() > safeSize;
            List<TransactionListItemData> pageItems = (hasNext ? rows.subList(0, safeSize) : rows).stream()
                    .map(PaymentServiceImpl::toTransactionListItem)
                    .map(PaymentServiceImpl::maskTransactionListItemPhone)
                    .toList();
            TransactionPageData pageData = new TransactionPageData(pageItems, safePage, safeSize, hasNext);
            String responseSummaryJson = buildTransactionListResponseSummaryJsonForLogs(pageData);
            structuredPaymentLogger.logPaymentEvent(
                    AppConstants.SERVICE_NAME, AppConstants.PROCESS_FETCH_TRANSACTIONS, traceId,
                    TRANSACTIONS_PATH, System.currentTimeMillis() - startedAtMs, HttpStatus.OK.value(),
                    "Transactions page loaded", AppConstants.LOG_ERROR_NONE, requestJson, responseSummaryJson);
            return pageData;
        } catch (RuntimeException ex) {
            structuredPaymentLogger.logPaymentEvent(
                    AppConstants.SERVICE_NAME, AppConstants.PROCESS_FETCH_TRANSACTIONS, traceId,
                    TRANSACTIONS_PATH, System.currentTimeMillis() - startedAtMs, HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Transactions page fetch failed",
                    ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName(),
                    requestJson, "{}");
            throw ex;
        }
    }

    private static TransactionListItemData toTransactionListItem(Object[] row) {
        // Column order matches native SQL in PaymentTransactionRepository.findTransactionPageNative(...)
        return new TransactionListItemData(
                (UUID) row[0],
                (String) row[1],
                (String) row[2],
                (BigDecimal) row[3],
                (String) row[4],
                PaymentStatus.valueOf((String) row[5]),
                (String) row[6],
                (String) row[7],
                (String) row[8],
                row[9] == null ? null : ((Number) row[9]).intValue(),
                (String) row[10],
                toInstantValue(row[11]),
                toInstantValue(row[12])
        );
    }

    private static java.time.Instant toInstantValue(Object value) {
        if (value == null) return null;
        if (value instanceof java.time.Instant instant) return instant;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant();
        if (value instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        throw new IllegalStateException("Unsupported timestamp value type: " + value.getClass().getName());
    }

    private static TransactionListItemData maskTransactionListItemPhone(TransactionListItemData item) {
        return new TransactionListItemData(
                item.getTransactionId(),
                item.getClientReference(),
                PiiMasking.maskPhone(item.getPhoneNumber()),
                item.getAmount(),
                item.getCurrencyCode(),
                item.getPaymentStatus(),
                item.getCheckoutRequestId(),
                item.getMerchantRequestId(),
                item.getMpesaReceiptNumber(),
                item.getMpesaResultCode(),
                item.getMpesaResultDescription(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private String updateTransactionFromStkCallback(StkCallbackDetail stkResult) {
        String checkoutRequestId = stkResult.getCheckoutRequestId();
        if (checkoutRequestId == null || checkoutRequestId.isBlank()) {
            return "missing_checkout_request_id";
        }

        PaymentTransaction transaction = transactionRepository.findByCheckoutRequestId(checkoutRequestId).orElse(null);
        if (transaction == null) {
            return "unknown_checkout";
        }

        if (stkResult.getMerchantRequestId() != null
                && transaction.getMerchantRequestId() != null
                && !Objects.equals(transaction.getMerchantRequestId(), stkResult.getMerchantRequestId())) {
            return "merchant_request_id_mismatch";
        }

        if (isFinalPaymentStatus(transaction.getStatus())) {
            return "ignored_terminal_" + transaction.getStatus().name();
        }

        if (transaction.getStatus() != PaymentStatus.STK_PENDING && transaction.getStatus() != PaymentStatus.INITIATED) {
            return "ignored_status_" + transaction.getStatus().name();
        }

        transaction.setMpesaResultCode(stkResult.getResultCode());
        transaction.setMpesaResultDescription(stkResult.getResultDesc());

        MpesaResultAssessment assessment = MpesaStkResultCode.classify(stkResult.getResultCode());
        switch (assessment) {
            case SUCCESS -> {
                transaction.setStatus(PaymentStatus.COMPLETED);
                if (stkResult.getCallbackMetadata() != null) {
                    applyCallbackMetadata(transaction, stkResult.getCallbackMetadata());
                }
            }
            case CANCELLED -> transaction.setStatus(PaymentStatus.CANCELLED);
            case FAILURE -> transaction.setStatus(PaymentStatus.FAILED);
            case UNKNOWN -> transaction.setStatus(PaymentStatus.UNKNOWN);
        }

        try {
            transactionRepository.saveAndFlush(transaction);
        } catch (ObjectOptimisticLockingFailureException ex) {
            PaymentTransaction latestRow =
                    transactionRepository.findByCheckoutRequestId(checkoutRequestId).orElse(null);
            if (latestRow != null && isFinalPaymentStatus(latestRow.getStatus())) {
                return "duplicate_concurrent";
            }
            if (latestRow != null) {
                latestRow.setMpesaResultCode(stkResult.getResultCode());
                latestRow.setMpesaResultDescription(stkResult.getResultDesc());
                latestRow.setStatus(transaction.getStatus());
                if (assessment == MpesaResultAssessment.SUCCESS && stkResult.getCallbackMetadata() != null) {
                    applyCallbackMetadata(latestRow, stkResult.getCallbackMetadata());
                }
                transactionRepository.saveAndFlush(latestRow);
                return "updated_" + latestRow.getStatus().name();
            }
            return "optimistic_lock_conflict";
        }

        return "updated_" + transaction.getStatus().name();
    }

    private static void applyCallbackMetadata(PaymentTransaction transaction, StkCallbackMetadata metadata) {
        metadata.findString("MpesaReceiptNumber")
                .ifPresent(transaction::setMpesaReceiptNumber);

        metadata.findString("PhoneNumber")
                .ifPresent(phone -> transaction.setMpesaConfirmedPhone(
                        PiiMasking.maskPhone(normalizePhone(phone))
                ));

        metadata.findString("Amount").ifPresent(amountStr -> {
            try {
                transaction.setMpesaConfirmedAmount(new BigDecimal(amountStr));
            } catch (NumberFormatException ignored) {
            }
        });

        metadata.findString("TransactionDate").ifPresent(dateStr -> {
            try {
                LocalDateTime ldt = LocalDateTime.parse(dateStr, MPESA_DATE_FORMAT);
                transaction.setMpesaTransactionDate(ldt.toInstant(EAT));
            } catch (DateTimeParseException ignored) {
            }
        });
    }

    private static boolean isFinalPaymentStatus(PaymentStatus status) {
        return status == PaymentStatus.COMPLETED
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.UNKNOWN
                || status == PaymentStatus.CANCELLED;
    }

    /** Builds a redacted log representation without re-parsing the raw JSON. */
    private String buildSafeCallbackLogJson(StkCallbackDetail stkResult) {
        if (stkResult == null) {
            return "{\"unparsed\":true}";
        }
        try {
            Map<String, Object> safeFields = new LinkedHashMap<>();
            safeFields.put("MerchantRequestID", stkResult.getMerchantRequestId());
            safeFields.put("CheckoutRequestID", stkResult.getCheckoutRequestId());
            safeFields.put("ResultCode", stkResult.getResultCode());
            return objectMapper.writeValueAsString(safeFields);
        } catch (Exception e) {
            return "{\"masked\":true}";
        }
    }

    // ── Shared helpers ───────────────────────────────────────────────────

    private static StkPushData toStkPushData(PaymentTransaction transaction) {
        return new StkPushData(
                transaction.getId().toString(),
                transaction.getCheckoutRequestId(),
                transaction.getMerchantRequestId(),
                transaction.getUpdatedAt(),
                transaction.getStatus().name()
        );
    }

    private void logStructuredStkInitiation(String traceId, long startedAtMs, String maskedRequestJson,
            StkPushData response, HttpStatus httpStatus, String outcomeMessage, String errorDetail) {
        String responseJson = response != null ? toJsonOrEmptyObject(response) : "{}";
        structuredPaymentLogger.logPaymentEvent(
                AppConstants.SERVICE_NAME, AppConstants.PROCESS_STK_PUSH, traceId,
                STK_PUSH_PATH, System.currentTimeMillis() - startedAtMs,
                httpStatus.value(), outcomeMessage, errorDetail, maskedRequestJson, responseJson);
    }

    private String technicalMessageForStkOutcome(StkPushData data) {
        boolean declinedByMpesa = PaymentStatus.FAILED.name().equals(data.getPaymentStatus());
        return declinedByMpesa ? AppConstants.MSG_STK_DECLINED_TECHNICAL : AppConstants.MSG_STK_INITIATED_TECHNICAL;
    }

    private String buildMaskedStkRequestJsonForLogs(StkPushRequest request) {
        Map<String, Object> maskedFields = new LinkedHashMap<>();
        maskedFields.put("phoneNumber", PiiMasking.maskPhone(request.getPhoneNumber()));
        maskedFields.put("amount", request.getAmount());
        maskedFields.put("clientReference", request.getClientReference());
        return toJsonOrEmptyObject(maskedFields);
    }

    private String buildTransactionListRequestJsonForLogs(int page, int size, PaymentStatus status) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("page", page);
        request.put("size", size);
        request.put("status", status != null ? status.name() : null);
        return toJsonOrEmptyObject(request);
    }

    private String buildTransactionListResponseSummaryJsonForLogs(TransactionPageData pageData) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("page", pageData.getPage());
        summary.put("size", pageData.getSize());
        summary.put("hasNext", pageData.isHasNext());
        summary.put("itemsCount", pageData.getItems() == null ? 0 : pageData.getItems().size());
        return toJsonOrEmptyObject(summary);
    }

    private String toJsonOrEmptyObject(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("\\s+", "");
    }

    private static BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount.stripTrailingZeros();
    }
}
