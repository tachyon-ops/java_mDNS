package ch.unitelabs.mdns.new_dns_impl;

import ch.unitelabs.mdns.sd.Channel;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import static ch.unitelabs.mdns.utils.MulticastDNS.*;

public class SimpleService {
    private static final Logger logger = Logger.getLogger(SimpleService.class.getName());

    private String hostname;
    private short port;
    private final String registrationType;

    private Attributes attributes;

    private volatile boolean awaitingResolution;

    private Optional<InetAddress> ipv4Address;
    private Optional<InetAddress> ipv6Address;

    private final Lock lock;
    private final Condition resolved;

    private final String instanceName;

    public SimpleService(final String anInstanceName, final SimpleService other) {
        instanceName = anInstanceName;
        registrationType = other.registrationType();

        attributes = other.attributes();
        ipv4Address = other.ipv4Address();
        ipv6Address = other.ipv6Address();
        port = other.port();
        hostname = other.hostname();

        awaitingResolution = false;
        lock = new ReentrantLock();
        resolved = lock.newCondition();
    }

    public SimpleService(final String anInstanceName, final String aRegistrationType) {
        instanceName = anInstanceName;
        registrationType = aRegistrationType;

        setAttributes(Attributes.create().with("").get());

        ipv4Address = Optional.empty();
        ipv6Address = Optional.empty();
        port = -1;
        hostname = null;

        awaitingResolution = false;
        lock = new ReentrantLock();
        resolved = lock.newCondition();
    }

    public SimpleService(final String anInstanceName, int port, final String aRegistrationType) {
        instanceName = anInstanceName;
        registrationType = aRegistrationType;

        setAttributes(Attributes.create().with("").get());

        ipv4Address = Optional.empty();
        ipv6Address = Optional.empty();
        this.port = (short)port;
        hostname = null;

        awaitingResolution = false;
        lock = new ReentrantLock();
        resolved = lock.newCondition();
    }

    static Optional<String> instanceNameOf(final String serviceName) {
        /* everything until first dot. */
        final int end = serviceName.indexOf('.');
        return end == -1 ? Optional.empty() : Optional.of(serviceName.substring(0, end));
    }

    static Optional<String> registrationTypeOf(final String serviceName) {
        final int begin = serviceName.indexOf('.');
        final int end = serviceName.indexOf(DOMAIN);
        /* everything after first dot and until local. */
        return begin == -1 || end == -1 ? Optional.empty() : Optional.of(serviceName.substring(begin + 1, end));
    }

    public final void setHostname(final String name) {
        String aHostname = name;
        final int index = aHostname.indexOf("." + DOMAIN);
        if (index > 0) {
            aHostname = aHostname.substring(0, index);
        }
        aHostname = aHostname.replaceAll("[:%\\.]", "-");
        aHostname += "." + DOMAIN + ".";
        hostname = aHostname;
    }

    final void setAttributes(final Attributes someAttributes) {
        attributes = someAttributes;
    }
    public final Attributes attributes() {
        return attributes;
    }

    public final String hostname() {
        return hostname;
    }

    public final String instanceName() {
        return instanceName;
    }

    public final Optional<InetAddress> ipv4Address() {
        return ipv4Address;
    }

    public final Optional<InetAddress> ipv6Address() {
        return ipv6Address;
    }

    public final String name() {
        return instanceName + "." + registrationPointerName();
    }

    public final short port() {
        return port;
    }

    public final String registrationPointerName() {
        return registrationType + DOMAIN + ".";
    }

    public final String registrationType() {
        return registrationType;
    }

    public final void responseReceived(final DnsMessage response) {
        lock.lock();
        logger.info("Handling {}" + response);
        try {
            response.answers().forEach(a -> update(a));
            awaitingResolution = !resolved();
            if (!awaitingResolution) {
                logger.info("Received response resolving service");
                resolved.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean resolved() {
        return hostname != null && (ipv4Address.isPresent() || ipv6Address.isPresent()) && attributes != null;
    }

    @Override
    public final String toString() {
        return "Service [instance name=" + instanceName + "; registration type=" + registrationType + "]";
    }

    private void update(final DnsRecord record) {
        if (!record.isExpired(Channel.now())) {
            final String serviceName = name();
            final boolean matchesService = record.name().equalsIgnoreCase(serviceName);
            final boolean matchesHost = record.name().equalsIgnoreCase(hostname);
            if (record.type() == TYPE_A && matchesHost) {
                ipv4Address = Optional.of((Inet4Address) ((AddressRecord) record).address());
                logger.info("IPV4 address of service [" + serviceName + "] updated to " + ipv4Address.get());

            } else if (record.type() == TYPE_AAAA && matchesHost) {
                ipv6Address = Optional.of((Inet6Address) ((AddressRecord) record).address());
                logger.info("Address of service [" + serviceName + "] updated to " + ipv6Address.get());

            } else if (record.type() == TYPE_SRV && matchesService) {
                final SrvRecord srv = (SrvRecord) record;
                port = srv.port();
                hostname = srv.server();
                logger.info("Port of service [" + serviceName + "] updated to " + port);
                logger.info("Server of service [" + serviceName + "] updated to " + hostname);
                Channel.cachedRecord(hostname, TYPE_A, CLASS_IN).ifPresent(r -> update(r));
                Channel.cachedRecord(hostname, TYPE_AAAA, CLASS_IN).ifPresent(r -> update(r));

            } else if (record.type() == TYPE_TXT && matchesService) {
                attributes = ((TxtRecord) record).attributes();
                logger.info("Attributes of service [" + serviceName + "] updated to " + attributes);

            } else {
                logger.info("Ignored irrelevant {}" + record);
            }
        } else {
            logger.info("Ignored expired {}" + record);
        }
    }

    public void setPort(int port) {
        this.port = (short)port;
    }

    public int getPort() {
        return port;
    }
}
