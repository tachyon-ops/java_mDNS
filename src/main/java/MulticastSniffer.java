import com.lavoulp.mdns.sd.Discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;

public class MulticastSniffer {
    final static Logger logger = LoggerFactory.getLogger(MulticastSniffer.class);

    public static void main(String[] args) {
        InetAddress ia = null;
        String serviceName = null;
        try {
            try {
                serviceName = args[0];
                ia = InetAddress.getByName(args[1]);
            }
            catch (UnknownHostException e)  {
                //
            }
            // int port = Integer.parseInt(args[1]);
        }  // end try
        catch (Exception e) {
            System.err.println(e);
            System.err.println("Usage: java MulticastSniffer [service name] [interface name/optional]");
            // System.exit(1);
        }

        // Service type
        Discovery disc;
        if (serviceName != null) disc = new Discovery(serviceName);
        else disc = new Discovery();
        
        // Internet Address
        if (ia != null) disc.run(ia);
        else disc.run();
    }
}
