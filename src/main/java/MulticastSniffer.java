import ch.unitelabs.mdns.sd.Discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;

public class MulticastSniffer {
    final static Logger logger = LoggerFactory.getLogger(MulticastSniffer.class);

    public static void main(String[] args) {
        String networkInterfaceName = null;
        String serviceName = null;

        try {
            serviceName = args[0];
            logger.debug("Service name: " + serviceName);
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error("Usage: java MulticastSniffer [service name] [interface name/optional]");
        }

        try {
            networkInterfaceName = args[1];
            logger.debug("Network name: " + args[1] + " " + networkInterfaceName );
        }  // end try
        catch (Exception e) {
            logger.debug(e.getMessage());
            logger.debug("Usage: java MulticastSniffer [service name] [interface name/optional]");
        }

        // Service type
        Discovery disc;
        if (serviceName != null) disc = new Discovery(serviceName);
        else disc = new Discovery();
        
        // Internet Address
        if (networkInterfaceName != null) disc.run(networkInterfaceName);
        else disc.run();
    }
}
