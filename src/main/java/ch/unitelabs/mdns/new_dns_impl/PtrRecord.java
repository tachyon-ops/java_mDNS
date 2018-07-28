package ch.unitelabs.mdns.new_dns_impl;

import ch.unitelabs.mdns.new_dns_impl.io.MessageOutputStream;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import static ch.unitelabs.mdns.utils.MulticastDNS.TYPE_PTR;


/**
 * Pointer Record (PTR) - maps a domain name representing an Internet Address to a hostname.
 */
public final class PtrRecord extends DnsRecord {

    /** target */
    private final String target;

    /**
     * Constructor.
     *
     * @param aName record name
     * @param aClass record class
     * @param aTtl record time-to-live
     * @param now current instant
     * @param aTarget pointer target
     */
    public PtrRecord(final String aName, final short aClass, final Duration aTtl, final Instant now,
                     final String aTarget) {
        super(aName, TYPE_PTR, aClass, aTtl, now);
        Objects.requireNonNull(aTarget);
        target = aTarget;
    }

    @Override
    public final String toString() {
        return "PtrRecord [name="
            + name()
            + ", type="
            + type()
            + ", class="
            + clazz()
            + ", ttl="
            + ttl()
            + ", target="
            + target
            + "]";
    }

    @Override
    protected final void write(final MessageOutputStream mos) {
        mos.writeName(target);
    }

    /**
     * @return pointer target.
     */
    final String target() {
        return target;
    }
}
