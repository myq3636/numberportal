package com.liugh.rcs.service;

public class RcsCustomerAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public RcsCustomerAccessException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
