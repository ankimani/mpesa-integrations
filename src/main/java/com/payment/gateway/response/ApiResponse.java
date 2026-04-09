package com.mpesa.integration.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private final String responseCode;
    private final String requestId;
    private final String customerMessage;
    private final String technicalMessage;
    private final T data;

    public static <T> ApiResponse<T> ok(UUID requestId, String customerMessage, String technicalMessage, T data) {
        return of("200", requestId, customerMessage, technicalMessage, data);
    }

    public static <T> ApiResponse<T> of(String responseCode, UUID requestId, String customerMessage, String technicalMessage, T data) {
        return new ApiResponse<>(responseCode, requestId.toString(), customerMessage, technicalMessage, data);
    }

    public static <T> ApiResponse<T> error(String responseCode, UUID requestId, String customerMessage, String technicalMessage, T data) {
        return of(responseCode, requestId, customerMessage, technicalMessage, data);
    }
}
