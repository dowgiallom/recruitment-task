package com.neverless.resources;

import com.neverless.domain.Amount;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.withdrawal.Destination;
import com.neverless.domain.withdrawal.InternalWithdrawalId;
import com.neverless.domain.withdrawal.InternalWithdrawalState;
import com.neverless.domain.withdrawal.Withdrawal;
import com.neverless.domain.withdrawal.WithdrawalCommandService;
import com.neverless.domain.withdrawal.WithdrawalQueryService;
import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Withdrawals {

    private final WithdrawalCommandService withdrawalCommandService;
    private final WithdrawalQueryService withdrawalQueryService;

    public Withdrawals(WithdrawalCommandService withdrawalCommandService,
                       WithdrawalQueryService withdrawalQueryService) {
        this.withdrawalCommandService = withdrawalCommandService;
        this.withdrawalQueryService = withdrawalQueryService;
    }

    public void post(@NotNull Context context) {
        final var accountId = AccountId.fromString(context.pathParam("accountId"));
        final var request = context.bodyAsClass(CreateWithdrawalRequest.class);
        Withdrawal init = Withdrawal.initialize(request.id(), accountId, request.destination(), request.amount());
        var result = withdrawalCommandService.initialize(init);
        context.json(new WithdrawalCreatedResult(result));
    }

    public record CreateWithdrawalRequest(InternalWithdrawalId id,
                                          Destination destination,
                                          Amount amount) {}

    public record WithdrawalCreatedResult(InternalWithdrawalId id,
                                          Destination destination,
                                          Amount amount,
                                          InternalWithdrawalState state,
                                          UUID externalWithdrawalId
    ) {

        public WithdrawalCreatedResult(Withdrawal result) {
            this(result.id(), result.destination(), result.amount(), result.state(), result.extWithdrawalId().value());
        }
    }

    public void get(@NotNull Context context) {
        // TODO-MD check if accountId is assigned to given withdrawalId. can be used for authorization checks later.
        final var accountId = AccountId.fromString(context.pathParam("accountId"));

        final var withdrawalId = InternalWithdrawalId.fromString(context.pathParam("withdrawalId"));
        InternalWithdrawalState currentState = withdrawalQueryService.getState(withdrawalId);
        context.json(new WithdrawalResponse(withdrawalId, currentState));
    }

    public record WithdrawalResponse(InternalWithdrawalId id, InternalWithdrawalState state) {}
}
