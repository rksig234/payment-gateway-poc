package com.sigmoid.paymentgateway.common.error;

/**
 * Runtime exception carrying a standard {@link ErrorCode}. The global handler
 * maps it to the matching HTTP status and the shared error envelope.
 */
public class ApiException extends RuntimeException {

    private final transient ErrorCode code;

    public ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
