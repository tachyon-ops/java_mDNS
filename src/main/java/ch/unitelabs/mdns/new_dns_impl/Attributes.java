package ch.unitelabs.mdns.new_dns_impl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Arbitrary key/value pairs conveying additional information about a named service, such as {@code paper=a4},
 * {@code plugins=}, {@code annon allowed}, etc.
 * <p>
 * Note: values are opaque binary data. Often the value for a particular attribute will be US-ASCII or UTF-8 text,
 * but it is legal for a value to be any binary data.
 *
 * <pre>
 * <code>
 * Attributes.create()
 *     .with("enabled")
 *     .with("version", "3.1", StandardCharsets.UTF8)
 *     .with("plugins", ByteBuffer.wrap(bytes))
 *     .get();
 * </code>
 * </pre>
 */
public interface Attributes {

    /**
     * {@link Attributes} builder.
     */
    public static final class Builder implements Supplier<Attributes> {

        /** key/value pairs. */
        private final Map<String, Optional<ByteBuffer>> map;

        /**
         * Constructor.
         */
        Builder() {
            map = new HashMap<>();
        }

        @Override
        public final Attributes get() {
            return new AttributesImpl(map);
        }

        /**
         * Adds the given key with no value attribute.
         * <p>
         * This method has no effect if the key already exists or is empty, as per RFC6763.
         *
         * @param key key
         * @return this
         */
        public final Builder with(final String key) {
            if (isValidKey(key)) {
                map.put(key, Optional.empty());
            }
            return this;
        }

        /**
         * Adds the given key/pair attribute.
         * <p>
         * This method has no effect if the key already exists or is empty, as per RFC6763.
         *
         * @param key key
         * @param value value
         * @return this
         */
        public final Builder with(final String key, final ByteBuffer value) {
            if (isValidKey(key)) {
                map.put(key, Optional.of(value));
            }
            return this;
        }

        /**
         * Adds the given key/pair attribute.
         * <p>
         * This method has no effect if the key already exists or is empty, as per RFC6763.
         *
         * @param key key
         * @param value value
         * @param charset character set
         * @return this
         */
        public final Builder with(final String key, final String value, final Charset charset) {
            final ByteBuffer bb = ByteBuffer.wrap(value.getBytes(charset));
            bb.order(ByteOrder.BIG_ENDIAN);
            return with(key, bb);
        }

        /**
         * Whether the given key does not exist and is not empty.
         *
         * @param key key
         * @return {@code true} iff key is valid
         */
        private boolean isValidKey(final String key) {
            return !key.isEmpty() && !map.containsKey(key);
        }

    }

    /**
     * @return a new {@link Builder}.
     */
    static Builder create() {
        return new Builder();
    }

    /**
     * @return all keys.
     */
    Set<String> keys();

    /**
     * Returns the value corresponding to the given key, if present.
     * <p>
     * Note: an empty Optional does not indicate that the key is absent: the key can be present but the attribute
     * has no value.
     * <p>
     * The returned {@link ByteBuffer} is a deep copy and is ready for read operation.
     *
     * @param key key
     * @return value corresponding to the given key or empty if no value or key is absent
     */
    Optional<ByteBuffer> value(final String key);

    /**
     * Returns the value corresponding to the given key using the given character set to decode the binary data, if
     * present.
     * <p>
     * Note: use this method when it is known that the value of the key is indeed a string.
     *
     * @param key key
     * @param charset character set
     * @return value corresponding to the given key or empty if no value or key is absent
     */
    Optional<String> value(final String key, final Charset charset);

}
