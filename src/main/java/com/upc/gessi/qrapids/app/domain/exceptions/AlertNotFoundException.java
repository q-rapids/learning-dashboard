package com.upc.gessi.qrapids.app.domain.exceptions;

import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.ResourceNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;

public class AlertNotFoundException extends ResourceNotFoundException {
    public AlertNotFoundException(String identifier) {
        super(String.format(Messages.ALERT_NOT_FOUND, identifier));
    }

    public AlertNotFoundException(String message, String... args) {
        super(message, args);
    }
}
