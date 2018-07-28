package ch.unitelabs.mdns.new_dns_impl;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link Attributes} implementation.
 */
final class AttributesImpl implements Attributes {

    /** key/value pairs. */
    private final Map<String, Optional<ByteBuffer>> map;

    /**
     * Constructor.
     *
     * @param aMap key/value pairs
     */
    AttributesImpl(final Map<String, Optional<ByteBuffer>> aMap) {
        Objects.requireNonNull(aMap);
        map = aMap;
    }

    @Override
    public final Set<String> keys() {
        return Collections.unmodifiableSet(map.keySet());
    }

    @Override
    public final String toString() {
        return "["
            + keys().stream().map(k -> k + "=" + value(k, StandardCharsets.UTF_8)).collect(Collectors.joining(";"))
            + "]";
    }

    @Override
    public final Optional<ByteBuffer> value(final String key) {
        return map.getOrDefault(key, Optional.empty()).map(ByteBuffer::asReadOnlyBuffer);
    }

    @Override
    public final Optional<String> value(final String key, final Charset charset) {
        final Optional<ByteBuffer> buffer = value(key);
        if (!buffer.isPresent()) {
            return Optional.empty();
        }
        final ByteBuffer bb = buffer.get();
        final byte[] bytes = new byte[bb.remaining()];
        bb.get(bytes);
        return Optional.of(new String(bytes, charset));
    }

}
