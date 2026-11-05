package com.slipcheck.backend.adapter.web.exception;

public class InvalidImageException extends RuntimeException {

    public InvalidImageException() {
        super("Attach a slip image");
    }
}
