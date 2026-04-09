package com.mpesa.integration.daraja;

import com.mpesa.integration.config.MpesaDarajaProperties;
import com.mpesa.integration.daraja.dto.OAuthTokenResponse;
import com.mpesa.integration.daraja.dto.StkInitiateResponse;
import com.mpesa.integration.daraja.dto.StkPushCommand;
import com.mpesa.integration.exception.PaymentGatewayException;
import com.mpesa.integration.util.AppConstants;
import com.mpesa.integration.util.LipaNaMpesaPassword;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal Daraja client: token fetch/cache + STK push + simple retry.
 */
@Component
public class MpesaDarajaClient {

    private static final Duration TOKEN_MAX_AGE = Duration.ofMinutes(55);

    private final RestClient restClient;
    private final MpesaDarajaProperties settings;

    private volatile String cachedAccessToken;
    private volatile Instant tokenFetchedAt;

    public MpesaDarajaClient(RestClient mpesaRestClient, MpesaDarajaProperties settings) {
        this.restClient = mpesaRestClient;
        this.settings = settings;
    }

    @PostConstruct
    void validateConfiguration() {
        requireNonBlank(settings.getConsumerKey(), "mpesa.daraja.consumer-key");
        requireNonBlank(settings.getConsumerSecret(), "mpesa.daraja.consumer-secret");
        requireNonBlank(settings.getPasskey(), "mpesa.daraja.passkey");
    }

    /**
     * Initiates an STK push, retrying only for transient failures.
     */
    public StkInitiateResponse initiateStk(StkPushCommand command) {
        int maxAttempts = settings.effectiveMaxAttempts();
        long delayMs = settings.effectiveRetryDelayMs();
        PaymentGatewayException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String token = getAccessToken();
                return postStkPush(command, token);
            } catch (PaymentGatewayException ex) {
                if (DarajaErrorMapper.CODE_404_001_03.equals(ex.getResponseCode())) {
                    evictCachedToken();
                }
                lastFailure = ex;
                if (attempt >= maxAttempts || !DarajaRetryPolicy.shouldRetry(ex)) {
                    throw ex;
                }
                sleepForRetry(delayMs);
            }
        }
        throw lastFailure != null ? lastFailure : integrationFailure("Safaricom request failed after retries");
    }

    /** Returns a valid access token, using the cache when possible. */
    public String getAccessToken() {
        String token = cachedAccessToken;
        Instant fetchedAt = tokenFetchedAt;
        if (token != null && fetchedAt != null && Instant.now().isBefore(fetchedAt.plus(TOKEN_MAX_AGE))) {
            return token;
        }
        return refreshAccessToken();
    }

    private synchronized String refreshAccessToken() {
        String token = cachedAccessToken;
        Instant fetchedAt = tokenFetchedAt;
        if (token != null && fetchedAt != null && Instant.now().isBefore(fetchedAt.plus(TOKEN_MAX_AGE))) {
            return token;
        }
        String accessToken = fetchTokenFromDaraja();
        cachedAccessToken = accessToken;
        tokenFetchedAt = Instant.now();
        return accessToken;
    }

    private synchronized void evictCachedToken() {
        cachedAccessToken = null;
        tokenFetchedAt = null;
    }

    private String fetchTokenFromDaraja() {
        try {
            OAuthTokenResponse resp = restClient.get()
                    .uri("/oauth/v1/generate?grant_type=client_credentials")
                    .headers(h -> h.setBasicAuth(settings.getConsumerKey(), settings.getConsumerSecret()))
                    .retrieve()
                    .onStatus(s -> s.isError(), (req, res) -> {
                        String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw DarajaErrorMapper.fromHttpError(res.getStatusCode().value(), body);
                    })
                    .body(OAuthTokenResponse.class);
            if (resp == null || resp.getAccessToken() == null || resp.getAccessToken().isBlank()) {
                throw integrationFailure("OAuth token response missing access_token");
            }
            return resp.getAccessToken();
        } catch (PaymentGatewayException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw integrationFailure("Timeout or network failure calling Safaricom OAuth", ex);
        }
    }

    private StkInitiateResponse postStkPush(StkPushCommand command, String bearerToken) {
        try {
            StkInitiateResponse resp = restClient.post()
                    .uri("/mpesa/stkpush/v1/processrequest")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildStkRequestBody(command))
                    .retrieve()
                    .onStatus(s -> s.isError(), (req, res) -> {
                        String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw DarajaErrorMapper.fromHttpError(res.getStatusCode().value(), body);
                    })
                    .body(StkInitiateResponse.class);
            if (resp == null) {
                throw integrationFailure("Empty STK push response body");
            }
            return resp;
        } catch (PaymentGatewayException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw integrationFailure("Timeout or network failure calling Safaricom STK push", ex);
        }
    }

    private Map<String, Object> buildStkRequestBody(StkPushCommand cmd) {
        String timestamp = LipaNaMpesaPassword.timestamp();
        String password = LipaNaMpesaPassword.encodePassword(settings.getShortCode(), settings.getPasskey(), timestamp);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("BusinessShortCode", settings.getShortCode());
        body.put("Password", password);
        body.put("Timestamp", timestamp);
        body.put("TransactionType", settings.effectiveTransactionType());
        body.put("Amount", cmd.getAmount().stripTrailingZeros().toPlainString());
        body.put("PartyA", cmd.getPhoneNumber());
        body.put("PartyB", settings.getShortCode());
        body.put("PhoneNumber", cmd.getPhoneNumber());
        body.put("CallBackURL", settings.getStkCallbackUrl());
        body.put("AccountReference", cmd.getAccountReference());
        body.put("TransactionDesc", cmd.getTransactionDescription());
        return body;
    }

    private static void sleepForRetry(long delayMs) {
        if (delayMs <= 0) return;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw integrationFailure("Interrupted during Safaricom retry backoff", e);
        }
    }

    private static PaymentGatewayException integrationFailure(String technicalMessage) {
        return integrationFailure(technicalMessage, null);
    }

    private static PaymentGatewayException integrationFailure(String technicalMessage, Throwable cause) {
        return new PaymentGatewayException(
                "502",
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                AppConstants.MSG_MPESA_UNAVAILABLE_CUSTOMER,
                technicalMessage,
                cause
        );
    }

    private static void requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must not be blank");
        }
    }
}
