package com.astral.express.pccms.common.exception;

import java.util.Map;

import lombok.Getter;

@Getter
public class BusinessValidationException extends BusinessException {
    private final Map<String, String> fieldErrors;

    public BusinessValidationException(Map<String, String> fieldErrors) {
        super(ErrorCode.ERR_VALIDATION_FAILED);
        this.fieldErrors = Map.copyOf(fieldErrors);
    }
}
