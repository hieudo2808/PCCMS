package com.astral.express.pccms.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(errorCode.getHttpStatus())
                .message(errorCode.getMessage())
                .errorCode(errorCode.getErrorCode())
                .build();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.ERR_400_BAD_REQUEST.getHttpStatus())
                .message(ErrorCode.ERR_400_BAD_REQUEST.getMessage())
                .errorCode(ErrorCode.ERR_400_BAD_REQUEST.getErrorCode())
                .errors(errors)
                .build();

        return ResponseEntity
                .status(ErrorCode.ERR_VALIDATION_FAILED.getHttpStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(value = {
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception) {
        log.warn("Invalid request parameter: {}", exception.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.ERR_400_BAD_REQUEST.getHttpStatus())
                .message(ErrorCode.ERR_400_BAD_REQUEST.getMessage())
                .errorCode(ErrorCode.ERR_400_BAD_REQUEST.getErrorCode())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(value = AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(AuthorizationDeniedException exception) {
        log.error("Authorization Denied: {}", exception.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.ERR_403_FORBIDDEN.getHttpStatus())
                .message(ErrorCode.ERR_403_FORBIDDEN.getMessage())
                .errorCode(ErrorCode.ERR_403_FORBIDDEN.getErrorCode())
                .build();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(errorResponse);
    }

    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(HttpStatus.NOT_FOUND.value())
                .message("Không tìm thấy tài nguyên")
                .errorCode("ERR_404_NOT_FOUND")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(Exception exception) {
        log.error("Unhandled Exception: {}", exception.getMessage(), exception);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.ERR_500_INTERNAL_SERVER.getHttpStatus())
                .message(ErrorCode.ERR_500_INTERNAL_SERVER.getMessage())
                .errorCode(ErrorCode.ERR_500_INTERNAL_SERVER.getErrorCode())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}
