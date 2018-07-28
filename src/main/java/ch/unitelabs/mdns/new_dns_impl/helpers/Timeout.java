package ch.unitelabs.mdns.new_dns_impl.helpers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class Timeout {

    /** remaining duration. */
    private final Duration duration;

    /** when the timeout was created, milliseconds from the epoch of 1970-01-01T00:00Z. */
    private final long start;

    /**
     * Class constructor.
     *
     * @param initialDuration initial duration, not null
     */
    public Timeout(final Duration initialDuration) {
        Objects.requireNonNull(initialDuration);
        duration = initialDuration;
        start = System.nanoTime();
    }

    /**
     * Returns a new {@link Timeout timeout} starting at the given {@code initialDuration}.
     *
     * @param initialDuration initial duration, not null
     * @return a new {@link Timeout timeout}
     */
    static Timeout of(final Duration initialDuration) {
        return new Timeout(initialDuration);
    }

    /**
     * Assess and returns the remaining duration or {@link Duration#ZERO} if the initial duration has elapsed.
     *
     * @return the remaining duration
     */
    final Duration remaining() {
        final long elapsedNs = System.nanoTime() - start;
        Duration remaining = duration.minus(elapsedNs, ChronoUnit.NANOS);
        if (remaining.isNegative()) {
            remaining = Duration.ZERO;
        }
        return remaining;
    }

}
