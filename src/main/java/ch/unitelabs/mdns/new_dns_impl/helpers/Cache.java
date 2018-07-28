package ch.unitelabs.mdns.new_dns_impl.helpers;

import ch.unitelabs.mdns.new_dns_impl.DnsRecord;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static ch.unitelabs.mdns.utils.MulticastDNS.CLASS_ANY;
import static ch.unitelabs.mdns.utils.MulticastDNS.EXPIRY_TTL;
import static ch.unitelabs.mdns.utils.MulticastDNS.TYPE_ANY;

/**
 * A cache of DNS records.
 */
public final class Cache {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(Cache.class.getName());

    /** maps a DNS record key to all cached DNS entries. */
    private final ConcurrentHashMap<String, List<DnsRecord>> map;

    /**
     * Constructor.
     */
    public Cache() {
        map = new ConcurrentHashMap<>();
    }

    /**
     * Adds the given DNS record to this cache.
     *
     * If a DNS record matching the given record name (ignoring case), type and class already exists, it is
     * replaced with the given one.
     */
    public final void add(final DnsRecord record) {
        Objects.requireNonNull(record);
        final int index = indexOf(record);
        final String key = key(record);
        if (index == -1) {
            LOGGER.fine(() -> "Adding " + record + " to cache");
            map.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(record);
        } else {
            LOGGER.fine(() -> "Replacing " + map.get(key).get(index) + " with " + record + " in cache");
            map.get(key).set(index, record);
        }
    }

    /**
     * Removes all expired DNS records.
     */
    final void clean(final Instant now) {
        final Set<String> services = new HashSet<>();
        for (final Map.Entry<String, List<DnsRecord>> e : map.entrySet()) {
            e.getValue().removeIf(r -> r.isExpired(now));
            if (e.getValue().isEmpty()) {
                services.add(e.getKey());
            }
        }
        services.forEach(map::remove);
    }

    /**
     * Clears all DNS records.
     */
    final void clear() {
        LOGGER.fine("Clearing cache");
        map.clear();
    }

    /**
     * Returns all DNS records matching the given name.
     */
    public final Collection<DnsRecord> entries(final String name) {
        return map.getOrDefault(name.toLowerCase(), Collections.emptyList());
    }

    /**
     * Sets the TTL of the given cached record to MulticastDNS.EXPIRY_TTL in order for the reaper to remove
     * it later.
     */
    public final void expire(final DnsRecord record) {
        Objects.requireNonNull(record);
        final int index = indexOf(record);
        final String key = key(record);
        if (index != -1) {
            LOGGER.fine(() -> "Setting TTL of " + record + " to " + EXPIRY_TTL);
            map.get(key).get(index).setTtl(EXPIRY_TTL);
        }
    }

    /**
     * Returns the DNS record matching the given name, type and class if it exists.
     */
    public final Optional<DnsRecord> get(final String name, final short type, final short clazz) {
        LOGGER.fine(() -> "Searching cache for DNS record matching [Name="
                + name
                + "; type="
                + type
                + "; class="
                + clazz
                + "]");
        final Optional<DnsRecord> result =
                entries(name).stream().filter(r -> isSameType(r, type) && isSameClass(r, clazz)).findFirst();
        logResult(result);
        return result;
    }

    /**
     * Removes all DNS records associated with the given name.
     */
    final void removeAll(final String name) {
        Objects.requireNonNull(name);
        LOGGER.fine(() -> "Removing all DNS records associated with" + name + " from cache");
        map.remove(name.toLowerCase());
    }

    /**
     * Returns the index of the DNS record matching the given DNS record if it exists.
     */
    private int indexOf(final DnsRecord record) {
        int index = 0;
        for (final DnsRecord r : entries(record.name())) {
            if (isSameType(r, record) && isSameClass(r, record)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Returns true if both DNS records have the same service class (or one has a service class of CLASS_ANY).
     */
    private boolean isSameClass(final DnsRecord r1, final DnsRecord r2) {
        return isSameClass(r1, r2.clazz());
    }

    /**
     * Returns true if given DNS record has given service class (or record/given class is CLASS_ANY).
     */
    private boolean isSameClass(final DnsRecord r, final short clazz) {
        return r.clazz() == CLASS_ANY || clazz == CLASS_ANY || r.clazz() == clazz;
    }

    /**
     * Returns true if both DNS records have the same service type (or one has a service type of TYPE_ANY
     */
    private boolean isSameType(final DnsRecord r1, final DnsRecord r2) {
        return isSameType(r1, r2.type());
    }

    /**
     * Returns true if given DNS record has given service type (or record/given type is TYPE_ANY
     */
    private boolean isSameType(final DnsRecord r, final short type) {
        return r.type() == TYPE_ANY || type == TYPE_ANY || r.type() == type;
    }

    /**
     * Returns the association key for the given record.
     */
    private String key(final DnsRecord record) {
        return record.name().toLowerCase();
    }

    /**
     * Logs search result.
     */
    private void logResult(final Optional<? extends DnsRecord> result) {
        if (result.isPresent()) {
            LOGGER.fine("Found cached " + result.get());
        } else {
            LOGGER.fine("No cached record found");
        }
    }

}