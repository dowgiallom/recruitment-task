package com.neverless.resources;

import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.neverless.domain.account.AccountRepository;
import com.neverless.domain.withdrawal.WithdrawalCommandService;
import com.neverless.domain.withdrawal.WithdrawalQueryService;
import com.neverless.exceptions.InsufficientFundsException;
import com.neverless.exceptions.NotFoundException;
import io.javalin.router.JavalinDefaultRouting;

public class Resources {
    private final Healthcheck healthcheck;
    private final Accounts accounts;
    private final Withdrawals withdrawals;

    public Resources(AccountRepository accountRepo,
                     WithdrawalCommandService withdrawalCommandService,
                     WithdrawalQueryService withdrawalQueryService) {
        healthcheck = new Healthcheck();
        accounts = new Accounts(accountRepo);
        withdrawals = new Withdrawals(withdrawalCommandService, withdrawalQueryService);
    }

    public void register(JavalinDefaultRouting router) {
        router.exception(NotFoundException.class, (ex, ctx) -> ctx.status(404));
        router.exception(InsufficientFundsException.class, (ex, ctx) -> ctx.status(422));
        router.exception(IllegalArgumentException.class, (ex, ctx) -> ctx.status(400));
        router.exception(ValueInstantiationException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("Invalid request", e.getOriginalMessage()));
        });

        router.get("/healthcheck", healthcheck::check);
        router.get("/accounts/{id}", accounts::get);
        router.post("/accounts", accounts::post);
        router.post("/accounts/{accountId}/withdrawals", withdrawals::post);
        router.get("/accounts/{accountId}/withdrawals/{withdrawalId}", withdrawals::get);

        // TODO-MD can be extended with SSE capability for streaming withdrawal state updates
//        router.sse("/accounts/{accountId}/withdrawals/{withdrawalId}/updates", withdrawals::stream);
    }

    public record ErrorResponse(String error, String message) {}
}
