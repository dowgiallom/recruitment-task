package com.neverless.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import com.neverless.exceptions.NegativeAmountException;

/**
 * Amount, represented in its smallest atomic unit.
 * E.g.: satoshi for BTC, cent for USD.
 * @param value
 */
public record Amount(@JsonValue int value) {

    public Amount {
        if (value < 0) {
            throw new NegativeAmountException("Amount cannot be negative.");
        }
    }

    public Amount deduct(Amount amount) {
        return new Amount(value - amount.value);
    }

    public Amount add(Amount amount) {
        return new Amount(value + amount.value);
    }

    public int compareTo(Amount other) {
        return Integer.compare(this.value, other.value);
    }
}
