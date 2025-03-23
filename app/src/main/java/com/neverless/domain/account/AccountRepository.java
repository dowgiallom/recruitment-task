package com.neverless.domain.account;

import java.util.Optional;

public interface AccountRepository {
    Optional<Account> find(AccountId id);
    void create(Account account);
}
