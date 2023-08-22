package com.upc.gessi.qrapids.app.presentation.rest.services.exceptions;

import com.upc.gessi.qrapids.app.presentation.rest.services.Alerts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<Object> buildErrorResponse(GeneralException exception, HttpServletRequest request){


        String message = exception.getMessage();

        logger.error(message, exception);
        ErrorResponse errorResponse = new ErrorResponse(exception.getStatus().value(), exception.getStatus().name().toUpperCase(),message, request.getRequestURI());

        return ResponseEntity.status(exception.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(value = {
    })
    public ResponseEntity<Object> handleException(GeneralException exception, HttpServletRequest request){
        return buildErrorResponse(exception, request);
    }
}
