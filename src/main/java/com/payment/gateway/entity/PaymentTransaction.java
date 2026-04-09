package com.mpesa.integration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 128, unique = true)
    private String idempotencyKey;

    @Column(name = "client_reference", nullable = false, length = 128)
    private String clientReference;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "KES";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "merchant_request_id", length = 128)
    private String merchantRequestId;

    @Column(name = "checkout_request_id", length = 128)
    private String checkoutRequestId;

    @Column(name = "mpesa_result_code")
    private Integer mpesaResultCode;

    @Column(name = "mpesa_result_description")
    private String mpesaResultDescription;

    @Column(name = "stk_response_code", length = 16)
    private String stkResponseCode;

    @Column(name = "stk_response_description")
    private String stkResponseDescription;

    @Column(name = "stk_customer_message")
    private String stkCustomerMessage;

    @Column(name = "mpesa_receipt_number", length = 64)
    private String mpesaReceiptNumber;

    /** Safaricom-confirmed transaction timestamp (EAT, stored as UTC). Only set on ResultCode 0 callbacks. */
    @Column(name = "mpesa_transaction_date")
    private Instant mpesaTransactionDate;

    /** Phone number as confirmed by Safaricom in the callback (may differ from the initiating number). */
    @Column(name = "mpesa_confirmed_phone", length = 20)
    private String mpesaConfirmedPhone;

    /** Amount confirmed by Safaricom in the callback (informational; authoritative value is {@link #amount}). */
    @Column(name = "mpesa_confirmed_amount", precision = 19, scale = 4)
    private BigDecimal mpesaConfirmedAmount;

    @Column(name = "last_error")
    private String lastError;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
