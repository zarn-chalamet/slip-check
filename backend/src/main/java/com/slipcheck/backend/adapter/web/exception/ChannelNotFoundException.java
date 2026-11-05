package com.slipcheck.backend.adapter.web.exception;

public class ChannelNotFoundException extends RuntimeException {

    public ChannelNotFoundException() {
        super("Channel not found");
    }
}
