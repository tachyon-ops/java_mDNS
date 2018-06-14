package ch.unitelabs.mdns.sd;

import ch.unitelabs.mdns.dns.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.*;


public class Discovery {
    private final static Logger logger = LoggerFactory.getLogger(Discovery.class);
    public InstancesCache instancesCache = new InstancesCache();

    private int port = 5353;
    byte[] buffer = new byte[65509];
    private InetAddress ia, ia1, ia2;
    private static String NAME = "_tcp.";
    private static final String MDNS_IP4_ADDRESS = "224.0.0.251";
    private static final String MDNS_IP6_ADDRESS = "FF02::FB";

    private static boolean reboot = true;
    private static boolean packetsListener = true;
    private static boolean rebootPacketsListener = true;

    private PacketReceiverHeartbeatAgent agent;
    private String networkInterfaceName;

    public class PacketReceiverHeartbeatAgent implements Runnable {
        private int SAMPLING_PERIOD = 1000;
        private int heartBeat = 0;
        private boolean active = true;
        private boolean packetReady = true;
        private Thread heartBeatThread;

        MulticastSocket ms;

        /**
         * Starts the PacketReceiverHeartbeatAgent asynchronously
         */
        public void start() {
            heartBeatThread = new Thread(this, "Discovery_Heartbeat");
            //terminate the thread with the VM.
            heartBeatThread.setDaemon(true);
            heartBeatThread.start();
        }

        void setupPackageReceiver() throws IOException {
            logger.info("Packet Receiver setup");

            // Create a socket to listen on the port.
            ms = new MulticastSocket(port);


            // Join Multicast Socket to Multicast Addresses IPv4 and IPv6
            if (ia1 != null) ms.joinGroup(ia1);
            if (ia2 != null) ms.joinGroup(ia2);

            if (ia == null) iterateInterfaces();

            packetReady = true;
        }

        public void run(){

            while(active) {
                if(Thread.interrupted()) {
                    //to quit from the middle of the loop
                    logger.info("Thread.interrupted()");
                    active = false;
                    return;
                }

                // logger.info("heartBeat: {} cache size: {} packetReady: " + packetReady, heartBeat, instancesCache.getCache().size());

                if (packetReady && ia != null) {
                    try {

                        // Create a DatagramPacket packet to receive data into the buffer
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ia, port);

                        // Wait to receive a datagram
                        if (ms != null) ms.receive(packet);

                        Response response = Response.createFrom(packet);
                        logger.info(response.toString());

                        boolean found = false;
                        PtrRecord ptr = null;
                        Instance instance;

                        for (Record record : response.getRecords()) {
                            if (record instanceof PtrRecord) {
                                ptr = ((PtrRecord) record);
                            }
                            if (record.getName().contains(NAME) && !record.getName().equals(NAME + "local.")) {
                                found = true;
                            }
                        }

                        if (found) {
                            if (ptr != null) {
                                instance = Instance.createFromRecords(ptr, response.getRecords());
                                instance.host = packet.getAddress().getHostAddress();
                                if (instance.ttl > 0) instancesCache.addInstance(instance);
                                else instancesCache.removeInstance(instance.getName());
                            }
                        }

                        // Reset the length of the packet before reusing it.
                        packet.setLength(buffer.length);

                    } catch (IOException e) {
                        packetReady = false;
                    }
                } else {
                    try {
                        setupPackageReceiver();
                        packetReady = true;
                    } catch (IOException e) {
                        packetReady = false;
                    }
                }

                try {
                    Thread.sleep(SAMPLING_PERIOD);
                } catch (InterruptedException e) {
                    logger.info("[PacketReceiverHeartbeatAgent#run] was interrupted");
                    active = false;
                }
                heartBeat++;
            }
        }
    }

    public Discovery() {}

    public Discovery(String name) {
        this.NAME = name;
    }

    public void addListener(InstancesCache.CacheListenerI listener) {
        instancesCache.addListener(listener);
    }

    public void removeListener(InstancesCache.CacheListenerI listener) {
        instancesCache.removeListener(listener);
    }

    private void query(InetAddress ia) throws IOException {
        // Do question on Network!
        Service service = Service.fromName(NAME);
        Query query = Query.createFor(service, Domain.LOCAL);;
        query.runOnceNoInstances(ia);
        logger.debug("CACHE >> size:" + instancesCache.getCache().size() );
    }

    class QueryRunner implements Runnable {
        private int SAMPLING_PERIOD = 5000;
        private int PING_SAMPLING = 2;
        private int heartBeat = 0;
        private boolean activeQueryRunner = true;
        private Thread heartBeatQueryThread;

        /**
         * Starts the PacketReceiverHeartbeatAgent asynchronously
         */
        public void start() {
            heartBeatQueryThread = new Thread(this, "Discovery_QueryRunner");
            //terminate the thread with the VM.
            heartBeatQueryThread.setDaemon(true);
            heartBeatQueryThread.start();
        }
        @Override
        public void run() {
            while(activeQueryRunner) {
                if(Thread.interrupted()) {
                    //to quit from the middle of the loop
                    logger.info("Thread.interrupted()");
                    activeQueryRunner = false;
                    return;
                }

                // logger.info("QueryRunner: {}", heartBeat);
                setupInterfaces();

                if (heartBeat % PING_SAMPLING == 0) {
                    queryInterfaceIa();
                }

                try {
                    Thread.sleep(SAMPLING_PERIOD);
                } catch (InterruptedException e) {
                    logger.info("[PacketReceiverHeartbeatAgent#run] was interrupted");
                    activeQueryRunner = false;
                }
                heartBeat++;
            }
        }

        void setupInterfaces() {

            // Try IPv4
            try {
                if (ia1 == null) ia1 = InetAddress.getByName(MDNS_IP4_ADDRESS);
            } catch (UnknownHostException e1) {
                logger.error(e1.getMessage());
                ia1 = null;
            }

            // Try IPv6
            try {
                if (ia2 == null) ia2 = InetAddress.getByName(MDNS_IP6_ADDRESS);
            } catch (UnknownHostException e1) {
                logger.error(e1.getMessage());
                ia2 = null;
            }
            iterateInterfaces();
        }

        void queryInterfaceIa() {
            if (ia != null) {
                try {
                    query(ia);
                    logger.info(">>> QUERIED! using network address {}", ia.getHostAddress());
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    void iterateInterfaces() {
        String interfaceName = "local";
        if (networkInterfaceName != null) {
            interfaceName = networkInterfaceName;
        }
        try {
            ia = getInetAddress(interfaceName);
        } catch (IOException e) {
            try {
                ia = getLocalHostLANAddress();
            } catch (UnknownHostException e1) {
                logger.error(e1.getMessage());
            }
        }
    }

    public void run() {
        run("local");
    }

    public void run(String networkInterfaceName) {
        this.networkInterfaceName = networkInterfaceName;
        QueryRunner queryAgent = new QueryRunner();
        queryAgent.start();

        PacketReceiverHeartbeatAgent packetAgent = new PacketReceiverHeartbeatAgent();
        packetAgent.start();

        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets IP Address that is assigned on a given network interface (currently
     * only supports IPv4)
     *
     * @param interfaceName Name of network interface
     */
    public static InetAddress getInetAddress(String interfaceName) throws IOException, UnknownHostException {
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
