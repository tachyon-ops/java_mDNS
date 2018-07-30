package multicast.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class ARecord extends Record {
    private InetAddress address;

    public ARecord(ByteBuffer buffer, String name, Class recordClass, long ttl) throws UnknownHostException {
        super(name, recordClass, ttl);
        byte[] addressBytes = new byte[4];
        buffer.get(addressBytes);
        address = InetAddress.getByAddress(addressBytes);
    }

    public InetAddress getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "ARecord{" +
                "name='" + name + '\'' +
                ", recordClass=" + recordClass +
                ", ttl=" + ttl +
                ", address=" + address +
                '}';
    }
}
