package com.fraguinha.ptisp.webhook.exception;

public class PtispApiException extends RuntimeException {

    public PtispApiException(final String message) {
        super(message);
    }

    public PtispApiException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
