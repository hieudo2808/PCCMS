package com.astral.express.pccms.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    @Builder.Default
    private boolean success = false;
    private int code;
    private String message;
    private String errorCode;
    private Object errors;
}
