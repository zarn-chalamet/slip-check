package com.slipcheck.backend.adapter.web.exception;

public class UnsupportedChannelException extends RuntimeException {

    public UnsupportedChannelException() {
        super("Link codes are for TELEGRAM or LINE");
    }
}