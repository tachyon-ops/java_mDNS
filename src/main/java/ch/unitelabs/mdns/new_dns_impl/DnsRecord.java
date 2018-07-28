package ch.unitelabs.mdns.new_dns_impl;

import ch.unitelabs.mdns.new_dns_impl.io.MessageOutputStream;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A DNS record.
 */
public abstract class DnsRecord extends DnsEntry {

    /** time-to-live */
    private Duration ttl;

    /** creation instant. */
    private final Instant ioc;

    /**
     * Constructor.
     *
     * @param aName record name
     * @param aType record type
     * @param aClass record class
     * @param aTtl record time-to-live
     * @param now current instant
     */
    public DnsRecord(final String aName, final short aType, final short aClass, final Duration aTtl,
            final Instant now) {
        super(aName, aType, aClass);
        Objects.requireNonNull(aTtl);
        Objects.requireNonNull(now);
        ttl = aTtl;
        ioc = now;
    }

    /**
     * Writes this record to the given stream.
     *
     * @param mos stream
     */
    protected abstract void write(final MessageOutputStream mos);

    /**
     * Returns the time at which this record will have expired by the given percentage.
     *
     * @param percent TTL percentage in the range [0 .. 100]
     * @return time at which this record will have expired by the given percentage
     */
    final Instant expirationTime(final int percent) {
        final long ttlPercent = (long) (ttl.toNanos() * (percent / 100.0));
        return ioc.plus(Duration.ofNanos(ttlPercent));
    }

    /**
     * Determines whether this record has expired: now +
     *
     * @param now current time
     * @return {@code true} iff this record has expired Returns true if this record has expired.
     */
    public final boolean isExpired(final Instant now) {
        final Instant t = expirationTime(100);
        return t.equals(now) || t.isBefore(now);
    }

    /**
     * Returns the remaining TTL duration.
     *
     * @param now current time
     * @return the remaining TTL duration
     */
    final Duration remainingTtl(final Instant now) {
        final Duration dur = Duration.between(now, expirationTime(100));
        if (dur.isNegative()) {
            return Duration.ZERO;
        }
        return dur;
    }

    /**
     * Sets TTL to given value.
     *
     * @param aTtl new TTL
     */
    public final void setTtl(final Duration aTtl) {
        ttl = aTtl;
    }

    /**
     * Determines whether any answer, authority or additional in the given message can suffice for the information
     * held in this record.
     *
     * @param msg DNS message
     * @return {@code true} iff the given message suppresses this record
     */
    final boolean suppressedBy(final DnsMessage msg) {
        return msg.answers().stream().anyMatch(this::suppressedBy);
    }

    /**
     * Determines whether the given record has same name, type and class, and if its TTL is at least half of this
     * record.
     *
     * @param other other DNS record
     * @return {@code true} iff the given record suppresses this record
     */
    final boolean suppressedBy(final DnsRecord other) {
        return name().equals(other.name())
            && type() == other.type()
            && clazz() == other.clazz()
            && other.ttl.compareTo(ttl.dividedBy(2)) >= 0;
    }

    /**
     * @return the time-to-live (TTL).
     */
    public final Duration ttl() {
        return ttl;
    }

}
