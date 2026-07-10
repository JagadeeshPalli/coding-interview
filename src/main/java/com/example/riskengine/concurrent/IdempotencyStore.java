package com.example.riskengine.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class IdempotencyStore {
    private final ConcurrentHashMap<String, AtomicReference<State>> states = new ConcurrentHashMap<>();

    public boolean markInProgress(String transactionId) {
        AtomicReference<State> state = states.computeIfAbsent(transactionId, ignored -> new AtomicReference<>(State.NEW));
        return state.compareAndSet(State.NEW, State.IN_PROGRESS);
    }

    public void markCompleted(String transactionId) {
        states.computeIfPresent(transactionId, (ignored, state) -> {
            state.set(State.COMPLETED);
            return state;
        });
    }

    private enum State {
        NEW,
        IN_PROGRESS,
        COMPLETED
    }
}
