package com.upc.gessi.qrapids.app.domain.exceptions;

import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.ResourceNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;

public class QualityFactorNotFoundException extends ResourceNotFoundException {
    public QualityFactorNotFoundException(String identifier) {
        super(String.format(Messages.FACTOR_NOT_FOUND, identifier));
    }

    public QualityFactorNotFoundException(String message, String... args) {
        super(message, args);
    }
}
