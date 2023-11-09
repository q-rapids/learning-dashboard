package com.upc.gessi.qrapids.app.domain.exceptions;

public class AlertNotFoundException extends Exception {
    public AlertNotFoundException() {}

    // Constructor that accepts a message
    public AlertNotFoundException(String message) {
        super(message);
    }
}
