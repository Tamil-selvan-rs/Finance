package com.myfinance.exception;

public enum ErrorCode {
    VALIDATION_FAILED("ERR-4001", "Input validation failed"),
    INVALID_FILE("ERR-4002", "Invalid or unsafe file attachment"),
    UNAUTHORIZED_ACCESS("ERR-4010", "Authentication required"),
    ACCESS_DENIED("ERR-4030", "Access denied to requested resource"),
    RESOURCE_NOT_FOUND("ERR-4040", "Requested resource was not found"),
    DUPLICATE_RESOURCE("ERR-4090", "Resource already exists"),
    EMAIL_DELIVERY_FAILED("ERR-5020", "Email notification delivery failed"),
    INTERNAL_ERROR("ERR-5000", "An unexpected server error occurred");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
}
