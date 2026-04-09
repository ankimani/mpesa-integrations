package com.mpesa.integration.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class StkPushData {

    private final String transactionId;
    private final String checkoutRequestId;
    private final String merchantRequestId;
    private final Instant timestamp;
    private final String paymentStatus;

}
