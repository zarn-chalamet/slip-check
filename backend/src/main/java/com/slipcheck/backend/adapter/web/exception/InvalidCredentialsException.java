package com.slipcheck.backend.adapter.web.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Wrong email or password");
    }
}