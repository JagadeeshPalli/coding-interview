package com.example.riskengine.concurrent;

import com.example.riskengine.domain.Transaction;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public final class VelocityStore {
    private final Duration window;
    private final ConcurrentHashMap<String, AccountWindow> windows = new ConcurrentHashMap<>();

    public VelocityStore(Duration window) {
        this.window = window;
    }

    public Snapshot record(Transaction transaction) {
        AccountWindow accountWindow = windows.computeIfAbsent(transaction.accountId(), ignored -> new AccountWindow());
        accountWindow.events.add(new Event(transaction.timestamp(), transaction.amount().longValueExact()));
        accountWindow.total.addAndGet(transaction.amount().longValueExact());
        evictExpired(accountWindow, transaction.timestamp().minus(window));
        return new Snapshot(accountWindow.total.get(), accountWindow.events.size());
    }

    private void evictExpired(AccountWindow accountWindow, Instant cutoff) {
        Event head = accountWindow.events.peek();
        while (head != null && head.timestamp().isBefore(cutoff)) {
            Event removed = accountWindow.events.poll();
            if (removed == null) {
                return;
            }
            accountWindow.total.addAndGet(-removed.amount());
            head = accountWindow.events.peek();
        }
    }

    public record Snapshot(long spend, int count) {
    }

    private record Event(Instant timestamp, long amount) {
    }

    private static final class AccountWindow {
        private final ConcurrentLinkedQueue<Event> events = new ConcurrentLinkedQueue<>();
        private final AtomicLong total = new AtomicLong();
    }
}
