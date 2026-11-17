package com.mazhar.portfolio.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class SlidingWindowRateLimiterTest {

    /** Hand-advanced clock so window behavior is tested without sleeping. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();
    private final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(3, Duration.ofMinutes(10), clock);

    @Test
    void allowsRequestsUpToTheLimit() {
        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
    }

    @Test
    void rejectsTheRequestOverTheLimitAndSaysHowLongToWait() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("a");
        }
        clock.advance(Duration.ofMinutes(4));

        SlidingWindowRateLimiter.Decision decision = limiter.tryAcquire("a");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.retryAfterSeconds()).isEqualTo(Duration.ofMinutes(6).toSeconds());
    }

    @Test
    void tracksEachKeyIndependently() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("a");
        }

        assertThat(limiter.tryAcquire("a").allowed()).isFalse();
        assertThat(limiter.tryAcquire("b").allowed()).isTrue();
    }

    @Test
    void allowsAgainOnceTheWindowHasPassed() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("a");
        }
        assertThat(limiter.tryAcquire("a").allowed()).isFalse();

        clock.advance(Duration.ofMinutes(10).plusSeconds(1));

        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
    }

    @Test
    void rejectedAttemptsDoNotExtendTheWait() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("a");
        }
        clock.advance(Duration.ofMinutes(9));
        for (int i = 0; i < 20; i++) {
            assertThat(limiter.tryAcquire("a").allowed()).isFalse();
        }

        clock.advance(Duration.ofMinutes(1).plusSeconds(1));

        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
    }

    @Test
    void rejectsAnInvalidLimit() {
        assertThatThrownBy(() -> new SlidingWindowRateLimiter(0, Duration.ofMinutes(1), clock))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
