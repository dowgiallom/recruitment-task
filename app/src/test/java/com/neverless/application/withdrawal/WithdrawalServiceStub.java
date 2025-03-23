package com.neverless.application.withdrawal;

import com.neverless.domain.Amount;
import com.neverless.integration.WithdrawalService;

class WithdrawalServiceStub implements WithdrawalService {

    private final WithdrawalState processingState;
    private final WithdrawalState finalState;
    private final boolean throwError;

    public WithdrawalServiceStub(WithdrawalState processingState, WithdrawalState finalState) {
        this(processingState, finalState, false);
    }

    public WithdrawalServiceStub(WithdrawalState processingState, boolean throwError) {
        this(processingState, WithdrawalState.PROCESSING, throwError);
    }

    public WithdrawalServiceStub(WithdrawalState processingState, WithdrawalState finalState, boolean throwError) {
        this.processingState = processingState;
        this.finalState = finalState;
        this.throwError = throwError;
    }

    @Override
    public WithdrawalState getRequestState(WithdrawalId extWithdrawalId) {
        if (throwError) {
            throw new RuntimeException("External service error");
        }
        return finalState;
    }

    @Override
    public void requestWithdrawal(WithdrawalId id, Address address, Amount amount) {
        // no op
    }
}

