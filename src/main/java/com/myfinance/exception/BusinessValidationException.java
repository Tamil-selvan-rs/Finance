package com.myfinance.exception;

import java.util.Collections;
import java.util.Map;

public class BusinessValidationException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, String> fieldErrors;

    public BusinessValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.errorCode = ErrorCode.VALIDATION_FAILED;
        this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyMap();
    }

    public BusinessValidationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = Collections.emptyMap();
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
}
