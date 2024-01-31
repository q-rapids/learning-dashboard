package com.upc.gessi.qrapids.app.presentation.rest.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalErrorException extends GeneralException {

    public InternalErrorException(String message) {
        super(message,HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public InternalErrorException(String message, String... args) {
        super(message,HttpStatus.INTERNAL_SERVER_ERROR,args);
    }

}
