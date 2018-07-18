package ch.unitelabs.mdns.sd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class InstancesCache {
    private static Logger logger = LoggerFactory.getLogger(InstancesCache.class);
    private static long MAX_SERVICE_TIMEOUT = 10; // [s]
    private final Map<String, Instance> instances = new HashMap<>();
    private final Set<CacheListenerI> listners = new HashSet<>();

    private final int CONNECT_TIMEOUT = 1500;

    public InstancesCache() {
        InstancesCache.HeartbeatAgent agent = new InstancesCache.HeartbeatAgent();
        agent.start();
    }

    public void compare(Set<Instance> newInstances) {
//        List<Instance> objectsToRemove = new ArrayList<>();
//        for (Instance instance : instances.values()) {
//
//            boolean found = false;
//            // compare
//            for (Instance newInstance : newInstances) {
//                if (instance.getName().equals(newInstance.getName())) {
//                    found = true;
//                    break;
//                }
//            }
//
//            if (!found) {
//                objectsToRemove.add(instance);
//            }
//        }
//        objectsToRemove.stream().forEach(o -> );
    }

    public static interface CacheListenerI {
        void deviceAdded(String deviceName, String host, int port);
        void deviceRemoved(String deviceName);
    }

    public void addListener(CacheListenerI listner) {
        this.listners.add(listner);
    }

    public void removeListener(CacheListenerI listner) {
        this.listners.remove(listner);
    }

    public class HeartbeatAgent implements Runnable {
        private int SAMPLING_PERIOD = 1000;
        private int PING_SAMPLING = 1; // every n^th time of sampling period
        private int heartBeat = 0;
        private boolean active = true;
        private Thread heartBeatThread;

        /**
         * Starts the PacketReceiverHeartbeatAgent asynchronously
         */
        public void start() {
            heartBeatThread = new Thread(this, "Discovery_Heartbeat");
            //terminate the thread with the VM.
            heartBeatThread.setDaemon(true);
            heartBeatThread.start();
        }

        public void run(){
            while(active) {
                if(Thread.interrupted()) {
                    //to quit from the middle of the loop
                    logger.info("Thread.interrupted()");
                    active = false;
                    return;
                }
                logger.info("heartBeat: {} cache size: {} " + instances.keySet().toString(), heartBeat, instances.size() );


                if (heartBeat % PING_SAMPLING == 0) {
                    try {
                        pingAll();
                    } catch (Exception e) {
                        logger.error(e.getMessage());
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

        void pingAll() {
            List<Instance> objectsToRemove = new ArrayList<>();
            for (Instance instance : instances.values()) {
                // do timeout
                // @todo: check if cache instances have timeout
                if(!isInstanceReachable(instance)) {
                    objectsToRemove.add(instance);
                }
            }
            objectsToRemove.stream().forEach(
                    o -> removeInstance(o.getName())
            );
        }

        private boolean isInstanceReachable(Instance instance) {
            SocketAddress socketAddress = new InetSocketAddress(instance.host, instance.getPort());
            Socket socket = new Socket();
            boolean online = true;
            // Connect with 10 s timeout
            try {
                socket.connect(socketAddress, CONNECT_TIMEOUT);
            } catch (IOException iOException) {
                online = false;
            } finally {
                // As the close() operation can also throw an IOException
                // it must caught here
                try {
                    socket.close();
                } catch (IOException ex) {
                    // feel free to do something moderately useful here, eg log the event
                    logger.error(ex.getMessage());
                }

            }
            return online;
        }
    }

    public Map<String, Instance> getCache() {
        return instances;
    }

    public void addInstance(Instance instance) {
        // already exists!
        if (checkInstance(instance.getName())) return;

        if (instance.ttl == 0) {
            removeInstance(instance.getName());
        } else {
            logger.info("Instance added: {}", instance.toString());
            instances.put(instance.getName(), instance);
            for (CacheListenerI listener : listners) {
                listener.deviceAdded(instance.getName(), instance.host, instance.getPort());
            }
        }
    }

    public void removeInstance(String instanceName){
        // doesn't exist
        if (!checkInstance(instanceName)) return;

        logger.info("Instance removed: {}", instanceName);
        instances.remove(instanceName);
        for (CacheListenerI listener : listners) {
            listener.deviceRemoved(instanceName);
        }
    }

    public boolean checkInstance(String instaneName) {
        Instance preExistent = instances.get(instaneName);
        return preExistent != null;
    }
}
