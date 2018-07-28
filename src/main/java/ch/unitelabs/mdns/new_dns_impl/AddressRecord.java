package ch.unitelabs.mdns.new_dns_impl;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;

import ch.unitelabs.mdns.new_dns_impl.io.MessageOutputStream;

import static ch.unitelabs.mdns.utils.MulticastDNS.TYPE_A;
import static ch.unitelabs.mdns.utils.MulticastDNS.TYPE_AAAA;

/**
 * Address record (A or AAAA).
 */
public final class AddressRecord extends DnsRecord {

    /** IP address. */
    private final InetAddress address;

    /**
     * Constructor.
     *
     * @param aName record name
     * @param aClass record class
     * @param aTtl record time-to-live
     * @param now current instant
     * @param anAddress IP address
     */
    public AddressRecord(final String aName, final short aClass, final Duration aTtl, final Instant now,
                         final InetAddress anAddress) {
        super(aName, type(anAddress), aClass, aTtl, now);
        address = anAddress;
    }

    /**
     * Returns the record type for the given address.
     *
     * @param address address
     * @return record type
     */
    private static short type(final InetAddress address) {
        return address instanceof Inet4Address ? TYPE_A : TYPE_AAAA;
    }

    @Override
    public final String toString() {
        return "AddressRecord [name="
            + name()
            + ", type="
            + type()
            + ", class="
            + clazz()
            + ", ttl="
            + ttl()
            + ", address="
            + address
            + "]";
    }

    @Override
    protected void write(final MessageOutputStream mos) {
        mos.writeBytes(address.getAddress());
    }

    /**
     * @return the IP address.
     */
    final InetAddress address() {
        return address;
    }

}
