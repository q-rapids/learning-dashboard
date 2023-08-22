package com.upc.gessi.qrapids.app.domain.exceptions;

import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.ConflictException;
import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.GeneralException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;

import java.util.List;

public class ElementAlreadyPresentException extends ConflictException {

    public ElementAlreadyPresentException(String message) {
        super(message);
    }

    public ElementAlreadyPresentException() {
        super("ERROR");
    }


    public ElementAlreadyPresentException(String message, String... args) {
        super(message, args);
    }
}
