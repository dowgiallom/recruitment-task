package com.neverless.domain.withdrawal;

public interface WithdrawalCommandService {
    /**
     * @param withdrawal request
     * @return request with current state
     * @throws com.neverless.exceptions.InsufficientFundsException when requested amount > available balance
     */
    Withdrawal initialize(Withdrawal withdrawal);
    Withdrawal onStateUpdated(Withdrawal updated);
}
