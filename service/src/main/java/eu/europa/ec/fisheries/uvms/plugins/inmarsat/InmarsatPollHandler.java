package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.common.v1.*;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollTypeType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import eu.europa.ec.fisheries.schema.exchange.v1.ExchangeLogStatusTypeType;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.message.PluginMessageProducer;
import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.jms.JMSException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
public class InmarsatPollHandler {

    private static final String  PORT = "PORT";
    private static final String  DNIDS = "DNIDS";
    private static final String  URL = "URL";
    private static final String  PSW = "PSW";
    private static final String  USERNAME = "USERNAME";

    private final ConcurrentMap<String, Object> connectSettings = new ConcurrentHashMap<>();


    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatPollHandler.class);

    @Inject
    private PluginPendingResponseList responseList;

    @Inject
    private PluginMessageProducer messageProducer;

    @Inject
    private HelperFunctions functions;

    @Inject
    private InmarsatPlugin inmarsatPlugin;

    public ConcurrentMap<String, Object> getSettings() {
        return connectSettings;
    }

    public Object getSetting(String setting) {
        return  connectSettings.get(setting);
    }


    public PluginPendingResponseList getPluginPendingResponseList(){
        return responseList;
    }

    public AcknowledgeTypeType setCommand(CommandType command) {


        PollType poll = command.getPoll();
        if (poll != null && CommandTypeType.POLL.equals(command.getCommand())) {
            if (PollTypeType.POLL == poll.getPollTypeType()) {
                try {
                    String reference = sendPoll(poll);
                    if (reference == null) {
                        LOGGER.error("Error no reference for poll with pollId: " + poll.getPollId());
                        return AcknowledgeTypeType.NOK; //consumed but erroneus
                    }
                    LOGGER.info("sent poll with pollId: {} and reference: {} ", poll.getPollId(), reference);
                    InmarsatPendingResponse ipr = createAnInmarsatPendingResponseObject(poll, reference);
                    responseList.addPendingPollResponse(ipr);
                    // Send status update to exchange
                    sentStatusToExchange(ipr);
                    return AcknowledgeTypeType.OK;
                } catch (Throwable e) {
                    LOGGER.error("Error while sending poll: {}", e.getMessage(), e);
                }
            }
        }
        return AcknowledgeTypeType.NOK;
    }

    private String sendPoll(PollType poll) {

        TelnetClient telnet = null;
        PrintStream output = null;
        try {

            String url = (String)connectSettings.get(URL);
            Integer port = (Integer) connectSettings.get(PORT);
            String user = (String)connectSettings.get(USERNAME);
            String pwd = (String)connectSettings.get(PSW);

            telnet = functions.createTelnetClient(url, port);

            // logon
            BufferedInputStream input = new BufferedInputStream(telnet.getInputStream());
            output = new PrintStream(telnet.getOutputStream());
            functions.readUntil("name:", input);
            functions.write(user, output);
            functions.readUntil("word:", input);
            functions.sendPwd(output, pwd);
            functions.readUntil(">", input);

            String result = "";
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
                        return referenceNumber;
                    }
                }
            }
        } catch (Throwable t) {
            if (poll != null) {
                LOGGER.error("SENDPOLL ERROR pollid : {}", poll.getPollId());
            }
            LOGGER.error(t.toString(), t);
        }
        finally {

            if (output != null) {
                output.print("QUIT \r\n");
                output.flush();
            }
            if ((telnet != null) && (telnet.isConnected())) {
                try {
                    telnet.disconnect();
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

    private String sendPollCommand(PollType poll, InputStream in, PrintStream out, InmarsatPoll.OceanRegion oceanRegion) throws TelnetException, IOException {

        String cmd = null;
        try{
            cmd = buildPollCommand(poll, oceanRegion);
        }
        catch(Throwable t){
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
        }catch(NumberFormatException nfe){
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


    private void sentStatusToExchange(InmarsatPendingResponse ipr) {

        AcknowledgeType ackType = new AcknowledgeType();
        ackType.setMessage("");
        ackType.setMessageId(ipr.getMsgId());

        PollStatusAcknowledgeType osat = new PollStatusAcknowledgeType();
        osat.setPollId(ipr.getPollType().getPollId());
        osat.setStatus(ExchangeLogStatusTypeType.SENT);

        ackType.setPollStatus(osat);
        ackType.setType(AcknowledgeTypeType.OK);

        try {
            String s = ExchangePluginResponseMapper.mapToSetCommandResponse(inmarsatPlugin.getApplicationName(), ackType);
            messageProducer.sendModuleMessage(s, ModuleQueue.EXCHANGE);
            LOGGER.debug("Poll response {} sent to exchange with status: {}",ipr.getMsgId(),ExchangeLogStatusTypeType.PENDING);
        } catch (ExchangeModelMarshallException ex) {
            LOGGER.debug("ExchangeModelMarshallException", ex);
        } catch (JMSException jex) {
            LOGGER.debug("JMSException", jex);
        }
    }

    private String sendConfigurationPoll(PollType poll) throws TelnetException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // for inmarsat connect-logon
    public void updateSettings(List<SettingType> settingsType) {
        for (SettingType setting : settingsType) {
            String key = setting.getKey();
            int pos = key.lastIndexOf(".");
            key = key.substring(pos + 1);
            String value = setting.getValue();

            if(key.equals(PORT)){
                try{
                    Integer port = Integer.parseInt(value);
                    connectSettings.put(key,port);
                }
                catch(NumberFormatException e){
                    LOGGER.error("Port is not an integer");
                    return;
                }
            }else{
                connectSettings.put(key,setting.getValue());
            }
        }
    }



}
