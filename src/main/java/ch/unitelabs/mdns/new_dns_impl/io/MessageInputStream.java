package ch.unitelabs.mdns.new_dns_impl.io;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class MessageInputStream extends ByteArrayInputStream {

    /** pointers for decompression: index of a string in this stream. */
    private final Map<Integer, String> pointers;

    public final byte[] buffer;

    /**
     * Creates a {@code MessageInputStream} so that it uses {@code buffer} as its buffer array. The buffer array is
     * not copied.
     *
     * @see ByteArrayInputStream#ByteArrayInputStream(byte[])
     * @param buffer the input buffer.
     */
    public MessageInputStream(final byte[] buffer) {
        super(buffer);
        this.buffer = buffer;
        pointers = new HashMap<>();
    }

    /**
     * Closing a {@code MessageInputStream} has no effect. The methods in this class can be called after the stream
     * has been closed without generating an {@code IOException}.
     *
     * @see ByteArrayInputStream#close()
     */
    @Override
    public final void close() {
        // empty.
    }

    /**
     * Reads the next byte of data from this input stream.
     *
     * @see ByteArrayInputStream#read()
     * @return the next byte of data, or {@code -1} if the end of the stream has been reached.
     */
    public final int readByte() {
        return read();
    }

    /**
     * Reads up to {@code length} bytes of data into an array of bytes from this input stream.
     *
     * @see ByteArrayInputStream#read(byte[], int, int)
     * @param length the maximum number of bytes to read
     * @return an array containing the read bytes
     */
    public final byte[] readBytes(final int length) {
        final byte[] bytes = new byte[length];
        read(bytes, 0, length);
        return bytes;
    }

    /**
     * Reads the next integer (4 bytes) from this input stream.
     *
     * @return the next integer, or {@code -1} if the end of the stream has been reached.
     */
    public final int readInt() {
        return readShort() << 16 | readShort();
    }

    /**
     * Reads the next name ({@link StandardCharsets#UTF_8 UTF8} String) from this input stream.
     *
     * @return the name
     */
    public final String readName() {
        final Map<Integer, StringBuilder> names = new HashMap<>();
        final StringBuilder sb = new StringBuilder();
        boolean finished = false;
        while (!finished) {
            final int b = readByte();
            if (b == 0) {
                break;
            }
            if ((b & 0xC0) == 0x00) {
                final int offset = pos - 1;
                final String label = readString(b) + ".";
                sb.append(label);
                for (final StringBuilder previousLabel : names.values()) {
                    previousLabel.append(label);
                }
                names.put(offset, new StringBuilder(label));

            } else {
                final int index = (b & 0x3F) << 8 | readByte();
                final String compressedLabel = pointers.get(Integer.valueOf(index));
                sb.append(compressedLabel);
                for (final StringBuilder previousLabel : names.values()) {
                    previousLabel.append(compressedLabel);
                }
                finished = true;
            }
        }

        for (final Map.Entry<Integer, StringBuilder> entry : names.entrySet()) {
            final Integer index = entry.getKey();
            pointers.put(index, entry.getValue().toString());
        }
        return sb.toString();
    }

    /**
     * Reads the next short (2 bytes) from this input stream.
     *
     * @return the next short, or {@code -1} if the end of the stream has been reached.
     */
    public final int readShort() {
        return readByte() << 8 | readByte();
    }

    /**
     * Reads up to {@code length} bytes of data into an {@link StandardCharsets#UTF_8 UTF8} String from this input
     * stream
     *
     * @param length the maximum number of bytes to read
     * @return a String
     */
    private String readString(final int length) {
        final byte[] bytes = readBytes(length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public ByteBuffer getByteBuffer() {
        // Create a byte array
        byte[] bytes = new byte[10];

        // Wrap a byte array into a buffer
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        // Retrieve bytes between the position and limit
        // (see Putting Bytes into a ByteBuffer)
        bytes = new byte[buf.remaining()];

        // transfer bytes from this buffer into the given destination array
        buf.get(bytes, 0, bytes.length);

        // Retrieve all bytes in the buffer
        buf.clear();
        bytes = new byte[buf.capacity()];

        // transfer bytes from this buffer into the given destination array
        buf.get(bytes, 0, bytes.length);
        return buf;
    }
}
