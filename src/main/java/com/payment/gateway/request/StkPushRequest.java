package com.mpesa.integration.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StkPushRequest {

    @NotBlank
    @Pattern(regexp = "^254\\d{9}$", message = "phoneNumber must be a 12-digit Kenyan MSISDN starting with 254")
    private String phoneNumber;

    @NotNull
    @DecimalMin(value = "1.0", inclusive = true)
    @Digits(integer = 12, fraction = 2)
    private BigDecimal amount;

    @NotBlank
    @Size(max = 128)
    private String clientReference;

    @NotBlank
    @Size(max = 128)
    private String transactionDescription;

}
