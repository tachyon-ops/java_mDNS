package ch.unitelabs.mdns.sd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InstancesCache {
    private static Logger log = LoggerFactory.getLogger(InstancesCache.class);
    private static long MAX_SERVICE_TIMEOUT = 10; // [s]
    private final Map<String, Instance> instances = new HashMap<>();
    private final Set<CacheListenerI> listners = new HashSet<>();

    public Map<String, Instance> getCache() {
        return instances;
    }

    public static interface CacheListenerI {
        void deviceAdded(String deviceName, String host, int port);
        void deviceRemoved(String deviceName);
    }

    public void InstancesCache(){ }

    public void addListener(CacheListenerI listner) {
        this.listners.add(listner);
    }

    public void removeListener(CacheListenerI listner) {
        this.listners.remove(listner);
    }

    public void addInstance(Instance instance) {
        // check if ttl == 0 (end of life)
        if (instance.ttl == 0) {
            removeInstance(instance.getName());
            return;
        }
        instances.put(instance.getName(), instance);

        // get host: important for getting shutdown ttls
        String hostName = null;
        for(InetAddress address : instance.getAddresses()) {
            hostName = address.getHostName();
        }

        // in case we don't get a proper hostname... there must be an address
        if (hostName == null) hostName = instance.getAddresses().toString();

        for (CacheListenerI listener : listners) {
            listener.deviceAdded(instance.getName(), hostName, instance.getPort());
        }
    }

    public void removeInstance(String instanceName){
        instances.remove(instanceName);
        for (CacheListenerI listener : listners) {
            listener.deviceRemoved(instanceName);
        }
    }

    // @todo: check if cache instances have timeout

}
