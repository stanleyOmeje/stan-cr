package com.stan.stancore.extended.exception;

import com.systemspecs.remita.exception.GenericAppException;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 * The error response follows RFC7807 - Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807).
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler
    public ResponseEntity<Object> handleNotFoundException(NotFoundException ex, WebRequest request) {
        return new ResponseEntity<>(new DefaultResponse(ex.getMessage(), ex.getCode()), new HttpHeaders(), HttpStatus.OK);
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex, WebRequest request) {
        return new ResponseEntity<>(new DefaultResponse(ex.getMessage(), ex.getCode()), new HttpHeaders(), HttpStatus.OK);
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler
    public ResponseEntity<Object> handleCategoryAlreadyExistException(AlreadyExistException ex, WebRequest request) {
        return new ResponseEntity<>(new DefaultResponse(ex.getMessage(), ex.getCode()), new HttpHeaders(), HttpStatus.OK);
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler
    public ResponseEntity<Object> handleUnknownException(UnknownException ex, WebRequest request) {
        return new ResponseEntity<>(new DefaultResponse(ex.getMessage(), ex.getCode()), new HttpHeaders(), HttpStatus.OK);
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler
    public ResponseEntity<Object> handleGenericAppException(GenericAppException ex, WebRequest request) {
        return new ResponseEntity<>(new DefaultResponse(ex.getMessage(), ex.getStatusCode()), new HttpHeaders(), HttpStatus.OK);
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Object> handleGenericAppException(NullPointerException ex, WebRequest request) {
        return new ResponseEntity<>(new DefaultResponse("System Malfunction", "77"), new HttpHeaders(), HttpStatus.OK);
    }
}
