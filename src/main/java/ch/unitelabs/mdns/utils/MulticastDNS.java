package ch.unitelabs.mdns.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Properties;

public class MulticastDNS {

    private final static Logger logger = LoggerFactory.getLogger(MulticastDNS.class);

    /** the domain: always local. */
    public static final String DOMAIN = "local";

    /** registration types discovery. */
    public static final String RT_DISCOVERY = "_services._dns-sd._udp.local.";

    /** maximum size of DNS message in bytes. */
    public static final int MAX_DNS_MESSAGE_SIZE = 65536;

    /** mDNS IPV4 address. */
    public static final InetAddress IPV4_ADDR;

    /** mDNS IPV6 address. */
    public static final InetAddress IPV6_ADDR;

    /** mDNS port. */
    public static final int MDNS_PORT;

    /** IPV4 socket address. */
    public static final InetSocketAddress IPV4_SOA;

    /** IPV6 socket address. */
    public static final InetSocketAddress IPV6_SOA;

    /** interval between probe message. */
    static final Duration PROBING_INTERVAL;

    /** probing timeout. */
    static final Duration PROBING_TIMEOUT;

    /** number of probes before announcing a registered service. */
    static final int PROBE_NUM;

    /** interval between goodbyes messages. */
    static final Duration CANCELLING_INTERVAL;

    /** number of cancel message sent when de-registering a service. */
    static final int CANCEL_NUM;

    /** cache record reaper interval. */
    static final Duration REAPING_INTERVAL;

    /** default resolution timeout. */
    static final Duration RESOLUTION_TIMEOUT;

    /** interval between resolution question. */
    public static final Duration RESOLUTION_INTERVAL;

    /** number of queries. */
    static final int QUERY_NUM;

    /** interval between browsing query. */
    static final Duration QUERYING_DELAY;

    /** interval between browsing query. */
    static final Duration QUERYING_INTERVAL;

    /** time to live: 1 hour. */
    public static final Duration TTL;
    public static final long TTL_INT = 3600;

    /** time to live after expiry: 1 second. */
    public static final Duration EXPIRY_TTL;

    /** query or response mask (unsigned). */
    public static final short FLAGS_QR_MASK = (short) 0x8000;

    /** query flag (unsigned). */
    public static final short FLAGS_QR_QUERY = 0x0000;

    /** response flag (unsigned). */
    public static final short FLAGS_QR_RESPONSE = (short) 0x8000;

    /** authoritative answer flag (unsigned). */
    public static final short FLAGS_AA = 0x0400;

    /** Internet class. */
    public static final short CLASS_IN = 1;

    /** any class. */
    public static final short CLASS_ANY = 255;

    /** type A (IPV4 address) record. */
    public static final short TYPE_A = 1;

    /** pointer record. */
    public static final short TYPE_PTR = 12;

    /** text record. */
    public static final short TYPE_TXT = 16;

    /** type AAAA (IPV6 address) record. */
    public static final short TYPE_AAAA = 28;

    /** server record. */
    public static final short TYPE_SRV = 33;

    /** any record. */
    public static final short TYPE_ANY = 255;

    /** class mask (unsigned). */
    private static final short CLASS_MASK = 0x7FFF;

    /** unique class (unsigned). */
    public static final short CLASS_UNIQUE = (short) 0x8000;

    public static final String MDNS_IP4_ADDRESS = "224.0.0.251";
    public static final String MDNS_IP6_ADDRESS = "FF02::FB";




    public final static int USHORT_MASK = 0xFFFF;
    public final static long UINT_MASK = 0xFFFFFFFFL;
    public final static String NAME_CHARSET = "UTF-8";

    private final static short UNICAST_RESPONSE_BIT = (short) 0x8000;

    static {
        try(final InputStream is = MulticastDNS.class.getClassLoader().getResourceAsStream("mdns.properties")) {
            final Properties props = new Properties();

            props.load(is);
            IPV4_ADDR = InetAddress.getByName(stringProp("mdns.ipv4", props));
            IPV6_ADDR = InetAddress.getByName(stringProp("mdns.ipv6", props));
            MDNS_PORT = intProp("mdns.port", props);
            IPV4_SOA = new InetSocketAddress(IPV4_ADDR, MDNS_PORT);
            IPV6_SOA = new InetSocketAddress(IPV6_ADDR, MDNS_PORT);

            RESOLUTION_TIMEOUT = durationProp("resolution.timeout", props);
            RESOLUTION_INTERVAL = durationProp("resolution.interval", props);

            PROBING_TIMEOUT = durationProp("probing.timeout", props);
            PROBING_INTERVAL = durationProp("probing.interval", props);
            PROBE_NUM = intProp("probing.number", props);

            QUERYING_DELAY = durationProp("querying.delay", props);
            QUERYING_INTERVAL = durationProp("querying.interval", props);
            QUERY_NUM = intProp("querying.number", props);

            CANCELLING_INTERVAL = durationProp("cancellation.interval", props);
            CANCEL_NUM = intProp("cancellation.number", props);

            REAPING_INTERVAL = durationProp("reaper.interval", props);

            TTL = durationProp("ttl.default", props);
            EXPIRY_TTL = durationProp("ttl.expiry", props);

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


//    public enum Type {
//        UNSUPPORTED(0),
//        A(1),
//        NS(2),
//        CNAME(5),
//        SOA(6),
//        NULL(10),
//        WKS(11),
//        PTR(12),
//        HINFO(13),
//        MINFO(14),
//        MX(15),
//        TXT(16),
//        AAAA(28),
//        SRV(33);
//
//        private final int value;
//
//        public static Type fromInt(int val) {
//            for (Type type : values()) {
//                if (type.value == val) {
//                    return type;
//                }
//            }
//            return UNSUPPORTED;
//        }
//
//        Type(int value) {
//            this.value = value;
//        }
//
//        public int asUnsignedShort() {
//            return value & USHORT_MASK;
//        }
//    }
//
//    public enum Class {
//        UNSUPPORTED(0),
//        IN(1),
//        ANY(255);
//
//        private final int value;
//
//        public static Class fromInt(int val) {
//            for (Class c : values()) {
//                if (c.value == val) {
//                    return c;
//                }
//            }
//            // throw new IllegalArgumentException(String.format("Can't convert 0x%04x to a Class", val));
//            // logger.error("Can't convert 0x%04x to a Class", val);
//            return UNSUPPORTED;
//        }
//
//        Class(int value) {
//            this.value = value;
//        }
//
//        public short asUnsignedShort() {
//            return (short) (value & USHORT_MASK);
//        }
//    }
//
//    public enum QType {
//        UNKNOWN(-1),
//        A(1),
//        NS(2),
//        CNAME(5),
//        SOA(6),
//        MB(7),
//        MG(8),
//        MR(9),
//        NULL(10),
//        WKS(11),
//        PTR(12),
//        HINFO(13),
//        MINFO(14),
//        MX(15),
//        TXT(16),
//        AAAA(28),
//        SRV(33),
//        ANY(255);
//
//        private final int value;
//
//        public static QType fromInt(int val) {
//            for (QType type : values()) {
//                if (type.value == val) {
//                    return type;
//                }
//            }
//            logger.error("Can't convert " + val + " to a QType");
//            return ANY;
//            // throw new IllegalArgumentException("Can't convert " + val + " to a QType");
//        }
//
//        QType(int value) {
//            this.value = value;
//        }
//
//        public int asUnsignedShort() {
//            return value & USHORT_MASK;
//        }
//    }
//
//    public enum QClass {
//        UNKNOWN(-1),
//        IN(1),
//        ANY(255);
//
//        private final int value;
//
//        public static QClass fromInt(int val) {
//            for (QClass c : values()) {
//                if (c.value == (val & ~UNICAST_RESPONSE_BIT)) {
//                    return c;
//                }
//            }
//            // throw new IllegalArgumentException("Can't convert " + val + " to a QClass");
//            // logger.error("Can't convert " + val + " to a QClass");
//            return QClass.IN;
//        }
//
//        QClass(int value) {
//            this.value = value;
//        }
//
//        public int asUnsignedShort() {
//            return value & USHORT_MASK;
//        }
//    }

    /**
     * Constructor.
     */
    private MulticastDNS() {
        // empty.
    }

    /**
     * Decodes the given class and returns an array with the class index and whether the class is unique (a value
     * different from 0 denotes a unique class).
     *
     * @param clazz class
     * @return an array of 2 shorts, first is class index, second whether class is unique
     */
    public static short[] decodeClass(final short clazz) {
        return new short[] { (short) (clazz & CLASS_MASK), (short) (clazz & CLASS_UNIQUE) };
    }

    /**
     * Encodes the given class index and whether it is unique into a class. This is the reverse operation of
     * {@link #encodeClass(short, boolean)}.
     *
     * @param classIndex class index
     * @param unique whether the class is unique
     * @return encoded class
     */
    public static short encodeClass(final short classIndex, final boolean unique) {
        return (short) (classIndex | (unique ? CLASS_UNIQUE : 0));
    }

    /**
     * Makes the given class unique.
     *
     * @param classIndex class index
     * @return unique class
     */
    public static short uniqueClass(final short classIndex) {
        return (short) (classIndex | CLASS_UNIQUE);
    }

    /**
     * Returns the {@code Duration} corresponding to the given key.
     *
     * @param key property key
     * @param props properties default values
     * @return value
     */
    private static Duration durationProp(final String key, final Properties props) {
        return Duration.ofMillis(Long.parseLong(stringProp(key, props)));
    }

    /**
     * Returns the {@code int} corresponding to the given key.
     *
     * @param key property key
     * @param props properties default values
     * @return value
     */
    private static int intProp(final String key, final Properties props) {
        return Integer.parseInt(stringProp(key, props));
    }

    /**
     * Returns the {@code String} corresponding to the given key.
     *
     * @param key property key
     * @param props properties default values
     * @return value
     */
    private static String stringProp(final String key, final Properties props) {
        return System.getProperty(key, props.getProperty(key));
    }
}
