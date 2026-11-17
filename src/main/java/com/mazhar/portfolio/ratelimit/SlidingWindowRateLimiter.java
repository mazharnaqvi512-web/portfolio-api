package com.mazhar.portfolio.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window limiter: at most `maxRequests` per `window` per key.
 * State is per-process, so it resets on restart and isn't shared between
 * instances — fine for a single small API; a shared store (e.g. Redis) would
 * be needed before running multiple instances.
 */
public class SlidingWindowRateLimiter {

    /** Past this many tracked keys, idle ones are swept so a flood of distinct clients can't grow memory forever. */
    private static final int SWEEP_THRESHOLD = 10_000;

    public record Decision(boolean allowed, long retryAfterSeconds) {}

    private final int maxRequests;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Instant>> requestsByKey = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(int maxRequests, Duration window, Clock clock) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("maxRequests must be at least 1");
        }
        this.maxRequests = maxRequests;
        this.window = window;
        this.clock = clock;
    }

    public Decision tryAcquire(String key) {
        Instant now = clock.instant();
        Instant windowStart = now.minus(window);
        Deque<Instant> timestamps = requestsByKey.computeIfAbsent(key, k -> new ArrayDeque<>());

        Decision decision;
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(windowStart)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                // Rejected attempts aren't recorded, so hammering the endpoint can't extend the wait.
                long millisUntilFree = Duration.between(now, timestamps.peekFirst().plus(window)).toMillis();
                decision = new Decision(false, Math.max(1, (long) Math.ceil(millisUntilFree / 1000.0)));
            } else {
                timestamps.addLast(now);
                decision = new Decision(true, 0);
            }
        }

        if (requestsByKey.size() > SWEEP_THRESHOLD) {
            sweepIdleKeys(windowStart);
        }
        return decision;
    }

    private void sweepIdleKeys(Instant windowStart) {
        requestsByKey.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                Instant newest = entry.getValue().peekLast();
                return newest == null || !newest.isAfter(windowStart);
            }
        });
    }
}
