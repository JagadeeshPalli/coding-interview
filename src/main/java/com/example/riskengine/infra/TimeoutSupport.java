package com.example.riskengine.infra;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class TimeoutSupport {
    private TimeoutSupport() {
    }

    static <T> CompletableFuture<T> withTimeout(
            CompletableFuture<T> source,
            Duration timeout,
            ScheduledExecutorService scheduler
    ) {
        CompletableFuture<T> timeoutFuture = new CompletableFuture<>();
        var timeoutTask = scheduler.schedule(
                () -> timeoutFuture.completeExceptionally(new TimeoutException("Operation timed out after " + timeout)),
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
        );

        return source
                .applyToEither(timeoutFuture, value -> value)
                .whenComplete((ignored, failure) -> timeoutTask.cancel(false));
    }
}
