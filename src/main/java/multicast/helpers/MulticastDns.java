/*
Copyright 2018 Cedric Liegeois

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the name of the copyright holder nor the names of other
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package multicast.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Multicast-DNS constants.
 */
public final class MulticastDns {

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
    public static final Duration PROBING_INTERVAL;

    /** probing timeout. */
    public static final Duration PROBING_TIMEOUT;

    /** number of probes before announcing a registered service. */
    public static final int PROBE_NUM;

    /** interval between goodbyes messages. */
    public static final Duration CANCELLING_INTERVAL;

    /** number of cancel message sent when de-registering a service. */
    public static final int CANCEL_NUM;

    /** cache record reaper interval. */
    public static final Duration REAPING_INTERVAL;

    /** default resolution timeout. */
    public static final Duration RESOLUTION_TIMEOUT;

    /** interval between resolution question. */
    public static final Duration RESOLUTION_INTERVAL;

    /** number of queries. */
    public static final int QUERY_NUM;

    /** interval between browsing query. */
    public static final Duration QUERYING_DELAY;

    /** interval between browsing query. */
    public static final Duration QUERYING_INTERVAL;

    /** time to live: 1 hour. */
    public static final Duration TTL;

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

    public static final short TYPE_UNKNOWN = -1;

    /** class mask (unsigned). */
    public static final short CLASS_MASK = 0x7FFF;

    /** unique class (unsigned). */
    public static final short CLASS_UNIQUE = (short) 0x8000;

    public final static int USHORT_MASK = 0xFFFF;
    public final static long UINT_MASK = 0xFFFFFFFFL;

    public final static int FLAG_QR_MASK = 0x8000;
    public final static int FLAG_OPCODE_MASK = 0x7800;
    public final static int FLAG_RCODE_MASK = 0xF;

    public static final String MDNS_IP4_ADDRESS;
    public static final String MDNS_IP6_ADDRESS;

    static {
        try (final InputStream is = MulticastDns.class.getClassLoader().getResourceAsStream("mdns.properties")) {
            final Properties props = new Properties();
            props.load(is);
            MDNS_IP4_ADDRESS = stringProp("mdns.ipv4", props);
            MDNS_IP6_ADDRESS = stringProp("mdns.ipv6", props);
            IPV4_ADDR = InetAddress.getByName(MDNS_IP4_ADDRESS);
            IPV6_ADDR = InetAddress.getByName(MDNS_IP6_ADDRESS);
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

        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Constructor.
     */
    public MulticastDns() {
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

    /**
     * Gets IP Address that is assigned on a given network interface (currently
     * only supports IPv4)
     *
     * @param interfaceName Name of network interface
     */
    public static InetAddress getInetAddress(String interfaceName) throws IOException {
        if (interfaceName.matches("local")) {
            return InetAddress.getLocalHost();
        }
        String interfaceDisplay = "Network interface " + interfaceName;
        NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
        if (networkInterface == null) {
            StringBuffer networkInterfaceNames = new StringBuffer();
            try {
                for (NetworkInterface nInterface : Collections
                        .list(NetworkInterface.getNetworkInterfaces())) {
                    networkInterfaceNames.append(nInterface.getName() + ", ");
                }
            } catch (SocketException e) {
                throw new RuntimeException(e.getMessage());
            }
            throw new ConnectException(interfaceDisplay + " doesn't exist on this machine\n"
                    + "Use one of these instead " + networkInterfaceNames.toString());
        }
        if (!networkInterface.isUp())
            throw new ConnectException(interfaceDisplay + " is not up");
        if (networkInterface.isPointToPoint())
            throw new ConnectException(interfaceDisplay + " can not be PtP");

        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
        if (!addresses.hasMoreElements())
            throw new ConnectException(interfaceDisplay + " doesnt have assigned IPs");

        for (InetAddress address : Collections.list(addresses)) {
            // @TODO: For now only support IPv4 - check IPv6
            if (address instanceof Inet4Address)
                return address;
        }
        throw new ConnectException(interfaceDisplay + " doesn't have an assigned IPv4 address");
    }


    /**
     * Returns an <code>InetAddress</code> object encapsulating what is most likely the machine's LAN IP address.
     * <p/>
     * This method is intended for use as a replacement of JDK method <code>InetAddress.getLocalHost</code>, because
     * that method is ambiguous on Linux systems. Linux systems enumerate the loopback network interface the same
     * way as regular LAN network interfaces, but the JDK <code>InetAddress.getLocalHost</code> method does not
     * specify the algorithm used to select the address returned under such circumstances, and will often return the
     * loopback address, which is not valid for network communication. Details
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037">here</a>.
     * <p/>
     * This method will scan all IP addresses on all network interfaces on the host machine to determine the IP address
     * most likely to be the machine's LAN address. If the machine has multiple IP addresses, this method will prefer
     * a site-local IP address (e.g. 192.168.x.x or 10.10.x.x, usually IPv4) if the machine has one (and will return the
     * first site-local address if the machine has more than one), but if the machine does not hold a site-local
     * address, this method will return simply the first non-loopback address found (IPv4 or IPv6).
     * <p/>
     * If this method cannot find a non-loopback address using this selection algorithm, it will fall back to
     * calling and returning the result of JDK method <code>InetAddress.getLocalHost</code>.
     * <p/>
     *
     * @throws UnknownHostException If the LAN address of the machine cannot be found.
     */
    public static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // Iterate all IP addresses assigned to each card...
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {

                        if (inetAddr.isSiteLocalAddress()) {
                            // Found non-loopback site-local address. Return it immediately...
                            return inetAddr;
                        }
                        else if (candidateAddress == null) {
                            // Found non-loopback address, but not necessarily site-local.
                            // Store it as a candidate to be returned if site-local address is not subsequently found...
                            candidateAddress = inetAddr;
                            // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
                            // only the first. For subsequent iterations, candidate will be non-null.
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                // We did not find a site-local address, but we found some other non-loopback address.
                // Server might have a non-site-local address assigned to its NIC (or it might be running
                // IPv6 which deprecates the "site-local" concept).
                // Return this non-loopback candidate address...
                return candidateAddress;
            }
            // At this point, we did not find a non-loopback address.
            // Fall back to returning whatever InetAddress.getLocalHost() returns...
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
//            logger.info(jdkSuppliedAddress.toString());
            return jdkSuppliedAddress;
        }
        catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }

}
