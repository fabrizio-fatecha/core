package com.core.resilience;

import com.core.error.TooBusyException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DbBulkhead {

    private static final long WAIT_MILLIS = 200;

    private final Semaphore permits;

    public DbBulkhead(@Value("${app.bulkhead.permits:10}") int permits) {
        this.permits = new Semaphore(permits);
    }

    public <T> T call(Supplier<T> work) {
        try {
            if (!permits.tryAcquire(WAIT_MILLIS, TimeUnit.MILLISECONDS)) {
                throw new TooBusyException();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TooBusyException();
        }
        try {
            return work.get();
        } finally {
            permits.release();
        }
    }
}
