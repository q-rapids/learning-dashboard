package com.upc.gessi.qrapids.app.domain.exceptions;

import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.BadRequestException;
import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.ResourceNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;

public class EnumBadRequestException extends BadRequestException {
    public EnumBadRequestException(String message) {
        super(message);
    }

    public EnumBadRequestException(String message, String... args) {
        super(message, args);
    }
}
