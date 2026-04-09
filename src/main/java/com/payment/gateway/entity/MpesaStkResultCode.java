package com.mpesa.integration.entity;

public enum MpesaStkResultCode {

    SUCCESS(0, MpesaResultAssessment.SUCCESS),

    GENERAL_ERROR(1, MpesaResultAssessment.FAILURE),
    CUSTOMER_CANNOT_REACH(1019, MpesaResultAssessment.FAILURE),
    DISABLED_OR_INVALID_OR_EXPIRED(1025, MpesaResultAssessment.FAILURE),
    REQUEST_CANCELLED_BY_USER(1032, MpesaResultAssessment.CANCELLED),
    BAD_REQUEST_OR_SERVICE_UNAVAILABLE(1037, MpesaResultAssessment.FAILURE),
    INITIATOR_INFORMATION_INVALID_OR_MISSING(2001, MpesaResultAssessment.FAILURE);

    private final int safaricomCode;
    private final MpesaResultAssessment assessment;

    MpesaStkResultCode(int safaricomCode, MpesaResultAssessment assessment) {
        this.safaricomCode = safaricomCode;
        this.assessment = assessment;
    }

    public static MpesaResultAssessment classify(Integer resultCode) {
        if (resultCode == null) {
            return MpesaResultAssessment.UNKNOWN;
        }
        for (MpesaStkResultCode value : values()) {
            if (value.safaricomCode == resultCode) {
                return value.assessment;
            }
        }
        return MpesaResultAssessment.UNKNOWN;
    }
}
