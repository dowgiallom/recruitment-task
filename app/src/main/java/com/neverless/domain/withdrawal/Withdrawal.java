package com.neverless.domain.withdrawal;

import com.neverless.domain.Amount;
import com.neverless.domain.account.AccountId;
import com.neverless.integration.WithdrawalService;

import java.util.UUID;

import static com.neverless.domain.withdrawal.InternalWithdrawalState.PROCESSING;

public record Withdrawal(InternalWithdrawalId id,
                         AccountId fromAccount,
                         Destination destination,
                         Amount amount,
                         WithdrawalService.WithdrawalId extWithdrawalId,
                         InternalWithdrawalState state
) {

    public static Withdrawal initialize(InternalWithdrawalId id,
                                        AccountId fromAccount,
                                        Destination destination,
                                        Amount amount) {
        return new Withdrawal(id, fromAccount, destination, amount, new WithdrawalService.WithdrawalId(UUID.randomUUID()), PROCESSING);
    }

    public Withdrawal moveTo(InternalWithdrawalState newState) {
        return Withdrawal.moveTo(this, newState);
    }

    private static Withdrawal moveTo(Withdrawal copyOf, InternalWithdrawalState newState) {
        return new Withdrawal(copyOf.id, copyOf.fromAccount, copyOf.destination, copyOf.amount, copyOf.extWithdrawalId, newState);
    }
}
