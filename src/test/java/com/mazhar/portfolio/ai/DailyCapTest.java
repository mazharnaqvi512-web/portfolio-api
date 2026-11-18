package com.mazhar.portfolio.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class DailyCapTest {

    /** A clock the test can move forward. */
    private static final class MovableClock extends Clock {
        private Instant now;

        MovableClock(String instant) {
            this.now = Instant.parse(instant);
        }

        void set(String instant) {
            this.now = Instant.parse(instant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void allowsExactlyTheCapWithinADay() {
        DailyCap cap = new DailyCap(3, new MovableClock("2026-11-04T08:00:00Z"));

        assertThat(cap.tryAcquire()).isTrue();
        assertThat(cap.tryAcquire()).isTrue();
        assertThat(cap.tryAcquire()).isTrue();
        assertThat(cap.tryAcquire()).isFalse();
        assertThat(cap.tryAcquire()).isFalse();
    }

    @Test
    void startsAFreshBudgetAtUtcMidnight() {
        MovableClock clock = new MovableClock("2026-11-04T23:59:00Z");
        DailyCap cap = new DailyCap(1, clock);
        assertThat(cap.tryAcquire()).isTrue();
        assertThat(cap.tryAcquire()).isFalse();

        clock.set("2026-11-05T00:00:01Z");

        assertThat(cap.tryAcquire()).isTrue();
        assertThat(cap.tryAcquire()).isFalse();
    }

    @Test
    void releaseGivesASlotBack() {
        DailyCap cap = new DailyCap(1, new MovableClock("2026-11-04T08:00:00Z"));
        assertThat(cap.tryAcquire()).isTrue();
        assertThat(cap.tryAcquire()).isFalse();

        cap.release();

        assertThat(cap.tryAcquire()).isTrue();
        assertThat(cap.tryAcquire()).isFalse();
    }

    @Test
    void releasingWithNothingAcquiredNeverCreatesExtraCapacity() {
        DailyCap cap = new DailyCap(1, new MovableClock("2026-11-04T08:00:00Z"));

        cap.release();
        cap.release();

        assertThat(cap.tryAcquire()).isTrue();
        assertThat(cap.tryAcquire()).isFalse();
    }

    @Test
    void refusesACapBelowOne() {
        assertThatThrownBy(() -> new DailyCap(0, Clock.systemUTC())).isInstanceOf(IllegalArgumentException.class);
    }
}
