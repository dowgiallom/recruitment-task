package com.neverless.resources;

import com.neverless.application.account.AccountImpl;
import com.neverless.domain.account.Account;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.account.AccountRepository;
import com.neverless.domain.Amount;
import com.neverless.exceptions.NotFoundException;
import io.javalin.http.Context;

public class Accounts {
    private final AccountRepository accountRepo;

    public Accounts(AccountRepository accountRepo) {
        this.accountRepo = accountRepo;
    }

    public void post(Context context) {
        final var createRequest = context.bodyAsClass(CreateAccountRequest.class);
        Account account = new AccountImpl(createRequest.id(), createRequest.initialBalance());
        accountRepo.create(account);
    }

    public record CreateAccountRequest(AccountId id, Amount initialBalance) {}

    public void get(Context context) {
        final var id = AccountId.fromString(context.pathParam("id"));
        final var account = accountRepo.find(id).orElseThrow(() -> new NotFoundException("%s is not found".formatted(id)));

        context.json(AccountResponse.of(account));
    }

    public record AccountResponse(AccountId id, Amount balance) {
        public static AccountResponse of(Account account) {
            return new AccountResponse(account.id(), account.balance());
        }
    }
}
