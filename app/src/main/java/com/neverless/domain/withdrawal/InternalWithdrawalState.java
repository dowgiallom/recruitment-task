package com.neverless.domain.withdrawal;

import com.neverless.integration.WithdrawalService;

public enum InternalWithdrawalState {
    PROCESSING(false), COMPLETED(true), FAILED(true);

    private final boolean isFinalState;

    InternalWithdrawalState(boolean isFinalState) {
        this.isFinalState = isFinalState;
    }

    public static InternalWithdrawalState from(WithdrawalService.WithdrawalState extState) {
        return switch (extState) {
            case PROCESSING -> PROCESSING;
            case COMPLETED -> COMPLETED;
            case FAILED -> FAILED;
        };
    }

    public boolean isFinal() {
        return isFinalState;
    }
}
