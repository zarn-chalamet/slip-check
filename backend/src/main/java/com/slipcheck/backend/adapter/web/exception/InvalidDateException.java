package com.slipcheck.backend.adapter.web.exception;

public class InvalidDateException extends RuntimeException {

    public InvalidDateException() {
        super("date must be YYYY-MM-DD");
    }
}