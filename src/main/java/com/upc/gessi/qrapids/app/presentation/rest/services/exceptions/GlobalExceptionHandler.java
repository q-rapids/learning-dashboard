package com.upc.gessi.qrapids.app.presentation.rest.services.exceptions;

import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;
import org.apache.tomcat.jni.Local;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<Object> buildErrorResponse(GeneralException exception, HttpServletRequest request){


        String message = exception.getMessage();
        LocalDateTime time = LocalDateTime.now();
        logger.error("Request: " + request.getRequestURI() + "failed, REASON: " + message);
        ErrorResponse errorResponse = new ErrorResponse(exception.getStatus().value(), exception.getStatus().name().toUpperCase(),message, request.getRequestURI(), time);

        return ResponseEntity.status(exception.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<Object> handleException(GeneralException exception, HttpServletRequest request){
        return buildErrorResponse(exception, request);
    }


    // Handle validation issues
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> "'" + error.getField() + "' : " + error.getDefaultMessage())
                .collect(Collectors.toList());
        return buildErrorResponse(new BadRequestException(Messages.BAD_REQUEST + " " +
                String.join(", ", errors)), request);
    }

    // Handle deserialization issues
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildErrorResponse(new BadRequestException(Messages.BAD_REQUEST + " Invalid request format, such as body content."), request);
    }
}
