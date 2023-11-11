package com.upc.gessi.qrapids.app.presentation.rest.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestException extends GeneralException {

    public BadRequestException(String message) {
        super(message,HttpStatus.BAD_REQUEST);
    }

    public BadRequestException(String message, String... args) {
        super(message,HttpStatus.BAD_REQUEST,args);
    }

}
