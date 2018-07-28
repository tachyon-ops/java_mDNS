package ch.unitelabs.mdns.sd;

import ch.unitelabs.mdns.new_dns_impl.SimpleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Clock;
import java.util.Collection;


public class Discovery {
    private final static Logger logger = LoggerFactory.getLogger(Discovery.class);

    SimpleService service;
    Channel channel;
    Clock clock;

    public Discovery(String name, Collection<NetworkInterface> nis) throws IOException {
        service = new SimpleService("IDENTIFIER", "_sila._tcp.");
        clock = Clock.systemDefaultZone();
        String hostname = InetAddress.getLocalHost().getHostName();
        service.setHostname(hostname);
        service.setPort(port);
        channel = new Channel(name, nis, clock, service, Channel.SDType.DISCOVERY);
    }

    public InstancesCache instancesCache = new InstancesCache();

    private int port = 5353;
    byte[] buffer = new byte[65509];
    private InetAddress ia, ia1, ia2;
    private static String NAME = "_tcp.";
    private static final String MDNS_IP4_ADDRESS = "224.0.0.251";
    private static final String MDNS_IP6_ADDRESS = "FF02::FB";

//    public class PacketReceiverHeartbeatAgent implements Runnable {
//        private int SAMPLING_PERIOD = 1000;
//        private int packetsReceiverHeartBeat = 0;
//        private boolean active = true;
//        private boolean packetReady = true;
//        private Thread heartBeatThread;
//
//        MulticastSocket ms;
//
//        /**
//         * Starts the PacketReceiverHeartbeatAgent asynchronously
//         */
//        public void start() {
//            heartBeatThread = new Thread(this, "Discovery_Heartbeat");
//            //terminate the thread with the VM.
//            heartBeatThread.setDaemon(true);
//            heartBeatThread.start();
//        }
//
//        void setupPackageReceiver() throws IOException {
//            logger.info("Packet Receiver setup");
//
//            // Create a socket to listen on the port.
//            ms = new MulticastSocket(port);
//
//
//            // Join Multicast Socket to Multicast Addresses IPv4 and IPv6
//            if (ia1 != null) ms.joinGroup(ia1);
//            if (ia2 != null) ms.joinGroup(ia2);
//
//            if (ia == null) assignInterfaceFromName();
//
//            packetReady = true;
//        }
//
//        public void run(){
//
//            while(active) {
//                if(Thread.interrupted()) {
//                    //to quit from the middle of the loop
//                    logger.info("Thread.interrupted()");
//                    active = false;
//                    return;
//                }
//
//                logger.info("packetsReceiverHeartBeat: {} cache size: {} packetReady: " + packetReady, packetsReceiverHeartBeat, instancesCache.getCache().size());
//
//                if (packetReady && ia != null) {
//                    try {
//
//                        // Create a DatagramPacket packet to receive data into the buffer
//                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ia, port);
//
//                        // Wait to receive a datagram
//                        if (ms != null) ms.receive(packet);
//
//
//                        Response response = Response.createFrom(packet);
//                        logger.info(response.toString());
//
//                        boolean found = false;
//                        PtrRecord ptr = null;
//                        Instance instance;
//
//                        for (Record record : response.getRecords()) {
//                            if (record instanceof PtrRecord) {
//                                ptr = ((PtrRecord) record);
//                            }
//                            if (record.getName().contains(NAME) && !record.getName().equals(NAME + "local.")) {
//                                found = true;
//                            }
//                        }
//
//                        if (found) {
//                            if (ptr != null) {
//                                instance = Instance.createFromRecords(ptr, response.getRecords());
//                                instance.host = packet.getAddress().getHostAddress();
//                                if (instance.ttl > 0) instancesCache.addInstance(instance);
//                                else instancesCache.removeInstance(instance.getName());
//                            }
//                        }
//
//                        // Reset the length of the packet before reusing it.
//                        packet.setLength(buffer.length);
//
//                    } catch (IOException e) {
//                        packetReady = false;
//                    }
//                } else {
//                    try {
//                        setupPackageReceiver();
//                        packetReady = true;
//                    } catch (IOException e) {
//                        packetReady = false;
//                    }
//                }
//
//                try {
//                    Thread.sleep(SAMPLING_PERIOD);
//                } catch (InterruptedException e) {
//                    logger.info("[PacketReceiverHeartbeatAgent#run] was interrupted");
//                    active = false;
//                }
//                packetsReceiverHeartBeat++;
//            }
//        }
//    }




//
//    public void addListener(InstancesCache.CacheListenerI listener) {
//        instancesCache.addListener(listener);
//    }
//
//    public void removeListener(InstancesCache.CacheListenerI listener) {
//        instancesCache.removeListener(listener);
//    }

//    class QueryRunner implements Runnable {
//        private int SAMPLING_PERIOD = 5000;
//        private int PING_SAMPLING = 2;
//        private int heartBeat = 0;
//        private boolean activeQueryRunner = true;
//        private Thread heartBeatQueryThread;
//
//        /**
//         * Starts the PacketReceiverHeartbeatAgent asynchronously
//         */
//        public void start() {
//            heartBeatQueryThread = new Thread(this, "Discovery_QueryRunner");
//            //terminate the thread with the VM.
//            heartBeatQueryThread.setDaemon(true);
//            heartBeatQueryThread.start();
//        }
//        @Override
//        public void run() {
//            while(activeQueryRunner) {
//                if(Thread.interrupted()) {
//                    //to quit from the middle of the loop
//                    logger.info("Thread.interrupted()");
//                    activeQueryRunner = false;
//                    return;
//                }
//
//                // logger.info("QueryRunner: {}", heartBeat);
//                setupInterfaces();
//
//                if (heartBeat % PING_SAMPLING == 0) {
//                    // queryInterfaceIa();
//                    try {
//                        iterateAndQueryAllInterfaces();
//                    } catch (IOException e) {
//                        logger.error("Could not iterateAndQueryAllInterfaces(): " + e.getMessage());
//                    }
//                }
//
//                try {
//                    Thread.sleep(SAMPLING_PERIOD);
//                } catch (InterruptedException e) {
//                    logger.info("[PacketReceiverHeartbeatAgent#run] was interrupted");
//                    activeQueryRunner = false;
//                }
//                heartBeat++;
//            }
//        }
//
//        void setupInterfaces() {
//
//            // Try IPv4
//            try {
//                if (ia1 == null) ia1 = InetAddress.getByName(MDNS_IP4_ADDRESS);
//            } catch (UnknownHostException e1) {
//                logger.error(e1.getMessage());
//                ia1 = null;
//            }
//
//            // Try IPv6
//            try {
//                if (ia2 == null) ia2 = InetAddress.getByName(MDNS_IP6_ADDRESS);
//            } catch (UnknownHostException e1) {
//                logger.error(e1.getMessage());
//                ia2 = null;
//            }
//            assignInterfaceFromName();
//        }
//    }
//
//    void assignInterfaceFromName() {
//        String interfaceName = "local";
//        try {
//            ia = getInetAddress(interfaceName);
//        } catch (IOException e) {
//            try {
//                ia = getLocalHostLANAddress();
//            } catch (UnknownHostException e1) {
//                logger.error(e1.getMessage());
//            }
//        }
//    }
//
//    void iterateAndQueryAllInterfaces() throws IOException {
//        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
//        for (NetworkInterface networkInterface : Collections.list(nets)) {
//            if (networkInterface.isUp() && !networkInterface.getName().equals("lo")) {
//                try {
//                    query(getInetAddress(networkInterface.getName()));
//                    logger.info("Queried interface {}", networkInterface.getName());
//                } catch (IOException e) {
//                    logger.error("Could not query interface {}: " + e.getMessage(), networkInterface.getName());
//                }
//            }
//        }
//    }
//
//    private void query(InetAddress ia) throws IOException {
//        Service service = Service.fromName(NAME);
//        Query query = Query.createFor(service, Domain.LOCAL);;
//        query.runOnceNoInstances(ia);
//    }
//
//    public void run() {
//        // Query runner
//        QueryRunner queryAgent = new QueryRunner();
//        queryAgent.start();
//
//        // Packet Listener
//        PacketReceiverHeartbeatAgent packetAgent = new PacketReceiverHeartbeatAgent();
//        packetAgent.start();
//
//        while(true) {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
}
