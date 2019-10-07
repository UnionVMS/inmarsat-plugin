package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollTypeType;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.data.ConfigPoll;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.data.InmarsatPoll;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.data.ManualPoll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.PrintStream;

@Stateless
public class PollSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollSender.class);

    @Inject
    private HelperFunctions functions;

    /**
     * Sends one or more poll commands, one for each ocean region, until a reference number is
     * received.
     *
     * @return result of first successful poll command, or null if poll failed on every ocean region
     */

    public String sendPollCommand(PollType poll, InputStream in, PrintStream out, String oceanRegion) throws Throwable {
        String command = buildPollCommand(poll, oceanRegion);
        String retVal;
        functions.write(command, out);
        retVal = functions.readUntil("Text:", in);
        functions.write(".S", out);
        retVal += functions.readUntil(">", in);
        return retVal;
    }

    private String buildPollCommand(PollType pollType, String oceanRegion) {
        InmarsatPoll poll = getPoll(pollType.getPollTypeType(), oceanRegion);
        poll.setFieldsFromPoll(pollType);
        return poll.asCommand();
    }

    private InmarsatPoll getPoll(PollTypeType pollTypeType, String oceanRegion) {
        InmarsatPoll poll = null;
        switch (pollTypeType) {
            case POLL:
                poll = new ManualPoll(oceanRegion);
                break;
            case CONFIG:
                poll = new ConfigPoll(oceanRegion);
                break;
            case SAMPLING:
                break;
        }
        return poll;
    }

    private void stopIndividualPoll(BufferedInputStream input, PrintStream out, String oceanRegion, String DNID, String satelliteNumber) {
        String cmd = String.format("poll %s,I,%s,N,1,%s,6", oceanRegion, DNID, satelliteNumber);
        sendPollCommand(input, out, cmd);
    }

    private void startIndividualPoll(BufferedInputStream input, PrintStream out, String oceanRegion, String DNID, String satelliteNumber) {
        String cmd = String.format("poll %s,I,%s,D,1,%s,5", oceanRegion, DNID, satelliteNumber);
        sendPollCommand(input, out, cmd);
    }

    private void configIndividualPoll(BufferedInputStream input, PrintStream out, int startHour, int startMinute,
                                      String oceanRegion, String DNID, String satelliteNumber, String frequency) {
        String startFrame = calcStartFrame(startHour, startMinute);
        String cmd = String.format("poll %s,I,%s,N,1,%S,4,,%s,%s", oceanRegion, DNID, satelliteNumber, startFrame, frequency);
        sendPollCommand(input, out, cmd);
    }

    private void sendPollCommand(BufferedInputStream input, PrintStream out, String cmd) {
        try {
            functions.write(cmd, out);
            functions.readUntil("Text:", input);
            functions.write(".s", out);
            String status = functions.readUntil(">", input);
            status = toReferenceNumber(status);
            LOGGER.info("Status Number: {}", status);
        } catch (Exception e) {
            LOGGER.error("Error when Polling", e);
        }
    }

    /**
     * (((hour * 60) + minute) * 60) / 8.64 = startFrame number.
     * Night and Day (24 hours) are divided in 10000 frames equal to 8,64 second.
     * Example: 24 * 60 * 60 / 10000 = 8,64
     * The "24" at the end indicate that the terminals shall send one report every hour.
     * Example: 13:28 o'clock will be equal to 5611,24
     *
     * @param hour   Hours of the day (24 hours format)
     * @param minute Minutes of the hour
     * @return startFrame
     */
    private String calcStartFrame(int hour, int minute) {
        if ((hour < 0) || (hour > 24)) {
            throw new IllegalArgumentException("Hour must be between 0 and 24. Was " + hour);
        }
        if ((minute < 0) || (minute > 60)) {
            throw new IllegalArgumentException("Minute must be between 0 and 60. Was " + minute);
        }

        int value = (int) ((((hour * 60) + minute) * 60) / 8.64);
        return String.valueOf(value);
    }

    private String toReferenceNumber(String response) {

        int pos = response.indexOf("number");
        String s = "";
        if (pos < 0) return response;
        s = response.substring(pos);
        return s.replaceAll("[^0-9]", ""); // returns 123
    }
}
