package ch.unitelabs.mdns.dns;

import java.nio.ByteBuffer;

public class SrvRecord extends Record {
    private final int priority;
    private final int weight;
    private final int port;
    private final String target;

    public SrvRecord(ByteBuffer buffer, String name, Record.Class recordClass, long ttl) {
        super(name, recordClass, ttl);
        priority = buffer.getShort() & USHORT_MASK;
        weight = buffer.getShort() & USHORT_MASK;
        port = buffer.getShort() & USHORT_MASK;
        target = readNameFromBuffer(buffer);
    }

    public int getPriority() {
        return priority;
    }

    public int getWeight() {
        return weight;
    }

    public int getPort() {
        return port;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "SrvRecord{" +
                "name='" + name + '\'' +
                ", recordClass=" + recordClass +
                ", ttl=" + ttl +
                ", priority=" + priority +
                ", weight=" + weight +
                ", port=" + port +
                ", target='" + target + '\'' +
                '}';
    }
}