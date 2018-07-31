import multicast.sd.Discovery;
import multicast.sd.Service;
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

        parser.addArgument("-st", "--serviceType")
                .type(String.class)
                .setDefault("_tcp")
                .help("Specify service type to use.");

        parser.addArgument("-i", "--interfaces")
                .type(String.class)
                .setDefault("")
                .help("Specify a list of interfaces to use, with a comma separated value like this:\n" +
                        "   -i interface1,interface2");

        parser.addArgument("-d", "--domain")
                .type(String.class)
                .setDefault("local")
                .help("Let's you use more uncommon domains");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        String serviceName = ns.getString("serviceName");
        String serviceType = ns.getString("serviceType");
        String domain = ns.getString("domain");
        String interfaceList = ns.getString("interfaces");
        String serviceRegistration = serviceName + "." + serviceType + ".";

        logger.info("SETTINGS >> Service NAME: {}", serviceName);
        logger.info("SETTINGS >> Service TYPE: {}", serviceType);
        logger.info("SETTINGS >> Domain: {}", domain);
        logger.info("SETTINGS >> Combined: {}", serviceRegistration + domain + ".");
        logger.info("SETTINGS >> Interface List: {}", interfaceList);

        // termination is always added
        // serviceName = serviceName + "_tcp.";

        ArrayList<String> interfaces = new ArrayList<>();
        if (!interfaceList.isEmpty()) {
            String[] list = interfaceList.split(",");
            for (String interfaceString : list) {
                interfaces.add(interfaceString);
            }
        }

        try {
            // If you want to iterate network interfaces
            final Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            final Collection<NetworkInterface> c = new ArrayList<>();


            if (interfaces.isEmpty()) {
                while (nics.hasMoreElements()) {
                    c.add(nics.nextElement());
                }
            } else {
                while (nics.hasMoreElements()) {
                    NetworkInterface networkInterface = nics.nextElement();
                    System.out.println(networkInterface.getName());
                    if (interfaces.contains(networkInterface.getName())) {
                        c.add(networkInterface);
                        System.out.println(networkInterface.getName() + " was added to list of network interfaces");
                    }
                    c.add(networkInterface);
                }
            }

            final Discovery discovery = new Discovery(
                    serviceRegistration,
                    domain, c, 5
            );
            discovery.run();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
