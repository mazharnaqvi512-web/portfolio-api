package com.mazhar.portfolio.ai;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Global ceiling on assistant calls per UTC day. The per-address rate limit stops one visitor
 * hammering the endpoint; this stops many addresses from running up the bill. In memory, so it
 * resets on restart — an upper bound on spend per process per day, not an exact meter.
 */
public class DailyCap {

    private final int maxPerDay;
    private final Clock clock;
    private LocalDate day;
    private int used;

    public DailyCap(int maxPerDay, Clock clock) {
        if (maxPerDay < 1) {
            throw new IllegalArgumentException("maxPerDay must be at least 1");
        }
        this.maxPerDay = maxPerDay;
        this.clock = clock;
    }

    public synchronized boolean tryAcquire() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(day)) {
            day = today;
            used = 0;
        }
        if (used >= maxPerDay) {
            return false;
        }
        used++;
        return true;
    }
}
