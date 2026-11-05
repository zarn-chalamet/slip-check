package com.slipcheck.backend.adapter.web.exception;

public class CannotUnlinkWebChannelException extends RuntimeException {

    public CannotUnlinkWebChannelException() {
        super("The web channel cannot be unlinked");
    }
}