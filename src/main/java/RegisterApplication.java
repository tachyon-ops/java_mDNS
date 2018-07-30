import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;

public class RegisterApplication {
    final static Logger logger = LoggerFactory.getLogger(RegisterApplication.class);

    public RegisterApplication() {
    }

    public static void main(String[] args) {

        // Argument Parser
        ArgumentParser parser = ArgumentParsers.newFor(RegisterApplication.class.toString()).build()
                .defaultHelp(true)
                .description("BioShake device has defaults and some options.");

        parser.addArgument("-p", "--port")
                .type(Integer.class)
                .required(true)
                .help("Specify port to use.");

        parser.addArgument("-s", "--serviceName")
                .type(String.class)
                .required(true)
                .help("Specify service name to use.");

        parser.addArgument("-n", "--networkInterface")
                .type(String.class)
                .setDefault("local")
                .help("Specify internet interface. Check ifconfig (LNX & MAC)"
                        + " and for windows, ask us for a tiny java app @ www.unitelabs.ch .");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        final String interfaceName = ns.getString("networkInterface");
        final int port = ns.getInt("port");
        String serviceName = ns.getString("serviceName");

        if (!serviceName.isEmpty()) {
            serviceName = "_" + serviceName + ".";
        }

        // termination is always added
        serviceName = serviceName + "_tcp.";

        logger.info("Service name: {}", serviceName);

        // Announce and respond!
        // runClient(interfaceName, serviceName, port);
        ArrayList<String> interfaces = new ArrayList<>(
                Arrays.asList("wlp1s0")
        );

        // interfaceName serves to handle nics now
        try {
            // If you want to iterate network interfaces
            final Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            final Collection<NetworkInterface> c = new ArrayList<>();

            while (nics.hasMoreElements()) {
                NetworkInterface networkInterface = nics.nextElement();
                System.out.println(networkInterface.getName());
                if (interfaces.contains(networkInterface.getName())) {
                    c.add(networkInterface);
                    System.out.println(networkInterface.getName() + " was added to list of network interfaces");
                }

            }

//            while (nics.hasMoreElements()) {
//                c.add(nics.nextElement());
//            }

            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.info("Interrupted ", e);
                    System.out.println("Bye!");
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
