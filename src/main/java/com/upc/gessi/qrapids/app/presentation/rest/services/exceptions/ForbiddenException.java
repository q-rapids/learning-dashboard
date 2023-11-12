package com.upc.gessi.qrapids.app.presentation.rest.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class ForbiddenException extends GeneralException {

    public ForbiddenException(String message) {
        super(message,HttpStatus.FORBIDDEN);
    }

    public ForbiddenException(String message, String... args) {
        super(message,HttpStatus.FORBIDDEN,args);
    }

}
