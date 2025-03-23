package com.neverless.application.withdrawal;

import com.neverless.domain.Amount;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.withdrawal.Destination;
import com.neverless.domain.withdrawal.InternalWithdrawalId;
import com.neverless.domain.withdrawal.InternalWithdrawalState;
import com.neverless.domain.withdrawal.Withdrawal;
import com.neverless.domain.withdrawal.WithdrawalCommandService;
import com.neverless.domain.withdrawal.WithdrawalRepository;
import com.neverless.exceptions.NotFoundException;
import com.neverless.integration.WithdrawalService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static com.neverless.domain.withdrawal.InternalWithdrawalState.COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WithdrawalQueryServiceImplTest {

    private WithdrawalQueryServiceImpl underTest;

    private final WithdrawalRepository withdrawalRepository = mock(WithdrawalRepository.class);
    private final WithdrawalService withdrawalService = mock(WithdrawalService.class);
    private final WithdrawalCommandService withdrawalCommandService = mock(WithdrawalCommandService.class);

    @BeforeEach
    void setUp() {
        underTest = new WithdrawalQueryServiceImpl(withdrawalRepository, withdrawalService, withdrawalCommandService);
    }

    @Test
    void unknownWithdrawalGet_throwsNotFoundException() {
        // when then
        assertThrows(NotFoundException.class, () -> underTest.getState(InternalWithdrawalId.random()));
    }

    @ParameterizedTest
    @EnumSource
    void finalStateIsReturned(InternalWithdrawalState expectedState) {
        // assume
        Assumptions.assumeTrue(expectedState.isFinal());

        // given
        InternalWithdrawalId withdrawalId = InternalWithdrawalId.random();
        Withdrawal withdrawal = getWithdrawal(expectedState, withdrawalId);
        when(withdrawalRepository.find(withdrawalId)).thenReturn(Optional.of(withdrawal));

        // when
        InternalWithdrawalState state = underTest.getState(withdrawalId);

        // then
        assertThat(state).isEqualTo(expectedState);
        verifyNoInteractions(withdrawalService, withdrawalCommandService);
    }

    @Test
    void nonFinalStateIsCheckedExternally() {
        // given
        Withdrawal withdrawal = arrangeInternalState(InternalWithdrawalState.PROCESSING);
        arrangeExternalState(withdrawal, WithdrawalService.WithdrawalState.COMPLETED);
        mockCommandService();

        // when
        InternalWithdrawalState result = underTest.getState(withdrawal.id());

        // then
        assertThat(result).isEqualTo(COMPLETED);
    }

    @Test
    void stateIsUpdatedWhenMovedToFinal() {
        // given
        Withdrawal withdrawal = arrangeInternalState(InternalWithdrawalState.PROCESSING);
        arrangeExternalState(withdrawal, WithdrawalService.WithdrawalState.COMPLETED);
        mockCommandService();

        // when
        underTest.getState(withdrawal.id());

        // then
        ArgumentCaptor<Withdrawal> captor = ArgumentCaptor.forClass(Withdrawal.class);
        verify(withdrawalCommandService).onStateUpdated(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(withdrawal.id());
        assertThat(captor.getValue().state()).isEqualTo(COMPLETED);
    }

    private Withdrawal arrangeInternalState(InternalWithdrawalState state) {
        InternalWithdrawalId withdrawalId = InternalWithdrawalId.random();
        Withdrawal withdrawal = getWithdrawal(state, withdrawalId);
        when(withdrawalRepository.find(withdrawalId)).thenReturn(Optional.of(withdrawal));
        return withdrawal;
    }

    private void arrangeExternalState(Withdrawal withdrawal, WithdrawalService.WithdrawalState withdrawalState) {
        when(withdrawalService.getRequestState(withdrawal.extWithdrawalId())).thenReturn(withdrawalState);
    }

    private void mockCommandService() {
        doAnswer(invocationOnMock -> invocationOnMock.getArgument(0))
            .when(withdrawalCommandService).onStateUpdated(any());
    }

    @NotNull
    private static Withdrawal getWithdrawal(InternalWithdrawalState internalWithdrawalState, InternalWithdrawalId withdrawalId) {
        return new Withdrawal(
            withdrawalId,
            AccountId.random(),
            Destination.random(),
            new Amount(1),
            new WithdrawalService.WithdrawalId(UUID.randomUUID()),
            internalWithdrawalState
        );
    }
}
