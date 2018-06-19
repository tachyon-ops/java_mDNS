import ch.unitelabs.mdns.sd.Discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MulticastSniffer {
    final static Logger logger = LoggerFactory.getLogger(MulticastSniffer.class);

    public static void main(String[] args) {
        String serviceName;

        try {
            serviceName = "_" + args[0] + "._tcp.";
            logger.debug("Service name: " + serviceName);
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error("Usage: java Multi-cast Sniffer [service name]");
            serviceName = "_tcp.";
        }

        logger.info("Service name: {}", serviceName);

        // Service type but no need for interface names ;)
        Discovery disc = new Discovery(serviceName);
        disc.run();
    }
}
