package com.mpesa.integration.repository;

import com.mpesa.integration.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentTransaction> findByCheckoutRequestId(String checkoutRequestId);

    @Query(value = """
            SELECT
                p.id AS "transactionId",
                p.client_reference AS "clientReference",
                p.phone_number AS "phoneNumber",
                p.amount AS "amount",
                p.currency_code AS "currencyCode",
                p.status AS "paymentStatus",
                p.checkout_request_id AS "checkoutRequestId",
                p.merchant_request_id AS "merchantRequestId",
                p.mpesa_receipt_number AS "mpesaReceiptNumber",
                p.mpesa_result_code AS "mpesaResultCode",
                p.mpesa_result_description AS "mpesaResultDescription",
                p.created_at AS "createdAt",
                p.updated_at AS "updatedAt"
            FROM payment_transactions p
            WHERE (:status IS NULL OR p.status = :status)
            ORDER BY p.created_at DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findTransactionPageNative(
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") long offset
    );
}
