package com.mpesa.integration.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mpesa.daraja")
@Getter
@Setter
public class MpesaDarajaProperties {

    private String baseUrl;
    private String consumerKey;
    private String consumerSecret;
    private String shortCode;
    private String passkey;
    private String stkCallbackUrl;
    private int connectTimeoutMs;
    private int readTimeoutMs;
    private String transactionType = "CustomerPayBillOnline";
    private int maxAttempts = 1;
    private long retryDelayMs;
    private String callbackSecretToken = "";

    public String effectiveTransactionType() {
        return (transactionType == null || transactionType.isBlank()) ? "CustomerPayBillOnline" : transactionType;
    }

    public int effectiveMaxAttempts() {
        return maxAttempts < 1 ? 1 : maxAttempts;
    }

    public long effectiveRetryDelayMs() {
        return Math.max(0, retryDelayMs);
    }

    public String effectiveCallbackSecretToken() {
        return callbackSecretToken == null ? "" : callbackSecretToken;
    }

    public boolean isCallbackSecurityEnabled() {
        return !effectiveCallbackSecretToken().isBlank();
    }
}
