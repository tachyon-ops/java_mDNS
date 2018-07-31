package multicast.sd;

import multicast.dns.Domain;
import multicast.dns.PtrRecord;
import multicast.dns.Record;
import multicast.dns.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.*;

import static multicast.helpers.MulticastDns.*;


public class Discovery {
    private final static Logger logger = LoggerFactory.getLogger(Discovery.class);

    public class PacketReceiverHeartbeatAgent implements Runnable {
        private int SAMPLING_PERIOD = 1000;
        private int packetsReceiverHeartBeat = 0;
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
            ms = new MulticastSocket(MDNS_PORT);


            // Join Multicast Socket to Multicast Addresses IPv4 and IPv6
            ms.joinGroup(IPV4_ADDR);
            ms.joinGroup(IPV6_ADDR);

//            assignInterfaceFromName();

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

                // logger.info("packetsReceiverHeartBeat: {} cache size: {} packetReady: " + packetReady, packetsReceiverHeartBeat, instancesCache.getCache().size());

                if (packetReady && (ms != null) ) {
                    try {

                        for (NetworkInterface networkInterface : nis) {
                            if (networkInterface.isUp()) {
                                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                                while (inetAddresses.hasMoreElements()) {
                                    receivePackets(inetAddresses.nextElement());
                                }
                            }
                        }

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
                packetsReceiverHeartBeat++;
            }
        }

        void receivePackets(InetAddress inetAddress) throws IOException {
            // Create a DatagramPacket packet to receive data into the buffer
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, inetAddress, MDNS_PORT);

            // Wait to receive a datagram
            ms.receive(packet);


            Response response = Response.createFrom(packet);
            logger.info(response.toString());

            boolean found = false;
            PtrRecord ptr = null;
            Instance instance;

            for (Record record : response.getRecords()) {
                if (record instanceof PtrRecord) {
                    ptr = ((PtrRecord) record);
                }
                if (record.getName().contains(SERVICE_REGISTRY)
                    // && !record.getName().equals(SERVICE_REGISTRY + "local.")
                        ) {
                    found = true;
                }
            }

            if (found) {
                if (ptr != null) {
                    instance = Instance.createFromRecords(ptr, response.getRecords());
                    // instance.host = packet.getAddress().getHostAddress();
                    if (instance.ttl > 0) instancesCache.addInstance(instance);
                    else instancesCache.removeInstance(instance.getName());
                }
            }

            // Reset the length of the packet before reusing it.
            packet.setLength(buffer.length);
        }
    }

    class QueryRunner implements Runnable {
        private int SAMPLING_PERIOD = 1000;
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

                if (heartBeat % QUERY_RATE == 0) {
                    try {
                        queryAll();
                    } catch (IOException e) {
                        logger.error("Could not queryAll(): " + e.getMessage());
                    }
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
            for (NetworkInterface networkInterface : nis) {
                try {
                    if (networkInterface.isUp() && networkInterface.supportsMulticast()) {
                        logger.warn("Interface " + networkInterface.getName() + " ready!");
                        interfaceList.add(networkInterface);
                    }
                } catch (SocketException e) {
                    interfaceList.remove(networkInterface);
                    logger.warn("Interface " + networkInterface.getName() + " is not ready.");
                }
            }
        }
    }


    public InstancesCache instancesCache = new InstancesCache();

    byte[] buffer = new byte[MAX_DNS_MESSAGE_SIZE];
    private static String SERVICE_REGISTRY;
    private static String DOMAIN_REGISTRY;

    private static int QUERY_RATE; // in [seconds]

    private final Collection<NetworkInterface> nis;

    private List<NetworkInterface> interfaceList;

    public Discovery(String name, String domain, final Collection<NetworkInterface> nis, int queryRateInSeconds) {
        QUERY_RATE = queryRateInSeconds;
        SERVICE_REGISTRY = name;
        DOMAIN_REGISTRY = domain;
        this.nis = nis;
        interfaceList = new ArrayList<>();
    }

    /**
     * PUBLIC API
     */

    public void run() {
        // Query runner
        QueryRunner queryAgent = new QueryRunner();
        queryAgent.start();

        // Packet Listener
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

    public Map<String, Instance> getCache() {
        return instancesCache.getCache();
    }

    public void cleanCache() {
        instancesCache.cleanCache();
    }

    public void queryAll() throws IOException {
        for (NetworkInterface networkInterface : interfaceList) {
            if (networkInterface.isUp()) {
                query(getInetAddress(networkInterface.getName()));
//                logger.info("Queried interface {}", networkInterface.getName());
            }
        }
    }

    public void query(InetAddress inetAddress) throws IOException {
        Query query = Query.createFor(
                Service.fromName(SERVICE_REGISTRY),
                Domain.fromName(DOMAIN_REGISTRY)
        );
        query.runOnceNoInstances(inetAddress);
    }
}
