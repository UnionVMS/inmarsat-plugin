package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.common.v1.*;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollTypeType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import eu.europa.ec.fisheries.schema.exchange.v1.ExchangeLogStatusTypeType;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.message.PluginMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Stateless
public class InmarsatPollHandler {

    private final ConcurrentMap<String, String> settings = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatPollHandler.class);

    @Inject
    private PluginPendingResponseList responseList;

    @Inject
    private PluginMessageProducer messageProducer;

    @Inject
    private HelperFunctions functions;


    @PostConstruct
    private void startup() {
        /*

        Properties pluginProperties = getPropertiesFromFile(PluginDataHolder.PLUGIN_PROPERTIES);
        setPluginApplicaitonProperties(pluginProperties);

        registerClassName = getPluginApplicationProperty("application.groupid") + "." + getPluginApplicationProperty("application.name");
        LOGGER.debug("Plugin will try to register as:{}", registerClassName);
        super.setPluginProperties(getPropertiesFromFile(PluginDataHolder.SETTINGS_PROPERTIES));
        super.setPluginCapabilities(getPropertiesFromFile(PluginDataHolder.CAPABILITIES_PROPERTIES));

        ServiceMapper.mapToMapFromProperties(super.getSettings(), super.getPluginProperties(), getRegisterClassName());
        ServiceMapper.mapToMapFromProperties(super.getCapabilities(), super.getPluginCapabilities(), null);

        capabilityList = ServiceMapper.getCapabilitiesListTypeFromMap(super.getCapabilities());
        settingList = ServiceMapper.getSettingsListTypeFromMap(super.getSettings());
        serviceType = ServiceMapper.getServiceType(getRegisterClassName(), "Thrane&Thrane", "inmarsat plugin for the Thrane&Thrane API", PluginType.SATELLITE_RECEIVER, getPluginResponseSubscriptionName(), "INMARSAT_C");

        register();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Settings updated in plugin {}", registerClassName);
            for (Map.Entry<String, String> entry : super.getSettings().entrySet()) {
                LOGGER.debug("Setting: KEY: {} , VALUE: {}", entry.getKey(), entry.getValue());
            }
        }

        LOGGER.info("PLUGIN STARTED");
        */
    }

    private ConcurrentMap<String, String> getSettings() {
        return settings;
    }

    public String getSetting(String setting) {
        String settingValue = getSettings().get(setting);
        return settingValue;
    }


    public AcknowledgeTypeType setCommand(CommandType command) {


        PollType poll = command.getPoll();
        if (poll != null && CommandTypeType.POLL.equals(command.getCommand())) {
            if (PollTypeType.POLL == poll.getPollTypeType()) {
                LOGGER.info("poll RECEIVED id: {} ", poll.getPollId());

                try {
                    //String reference = sendPoll(poll, input, output);
                    String reference = null;

                    // FAILED ROLLBACK   DONT POP the Queue
                    if (reference == null) {

                        //
                        return null;
                    }
                    LOGGER.info("poll SEND id: {}  reference: {} ", poll.getPollId(), reference);
                    // Register Not acknowledge response
                    InmarsatPendingResponse ipr = createAnInmarsatPendingResponseObject(poll, reference);
                    responseList.addPendingPollResponse(ipr);
                    // Send status update to exchange
                    sentStatusToExchange(ipr);
                } catch (Exception e) {
                    LOGGER.warn("Error while sending poll: {}", e.getMessage());
                }
            }
        }

        return null;
    }

    private String sendPoll(PollType poll, BufferedInputStream input, PrintStream output) {

        try {

            String result = "";
            for (InmarsatPoll.OceanRegion oceanRegion : InmarsatPoll.OceanRegion.values()) {
                try {
                    result = sendPollCommand(poll, input, output, oceanRegion);
                } catch (TelnetException te) {
                    LOGGER.warn("could not send pollcommand for region " + oceanRegion.name(), te);
                    continue;
                }
                if (result != null) {
                    if (result.contains("Reference number")) {
                        String referenceNumber = parseResponse(result);
                        LOGGER.info("sendPoll invoked. Reference number : {} {}", referenceNumber, result);
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

        String cmd = buildPollCommand(poll, oceanRegion);
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
        // Register response as pending
        InmarsatPendingResponse ipr = new InmarsatPendingResponse();
        ipr.setPollType(poll);
        ipr.setMsgId(poll.getPollId());
        ipr.setReferenceNumber(Integer.parseInt(result));
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
            String s = ExchangePluginResponseMapper.mapToSetCommandResponse(functions.getApplicationName(), ackType);
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
    public void updateSettings(List<SettingType> settings) {
        for (SettingType setting : settings) {
            String key = setting.getKey();
            int pos = key.lastIndexOf(".");
            key = key.substring(pos + 1);
            getSettings().put(key, setting.getValue());
            LOGGER.info("Updating setting: {} = {}", key, setting.getValue());

        }
    }




}
