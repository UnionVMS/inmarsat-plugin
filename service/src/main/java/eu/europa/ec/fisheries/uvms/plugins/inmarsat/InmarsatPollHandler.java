package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.KeyValueType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollTypeType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.data.AckEnum;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.data.InmarsatPoll;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.data.StatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Inmarsat har ikke definert Polling&Data rapport tjenesten hundre prosent, med det mener jeg at terminal leverandører
 * har hatt mulighet til å lage sine «tolkninger» på endel punkter. Hvis du ser på data rapport pakken (PosRep-C_2l.doc)
 * så ser du at nederst i pakke en er MEM-code. Den mest brukte MEM code er 11 I attribute bytene nederst i pakke en
 * vil mem 11 angi at atribute bytene inneholder  «time of position».
 *
 * Angående scheduled programming. Legger med noe veldig gammel informasjon jeg laget på gruppe programmering:
 *
 * First you have to program the terminals to send data report, and afterwards you have to start the reporting.
 * You have to send 2 poll commands to get the mobiles to report scheduled
 *
 * 1)  First you have to send the program command:
 * If you want the report to start at e.g. 13:28 UTC today you have to send the following command:
 *
 * poll 0,G,4661,N,1,0,4,,5611,24
 * The P8 parameter is the start frame. The start time must be calculated according to the formula:
 * (((hour*60)+minute)*60)/8.64 = start frame number. (night and day (24 hours) are divided in  10000 frame's a'8.64 sec.).
 * The “24” at the end indicate that the terminals shall send one report every hour.
 *
 * 2)  Afterwards you have to start the reporting command:
 * e.g:  poll 0,G,4661,D,1,0,5
 *
 * 3)  TO STOP THE REPORTING.
 * The data reporting are stopped using the command
 * e.g.: poll 0,G,4661,N,1,0,6
 */
@Singleton
public class InmarsatPollHandler {

    private static final String PORT = "PORT";
    private static final String DNIDS = "DNIDS";
    private static final String URL = "URL";
    private static final String PSW = "PSW";
    private static final String USERNAME = "USERNAME";

    private final ConcurrentMap<String, Object> connectSettings = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatPollHandler.class);

    @Inject
    private PluginPendingResponseList responseList;

    @Inject
    private HelperFunctions functions;

    public ConcurrentMap<String, Object> getSettings() {
        return connectSettings;
    }

    public Object getSetting(String setting) {
        return connectSettings.get(setting);
    }


    public PluginPendingResponseList getPluginPendingResponseList() {
        return responseList;
    }

    public AcknowledgeTypeType processCommandTypeAndReturnAck(CommandType command) {
        PollType pollType = command.getPoll();
        if (PollTypeType.POLL == pollType.getPollTypeType()) {
            try {
                String reference = sendPoll(pollType);
                if (reference == null) {
                    LOGGER.error("Error no reference for poll with pollId: " + pollType.getPollId());
                    return AcknowledgeTypeType.NOK; //consumed but erroneous
                }
                LOGGER.info("sent poll with pollId: {} and reference: {} ", pollType.getPollId(), reference);
                constructIPRAndAddInPPRL(command, pollType, reference);
                return AcknowledgeTypeType.OK;
            } catch (Throwable e) {
                LOGGER.error("Error while sending poll: {}", e.getMessage(), e);
            }
        }
        return AcknowledgeTypeType.NOK;
    }

    private void constructIPRAndAddInPPRL(CommandType command, PollType pollType, String reference) {
        InmarsatPendingResponse ipr = createAnInmarsatPendingResponseObject(pollType, reference);
        ipr.setUnsentMsgId(command.getUnsentMessageGuid());
        responseList.addPendingPollResponse(ipr);
    }

    private String sendPoll(PollType poll) {
        String url = (String) connectSettings.get(URL);
        Integer port = (Integer) connectSettings.get(PORT);
        String user = (String) connectSettings.get(USERNAME);
        String pwd = (String) connectSettings.get(PSW);

        List<String> wrkOceanRegions = getOceanRegions(poll);

        if(wrkOceanRegions.isEmpty()){
            LOGGER.error("No Ocean Region in request. Check MobileTerminal. No Poll can be executed.");
            return null;
        }

        try(Socket socket = new Socket(url, port);
            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            PrintStream output = new PrintStream(socket.getOutputStream())) {

            functions.readUntil("name:", input);
            functions.write(user, output);
            functions.readUntil("word:", input);
            functions.sendPwd(output, pwd);
            functions.readUntil(">", input);

            String result;
            for(String oceanRegion : wrkOceanRegions) {
                result = sendPollCommand(poll, input, output, oceanRegion);
                if (result != null) {
                    // success in this region return with reference number
                    if (result.contains("Reference number")) {
                        String referenceNumber = parseResponse(result);
                        LOGGER.info("sendPoll invoked. Reference number : {} ", referenceNumber);
                        return referenceNumber;
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("SEND POLL ERROR! pollId: {}", poll.getPollId());
            LOGGER.error(t.toString(), t);
        }
        return null;
    }

    private List<String> getOceanRegions(PollType poll) {
        List<KeyValueType> pollReceiver = poll.getPollReceiver();
        List<String> wrkOceanRegions = new ArrayList<>();
        for (KeyValueType value : pollReceiver) {
            String key = value.getKey();
            if (key.equalsIgnoreCase("OCEAN_REGION")) {
                String wrkOceanRegionsStr = value.getValue() == null ? "" : value.getValue().trim();
                wrkOceanRegions.add(wrkOceanRegionsStr);
            }
        }
        return wrkOceanRegions;
    }

    /**
     * Sends one or more poll commands, one for each ocean region, until a reference number is
     * received.
     *
     * @return result of first successful poll command, or null if poll failed on every ocean region
     */

    private String sendPollCommand(PollType poll, InputStream in, PrintStream out, String oceanRegion) throws Throwable {
        String command = buildPollCommand(poll, oceanRegion);
        String retVal;
        functions.write(command, out);
        retVal = functions.readUntil("Text:", in);
        functions.write(".S", out);
        retVal += functions.readUntil(">", in);
        return retVal;
    }

    private String buildPollCommand(PollType poll, String oceanRegion) {
        InmarsatPoll inmPoll = new InmarsatPoll();
        inmPoll.setFieldsFromPoll(poll);
        inmPoll.setOceanRegion(oceanRegion);
        if (poll.getPollTypeType() == PollTypeType.CONFIG)
            inmPoll.setAckEnum(AckEnum.TRUE);
        return inmPoll.asCommand();
    }

    // Extract refnr from LES response
    private String parseResponse(String response) {
        String s = response.substring(response.indexOf("number"));
        return s.replaceAll("[^0-9]", ""); // returns 123
    }

    private InmarsatPendingResponse createAnInmarsatPendingResponseObject(PollType poll, String result) {

        int refNumber = -1;
        try {
            refNumber = Integer.parseInt(result);
        } catch (NumberFormatException nfe) {
            LOGGER.error("Reference is not a number. Code should not reach this point. Erroneous usage of function");
        }

        // Register response as pending
        InmarsatPendingResponse ipr = new InmarsatPendingResponse();
        ipr.setPollType(poll);
        ipr.setMsgId(poll.getPollId());
        ipr.setReferenceNumber(refNumber);
        List<KeyValueType> pollReciver = poll.getPollReceiver();
        for (KeyValueType element : pollReciver) {
            if (element.getKey().equalsIgnoreCase("MOBILE_TERMINAL_ID")) {
                ipr.setMobTermId(element.getValue());
            } else if (element.getKey().equalsIgnoreCase("DNID")) {
                ipr.setDnId(element.getValue());
            } else if (element.getKey().equalsIgnoreCase("MEMBER_NUMBER")) {
                ipr.setMembId(element.getValue());
            }
        }
        ipr.setStatus(StatusEnum.PENDING);
        return ipr;
    }

    // for inmarsat connect-logon
    public void updateSettings(List<SettingType> settingTypes) {
        for (SettingType setting : settingTypes) {
            String key = setting.getKey();
            int pos = key.lastIndexOf(".");
            key = key.substring(pos + 1);
            String value = setting.getValue();

            if (key.equals(PORT)) {
                try {
                    Integer port = Integer.parseInt(value);
                    connectSettings.put(key, port);
                } catch (NumberFormatException e) {
                    LOGGER.error("Port is not an integer");
                    return;
                }
            } else {
                connectSettings.put(key, setting.getValue());
            }
        }
    }
}
