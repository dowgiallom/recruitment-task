package com.neverless.domain.withdrawal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.neverless.integration.WithdrawalService;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record Destination(@JsonValue String value) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Destination {
        requireNonNull(value, "Address must not be null");
    }

    public static Destination random() {
        return new Destination(UUID.randomUUID().toString());
    }

    public WithdrawalService.Address extAddress() {
        return new WithdrawalService.Address(value);
    }
}
