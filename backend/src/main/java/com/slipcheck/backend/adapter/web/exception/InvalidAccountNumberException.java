package com.slipcheck.backend.adapter.web.exception;

public class InvalidAccountNumberException extends RuntimeException {

    public InvalidAccountNumberException() {
        super("Account number must be 6-16 digits");
    }
}