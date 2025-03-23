package com.neverless.application.account;

import com.neverless.domain.Amount;
import com.neverless.domain.account.Account;
import com.neverless.domain.account.AccountId;
import com.neverless.exceptions.InsufficientFundsException;

import java.util.concurrent.atomic.AtomicReference;

public class AccountImpl implements Account {

    private final AccountId id;
    private final AtomicReference<Amount> balance;

    public AccountImpl(AccountId id, Amount initialBalance) {
        this.id = id;
        this.balance = new AtomicReference<>(initialBalance);
    }

    @Override
    public AccountId id() {
        return id;
    }

    @Override
    public Amount balance() {
        return balance.get();
    }

    @Override
    public void deduct(Amount amount) {
        balance.updateAndGet(currentBalance -> {
            if (currentBalance.compareTo(amount) < 0) {
                throw new InsufficientFundsException(
                    "Insufficient funds to deduct %s from %s".formatted(amount, id)
                );
            }
            return currentBalance.deduct(amount);
        });
    }

    @Override
    public void add(Amount amount) {
        balance.updateAndGet(currentBalance -> currentBalance.add(amount));
    }
}
