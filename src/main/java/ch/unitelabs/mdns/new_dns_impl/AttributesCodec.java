package ch.unitelabs.mdns.new_dns_impl;

import ch.unitelabs.mdns.new_dns_impl.io.MessageInputStream;
import ch.unitelabs.mdns.new_dns_impl.io.MessageOutputStream;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Codec to read/write {@link Attributes} from/to byte stream
 */
final class AttributesCodec {

    /**
     * Constructor
     */
    private AttributesCodec() {
        // empty.
    }

    /**
     * Obtains an instance of {@code Attributes} by reading the given number of bytes from the given stream.
     *
     * @param is stream of bytes
     * @param length number of bytes to read
     * @return attributes, not null
     */
    static Attributes decode(final MessageInputStream is, final int length) {
        final Map<String, Optional<ByteBuffer>> map = new HashMap<>();
        int readBytes = 0;
        while (readBytes < length) {
            final int pairLength = is.readByte();
            final byte[] bytes = is.readBytes(pairLength);
            final int sep = separator(bytes);
            final String key;
            final Optional<ByteBuffer> value;
            if (sep == -1) {
                key = new String(bytes, StandardCharsets.UTF_8);
                value = Optional.empty();
            } else {
                key = new String(subarray(bytes, 0, sep), StandardCharsets.UTF_8);
                value = Optional.of(value(bytes, sep));
            }
            if (!key.isEmpty() && !map.containsKey(key)) {
                map.put(key, value);
            }
            readBytes = readBytes + pairLength + 1;
        }
        return new AttributesImpl(map);
    }

    /**
     * Writes the given {@code Attributes} to the given stram.
     *
     * @param attributes attributes
     * @param os stream of bytes
     */
    static void encode(final Attributes attributes, final MessageOutputStream os) {
        final Set<String> keys = attributes.keys();
        for (final String key : keys) {
            try (final MessageOutputStream attos = new MessageOutputStream()) {
                attos.writeString(key);
                final Optional<ByteBuffer> value = attributes.value(key);
                if (value.isPresent()) {
                    attos.writeString("=");
                    final byte[] bytes = new byte[value.get().remaining()];
                    value.get().get(bytes);
                    attos.writeBytes(bytes);
                }
                os.writeByte(attos.size());
                os.writeBytes(attos.toByteArray());
            }
        }
    }

    /**
     * Returns the index of the key/value separator ('=').
     *
     * @param bytes array of bytes
     * @return index of the key/value separator ('=') or {@code -1} if no separator was found
     */
    private static int separator(final byte[] bytes) {
        for (int index = 0; index < bytes.length; index++) {
            if (bytes[index] == '=') {
                return index;
            }
        }
        return -1;
    }

    /**
     * Sub array of given array from begin inclusive to end exclusive.
     *
     * @param bytes array of bytes
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex the ending index, exclusive.
     * @return sub array
     */
    private static byte[] subarray(final byte[] bytes, final int beginIndex, final int endIndex) {
        final int length = endIndex - beginIndex;
        final byte[] arr = new byte[length];
        System.arraycopy(bytes, beginIndex, arr, 0, length);
        return arr;
    }

    /**
     * Reads the value: everything after the given index.
     *
     * @param bytes array of bytes
     * @param sep index of the key/value separator
     * @return value
     */
    private static ByteBuffer value(final byte[] bytes, final int sep) {
        final int length = bytes.length - sep;
        if (length == 1) {
            return ByteBuffer.allocate(0);
        }
        return ByteBuffer.wrap(subarray(bytes, sep + 1, bytes.length));
    }

}
