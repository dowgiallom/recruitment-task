package com.neverless.application.withdrawal;

import com.neverless.domain.account.Account;
import com.neverless.domain.account.AccountRepository;
import com.neverless.domain.withdrawal.WithdrawalCommandService;
import com.neverless.domain.withdrawal.Withdrawal;
import com.neverless.domain.withdrawal.WithdrawalRepository;
import com.neverless.exceptions.InsufficientFundsException;
import com.neverless.integration.WithdrawalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.neverless.domain.withdrawal.InternalWithdrawalState.FAILED;
import static com.neverless.domain.withdrawal.InternalWithdrawalState.PROCESSING;

public class WithdrawalCommandServiceImpl implements WithdrawalCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WithdrawalCommandServiceImpl.class);

    private final WithdrawalRepository withdrawalRepository;
    private final AccountRepository accountRepository;
    private final WithdrawalService withdrawalService;

    public WithdrawalCommandServiceImpl(WithdrawalRepository withdrawalRepository,
                                        AccountRepository accountRepository,
                                        WithdrawalService withdrawalService) {
        this.withdrawalRepository = withdrawalRepository;
        this.accountRepository = accountRepository;
        this.withdrawalService = withdrawalService;
    }

    @Override
    public Withdrawal initialize(Withdrawal withdrawal) {
        LOGGER.info("Requesting external Withdrawal {}", withdrawal);
        try {
            final var account = getAccount(withdrawal);
            account.deduct(withdrawal.amount());
            withdrawalRepository.create(withdrawal);
            withdrawalService.requestWithdrawal(withdrawal.extWithdrawalId(), withdrawal.destination().extAddress(), withdrawal.amount());
            return withdrawal;
        } catch (InsufficientFundsException e) {
            LOGGER.warn("Withdrawal request rejected {} - insufficient funds", withdrawal, e);
            throw e;
        } catch (Exception e) {
            Withdrawal updatedWithdrawal = onStateUpdated(withdrawal.moveTo(FAILED));
            LOGGER.error("Failed to request Withdrawal {}", updatedWithdrawal, e);
            return updatedWithdrawal;
        }
    }

    @Override
    public Withdrawal onStateUpdated(Withdrawal withdrawal) {
        var previous = withdrawalRepository.update(withdrawal);
        if (withdrawal.state() == FAILED && previous.state() == PROCESSING) {
            Account account = getAccount(withdrawal);
            account.add(withdrawal.amount());
        }
        return withdrawal;
    }

    private Account getAccount(Withdrawal withdrawal) {
        return accountRepository.find(withdrawal.fromAccount())
            .orElseThrow(() -> new IllegalArgumentException("Unknown %s".formatted(withdrawal.fromAccount())));
    }
}
