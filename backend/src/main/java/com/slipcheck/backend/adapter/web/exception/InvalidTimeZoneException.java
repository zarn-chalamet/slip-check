package com.slipcheck.backend.adapter.web.exception;

public class InvalidTimeZoneException extends RuntimeException {

    public InvalidTimeZoneException() {
        super("Unknown time zone");
    }
}