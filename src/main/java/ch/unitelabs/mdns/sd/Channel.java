package ch.unitelabs.mdns.sd;

import ch.unitelabs.mdns.new_dns_impl.*;
import ch.unitelabs.mdns.new_dns_impl.helpers.Cache;
import ch.unitelabs.mdns.new_dns_impl.helpers.MdnsThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static ch.unitelabs.mdns.utils.MulticastDNS.*;

public class Channel implements AutoCloseable {

    private final static Logger logger = LoggerFactory.getLogger(Channel.class);

    enum SDType {
        DISCOVERY, ANNOUNCE
    }

    private final class Sender implements Runnable {
        /**
         * Constructor.
         */
        Sender() {
            // empty.
        }

        @Override
        public final void run() {
            final ByteBuffer buf = ByteBuffer.allocate(MAX_DNS_MESSAGE_SIZE);
            buf.order(ByteOrder.BIG_ENDIAN);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    final DnsMessage msg = sq.take();
                    logger.info("Sending " + msg);
                    final byte[] packet = msg.encode();
                    buf.clear();
                    buf.put(packet);
                    buf.flip();
                    ipv4.forEach(ni -> send(ni, buf, IPV4_SOA));
                    ipv6.forEach(ni -> send(ni, buf, IPV6_SOA));
                } catch (final InterruptedException e) {
                    logger.info("Interrupted while waiting to send DNS message", e);
                    Thread.currentThread().interrupt();
                }
            }
        }

        /**
         * Sends given datagram to given channel an address
         *
         * @param key channel
         * @param src the buffer containing the datagram to be sent
         * @param target the address to which the datagram is to be sent
         */
        private void send(final SelectionKey key, final ByteBuffer src, final InetSocketAddress target) {
            final int position = src.position();
            try {
                ((DatagramChannel) key.channel()).send(src, target);
                logger.info("Sent DNS message to " + target);
            } catch (final IOException e) {
                logger.warn("I/O error while sending DNS message to " + target, e);
            } finally {
                src.position(position);
            }
        }

    }

    private final class Receiver implements Runnable {

        /**
         * Constructor.
         */
        Receiver() {
            // empty.
        }

        @Override
        public final void run() {
            final ByteBuffer buf = ByteBuffer.allocate(MAX_DNS_MESSAGE_SIZE);
            buf.order(ByteOrder.BIG_ENDIAN);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    selector.select();
                    logger.info("Channels ready for I/O operations");
                    final Set<SelectionKey> selected = selector.selectedKeys();
                    for (final SelectionKey key : selected) {
                        final DatagramChannel channel = (DatagramChannel) key.channel();
                        buf.clear();
                        final InetSocketAddress address = (InetSocketAddress) channel.receive(buf);
                        if (address != null && buf.position() != 0) {
                            buf.flip();
                            final byte[] bytes = new byte[buf.remaining()];
                            buf.get(bytes);
                            final DnsMessage msg = DnsMessage.decode(bytes, clock.instant());
                            logger.info("Received " + msg + " on " + address);
                            listener.accept(msg);
                        }
                    }
                } catch (final ClosedChannelException e) {
                    logger.info("Channel closed while waiting to receive DNS message", e);
                    Thread.currentThread().interrupt();
                } catch (final IOException e) {
                    logger.warn("I/O error while receiving DNS message", e);
                }
            }
        }

    }

    private static String NAME = "_tcp.";

    /** multiplexor. */
    private final Selector selector;

    /** queue of sent DNS messages. */
    private final BlockingQueue<DnsMessage> sq;

    /** IPV4 channel(s). */
    private final List<SelectionKey> ipv4;

    /** IPV6 channel(s). */
    private final List<SelectionKey> ipv6;

    /** DNS record cache. */
    public static Cache cache;

    private static Clock clock;

    private static SimpleService service;

    /** listener to be invoked whenever a new message is received. */
    public final ServiceListener listener;

    private final ExecutorService es;

    /** future to cancel sending messages. */
    private Future<?> sender;
    private Future<?> receiver;

    SDType type;

    Channel(
            String name, final Collection<NetworkInterface> nis, Clock clock,
            SimpleService service, SDType type
    ) throws IOException {
        this.NAME = name;

        this.type = type;

        listener = new ServiceListener(this);
        listener.add(service);

        es = Executors.newFixedThreadPool(2, new MdnsThreadFactory("channel"));

        Channel.clock = clock;
        Channel.service = service;

        cache = new Cache();
        selector = Selector.open();
        sq = new LinkedBlockingQueue<>();

        ipv4 = new ArrayList<>();
        ipv6 = new ArrayList<>();

        for (final NetworkInterface ni : nis) {
            openChannel(ni, StandardProtocolFamily.INET, false).map(this::register).ifPresent(ipv4::add);
            openChannel(ni, StandardProtocolFamily.INET6, false).map(this::register).ifPresent(ipv6::add);
        }

        if (ipv4.isEmpty() && ipv6.isEmpty()) {
            for (final NetworkInterface ni : nis) {
                logger.info("No Network Interface found, adding Loopback interface");
                openChannel(ni, StandardProtocolFamily.INET, true).map(this::register).ifPresent(ipv4::add);
                openChannel(ni, StandardProtocolFamily.INET6, true).map(this::register).ifPresent(ipv6::add);
            }
        }

        if (ipv4.isEmpty() && ipv6.isEmpty()) {
            throw new IOException("No network interface suitable for multicast");
        }

        enable();

        QueryRunner queryAgent = new QueryRunner();
        queryAgent.start();

        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("Interrupted Exception: ", e);
            }
        }
    }

    class QueryRunner implements Runnable {
        private int SAMPLING_PERIOD = 5000;
        private int PING_SAMPLING = 2;
        private int heartBeat = 0;
        private boolean activeQueryRunner = true;
        private Thread heartBeatQueryThread;

        /**
         * Starts the PacketReceiverHeartbeatAgent asynchronously
         */
        public void start() {
            heartBeatQueryThread = new Thread(this, "Discovery_QueryRunner");
            //terminate the thread with the VM.
            heartBeatQueryThread.setDaemon(true);
            heartBeatQueryThread.start();
        }
        @Override
        public void run() {
            while(activeQueryRunner) {
                if(Thread.interrupted()) {
                    //to quit from the middle of the loop
                    logger.info("Thread.interrupted()");
                    activeQueryRunner = false;
                    return;
                }

                if (heartBeat % PING_SAMPLING == 0) {
                    logger.info("[PacketReceiverHeartbeatAgent#run] Calling QUERY");
                    callQuery();
                }

                try {
                    Thread.sleep(SAMPLING_PERIOD);
                } catch (InterruptedException e) {
                    logger.info("[PacketReceiverHeartbeatAgent#run] was interrupted");
                    activeQueryRunner = false;
                }
                heartBeat++;
            }
        }
    }

    private boolean checkIfQuestionsBelongToService(List<DnsQuestion> questions) {
        String fullQualifiedName = NAME + "local.";
        for (DnsQuestion question : questions) {
            if (fullQualifiedName.equals(question.name())) {
                // it's query from service!
                logger.info("REQUESTED QUERY FROM " + question.name());

                return true;
            }
        }
        return false;
    }

    public final Void respond() {
        final Instant now = now();
        final String hostname = service.hostname();
        final Attributes attributes = service.attributes();
        final String serviceName = service.name();
        final short unique = uniqueClass(CLASS_IN);
        /* no stamp when announcing, TTL will be the one given. */
        final Optional<Instant> stamp = Optional.empty();
        final DnsMessage.Builder b = DnsMessage
                .response(FLAGS_AA)
                .addAnswer(new PtrRecord(service.registrationPointerName(), CLASS_IN, TTL, now, service.instanceName()), stamp)
                .addAnswer(new SrvRecord(serviceName, unique, TTL, now, service.port(), hostname), stamp)
                .addAnswer(new TxtRecord(serviceName, unique, TTL, now, attributes), stamp);

        service.ipv4Address().ifPresent(a -> b.addAnswer(new AddressRecord(hostname, unique, TTL, now, a), stamp));

        service.ipv6Address().ifPresent(a -> b.addAnswer(new AddressRecord(hostname, unique, TTL, now, a), stamp));

        sendMessage(b.get());
        return null;
    }

    public final Void callQuery() {
        final Instant now = Instant.now();
        final String hostname = service.hostname();
        final String serviceName = service.name();
        final DnsMessage.Builder b = DnsMessage
                .query()
                .addQuestion(new DnsQuestion(hostname, TYPE_ANY, CLASS_IN))
                .addQuestion(new DnsQuestion(serviceName, TYPE_ANY, CLASS_IN))
                .addAuthority(new SrvRecord(serviceName, CLASS_IN, TTL, now, service.port(), hostname));

        service.ipv4Address().ifPresent(a -> b.addAuthority(new AddressRecord(hostname, CLASS_IN, TTL, now, a)));
        service.ipv6Address().ifPresent(a -> b.addAuthority(new AddressRecord(hostname, CLASS_IN, TTL, now, a)));

        sendMessage(b.get());

        return null;
    }

    void sendMessage(DnsMessage msg) {
        sq.add(msg);
    }

    public static final Optional<DnsRecord> cachedRecord(final String name, final short type, final short clazz) {
        return cache.get(name, type, clazz);
    }

    private SelectionKey register(final DatagramChannel channel) {
        try {
            return channel.register(selector, SelectionKey.OP_READ);
        } catch (final ClosedChannelException e) {
            logger.error("Could not register channel with selector");
            throw new IllegalStateException(e);
        }
    }

    private Optional<DatagramChannel> openChannel(final NetworkInterface ni, final ProtocolFamily family,
                                                  final boolean loopback) {
        final boolean ipv4Protocol = family == StandardProtocolFamily.INET;
        final InetAddress addr = ipv4Protocol ? IPV4_ADDR : IPV6_ADDR;
        try {
            final Class<? extends InetAddress> ipvClass = ipv4Protocol ? Inet4Address.class : Inet6Address.class;
            if (ni.supportsMulticast() && ni.isUp() && ni.isLoopback() == loopback && hasIpv(ni, ipvClass)) {
                final Optional<DatagramChannel> channel = openChannel(family);
                if (channel.isPresent()) {
                    channel.get().setOption(StandardSocketOptions.IP_MULTICAST_IF, ni);
                    channel.get().join(addr, ni);
                    logger.info("Joined multicast address " + addr + " on " + ni);
                    return channel;
                }
            }
            logger.info("Ignored " + ni + " for " + addr);
            return Optional.empty();
        } catch (final IOException e) {
            logger.warn("Ignored " + ni + " for " + addr);
            return Optional.empty();
        }
    }

    /**
     * Opens a new {@link DatagramChannel}.
     *
     * @param family the protocol family
     * @return a new datagram channel
     */
    private Optional<DatagramChannel> openChannel(final ProtocolFamily family) {
        try {
            final DatagramChannel channel = DatagramChannel.open(family);
            channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.setOption(StandardSocketOptions.IP_MULTICAST_TTL, 255);
            channel.bind(new InetSocketAddress(MDNS_PORT));
            return Optional.of(channel);
        } catch (final UnsupportedOperationException e) {
            logger.info("Protocol Family [" + family.name() + "] not supported on this machine.", e);
            return Optional.empty();
        } catch (final IOException e) {
            logger.warn("Fail to create channel", e);
            return Optional.empty();
        }
    }

    private boolean hasIpv(final NetworkInterface ni, final Class<? extends InetAddress> ipv) {
        for (final Enumeration<InetAddress> e = ni.getInetAddresses(); e.hasMoreElements();) {
            if (e.nextElement().getClass().isAssignableFrom(ipv)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Adds the given message to the queue of messages to send.
     *
     * @param message message to send
     */
    final void send(final DnsMessage message) {
        sq.add(message);
    }

    public static Instant now() {
        return clock.instant();
    }

    public final synchronized void close() {
        logger.info("Closing channel");
        selector.wakeup();
        disable();
        es.shutdownNow();
        close(ipv4);
        close(ipv6);
    }

    private void close(final List<SelectionKey> keys) {
        for (final SelectionKey key : keys) {
            try {
                key.channel().close();
            } catch (final IOException e) {
                logger.info("I/O error when closing channel", e);
            }
        }
    }

    private synchronized void disable() {
        if (sender != null) {
            sender.cancel(true);
        }
        if (receiver != null) {
            receiver.cancel(true);
        }
    }

    final synchronized void enable() {
        if (sender == null) {
            sender = es.submit(new Sender());
        }
        if (receiver == null) {
            receiver = es.submit(new Receiver());
        }
    }
}
