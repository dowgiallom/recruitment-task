package com.neverless.domain.account;

import com.neverless.domain.Amount;

public interface Account {
    AccountId id();
    Amount balance();
    void deduct(Amount amount);
    void add(Amount amount);

}
