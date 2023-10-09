package com.upc.gessi.qrapids.app.presentation.rest.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends GeneralException {

    public UnauthorizedException(String message) {
        super(message,HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(String message, String... args) {
        super(message,HttpStatus.UNAUTHORIZED,args);
    }

}
