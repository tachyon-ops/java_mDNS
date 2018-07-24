import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;

public class AnnouncerApplication {
    final static Logger logger = LoggerFactory.getLogger(AnnouncerApplication.class);

    public AnnouncerApplication() {}

    public static void main(String[] args) {

        // Argument Parser
        ArgumentParser parser = ArgumentParsers.newFor(AnnouncerApplication.class.toString()).build()
                .defaultHelp(true)
                .description("BioShake device has defaults and some options.");

        parser.addArgument("-p", "--port")
                .type(Integer.class)
                .required(true)
                .help("Specify port to use.");

        parser.addArgument("-s", "--serviceName")
                .type(Integer.class)
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
        final String serviceName = ns.getString("serviceName");

        // Announce!
        runClient(interfaceName, serviceName, port);

        System.out.println("Bye!");
    }

    public static void runClient(String interfaceName, String serviceName, int port) {
        try {
            DatagramSocket socket = new DatagramSocket();

            String msg = "";
            byte[] msgData = msg.getBytes();

            InetAddress inetAddress = InetAddress.getByName(interfaceName);
            DatagramPacket datagramPacket = new DatagramPacket(msgData, msgData.length, inetAddress, port);

            socket.send(datagramPacket);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

