package com.neverless.spec.stubs;

import com.neverless.domain.Amount;
import com.neverless.integration.WithdrawalService;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

import static com.neverless.integration.WithdrawalService.WithdrawalState.COMPLETED;
import static com.neverless.integration.WithdrawalService.WithdrawalState.FAILED;
import static com.neverless.integration.WithdrawalService.WithdrawalState.PROCESSING;

/**
 * This is a sample stub for withdrawal service.
 * You can use this one or implement your own, given it follow correct specification
 */
public class WithdrawalServiceStub implements WithdrawalService {
    private final ConcurrentMap<WithdrawalId, Withdrawal> requests = new ConcurrentHashMap<>();
    private boolean expectDowntime = false;

    @Override
    public void requestWithdrawal(WithdrawalId id, Address address, Amount amount) {
        if (expectDowntime) {
            throw new RuntimeException("500 Service Unavailable");
        }
        final var existing = requests.putIfAbsent(id, new Withdrawal(finalState(), finaliseAt(), address, amount));
        if (existing != null && !(Objects.equals(existing.address, address) && Objects.equals(existing.amount, amount)))
            throw new IllegalStateException("Withdrawal request with id[%s] is already present".formatted(id));
    }

    private WithdrawalState finalState() {
        return ThreadLocalRandom.current().nextBoolean() ? COMPLETED : FAILED;
    }

    private long finaliseAt() {
        return System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(1000, 10000);
    }

    @Override
    public WithdrawalState getRequestState(WithdrawalId id) {
        final var request = requests.get(id);
        if (request == null)
            throw new IllegalArgumentException("Request %s is not found".formatted(id));
        return request.finalState();
    }

    public void reset() {
        requests.clear();
        expectDowntime = false;
    }

    public int invocationCount() {
        return requests.size();
    }

    public void registerResponse(String extWithdrawalId, WithdrawalState externalState) {
        requests.computeIfPresent(
            new WithdrawalId(UUID.fromString(extWithdrawalId)),
            (wid, old) -> new Withdrawal(externalState, 0, old.address, old.amount)
        );
    }

    public void expectDowntime() {
        expectDowntime = true;
    }

    public Withdrawal getRequest(WithdrawalId withdrawalId) {
        return requests.get(withdrawalId);
    }

    public record Withdrawal(WithdrawalState state, long finaliseAt, Address address, Amount amount) {
        public WithdrawalState finalState() {
            return finaliseAt <= System.currentTimeMillis() ? state : PROCESSING;
        }
    }
}

