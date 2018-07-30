package multicast.dns;

import java.nio.ByteBuffer;

public abstract class Message {
    protected final ByteBuffer buffer;

    public final static int MAX_LENGTH = 9000; // max size of mDNS packets, in bytes

    private final static int USHORT_MASK = 0xFFFF;

    protected Message() {
        buffer = ByteBuffer.allocate(MAX_LENGTH);
    }

    protected int readUnsignedShort() {
        try {
            return buffer.getShort() & USHORT_MASK;
        } catch (Exception e) {
            return 0;
        }
    }

    public String dumpBuffer() {
        StringBuilder sb = new StringBuilder();
        int length = buffer.position();
        if (length == 0) {
            length = buffer.limit();
        }
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x", buffer.get(i)));
            if ((i + 1) % 8 == 0) {
                sb.append('\n');
            } else if ((i + 1) % 2 == 0) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}
