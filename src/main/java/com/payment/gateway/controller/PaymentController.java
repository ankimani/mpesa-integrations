package com.mpesa.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpesa.integration.config.MpesaDarajaProperties;
import com.mpesa.integration.daraja.dto.StkCallbackAckResponse;
import com.mpesa.integration.entity.PaymentStatus;
import com.mpesa.integration.request.StkPushRequest;
import com.mpesa.integration.response.ApiResponse;
import com.mpesa.integration.response.StkPushData;
import com.mpesa.integration.response.TransactionPageData;
import com.mpesa.integration.service.PaymentService;
import com.mpesa.integration.util.AppConstants;
import com.mpesa.integration.util.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final int MAX_CALLBACK_PAYLOAD_BYTES = 65_536;

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    private final MpesaDarajaProperties mpesaDarajaSettings;

    public PaymentController(PaymentService paymentService, ObjectMapper objectMapper,
                             MpesaDarajaProperties mpesaDarajaSettings) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
        this.mpesaDarajaSettings = mpesaDarajaSettings;
    }

    @PostMapping("/stk-push")
    public ResponseEntity<ApiResponse<StkPushData>> initiateStkPayment(
            @RequestHeader(AppConstants.HEADER_IDEMPOTENCY_KEY) String idempotencyKey,
            @Valid @RequestBody StkPushRequest requestBody,
            HttpServletRequest httpRequest
    ) {
        UUID requestCorrelationId = RequestIds.resolve(httpRequest);
        StkPushData stkPayload = paymentService.initiateStkPayment(
                requestBody, idempotencyKey.trim(), requestCorrelationId.toString());
        boolean declinedByMpesa = PaymentStatus.FAILED.name().equals(stkPayload.getPaymentStatus());
        String customerMessage = declinedByMpesa ? AppConstants.MSG_STK_DECLINED_CUSTOMER : AppConstants.MSG_STK_INITIATED_CUSTOMER;
        String technicalMessage = declinedByMpesa ? AppConstants.MSG_STK_DECLINED_TECHNICAL : AppConstants.MSG_STK_INITIATED_TECHNICAL;
        return ResponseEntity.ok(ApiResponse.ok(requestCorrelationId, customerMessage, technicalMessage, stkPayload));
    }

    @PostMapping(value = {"/callback", "/callback/{callbackToken}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> receiveMpesaStkCallback(
            @PathVariable(required = false) String callbackToken,
            @RequestBody String rawJsonPayload,
            HttpServletRequest httpRequest
    ) {
        if (mpesaDarajaSettings.isCallbackSecurityEnabled()
                && (callbackToken == null || !callbackToken.equals(mpesaDarajaSettings.effectiveCallbackSecretToken()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (rawJsonPayload != null && rawJsonPayload.length() > MAX_CALLBACK_PAYLOAD_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }
        paymentService.processMpesaStkCallback(rawJsonPayload, httpRequest);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(StkCallbackAckResponse.toJson(StkCallbackAckResponse.accepted(), objectMapper));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<TransactionPageData>> listTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) PaymentStatus status,
            HttpServletRequest httpRequest
    ) {
        UUID requestCorrelationId = RequestIds.resolve(httpRequest);
        TransactionPageData data = paymentService.listTransactions(page, size, status, requestCorrelationId.toString());
        return ResponseEntity.ok(ApiResponse.ok(
                requestCorrelationId,
                "Transactions fetched successfully.",
                "Transactions page loaded",
                data
        ));
    }
}
