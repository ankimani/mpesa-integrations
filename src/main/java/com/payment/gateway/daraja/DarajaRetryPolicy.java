package com.mpesa.integration.daraja;

import com.mpesa.integration.exception.PaymentGatewayException;
import org.springframework.web.client.RestClientException;

/**
 * Minimal retry strategy for transient Daraja failures.
 */
public final class DarajaRetryPolicy {

    private DarajaRetryPolicy() {
    }

    public static boolean shouldRetry(PaymentGatewayException ex) {
        if (ex.getCause() instanceof RestClientException) {
            return true;
        }
        String code = ex.getResponseCode();
        // Documented transient errors (busy/internal service pressure).
        return DarajaErrorMapper.CODE_500_003_02.equals(code)
                || DarajaErrorMapper.CODE_500_003_1001.equals(code);
    }
}
