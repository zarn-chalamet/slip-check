package com.slipcheck.backend.adapter.web.exception;

public class EmailTakenException extends RuntimeException {

    public EmailTakenException() {
        super("An account with this email already exists");
    }
}