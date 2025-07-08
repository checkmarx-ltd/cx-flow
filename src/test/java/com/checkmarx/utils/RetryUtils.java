package com.checkmarx.utils;

import java.util.function.Supplier;
import java.util.function.Predicate;

public class RetryUtils {

    public static <T> T waitUntil(Supplier<T> supplier, Predicate<T> condition,
                                  int maxRetries, long retryIntervalMs) {
        int retries = 0;
        T result;

        while (retries < maxRetries) {
            result = supplier.get();
            if (condition.test(result)) {
                return result;
            }

            retries++;
            try {
                Thread.sleep(retryIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Retry interrupted", e);
            }
        }

        result = supplier.get();
        return result;
    }
}
