package com.mpesa.integration.response;

import com.mpesa.integration.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionListItemData {
    private UUID transactionId;
    private String clientReference;
    private String phoneNumber;
    private BigDecimal amount;
    private String currencyCode;
    private PaymentStatus paymentStatus;
    private String checkoutRequestId;
    private String merchantRequestId;
    private String mpesaReceiptNumber;
    private Integer mpesaResultCode;
    private String mpesaResultDescription;
    private Instant createdAt;
    private Instant updatedAt;
}
