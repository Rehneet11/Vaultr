package com.example.vaultr.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConcurrentRequestException.class)
    public ProblemDetail handleConcurrentRequest(ConcurrentRequestException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_EARLY, ex.getMessage());

        problemDetail.setTitle("Concurrent Request Detected");
        problemDetail.setType(URI.create("https://localhost:8080/api/transactions"));

        problemDetail.setProperty("retryAfterSeconds", 5);

        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Invalid Request");
        return problemDetail;
    }
}