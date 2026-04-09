CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    client_reference VARCHAR(128) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'KES',
    status VARCHAR(32) NOT NULL,
    merchant_request_id VARCHAR(128),
    checkout_request_id VARCHAR(128),
    mpesa_result_code INTEGER,
    mpesa_result_description TEXT,
    stk_response_code VARCHAR(16),
    stk_response_description TEXT,
    stk_customer_message TEXT,
    mpesa_receipt_number VARCHAR(64),
    mpesa_transaction_date TIMESTAMPTZ,
    mpesa_confirmed_phone VARCHAR(20),
    mpesa_confirmed_amount NUMERIC(19, 4),
    last_error TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_payment_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_payment_checkout ON payment_transactions (checkout_request_id);
CREATE INDEX idx_payment_merchant ON payment_transactions (merchant_request_id);
CREATE INDEX idx_payment_status ON payment_transactions (status);
CREATE INDEX idx_payment_receipt ON payment_transactions (mpesa_receipt_number);
CREATE INDEX idx_payment_created_at_desc ON payment_transactions (created_at DESC);
CREATE INDEX idx_payment_status_created_at_desc ON payment_transactions (status, created_at DESC);
