package com.neverless.spec;

import com.neverless.domain.account.AccountId;
import com.neverless.domain.withdrawal.Destination;
import com.neverless.domain.withdrawal.InternalWithdrawalId;
import com.neverless.integration.WithdrawalService;
import com.neverless.spec.stubs.WithdrawalServiceStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class WithdrawalCommandSpec extends FunctionalSpec {

    private final WithdrawalServiceStub withdrawalServiceStub;
    private AccountId accountId;
    private int initialFunds;

    protected WithdrawalCommandSpec(ApplicationContext context) {
        super(context);
        withdrawalServiceStub = context.withdrawalServiceStub;
    }

    @BeforeEach
    void setUp() {
        // reset stub invokations
        withdrawalServiceStub.reset();

        // precondition - Account created with funds
        accountId = AccountId.random();
        initialFunds = 100;
        given()
            .body("""
            {
                "id": "%s",
                "initialBalance": "%s"
            }
            """.formatted(accountId.value(), initialFunds))
            .post("/accounts");

    }

    @Test
    void should_forward_withdrawal_request_to_external_party() throws Exception {
        var withdrawalId = InternalWithdrawalId.random();
        var destination = Destination.random();
        int withdrawalAmount = initialFunds;

        // when
        String externalWithdrawalId = when()
            .body("""
                {
                    "id": "%s",
                    "destination": "%s",
                    "amount": "%s"
                }
                """.formatted(withdrawalId.value(), destination.value(), withdrawalAmount))
            .post("/accounts/{accountId}/withdrawals", accountId.value())
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("externalWithdrawalId");

        // then
        WithdrawalService.WithdrawalId extWithdrawalId = new WithdrawalService.WithdrawalId(UUID.fromString(externalWithdrawalId));
        assertThat(withdrawalServiceStub.getRequestState(extWithdrawalId))
            .isNotNull();
        assertThat(withdrawalServiceStub.getRequest(extWithdrawalId).address().value()).isEqualTo(destination.value());
        assertThat(withdrawalServiceStub.getRequest(extWithdrawalId).amount().value()).isEqualTo(withdrawalAmount);
    }

    @Test
    void should_reject_withdrawing_over_balance() throws Exception {
        var withdrawalId = InternalWithdrawalId.random();
        var destination = Destination.random();
        int withdrawalAmount = initialFunds + 1;

        // when
        when()
            .body("""
                {
                    "id": "%s",
                    "destination": "%s",
                    "amount": "%s"
                }
                """.formatted(withdrawalId.value(), destination.value(), withdrawalAmount))
            .post("/accounts/{accountId}/withdrawals", accountId.value())
            .then()
            .statusCode(422);

        // then
        assertThat(withdrawalServiceStub.invocationCount()).isZero();
    }

    @Test
    void should_reject_withdrawing_negative_amount() throws Exception {
        var withdrawalId = InternalWithdrawalId.random();
        var destination = Destination.random();
        int withdrawalAmount = -1;

        // when
        when()
            .body("""
                {
                    "id": "%s",
                    "destination": "%s",
                    "amount": "%s"
                }
                """.formatted(withdrawalId.value(), destination.value(), withdrawalAmount))
            .post("/accounts/{accountId}/withdrawals", accountId.value())
            .then()
            .statusCode(400);

        // then
        assertThat(withdrawalServiceStub.invocationCount()).isZero();
    }

    @Test
    void should_respond_with_404_when_withdrawal_doesnt_exist() throws Exception {
        final var withdrawalId = InternalWithdrawalId.random();

        // when
        final var response = when().get("/accounts/{accountId}/withdrawals/{withdrawalId}", accountId.value(), withdrawalId.value())
            .thenReturn();

        // then
        assertThat(response.statusCode()).isEqualTo(404);
    }
}
