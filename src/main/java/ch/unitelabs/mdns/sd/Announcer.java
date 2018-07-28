package ch.unitelabs.mdns.sd;

import ch.unitelabs.mdns.new_dns_impl.SimpleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Clock;
import java.util.Collection;

public class Announcer {
    private final static Logger logger = LoggerFactory.getLogger(Announcer.class);

    SimpleService service;
    Channel channel;
    Clock clock;

    public Announcer(String name, int port, final Collection<NetworkInterface> nis) throws IOException {
        service = new SimpleService("IDENTIFIER", "_sila._tcp.");
        clock = Clock.systemDefaultZone();
        String hostname = InetAddress.getLocalHost().getHostName();
        service.setHostname(hostname);
        service.setPort(port);
        channel = new Channel(name, nis, clock, service, Channel.SDType.ANNOUNCE);
        channel.respond();
    }
}