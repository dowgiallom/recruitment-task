package com.neverless.application.withdrawal;

import com.neverless.domain.withdrawal.InternalWithdrawalId;
import com.neverless.domain.withdrawal.Withdrawal;
import com.neverless.domain.withdrawal.WithdrawalRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryWithdrawalRepository implements WithdrawalRepository {

    private final Map<InternalWithdrawalId, Withdrawal> withdrawals = new ConcurrentHashMap<>();

    @Override
    public Optional<Withdrawal> find(InternalWithdrawalId id) {
        return Optional.ofNullable(withdrawals.get(id));
    }

    @Override
    public void create(Withdrawal withdrawal) {
        if (withdrawals.putIfAbsent(withdrawal.id(), withdrawal) != null) {
            throw new IllegalStateException("Withdrawal %s already exists".formatted(withdrawal.id()));
        }
    }

    @Override
    public Withdrawal update(Withdrawal updated) {
        Withdrawal previous = withdrawals.replace(updated.id(), updated);
        if (previous == null) {
            throw new IllegalStateException("Withdrawal %s does not exist".formatted(updated.id()));
        }
        return previous;
    }
}
