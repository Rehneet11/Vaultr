package com.example.vaultr.exceptions;

public class ConcurrentRequestException extends RuntimeException{
    public ConcurrentRequestException(String message) {
        super(message);
    }
}
