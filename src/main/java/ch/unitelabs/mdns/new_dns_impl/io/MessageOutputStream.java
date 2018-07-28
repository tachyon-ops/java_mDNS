package ch.unitelabs.mdns.new_dns_impl.io;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class MessageOutputStream extends ByteArrayOutputStream {

    /** pointers for decompression: string to index in this stream. */
    private final Map<String, Integer> pointers;

    /**
     * Creates a new byte array output stream. The buffer capacity is initially 32 bytes, though its size increases
     * if necessary.
     *
     * @see ByteArrayOutputStream#ByteArrayOutputStream()
     */
    public MessageOutputStream() {
        pointers = new HashMap<>();
    }

    /**
     * Closing a {@code MessageOutputStream} has no effect. The methods in this class can be called after the
     * stream has been closed without generating an {@code IOException}.
     *
     * @see ByteArrayOutputStream#close()
     */
    @Override
    public final void close() {
        // empty.
    }

    /**
     * @return the number of valid bytes in the buffer.
     */
    public final int position() {
        return count;
    }

    /**
     * Skips over the given number of bytes.
     *
     * @param length number of bytes to skip over
     */
    public final void skip(final int length) {
        count += length;
    }

    /**
     * Writes the given byte to this output stream.
     *
     * @see ByteArrayOutputStream#write(int)
     * @param b byte
     */
    public final void writeByte(final int b) {
        write(b & 0xFF);
    }

    /**
     * Writes the given byte array to this output stream.
     *
     * @see ByteArrayOutputStream#write(byte[], int, int)
     * @param bytes bytes
     */
    public final void writeBytes(final byte[] bytes) {
        write(bytes, 0, bytes.length);
    }

    /**
     * Writes the given integer to this output stream.
     *
     * @param i integer
     */
    public final void writeInt(final int i) {
        writeShort(i >> 16);
        writeShort(i);
    }

    /**
     * Writes the given name, using compression, to this output stream.
     *
     * @param name name
     */
    public final void writeName(final String name) {
        String sub = name;
        while (true) {
            int n = sub.indexOf('.');
            if (n < 0) {
                n = sub.length();
            }
            if (n <= 0) {
                writeByte(0);
                return;
            }
            final String label = sub.substring(0, n);
            final Integer offset = pointers.get(sub);
            if (offset != null) {
                final int val = offset.intValue();
                writeByte(val >> 8 | 0xC0);
                writeByte(val & 0xFF);
                return;
            }
            pointers.put(sub, Integer.valueOf(size()));
            writeCharacterString(label);
            sub = sub.substring(n);
            if (sub.startsWith(".")) {
                sub = sub.substring(1);
            }
        }
    }

    /**
     * Writes the given short to this output stream.
     *
     * @param s short
     */
    public final void writeShort(final int s) {
        writeByte(s >> 8);
        writeByte(s);
    }

    /**
     * Writes the given short at the given index to this output stream without advancing the position.
     *
     * @param index index
     * @param s short
     */
    public final void writeShort(final int index, final short s) {
        buf[index] = (byte) (s >> 8 & 0xFF);
        buf[index + 1] = (byte) (s & 0xFF);
    }

    /**
     * Writes the given {@link StandardCharsets#UTF_8 UTF8} String to this output stream.
     *
     * @param str UTF8 string
     */
    public final void writeString(final String str) {
        final byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeBytes(bytes);
    }

    /**
     * Writes the size of the given {@link StandardCharsets#UTF_8 UTF8} String, followed by the given String to
     * this output stream.
     *
     * @param str UTF8 string
     */
    private void writeCharacterString(final String str) {
        final byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeByte(bytes.length);
        writeBytes(bytes);
    }

}