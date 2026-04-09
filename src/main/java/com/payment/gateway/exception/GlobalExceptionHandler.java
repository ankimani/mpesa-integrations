package com.mpesa.integration.exception;

import com.mpesa.integration.response.ApiResponse;
import com.mpesa.integration.util.AppConstants;
import com.mpesa.integration.util.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "invalid",
                        (a, b) -> a + "; " + b, LinkedHashMap::new));
        UUID requestCorrelationId = RequestIds.resolve(request);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("fieldErrors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        String.valueOf(HttpStatus.BAD_REQUEST.value()),
                        requestCorrelationId,
                        AppConstants.MSG_BAD_REQUEST_CUSTOMER,
                        AppConstants.MSG_BAD_REQUEST_TECHNICAL,
                        details
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleConstraints(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        UUID requestCorrelationId = RequestIds.resolve(request);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("violations", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        String.valueOf(HttpStatus.BAD_REQUEST.value()),
                        requestCorrelationId,
                        AppConstants.MSG_BAD_REQUEST_CUSTOMER,
                        AppConstants.MSG_BAD_REQUEST_TECHNICAL,
                        details
                ));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        UUID requestCorrelationId = RequestIds.resolve(request);
        if (AppConstants.HEADER_IDEMPOTENCY_KEY.equalsIgnoreCase(ex.getHeaderName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                            String.valueOf(HttpStatus.BAD_REQUEST.value()),
                            requestCorrelationId,
                            AppConstants.MSG_BAD_REQUEST_CUSTOMER,
                            "Missing " + AppConstants.HEADER_IDEMPOTENCY_KEY + " header",
                            null
                    ));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        String.valueOf(HttpStatus.BAD_REQUEST.value()),
                        requestCorrelationId,
                        AppConstants.MSG_BAD_REQUEST_CUSTOMER,
                        "Missing required header: " + ex.getHeaderName(),
                        null
                ));
    }

    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentGateway(PaymentGatewayException ex, HttpServletRequest request) {
        UUID requestCorrelationId = RequestIds.resolve(request);
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.error(
                        ex.getResponseCode(),
                        requestCorrelationId,
                        ex.getCustomerMessage(),
                        ex.getTechnicalMessage(),
                        null
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        UUID requestCorrelationId = RequestIds.resolve(request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                        requestCorrelationId,
                        AppConstants.MSG_INTERNAL_ERROR_CUSTOMER,
                        AppConstants.MSG_INTERNAL_ERROR_TECHNICAL,
                        null
                ));
    }
}
