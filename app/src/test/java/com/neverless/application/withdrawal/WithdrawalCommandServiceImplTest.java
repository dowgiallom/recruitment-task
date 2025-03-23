package com.neverless.application.withdrawal;

import com.neverless.application.account.AccountImpl;
import com.neverless.application.account.InMemoryAccountRepository;
import com.neverless.domain.Amount;
import com.neverless.domain.account.Account;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.withdrawal.Destination;
import com.neverless.domain.withdrawal.InternalWithdrawalId;
import com.neverless.domain.withdrawal.InternalWithdrawalState;
import com.neverless.domain.withdrawal.Withdrawal;
import com.neverless.exceptions.InsufficientFundsException;
import com.neverless.integration.WithdrawalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class WithdrawalCommandServiceImplTest {

    private WithdrawalCommandServiceImpl withdrawalCommandService;
    private InMemoryWithdrawalRepository withdrawalRepository;

    private Account account;
    private Withdrawal withdrawal;
    private InternalWithdrawalId withdrawalId;

    @BeforeEach
    void setUp() {
        withdrawalRepository = new InMemoryWithdrawalRepository();
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();

        WithdrawalService withdrawalService = mock(WithdrawalService.class);

        // Initialize the WithdrawalCommandServiceImpl
        withdrawalCommandService = new WithdrawalCommandServiceImpl(
            withdrawalRepository,
            accountRepository,
            withdrawalService
        );

        // Create a sample Account with a balance
        account = new AccountImpl(AccountId.random(), new Amount(200)); // Account with 200 balance
        accountRepository.create(account);

        // Create a sample Withdrawal
        withdrawalId = InternalWithdrawalId.random();
        withdrawal = Withdrawal.initialize(withdrawalId, account.id(), new Destination("address123"), new Amount(100));
    }
    @Test
    void testInitializeWithdrawal_Successful() {
        Withdrawal initializedWithdrawal = withdrawalCommandService.initialize(withdrawal);

        assertThat(withdrawalRepository.find(withdrawalId))
            .as("Withdrawal should be created and stored in the repository.")
            .isPresent();

        assertThat(account.balance().value())
            .as("Account balance should be deducted correctly.")
            .isEqualTo(100);

        assertThat(initializedWithdrawal.state())
            .as("Withdrawal state should be 'PROCESSING' initially.")
            .isEqualTo(InternalWithdrawalState.PROCESSING);
    }

    @Test
    void testInitializeWithdrawal_InsufficientFunds() {
        // when
        Withdrawal withdrawalWithInsufficientFunds = Withdrawal.initialize(withdrawalId, account.id(), new Destination("address124"), new Amount(300));

        // Expecting an InsufficientFundsException to be thrown
        assertThatThrownBy(() -> withdrawalCommandService.initialize(withdrawalWithInsufficientFunds))
            .as("Expected initialize() to throw InsufficientFundsException, but it didn't")
            .isInstanceOf(InsufficientFundsException.class)
            .hasMessageContaining("Insufficient funds");
    }

    @Test
    void testOnStateUpdated_toFailed() {
        // given
        Withdrawal initialState = Withdrawal.initialize(withdrawalId, account.id(), new Destination("address125"), new Amount(100));
        withdrawalCommandService.initialize(initialState);

        // when
        Withdrawal failedWithdrawal = initialState.moveTo(InternalWithdrawalState.FAILED);
        Withdrawal updatedWithdrawal = withdrawalCommandService.onStateUpdated(failedWithdrawal);

        // Verify that the withdrawal was updated and the funds were restored
        assertThat(updatedWithdrawal.state())
            .as("Withdrawal state should be 'FAILED'.")
            .isEqualTo(InternalWithdrawalState.FAILED);
        assertThat(account.balance().value())
            .as("Account balance should be restored after failed withdrawal.")
            .isEqualTo(200);
    }

    @Test
    void testOnStateUpdated_toCompleted() {
        // Given
        Withdrawal init = Withdrawal.initialize(withdrawalId, account.id(), new Destination("address126"), new Amount(50));
        withdrawalCommandService.initialize(init);

        // Simulate state update to "COMPLETED"
        Withdrawal completed = init.moveTo(InternalWithdrawalState.COMPLETED);
        Withdrawal updatedWithdrawal = withdrawalCommandService.onStateUpdated(completed);

        // Verify that the withdrawal state was updated correctly
        assertThat(updatedWithdrawal.state())
            .as("Withdrawal state should be 'SUCCESS'.")
            .isEqualTo(InternalWithdrawalState.COMPLETED);
        assertThat(account.balance().value())
            .as("Account balance should remain deducted after successful withdrawal.")
            .isEqualTo(150);
    }
}
