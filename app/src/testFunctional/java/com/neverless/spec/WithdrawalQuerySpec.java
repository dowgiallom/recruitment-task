package com.neverless.spec;

import com.neverless.domain.account.AccountId;
import com.neverless.domain.withdrawal.Destination;
import com.neverless.domain.withdrawal.InternalWithdrawalId;
import com.neverless.domain.withdrawal.InternalWithdrawalState;
import com.neverless.integration.WithdrawalService;
import com.neverless.spec.stubs.WithdrawalServiceStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.neverless.domain.withdrawal.InternalWithdrawalState.COMPLETED;
import static com.neverless.domain.withdrawal.InternalWithdrawalState.FAILED;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class WithdrawalQuerySpec extends FunctionalSpec {

    private final WithdrawalServiceStub withdrawalServiceStub;
    private AccountId accountId;
    private int initialFunds;
    private InternalWithdrawalId withdrawalId;

    protected WithdrawalQuerySpec(ApplicationContext context) {
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
    void unknown_withdrawal_should_return_404() throws Exception {
        final var accountId = AccountId.random();
        final var withdrawalId = InternalWithdrawalId.random();

        // when
        final var response = when().get("/accounts/{accountId}/withdrawals/{withdrawalId}", accountId.value(), withdrawalId.value())
            .thenReturn();

        // then
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @ParameterizedTest
    @EnumSource(WithdrawalService.WithdrawalState.class)
    void should_mirror_external_withdrawal_state(WithdrawalService.WithdrawalState externalState) throws Exception {
        // precondition - a known Withdrawal is registered
        String extWithdrawalId = withdrawalRequested();

        // given
        withdrawalServiceStub.registerResponse(extWithdrawalId, externalState);

        // when
        final var response = when().get("/accounts/{accountId}/withdrawals/{withdrawalId}", accountId.value(), withdrawalId.value())
            .thenReturn();

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThatJson(response.body().asString()).isEqualTo(
            """
            {
                "id": "%s",
                "state": "%s"
            }
            """.formatted(withdrawalId.value(), InternalWithdrawalState.from(externalState))
        );
    }

    @Test
    void continuous_polling_should_return_final_state_eventually() {
        // given
        withdrawalRequested();

        // when then
        await().until(() -> when()
            .get("/accounts/{accountId}/withdrawals/{withdrawalId}", accountId.value(), withdrawalId.value())
            .thenReturn()
            .jsonPath()
            .getString("state"), result -> result.equals(COMPLETED.toString()) || result.equals(FAILED.toString()));
    }

    @Test
    void failed_withdrawal_service_call_results_in_failed_state() {
        // given
        withdrawalServiceStub.expectDowntime();
        withdrawalRequested();

        // when
        final var response = when().get("/accounts/{accountId}/withdrawals/{withdrawalId}", accountId.value(), withdrawalId.value())
            .thenReturn();

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThatJson(response.body().asString()).isEqualTo(
            """
            {
                "id": "%s",
                "state": "%s"
            }
            """.formatted(withdrawalId.value(), InternalWithdrawalState.FAILED)
        );
    }

    private String withdrawalRequested() {
        withdrawalId = InternalWithdrawalId.random();
        return when()
            .body("""
                {
                    "id": "%s",
                    "destination": "%s",
                    "amount": "%s"
                }
                """.formatted(withdrawalId.value(), Destination.random().value(), 1))
            .post("/accounts/{accountId}/withdrawals", accountId.value())
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("externalWithdrawalId");
    }
}
