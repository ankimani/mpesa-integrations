package com.mpesa.integration.service;

import com.mpesa.integration.request.StkPushRequest;
import com.mpesa.integration.response.StkPushData;
import com.mpesa.integration.response.TransactionPageData;
import com.mpesa.integration.entity.PaymentStatus;
import jakarta.servlet.http.HttpServletRequest;

public interface PaymentService {

    StkPushData initiateStkPayment(StkPushRequest request, String idempotencyKey, String traceId);

    void processMpesaStkCallback(String rawJsonBody, HttpServletRequest request);

    TransactionPageData listTransactions(int page, int size, PaymentStatus status, String traceId);
}
