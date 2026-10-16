package be.abis.sandwich.controller;

import be.abis.sandwich.exception.ConflictException;
import be.abis.sandwich.exception.PrintOrderException;
import be.abis.sandwich.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * Turns exceptions into JSON error answers (RFC 9457 "problem details"): the field {@code detail} holds the message
 * that the frontend shows to the user.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Too many orders at the same moment: the retries of the service were used up. */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ProblemDetail handleBusy(ConcurrencyFailureException e) {
        log.warn("Too many simultaneous changes", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The server is busy with other orders. Please try again.");
    }

    /** Safety net: the database refused the change (e.g. a unique constraint) because of a concurrent change. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Database constraint violated", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The data conflicts with existing data or was changed at the same time by someone else. Please try again.");
    }

    @ExceptionHandler(PrintOrderException.class)
    public ProblemDetail handlePrintFailure(PrintOrderException e) {
        log.error("Printing the order failed", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on the server");
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "The request body is missing or not valid JSON. Check the field names and values, e.g. breadType must be WHITE or BROWN, dates look like 2026-10-16");
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        String problems = ex.getBindingResult().getFieldErrors().stream()
                .map((FieldError error) -> error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, problems.isEmpty() ? "Invalid request" : problems);
        return handleExceptionInternal(ex, body, headers, status, request);
    }
}
