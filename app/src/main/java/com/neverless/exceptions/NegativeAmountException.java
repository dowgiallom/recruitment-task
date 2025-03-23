package com.neverless.exceptions;

public class NegativeAmountException extends IllegalArgumentException {
    public NegativeAmountException(String message) {
        super(message);
    }
}