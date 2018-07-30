package multicast.dns;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.util.*;

import static multicast.helpers.MulticastDns.*;

public class Response extends Message {
    private final List<Question> questions;
    private final List<Record> records;
    private int numQuestions;
    private int numAnswers;
    private int numNameServers;
    private int numAdditionalRecords;

    private final static Logger logger = LoggerFactory.getLogger(Response.class);

    public static Response createFrom(DatagramPacket packet) {
        Response response = new Response(packet);
        response.parseRecords();
        return response;
    }

    private Response() {
        questions = new ArrayList<>();
        records = new ArrayList<>();
    }

    private Response(DatagramPacket packet) {
        this();
        byte[] dstBuffer = buffer.array();
        try {
            System.arraycopy(packet.getData(), packet.getOffset(), dstBuffer, 0, packet.getLength());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        try {
            buffer.limit(packet.getLength());
            buffer.position(0);
        } catch (Exception e) {
            logger.error("Strange error: ", e);
        }
    }

    private void parseRecords() {
        parseHeader();
        for (int i = 0; i < numQuestions; i++) {
            Question question = Question.fromBuffer(buffer);
            questions.add(question);
        }
        for (int i = 0; i < numAnswers; i++) {
            Record record = Record.fromBuffer(buffer);
            records.add(record);
        }
        for (int i = 0; i < numNameServers; i++) {
            Record record = Record.fromBuffer(buffer);
            records.add(record);
        }
        for (int i = 0; i < numAdditionalRecords; i++) {
            Record record = Record.fromBuffer(buffer);
            records.add(record);
        }
    }

    private void parseHeader() {
        readUnsignedShort(); // Skip over the ID
        int codes = readUnsignedShort();
        if ((codes & FLAG_QR_MASK) != FLAG_QR_MASK) {
            // FIXME create a custom Exception for DNS errors
            // throw new IllegalArgumentException("Packet is not a DNS response");
            // logger.debug("Packet is not a DNS response");
            return;
        }
        if ((codes & FLAG_OPCODE_MASK) != 0) {
            // throw new IllegalArgumentException("mDNS response packets can't have OPCODE values");
            logger.debug("mDNS response packets can't have OPCODE values");
            return;
        }
        if ((codes & FLAG_RCODE_MASK) != 0) {
            // throw new IllegalArgumentException("mDNS response packets can't have RCODE values");
            logger.debug("mDNS response packets can't have RCODE values");
            return;
        }
        numQuestions = readUnsignedShort();
        numAnswers = readUnsignedShort();
        numNameServers = readUnsignedShort();
        numAdditionalRecords = readUnsignedShort();
        // logger.debug("Questions={}, Answers={}, NameServers={}, AdditionalRecords={}", numQuestions, numAnswers, numNameServers, numAdditionalRecords);
    }

    public Set<Record> getRecords() {
        return new HashSet<>(Collections.unmodifiableSet(new HashSet<>(records)));
    }

    public String getUserVisibleName() {
        Optional<PtrRecord> record = records.stream().filter(r -> r instanceof PtrRecord).map(r -> (PtrRecord) r).findAny();
        if (record.isPresent()) {
            return record.get().getUserVisibleName();
        } else {
            logger.debug("No PTR records: {}", records);
            throw new IllegalStateException("Cannot call getUserVisibleName when no PTR record is available");
        }
    }

    public boolean answers(Set<Question> questions) {
        return (records.stream().filter(r -> {
            boolean match = false;
            String name = r.getName();
            for (Question q : questions) {
                if (name.equals(q.getQName())) {
                    match = true;
                    break;
                }
            }
            return match;
        }).count() > 0);
    }


    @Override
    public String toString() {
        return "Response{" +
                "questions=" + questions +
                ", records=" + records +
                ", numQuestions=" + numQuestions +
                ", numAnswers=" + numAnswers +
                ", numNameServers=" + numNameServers +
                ", numAdditionalRecords=" + numAdditionalRecords +
                '}';
    }

    // Package-private methods for unit tests

    int getNumQuestions() {
        return numQuestions;
    }

    int getNumAnswers() {
        return numAnswers;
    }

    int getNumNameServers() {
        return numNameServers;
    }

    int getNumAdditionalRecords() {
        return numAdditionalRecords;
    }
}
