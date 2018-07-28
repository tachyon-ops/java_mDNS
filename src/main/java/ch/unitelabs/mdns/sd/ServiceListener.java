package ch.unitelabs.mdns.sd;

import ch.unitelabs.mdns.new_dns_impl.*;
import ch.unitelabs.mdns.new_dns_impl.DnsMessage.Builder;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ch.unitelabs.mdns.sd.Channel.now;
import static ch.unitelabs.mdns.utils.MulticastDNS.*;
import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;

public class ServiceListener {
    interface ResponseListener {
        void responseReceived(final DnsMessage response);
    }

    Channel channel;

    private static final Pattern INSTANCE_NAME_PATTERN = Pattern.compile("([\\s\\S]*?)( \\((?<i>\\d+)\\))$");

    private final List<ResponseListener> rls;

    private final Map<String, SimpleService> services;

    private final Set<String> registrationPointerNames;

    ServiceListener(Channel channel) {
        this.channel = channel;
        rls = new CopyOnWriteArrayList<>();
        services = new ConcurrentHashMap<>();
        registrationPointerNames = ConcurrentHashMap.newKeySet();
    }

    public final void accept(final DnsMessage in) {
        if (in.isQuery()) {
            handleQuery(in);
        } else if (in.isResponse()) {
            handleResponse(in);
        } else {
            LOGGER.warning("Ignored received DNS message.");
        }
    }

    // Adds the given registered service.
    public void add(final SimpleService s) {
        services.put(s.name().toLowerCase(), s);
        final String rpn = s.registrationPointerName();
        registrationPointerNames.add(rpn);
    }

    /**
     * Adds a DNS record type A corresponding to an answer the given question if it exits.
     */
    private void addIpv4Address(final DnsMessage query, final DnsQuestion question, final Builder builder,
                                final Instant now) {
        services
                .values()
                .stream()
                .filter(s -> s.hostname().equalsIgnoreCase(question.name()))
                .filter(h -> h.ipv4Address().isPresent())
                .forEach(s -> {
                    final InetAddress addr = s.ipv4Address().get();
                    builder.addAnswer(query,
                            new AddressRecord(question.name(), uniqueClass(CLASS_IN), TTL, now, addr));
                });
    }

    /**
     * Adds a DNS record type AAAA corresponding to an answer the given question if it exits.
     */
    private void addIpv6Address(final DnsMessage query, final DnsQuestion question, final Builder builder,
                                final Instant now) {
        services
                .values()
                .stream()
                .filter(s -> s.hostname().equalsIgnoreCase(question.name()))
                .filter(h -> h.ipv6Address().isPresent())
                .forEach(s -> {
                    final InetAddress addr = s.ipv6Address().get();
                    builder.addAnswer(query,
                            new AddressRecord(question.name(), uniqueClass(CLASS_IN), TTL, now, addr));
                });
    }

    /**
     * Adds a DNS record type PTR corresponding to an answer the given question if it exits.
     */
    private void addPtrAnswer(final DnsMessage query, final DnsQuestion question, final Builder builder,
                              final Instant now) {
        if (question.name().equals(RT_DISCOVERY)) {
            for (final String rpn : registrationPointerNames) {
                builder.addAnswer(query, new PtrRecord(RT_DISCOVERY, CLASS_IN, TTL, now, rpn));
            }
        } else {
            for (final SimpleService s : services.values()) {
                if (question.name().equalsIgnoreCase(s.registrationPointerName())) {
                    builder.addAnswer(query,
                            new PtrRecord(s.registrationPointerName(), CLASS_IN, TTL, now, s.name()));
                }
            }
        }
    }

    /**
     * Adds DNS record types SRV, TXT, A and AAAA corresponding to an answer the given question if it exits.
     */
    private void addServiceAnswer(final DnsMessage query, final DnsQuestion question, final SimpleService service,
                                  final Builder builder, final Instant now) {
        final short unique = uniqueClass(CLASS_IN);
        final String hostname = service.hostname();
        if (question.type() == TYPE_SRV || question.type() == TYPE_ANY) {
            builder.addAnswer(query, new SrvRecord(question.name(), unique, TTL, now, service.port(), hostname));
        }

        if (question.type() == TYPE_TXT || question.type() == TYPE_ANY) {
            builder.addAnswer(query, new TxtRecord(question.name(), unique, TTL, now, service.attributes()));
        }

        if (question.type() == TYPE_SRV) {
            service.ipv4Address().ifPresent(
                    a -> builder.addAnswer(query, new AddressRecord(hostname, unique, TTL, now, a)));
            service.ipv6Address().ifPresent(
                    a -> builder.addAnswer(query, new AddressRecord(hostname, unique, TTL, now, a)));
        }
    }

    /**
     * Builds a response to the given query.
     */
    private DnsMessage buildResponse(final DnsMessage query) {
        final Builder builder = DnsMessage.response(FLAGS_AA);
        final Instant now = now();
        for (final DnsQuestion question : query.questions()) {
            if (question.type() == TYPE_PTR) {
                addPtrAnswer(query, question, builder, now);
            } else {
                if (question.type() == TYPE_A || question.type() == TYPE_ANY) {
                    addIpv4Address(query, question, builder, now);
                }
                if (question.type() == TYPE_AAAA || question.type() == TYPE_ANY) {
                    addIpv6Address(query, question, builder, now);
                }

                final SimpleService s = services.get(question.name().toLowerCase());
                if (s != null) {
                    addServiceAnswer(query, question, s, builder, now);
                }
            }
        }
        return builder.get();
    }

    /**
     * Checks the network for a unique instance name, returning a new {@link Service} if it is not unique.
     */
    private SimpleService checkInstanceName(final SimpleService service, final boolean allowNameChange) throws IOException {
        final String hostname = service.hostname();
        final int port = service.port();
        boolean collision = false;
        SimpleService result = service;
        do {
            collision = false;
            final Instant now = now();

            /* check own services. */
            final SimpleService own = services.get(result.name().toLowerCase());
            if (own != null) {
                final String otherHostname = own.hostname();
                collision = own.port() != port || !otherHostname.equals(hostname);
                if (collision) {
                    final String msg = "Registered service collision: " + own;
                    result = tryResolveCollision(result, allowNameChange, msg);
                }
            }

            /* check cache. */
            final Optional<SrvRecord> rec = Channel.cache
                    .entries(result.name())
                    .stream()
                    .filter(e -> e instanceof SrvRecord)
                    .filter(e -> !e.isExpired(now))
                    .map(e -> (SrvRecord) e)
                    .filter(e -> e.port() != port || !e.server().equals(hostname))
                    .findFirst();
            if (rec.isPresent()) {
                collision = true;
                final String msg = "Cache collision: " + rec.get();
                result = tryResolveCollision(result, allowNameChange, msg);
            }

        } while (collision);
        return result;
    }

    /**
     * Handles the given query.
     */
    private void handleQuery(final DnsMessage query) {
        LOGGER.fine(() -> "Trying to respond to " + query);
        final DnsMessage response = buildResponse(query);
        if (!response.answers().isEmpty()) {
            LOGGER.fine(() -> "Responding with " + response);
            channel.send(response);
        } else {
            LOGGER.fine("Ignoring query");
        }
    }

    /**
     * Handles the given response.
     */
    private void handleResponse(final DnsMessage response) {
        LOGGER.fine(() -> "Handling response " + response);
        for (final DnsRecord record : response.answers()) {
            if (record.ttl().isZero()) {
                Channel.cache.expire(record);
            } else {
                Channel.cache.add(record);
            }
        }
        if (rls.isEmpty()) {
            LOGGER.fine(() -> "No listener registered for " + response);
        } else {
            rls.forEach(l -> l.responseReceived(response));
        }
    }

    /**
     * Removes the given registered service.
     */
    private void remove(final SimpleService s) {
        registrationPointerNames.remove(s.registrationPointerName());
        services.remove(s.name().toLowerCase());
    }

    /**
     * Tries to resolve a service instance name collision by changing its instance name if allowed.
     */
    private SimpleService tryResolveCollision(final SimpleService service, final boolean allowNameChange, final String msg)
            throws IOException {
        if (!allowNameChange) {
            throw new IOException(msg);
        }
        LOGGER.info(msg);
        final String instanceName = changeInstanceName(service.instanceName());
        return new SimpleService(instanceName, service);
    }

    private static String changeInstanceName(final String instanceName) {
        final Matcher m = INSTANCE_NAME_PATTERN.matcher(instanceName);
        final String result;
        if (!m.matches()) {
            result = instanceName + " (2)";
        } else {
            final int next = Integer.parseInt(m.group("i")) + 1;
            final int start = m.start("i");
            final int end = m.end("i");
            result = instanceName.substring(0, start) + next + instanceName.substring(end);
        }
        LOGGER.fine(() -> "Change service instance name from: [" + instanceName + "] to [" + result + "]");
        return result;
    }
}