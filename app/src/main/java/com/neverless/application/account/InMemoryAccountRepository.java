package com.neverless.application.account;

import com.neverless.domain.account.Account;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.account.AccountRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAccountRepository implements AccountRepository {

    private final Map<AccountId, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> find(AccountId id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public void create(Account account) {
        if (accounts.putIfAbsent(account.id(), account) != null) {
            throw new IllegalStateException("Account %s already exists".formatted(account.id()));
        }
    }
}
