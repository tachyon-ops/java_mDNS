package ch.unitelabs.mdns.new_dns_impl;

import ch.unitelabs.mdns.new_dns_impl.io.MessageOutputStream;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import static ch.unitelabs.mdns.utils.MulticastDNS.TYPE_TXT;


/**
 * Text (TXT) record.
 */
public final class TxtRecord extends DnsRecord {

    /** attributes. */
    private final Attributes attributes;

    /**
     * Constructor.
     *
     * @param aName record name
     * @param aClass record class
     * @param aTtl record time-to-live
     * @param now current instant
     * @param someAttributes attributes
     */
    public TxtRecord(final String aName, final short aClass, final Duration aTtl, final Instant now,
                     final Attributes someAttributes) {
        super(aName, TYPE_TXT, aClass, aTtl, now);
        Objects.requireNonNull(someAttributes);
        attributes = someAttributes;
    }

    @Override
    public final String toString() {
        return "TxtRecord [name="
            + name()
            + ", type="
            + type()
            + ", class="
            + clazz()
            + ", ttl="
            + ttl()
            + ", attributes="
            + attributes
            + "]";
    }

    @Override
    protected final void write(final MessageOutputStream mos) {
        AttributesCodec.encode(attributes, mos);
    }

    /**
     * @return attributes.
     */
    final Attributes attributes() {
        return attributes;
    }

}
