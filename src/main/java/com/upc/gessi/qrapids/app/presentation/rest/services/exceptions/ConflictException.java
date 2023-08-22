package com.upc.gessi.qrapids.app.presentation.rest.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class ConflictException extends GeneralException {

    public ConflictException(String message) {
        super(message,HttpStatus.CONFLICT);
    }

    public ConflictException(String message, String... args) {
        super(message,HttpStatus.CONFLICT,args);
    }

}
