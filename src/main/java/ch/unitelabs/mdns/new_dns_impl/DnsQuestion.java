package ch.unitelabs.mdns.new_dns_impl;

/**
 * A DNS question.
 */
public final class DnsQuestion extends DnsEntry {

    /**
     * Constructor.
     *
     * @param aName question name
     * @param aType question type
     * @param aClass question class
     */
    public DnsQuestion(final String aName, final short aType, final short aClass) {
        super(aName, aType, aClass);
    }

    @Override
    public final String toString() {
        return "DnsQuestion [name=" + name() + ", type=" + type() + ", clazz=" + clazz() + "]";
    }

}
