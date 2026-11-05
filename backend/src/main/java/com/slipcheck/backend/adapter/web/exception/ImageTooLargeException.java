package com.slipcheck.backend.adapter.web.exception;

public class ImageTooLargeException extends RuntimeException {

    public ImageTooLargeException() {
        super("The image exceeds the 4 MB limit");
    }
}