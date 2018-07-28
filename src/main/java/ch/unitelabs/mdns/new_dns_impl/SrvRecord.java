package ch.unitelabs.mdns.new_dns_impl;

import ch.unitelabs.mdns.new_dns_impl.io.MessageOutputStream;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import static ch.unitelabs.mdns.utils.MulticastDNS.TYPE_SRV;


/**
 * Service record (SRV).
 * <p>
 * According to <a href=
 * "https://developer.apple.com/library/content/documentation/Cocoa/Conceptual/NetServices/Introduction.html#//apple_ref/doc/uid/TP40002445-SW1">Apple
 * Bonjour</a>, both the priority and weight are always {@code 0}.
 */
public final class SrvRecord extends DnsRecord {

    /** service port. */
    private final short port;

    /** service server. */
    private final String server;

    /**
     * Constructor.
     *
     * @param aName record name
     * @param aClass record class
     * @param aTtl record time-to-live
     * @param now current instant
     * @param aPort service port
     * @param aServer service server
     */
    public SrvRecord(final String aName, final short aClass, final Duration aTtl, final Instant now, final short aPort,
                     final String aServer) {
        super(aName, TYPE_SRV, aClass, aTtl, now);
        Objects.requireNonNull(aServer);
        port = aPort;
        server = aServer;
    }

    @Override
    public final String toString() {
        return "SrvRecord [name="
            + name()
            + ", type="
            + type()
            + ", class="
            + clazz()
            + ", ttl="
            + ttl()
            + ", server="
            + server
            + ", port="
            + port
            + "]";
    }

    @Override
    protected final void write(final MessageOutputStream mos) {
        /* priority and weight are always 0. */
        mos.writeShort(0);
        mos.writeShort(0);
        mos.writeShort(port);
        mos.writeName(server);
    }

    /**
     * @return service port.
     */
    public final short port() {
        return port;
    }

    /**
     * @return service server.
     */
    public final String server() {
        return server;
    }

}
