package ch.unitelabs.mdns.new_dns_impl;

import java.util.Objects;

import static ch.unitelabs.mdns.utils.MulticastDNS.decodeClass;

/**
 * DNS entry base class.
 */
abstract class DnsEntry {

    /** entry name. */
    private final String name;

    /** entry type */
    private final short type;

    /** entry class */
    private final short clazz;

    /** whether the entry class is unique. */
    private final boolean unique;

    /**
     * Constructor.
     *
     * @param aName entry name
     * @param aType entry type
     * @param aClass entry class
     */
    protected DnsEntry(final String aName, final short aType, final short aClass) {
        Objects.requireNonNull(aName);
        name = aName;
        type = aType;
        final short[] arr = decodeClass(aClass);
        clazz = arr[0];
        unique = arr[1] != 0;
    }

    /**
     * @return entry class.
     */
    public final short clazz() {
        return clazz;
    }

    /**
     * @return {@code true} iff the class of this entry is unique.
     */
    final boolean isUnique() {
        return unique;
    }

    /**
     * @return entry name.
     */
    public final String name() {
        return name;
    }

    /**
     * @return entry type.
     */
    public final short type() {
        return type;
    }

}
