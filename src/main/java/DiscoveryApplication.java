import ch.unitelabs.mdns.sd.Discovery;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

public class DiscoveryApplication {
    private final static Logger logger = LoggerFactory.getLogger(DiscoveryApplication.class);

    public static void main(String[] args) {
        // Argument Parser
        ArgumentParser parser = ArgumentParsers.newFor(DiscoveryApplication.class.toString()).build()
                .defaultHelp(true)
                .description("BioShake device has defaults and some options.");

        parser.addArgument("-s", "--serviceName")
                .type(String.class)
                .setDefault("")
                .help("Specify service name to use.");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        String serviceName = ns.getString("serviceName");

        if (!serviceName.isEmpty()) {
            serviceName = "_" + serviceName + ".";
        }

        // termination is always added
        serviceName = serviceName + "_tcp.";

        logger.info("Service name: {}", serviceName);

        try {
            // If you want to iterate network interfaces
            final Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            final Collection<NetworkInterface> c = new ArrayList<>();
            while (nics.hasMoreElements()) {
                c.add(nics.nextElement());
            }

            // Service type but no need for interface names ;)
            new Discovery(serviceName, c);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Bye!");
    }
}
