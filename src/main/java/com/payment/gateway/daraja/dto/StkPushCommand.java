package com.mpesa.integration.daraja.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StkPushCommand {
    private BigDecimal amount;
    private String phoneNumber;
    private String accountReference;
    private String transactionDescription;
}
