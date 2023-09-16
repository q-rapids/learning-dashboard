package com.upc.gessi.qrapids.app.domain.exceptions;

import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.ResourceNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;

public class StrategicIndicatorNotFoundException extends ResourceNotFoundException {
    public StrategicIndicatorNotFoundException(String identifier) {
        super(String.format(Messages.STRATEGIC_INDICATOR_NOT_FOUND, identifier));
    }

    public StrategicIndicatorNotFoundException(String message, String... args) {
        super(message, args);
    }
}
