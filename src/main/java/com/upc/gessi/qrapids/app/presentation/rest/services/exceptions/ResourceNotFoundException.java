package com.upc.gessi.qrapids.app.presentation.rest.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends GeneralException {

    public ResourceNotFoundException(String message) {
        super(message,HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message, String... args) {
        super(message,HttpStatus.NOT_FOUND,args);
    }

}
