package com.mpesa.integration.daraja.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StkCallbackDetail {
    @JsonProperty("MerchantRequestID")
    private String merchantRequestId;

    @JsonProperty("CheckoutRequestID")
    private String checkoutRequestId;

    @JsonProperty("ResultCode")
    private Integer resultCode;

    @JsonProperty("ResultDesc")
    private String resultDesc;

    @JsonProperty("CallbackMetadata")
    private StkCallbackMetadata callbackMetadata;
}
