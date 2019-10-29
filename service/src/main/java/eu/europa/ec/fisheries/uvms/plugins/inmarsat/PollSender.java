package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.data.ConfigPoll;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.data.InmarsatPoll;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.data.InmarsatSocketException;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.data.ManualPoll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    public String sendPollCommand(PollType pollType, BufferedInputStream in, PrintStream out, String oceanRegion) {
        InmarsatPoll poll = getPoll(pollType, oceanRegion);
        List<String> pollCommandList = poll.asCommand();
        String result = null;
        try  {
            for (String pollCommand : pollCommandList) {
                LOGGER.info(pollCommand);
                result = sendPollCommand(in, out, pollCommand);
                if (result == null) {
                    LOGGER.info("NO  referencenumber. Message not send");
                }
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        } catch (IOException | InmarsatSocketException e) {
            LOGGER.error(e.toString(), e);
        }
        return result;
    }

    private InmarsatPoll getPoll(PollType pollType, String oceanRegion) {
        InmarsatPoll poll = null;
        switch (pollType.getPollTypeType()) {
            case POLL:
                poll = new ManualPoll(oceanRegion);
                poll.setFieldsFromPollRequest(pollType);
                break;
            case CONFIG:
                poll = new ConfigPoll(oceanRegion);
                poll.setFieldsFromPollRequest(pollType);
                break;
            case SAMPLING:
                break;
        }
        return poll;
    }

    private String sendPollCommand(BufferedInputStream bis, PrintStream out, String cmd) throws InmarsatSocketException, IOException {
        functions.write(cmd, out);
        functions.readUntil("Text:", bis);
        functions.write(".s", out);
        String status = functions.readUntil(">", bis);
        status = toReferenceNumber(status);
        LOGGER.info("Status Number: {}", status);
        return status;

    }

    private String toReferenceNumber(String response) {
        int pos = response.indexOf("number");
        String reference;
        if (pos < 0) return null;
        reference = response.substring(pos);
        return reference.replaceAll("[^0-9]", ""); // returns 123
    }
}
