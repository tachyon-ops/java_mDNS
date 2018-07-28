package ch.unitelabs.mdns.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class AaaaRecord extends Record {
    private InetAddress address;

    public AaaaRecord(ByteBuffer buffer, String name, Class recordClass, long ttl) throws UnknownHostException {
        super(name, recordClass, ttl);
        byte[] addressBytes = new byte[16];
        buffer.get(addressBytes);
        address = InetAddress.getByAddress(addressBytes);
    }

    public InetAddress getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "AaaaRecord{" +
                "name='" + name + '\'' +
                ", recordClass=" + recordClass +
                ", ttl=" + ttl +
                ", address=" + address +
                '}';
    }
}