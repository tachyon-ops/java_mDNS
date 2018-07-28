package ch.unitelabs.mdns.dns;

import java.nio.ByteBuffer;

public class PtrRecord extends Record {
    private final String userVisibleName;
    private final String ptrName;

    public final static String UNTITLED_NAME = "Untitled";

    public PtrRecord(ByteBuffer buffer, String name, Class recordClass, long ttl, int rdLength) {
        super(name, recordClass, ttl);
        if (rdLength > 0) {
            ptrName = readNameFromBuffer(buffer);
        } else {
            ptrName = "";
        }
        userVisibleName = buildUserVisibleName();
    }

    public String getPtrName() {
        return ptrName;
    }

    public String getUserVisibleName() {
        return userVisibleName;
    }

    private String buildUserVisibleName() {
        String[] parts = ptrName.split("\\.");
        if (parts[0].length() > 0) {
            return parts[0];
        } else {
            return UNTITLED_NAME;
        }
    }

    @Override
    public String toString() {
        return "PtrRecord{" +
                "name='" + name + '\'' +
                ", recordClass=" + recordClass +
                ", ttl=" + ttl +
                ", ptrName='" + ptrName + '\'' +
                '}';
    }
}