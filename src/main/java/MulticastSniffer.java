import com.lavoulp.mdns.sd.Discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;

public class MulticastSniffer {
    final static Logger logger = LoggerFactory.getLogger(MulticastSniffer.class);

    public static void main(String[] args) {
        try {
            try {
                InetAddress ia = InetAddress.getByName(args[0]);
            }
            catch (UnknownHostException e)  {
                //
            }
            // int port = Integer.parseInt(args[1]);
        }  // end try
        catch (Exception e) {
            System.err.println(e);
            System.err.println("Usage: java MulticastSniffer MulticastAddress port");
            // System.exit(1);
        }

        // Service type
        if (args[1] != null) Discovery disc = new Discovery();
        else Discovery disc = new Discovery(args[1]);
        
        // Internet Address
        if (ia != null) disc.run(ia);
        else disc.run();
    }
}
