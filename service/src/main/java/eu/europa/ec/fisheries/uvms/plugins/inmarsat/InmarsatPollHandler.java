package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.KeyValueType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollTypeType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
public class InmarsatPollHandler {

    /*


Inmarsat har ikke definert Polling&Data rapport tjenesten hundre prosent, med det mener jeg at terminal leverandører har hatt mulighet til å lage sine «tolkninger» på endel punkter
Hvis du ser på data rapport pakken (PosRep-C_2l.doc) så ser du at nederst i pakke en er MEM-code. Den mest brukte MEM code er 11 I attribute bytene nederst i pakke en vil mem 11 angi at atribute bytene inneholder  «time of position».

Angående scheduled programming. Legger med noe veldig gammel informasjon jeg laget på gruppe programmering:

First you have to program the terminals to send data report, and afterwards you have to start the reporting.
You have to send 2 poll commands to get the mobiles to report scheduled

1) First you have to send the program command:
If you want the report to start at e.g. 13:28 UTC today you have to send the following command:

 poll 0,G,4661,N,1,0,4,,5611,24
The P8 parameter is the start frame. The start time must be calculated according to the formula: (((hour*60)+minute)*60)/8.64 = start frame number. (night and day (24 hours) are divided in  10000 frame's a'8.64 sec.). The “24” at the end indicate that the terminals shall send one report every hour.

2) Afterwards you have to start the reporting command:
                e.g:  poll 0,G,4661,D,1,0,5


3) TO STOP THE REPORTING.
The data reporting are stopped using the command
                e.g.: poll 0,G,4661,N,1,0,6




     */




    private static final String PORT = "PORT";
    private static final String DNIDS = "DNIDS";
    private static final String URL = "URL";
    private static final String PSW = "PSW";
    private static final String USERNAME = "USERNAME";

    private final ConcurrentMap<String, Object> connectSettings = new ConcurrentHashMap<>();

    private final Map<String, String> dnidMemberLastFoundInRegion = new ConcurrentHashMap<>();


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

    public AcknowledgeTypeType setCommand(CommandType command) {

        PollType pollType = command.getPoll();
        if (PollTypeType.POLL == pollType.getPollTypeType()) {
            try {
                String reference = sendPoll(pollType);
                if (reference == null) {
                    LOGGER.error("Error no reference for poll with pollId: " + pollType.getPollId());
                    return AcknowledgeTypeType.NOK; //consumed but erroneus
                }
                LOGGER.info("sent poll with pollId: {} and reference: {} ", pollType.getPollId(), reference);
                InmarsatPendingResponse ipr = createAnInmarsatPendingResponseObject(pollType, reference);
                ipr.setUnsentMsgId(command.getUnsentMessageGuid());
                responseList.addPendingPollResponse(ipr);
                return AcknowledgeTypeType.OK;
            } catch (Throwable e) {
                LOGGER.error("Error while sending poll: {}", e.getMessage(), e);
            }
        }
        return AcknowledgeTypeType.NOK;
    }


    private String sendPoll(PollType poll) {

        Socket socket = null;
        PrintStream output = null;
        try {

            String url = (String) connectSettings.get(URL);
            Integer port = (Integer) connectSettings.get(PORT);
            String user = (String) connectSettings.get(USERNAME);
            String pwd = (String) connectSettings.get(PSW);

            socket = new Socket(url, port);

            // logon
            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            output = new PrintStream(socket.getOutputStream());
            functions.readUntil("name:", input);
            functions.write(user, output);
            functions.readUntil("word:", input);
            functions.sendPwd(output, pwd);
            functions.readUntil(">", input);


            // first we try to poll at the OceanRegion we found the vessel last time
            // if not found we send poll in all OceanRegions
            List<KeyValueType> pollReceiver = poll.getPollReceiver();
            String wrkDnid = "";
            String wrkMemberNo = "";
            for (KeyValueType value : pollReceiver) {
                String key = value.getKey();
                if (key.equalsIgnoreCase("DNID")) {
                    wrkDnid = value.getValue() == null ? "" : value.getValue().trim();
                }
                if (key.equalsIgnoreCase("MEMBER_NUMBER")) {
                    wrkMemberNo = value.getValue() == null ? "" : value.getValue().trim();
                }
            }
            String keyDnidMemberLastFoundInRegion = wrkDnid + wrkMemberNo;
            String result = "";
            if (dnidMemberLastFoundInRegion.containsKey(keyDnidMemberLastFoundInRegion)) {
                InmarsatPoll.OceanRegion oceanRegion = InmarsatPoll.OceanRegion.valueOf(dnidMemberLastFoundInRegion.get(keyDnidMemberLastFoundInRegion));
                try {
                    result = sendPollCommand(poll, input, output, oceanRegion);
                    if (result != null) {

                        // success in this region return with referencenumber
                        if (result.contains("Reference number")) {
                            String referenceNumber = parseResponse(result);
                            LOGGER.info("sendPoll invoked. Reference number : {} ", referenceNumber);
                            return referenceNumber;
                        }
                    }
                } catch (Throwable te) {
                    LOGGER.warn(te.toString(), te);
                }
            }

            // try with all regions  for not found in previous OceanRegion AND if moved o a new OceanRegion

            for (InmarsatPoll.OceanRegion oceanRegion : InmarsatPoll.OceanRegion.values()) {
                try {
                    result = sendPollCommand(poll, input, output, oceanRegion);
                } catch (Throwable te) {
                    continue;
                }
                if (result != null) {
                    if (result.contains("Reference number")) {
                        String referenceNumber = parseResponse(result);
                        LOGGER.info("sendPoll invoked. Reference number : {} ", referenceNumber);

                        // success put it in the map for next time
                        dnidMemberLastFoundInRegion.put(keyDnidMemberLastFoundInRegion, oceanRegion.name());
                        return referenceNumber;
                    }
                }
            }

        } catch (Throwable t) {
            if (poll != null) {
                LOGGER.error("SENDPOLL ERROR pollid : {}", poll.getPollId());
            }
            LOGGER.error(t.toString(), t);
        } finally {

            if (output != null) {
                output.print("QUIT \r\n");
                output.flush();
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // OK
                }
            }


        }
        return null;
    }

    private String buildPollCommand(PollType poll, InmarsatPoll.OceanRegion oceanRegion) {

        InmarsatPoll inmPoll = new InmarsatPoll();
        inmPoll.setPollType(poll);
        inmPoll.setOceanRegion(oceanRegion);
        if (poll.getPollTypeType() == PollTypeType.CONFIG) inmPoll.setAck(InmarsatPoll.AckType.TRUE);
        return inmPoll.asCommand();
    }


    /**
     * Sends one or more poll commands, one for each ocean region, until a reference number is
     * received.
     *
     * @return result of first successful poll command, or null if poll failed on every ocean region
     */

    private String sendPollCommand(PollType poll, InputStream in, PrintStream out, InmarsatPoll.OceanRegion oceanRegion) throws InmarsatSocketException, IOException {

        String cmd = null;
        try {
            cmd = buildPollCommand(poll, oceanRegion);
        } catch (Throwable t) {
            LOGGER.error("could not build pollcommand", t);
            return null;
        }

        String ret;
        functions.write(cmd, out);
        ret = functions.readUntil("Text:", in);
        functions.write(".S", out);
        ret += functions.readUntil(">", in);
        return ret;
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
            LOGGER.error("Reference is not a number. Code should not reach this point. Erroneus usage of function");
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
        ipr.setStatus(InmarsatPendingResponse.StatusType.PENDING);
        return ipr;
    }


    private String sendConfigurationPoll(PollType poll) throws InmarsatSocketException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // for inmarsat connect-logon
    public void updateSettings(List<SettingType> settingsType) {
        for (SettingType setting : settingsType) {
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
