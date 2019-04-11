package fish.focus.uvms.commons.les.inmarsat;


import fish.focus.uvms.commons.les.inmarsat.header.HeaderDataPresentation;
import fish.focus.uvms.commons.les.inmarsat.header.HeaderStruct;
import fish.focus.uvms.commons.les.inmarsat.header.HeaderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.*;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

@Stateless
public class InmarsatInterpreter {


    private static final String INMARSAT_FAILED_REPORT_QUEUE = "jms/queue/UVMSPluginFailedReport";

    @Resource(mappedName = "java:/" + INMARSAT_FAILED_REPORT_QUEUE)
    private Queue inmarsatFailedReportQueue;

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;


    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatInterpreter.class);
    private static final byte[] HEADER_PATTERN = ByteBuffer.allocate(4).put((byte) InmarsatDefinition.API_SOH)
            .put(InmarsatDefinition.API_LEAD_TEXT.getBytes()).array();
    private static final int PATTERN_LENGTH = HEADER_PATTERN.length;


    @PostConstruct
    public void resourceLookup() {
    }

    public InmarsatMessage[] byteToInmMessage(final byte[] originalMessageFromInmarsat) {
        byte[] bytes = insertMissingData(originalMessageFromInmarsat);

        ArrayList<InmarsatMessage> messages = new ArrayList<>();
        if (bytes == null || bytes.length <= PATTERN_LENGTH) {
            LOGGER.error("Not a valid Inmarsat Message: {}", Arrays.toString(bytes));
            return new InmarsatMessage[]{};
        }
        // Parse bytes for messages
        for (int i = 0; i < (bytes.length - PATTERN_LENGTH); i++) {
            // Find message
            if (InmarsatHeader.isStartOfMessage(bytes, i)) {
                InmarsatMessage message;
                byte[] currentMessageBytes = Arrays.copyOfRange(bytes, i, bytes.length);
                try {
                    message = new InmarsatMessage(currentMessageBytes);
                } catch (InmarsatException e) {
                    LOGGER.error(e.toString(), e);
                    LOGGER.error("{}", Arrays.toString(originalMessageFromInmarsat));
                    LOGGER.error("{}", Arrays.toString(bytes));

                    try {
                        sendFailedReportMessage(currentMessageBytes, originalMessageFromInmarsat, bytes, e);
                    } catch (JMSException ee) {
                        LOGGER.error("could not post rejected message to queue : ");
                        LOGGER.error(Arrays.toString(currentMessageBytes));
                    }
                    continue;
                }

                if (message.validate()) {
                    messages.add(message);
                } else {
                    LOGGER.error("Could not validate position(s)");
                    try {
                        sendFailedReportMessage(currentMessageBytes, originalMessageFromInmarsat, bytes, null);
                    } catch (JMSException e) {
                        LOGGER.error("could not post rejected message to queue : ");
                        LOGGER.error(Arrays.toString(currentMessageBytes));
                    }
                }
            }
        }
        return messages.toArray(new InmarsatMessage[0]); // "new InmarsatMessage[0]" is used instead of "new
        // Inmarsat[messages.size()]" to get better performance
    }

    String nullGuard(byte[] bytes) {
        if (bytes == null) return "";
        if (bytes.length == 0) return "";
        return Arrays.toString(bytes);
    }

    public String sendFailedReportMessage(byte[] currentMessageBytes, byte[] originalMessageFromInmarsat, byte[] fixedBytes, Exception exception) throws JMSException {

        // we slice it up because buffersize is limited to 50000isch in Artemis
        if (currentMessageBytes.length > 50000) {
            byte[] truncated = Arrays.copyOfRange(currentMessageBytes, 0, 50000);
            sendFailedReportMessage(truncated, originalMessageFromInmarsat, fixedBytes, exception);
            currentMessageBytes = Arrays.copyOfRange(currentMessageBytes, 50000, currentMessageBytes.length);
        }

        String exceptionStr = "";
        if (exception != null) {
            exceptionStr = exception.toString();
        }


        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, 1);
             MessageProducer producer = session.createProducer(inmarsatFailedReportQueue);
        ) {

            BytesMessage message = session.createBytesMessage();
            message.setStringProperty("messagesource", "INMARSAT_C");
            message.setStringProperty("incoming_message_as_string", nullGuard(currentMessageBytes));
            message.setStringProperty("filebytes_before_corr", nullGuard(originalMessageFromInmarsat));
            message.setStringProperty("filebytes_after__corr", nullGuard(fixedBytes));
            if (exception != null) {
                message.setStringProperty("exception", exceptionStr);
            }
            message.writeBytes(currentMessageBytes);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(message);


            return message.getJMSMessageID();
        } catch (JMSException e) {
            throw e;
        }
    }

    private String byte2str(byte[] message) {
        if (message == null) return "";
        if (message.length < 1) return "";

        StringBuilder sb = new StringBuilder(message.length * 2);
        for (byte b : message) {
            sb.append(String.format("%02x", b & 0xff));
        }
        String s = sb.toString();
        return s;
    }

    /**
     * Header sent doesn't always adhere to the byte contract.. This method tries to insert fix the missing parts..
     *
     * @param input bytes that might contain miss some bytes
     * @return message with fixed bytes
     */
    public byte[] insertMissingData(byte[] input) {

        byte[] output = input;
        output = insertMissingEOH(output);
        output = insertMissingMsgRefNo(output);
        output = insertMissingStoredTime(output);
        output = insertMissingMemberNo(output);

        if (input.length < output.length){
            LOGGER.warn("Message fixed: {} -> {}", InmarsatUtils.bytesArrayToHexString(input),
                    InmarsatUtils.bytesArrayToHexString(output));
        }
        return output;

    }

    private byte[] insertMissingEOH(final byte[] contents) {
        byte[] input = contents.clone();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean insert = false;
        int insertPosition = 0;
        for (int i = 0; i < input.length; i++) {
            // Find SOH
            if (InmarsatHeader.isStartOfMessage(input, i)) {
                byte[] header = Arrays.copyOfRange(input, i, input.length);
                HeaderType headerType = InmarsatHeader.getType(header);

                int headerLength = headerType.getHeaderLength();
                int token = header[headerLength];
                if (token != InmarsatDefinition.API_EOH) {
                    LOGGER.warn("API_EOH missing at given position so we add it");
                    insert = true;
                    insertPosition = i + InmarsatDefinition.API_EOH;
                }
            }
            if (insert && (insertPosition == i)) {
                insert = false;
                insertPosition = 0;
                output.write((byte) InmarsatDefinition.API_EOH); // END_OF_HEADER
            }
            output.write(input[i]);
        }
        return output.toByteArray();
    }

    private byte[] insertMissingMsgRefNo(final byte[] contents) {
        byte[] input = contents.clone();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        // Missing last MsgRefNo- insert #00 in before presentation..
        boolean insert = false;
        int insertPosition = 0;
        for (int i = 0; i < input.length; i++) {
            // Find SOH
            if (InmarsatHeader.isStartOfMessage(input, i)) {
                byte[] header = Arrays.copyOfRange(input, i, input.length);
                HeaderType headerType = InmarsatHeader.getType(header);

                if (headerType.getHeaderStruct().isPresentation()) {
                    HeaderDataPresentation presentation = InmarsatHeader.getDataPresentation(header);

                    if (presentation == null) {
                        LOGGER.warn("Presentation is not correct so we add 00 to msg ref no");
                        insert = true;
                        insertPosition = i + HeaderStruct.POS_REF_NO_END;
                    }
                }
            }
            if (insert && (insertPosition == i)) {
                insert = false;
                insertPosition = 0;
                output.write((byte) 0x00);
            }
            output.write(input[i]);
        }
        return output.toByteArray();

    }

    private byte[] insertMissingStoredTime(byte[] contents) {
        // Missing Date byte (incorrect date..)? - insert #00 in date first position
        byte[] input = contents.clone();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean insert = false;
        int insertPosition = 0;

        for (int i = 0; i < input.length; i++) {
            // Find SOH
            if (InmarsatHeader.isStartOfMessage(input, i)) {
                byte[] header = Arrays.copyOfRange(input, i, input.length);
                HeaderType headerType = InmarsatHeader.getType(header);

                Date headerDate = InmarsatHeader.getStoredTime(header);

                if (headerDate.after(Calendar.getInstance(InmarsatDefinition.API_TIMEZONE).getTime())) {
                    LOGGER.warn("Stored time is not correct so we add 00 to in first position");
                    insert = true;
                    insertPosition = i + headerType.getHeaderStruct().getPositionStoredTime();
                }

            }
            if (insert && (insertPosition == i)) {
                insert = false;
                insertPosition = 0;
                output.write((byte) 0x00);
            }
            output.write(input[i]);
        }
        return output.toByteArray();
    }

    private byte[] insertMissingMemberNo(byte[] contents) {

        // Missing Member number - insert #FF before EOH
        // continue from previous cleaned data
        byte[] input = contents.clone();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean insert = false;
        int insertPosition = 0;

        for (int i = 0; i < input.length; i++) {
            // Find SOH
            if (InmarsatHeader.isStartOfMessage(input, i)) {
                int headerLength = input[i + HeaderStruct.POS_HEADER_LENGTH];
                int expectedEOHPosition = i + headerLength - 1;
                // Check if memberNo exits
                if ((expectedEOHPosition >= input.length)
                        || ((input[expectedEOHPosition - 1] == (byte) InmarsatDefinition.API_EOH)
                        && input[expectedEOHPosition] != (byte) InmarsatDefinition.API_EOH)) {
                    insert = true;
                    insertPosition = expectedEOHPosition - 1;
                }

            }
            // Find EOH
            if (insert && (input[i] == (byte) InmarsatDefinition.API_EOH) && (insertPosition == i)) {
                LOGGER.warn("Message is missing member no");
                output.write((byte) 0xFF);
                insert = false;
                insertPosition = 0;

            }
            output.write(input[i]);
        }
        return output.toByteArray();
    }
}
