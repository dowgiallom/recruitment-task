package com.neverless.domain.withdrawal;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record InternalWithdrawalId(@JsonValue UUID value) {

    public InternalWithdrawalId {
        requireNonNull(value, "UUID must not be null");
    }

    public static InternalWithdrawalId fromString(String val) {
        requireNonNull(val, "String id must not be null");
        return new InternalWithdrawalId(UUID.fromString(val));
    }

    public static InternalWithdrawalId random() {
        return new InternalWithdrawalId(UUID.randomUUID());
    }
}
