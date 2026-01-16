package com.example.resilient_api.domain.exceptions;

import com.example.resilient_api.domain.enums.TechnicalMessage;
import lombok.Getter;

@Getter
public class ProcessorException extends RuntimeException {

    private final TechnicalMessage technicalMessage;

    public ProcessorException(Throwable cause, TechnicalMessage message) {
        super(cause);
        technicalMessage = message;
    }

    public ProcessorException(String message,
                              TechnicalMessage technicalMessage) {

        super(message);
        this.technicalMessage = technicalMessage;
    }
}
