package com.mpesa.integration.daraja.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StkCallbackAckResponse {
    @JsonProperty("ResultCode")
    private int resultCode;

    @JsonProperty("ResultDesc")
    private String resultDesc;

    public static StkCallbackAckResponse accepted() {
        return new StkCallbackAckResponse(0, "Accepted");
    }

    public static String toJson(StkCallbackAckResponse ack, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(ack);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize STK callback acknowledgement", e);
        }
    }
}
