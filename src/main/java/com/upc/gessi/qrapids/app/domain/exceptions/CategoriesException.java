package com.upc.gessi.qrapids.app.domain.exceptions;

import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.ConflictException;

public class CategoriesException extends ConflictException {


    // Constructor that accepts a message
    public CategoriesException(String message)
    {
        super(message);
    }

}
