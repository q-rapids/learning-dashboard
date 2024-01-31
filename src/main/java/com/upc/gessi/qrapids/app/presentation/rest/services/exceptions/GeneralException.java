package com.upc.gessi.qrapids.app.presentation.rest.services.exceptions;

import org.springframework.http.HttpStatus;

public class GeneralException extends RuntimeException {

    private String[] args;
    HttpStatus status;

    public GeneralException() {
        super();
    }

    public GeneralException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeneralException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public GeneralException(String message, HttpStatus status, String... args) {
        super(message);
        this.args = args;
        this.status = status;
    }

    public GeneralException(Throwable cause) {
        super(cause);
    }

    public String[] getArgs(){
        return args;
    }

    public void setArgs(String... args){
        this.args = args;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }
}
