package com.slipcheck.backend.adapter.web.exception;

public class SellerNotFoundException extends RuntimeException {

    public SellerNotFoundException() {
        super("Seller not found");
    }
}