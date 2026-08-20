package com.businessos.modules.ai.exception;

import com.businessos.shared.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AiProviderException extends ApiException {

    private final boolean retryable;

    public AiProviderException(String message) {
        this(message, true);
    }

    public AiProviderException(String message, boolean retryable) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
