package ch.unitelabs.mdns.dns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TxtRecord extends Record {
    private Map<String, String> attributes;

    @SuppressWarnings("unused")
    private final static Logger logger = LoggerFactory.getLogger(TxtRecord.class);

    public TxtRecord(ByteBuffer buffer, String name, Record.Class recordClass, long ttl, int length) {
        super(name, recordClass, ttl);
        List<String> strings = readStringsFromBuffer(buffer, length);
        attributes = parseDataStrings(strings);
    }

    private Map<String, String> parseDataStrings(List<String> strings) {
        Map<String, String> pairs = new HashMap<>();
        strings.stream().forEach(s -> {
            String[] parts = s.split("=");
            if (parts.length > 1) {
                pairs.put(parts[0], parts[1]);
            } else {
                pairs.put(parts[0], "");
            }
        });
        return pairs;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public String toString() {
        return "TxtRecord{" +
                "name='" + name + '\'' +
                ", recordClass=" + recordClass +
                ", ttl=" + ttl +
                ", attributes=" + attributes +
                '}';
    }
}