package com.neverless.domain.withdrawal;

import com.neverless.exceptions.NotFoundException;

public interface WithdrawalQueryService {
    /**
     * Queries current state for given withdrawal id.
     * @param internalWithdrawalId id
     * @return current withdrawal state
     * @throws NotFoundException when withdrawal does not exist
     */
    InternalWithdrawalState getState(InternalWithdrawalId internalWithdrawalId);
}
