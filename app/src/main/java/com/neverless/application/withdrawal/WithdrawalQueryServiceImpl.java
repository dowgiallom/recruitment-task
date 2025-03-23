package com.neverless.application.withdrawal;

import com.neverless.domain.withdrawal.InternalWithdrawalId;
import com.neverless.domain.withdrawal.InternalWithdrawalState;
import com.neverless.domain.withdrawal.Withdrawal;
import com.neverless.domain.withdrawal.WithdrawalQueryService;
import com.neverless.domain.withdrawal.WithdrawalCommandService;
import com.neverless.domain.withdrawal.WithdrawalRepository;
import com.neverless.exceptions.NotFoundException;
import com.neverless.integration.WithdrawalService;

public class WithdrawalQueryServiceImpl implements WithdrawalQueryService {

    private final WithdrawalRepository withdrawalRepository;
    private final WithdrawalService externalWithdrawalService;
    private final WithdrawalCommandService withdrawalCommandService;

    public WithdrawalQueryServiceImpl(WithdrawalRepository withdrawalRepository,
                                      WithdrawalService externalWithdrawalService,
                                      WithdrawalCommandService withdrawalCommandService) {
        this.withdrawalRepository = withdrawalRepository;
        this.externalWithdrawalService = externalWithdrawalService;
        this.withdrawalCommandService = withdrawalCommandService;
    }

    @Override
    public InternalWithdrawalState getState(InternalWithdrawalId internalWithdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.find(internalWithdrawalId)
            .orElseThrow(() -> new NotFoundException("Unknown withdrawal: %s".formatted(internalWithdrawalId)));

        if (withdrawal.state().isFinal()) {
            return withdrawal.state();
        } else {
            // TODO-MD external service calls should be rate limited (fallback to internal state on breach)
            //  so the external service does not ban us
            return fetchAndUpdate(withdrawal);
        }
    }

    private InternalWithdrawalState fetchAndUpdate(Withdrawal withdrawal) {
        WithdrawalService.WithdrawalState extState = externalWithdrawalService.getRequestState(withdrawal.extWithdrawalId());
        InternalWithdrawalState newState = InternalWithdrawalState.from(extState);
        Withdrawal updated = withdrawal.moveTo(newState);
        return withdrawalCommandService.onStateUpdated(updated).state();
    }
}
