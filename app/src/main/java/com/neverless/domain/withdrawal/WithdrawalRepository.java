package com.neverless.domain.withdrawal;

import java.util.Optional;

public interface WithdrawalRepository {
    Optional<Withdrawal> find(InternalWithdrawalId id);
    void create(Withdrawal withdrawal);

    /**
     * @param updated updated Withdrawal view
     * @return previous Withdrawal view
     * @throws IllegalStateException if Withdrawal does not exist in the repository
     */
    Withdrawal update(Withdrawal updated);
}
