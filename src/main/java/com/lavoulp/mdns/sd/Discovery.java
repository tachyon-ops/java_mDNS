package com.lavoulp.mdns.sd;

import com.lavoulp.mdns.dns.Domain;
import com.lavoulp.mdns.dns.Record;
import com.lavoulp.mdns.dns.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.*;


public class Discovery {

    public static class Device {
        String name;
        long ttl;
        String identifier;
        int port;
        String host;
        String target;
        String path;
        Response response;
    }

    private final static Logger logger = LoggerFactory.getLogger(Discovery.class);

    public HashMap<String, Discovery.Device> cache = new HashMap<>();

    private int port = 5353;
    byte[] buffer = new byte[65509];
    private InetAddress ia, ia1, ia2;

    private static String NAME = "_tcp.";

    private static final String MDNS_IP4_ADDRESS = "224.0.0.251";
    private static final String MDNS_IP6_ADDRESS = "FF02::FB";

    public Discovery() {
        // nothing
    }

    public Discovery(String name) {
        this.NAME = name;
    }

    public void run() {
        try {
            ia = getLocalHostLANAddress();
            run(ia);
        } catch (UnknownHostException e) {
            logger.error("UnknownHostException" + e.getMessage());
        }
    }
    public void run(InetAddress ia) {
        this.ia = ia;
        System.out.println("Using 127.0.0.1 5353");

        try {
            // ia = getInetAddress("local");
            ia1 = InetAddress.getByName(MDNS_IP4_ADDRESS);
            ia2 = InetAddress.getByName(MDNS_IP6_ADDRESS);
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        }

        try {
            // Create a socket to listen on the port.
            MulticastSocket ms = new MulticastSocket(port);
            if (ia == null) {
                ia = getLocalHostLANAddress();
            }

            // Join Multicast Socket to Multicast Addresses IPv4 and IPv6
            if (ia1 != null) ms.joinGroup(ia1);
            if (ia2 != null) ms.joinGroup(ia2);

            // Create a DatagramPacket packet to receive data into the buffer
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ia, port);

            // Do question on Network!
            Service service = Service.fromName(NAME);
            Query query = Query.createFor(service, Domain.LOCAL);

            try {
                Set<Instance> instances = query.runOnceOn(ia);
//                instances.stream().forEach(System.out::println);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

            while (true) {
                // Wait to receive a datagram
                ms.receive(packet);
                System.out.println("UDP >> " + packet.getAddress() + ":" + packet.getPort());

                Response response = Response.createFrom(packet);

                for (Record record : response.getRecords()) {
                    if (record.getName().contains(NAME) && !record.getName().equals(NAME + "local.")) {
                        Discovery.Device device = new Discovery.Device();
                        device.name = record.getName();
                        device.ttl = record.getTTL();
                        device.host = packet.getAddress().getHostAddress();
                        device.port = packet.getPort();
                        device.response = response;

                        if (record.getTTL() == 0) cache.remove(device.name);
                        else cache.put(device.name, device);
                        // System.out.println("DEVICE >> " + device.name );
                    }
                }
                System.out.println("CACHE >> " + cache );

                // Reset the length of the packet before reusing it.
                packet.setLength(buffer.length);
            }
        }
        catch (IOException se) {
            // System.err.println("IO Exception " +  se);
            logger.error("IO Exception " + se.getMessage());
        }
    }

    /**
     * Gets IP Address that is assigned on a given network interface (currently
     * only supports IPv4)
     *
     * @param interfaceName Name of network interface
     */
    public static InetAddress getInetAddress(String interfaceName) throws SocketException, UnknownHostException {
        try {
            if (interfaceName.matches("local")) {
                return InetAddress.getLocalHost();
            }
            final String interfaceDisplay = "Network interface " + interfaceName;
            final NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
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
        } catch (SocketException | UnknownHostException e) {
            throw e;
        }
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
    private static InetAddress getLocalHostLANAddress() throws UnknownHostException {
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
            logger.info(jdkSuppliedAddress.toString());
            return jdkSuppliedAddress;
        }
        catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }
}
