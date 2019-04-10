package eu.europa.ec.fisheries.uvms.plugins.inmarsat;


import eu.europa.ec.fisheries.schema.exchange.common.v1.*;
import eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.IdList;
import eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.IdType;
import eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.MobileTerminalId;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.*;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.CapabilityListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.ServiceType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import eu.europa.ec.fisheries.schema.exchange.v1.ExchangeLogStatusTypeType;
import eu.europa.ec.fisheries.uvms.commons.date.DateUtils;
import eu.europa.ec.fisheries.uvms.exchange.model.constant.ExchangeModelConstants;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.message.PluginMessageProducer;
import fish.focus.uvms.commons.les.inmarsat.InmarsatException;
import fish.focus.uvms.commons.les.inmarsat.InmarsatInterpreter;
import fish.focus.uvms.commons.les.inmarsat.InmarsatMessage;
import fish.focus.uvms.commons.les.inmarsat.body.PositionReport;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.inject.Inject;
import javax.jms.JMSException;
import java.io.*;
import java.util.*;

@Startup
@Singleton
public class InmarsatPlugin extends PluginDataHolder {


    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatPlugin.class);

    private static final int MAX_NUMBER_OF_TRIES = 20;
    private boolean isRegistered = false;
    private boolean isEnabled = false;
    private int numberOfTriesExecuted = 0;
    private String registerClassName;

    @Inject
    private PluginMessageProducer messageProducer;

    @Inject
    private InmarsatInterpreter inmarsatInterpreter;

    @Inject
    private InmarsatPollHandler inmarsatPollHandler;

    @Inject
    private HelperFunctions functions;


    private CapabilityListType capabilityList;
    private SettingListType settingList;
    private ServiceType serviceType;


    @PostConstruct
    private void startup() {

        Properties pluginProperties = functions.getPropertiesFromFile(this.getClass(), PluginDataHolder.PLUGIN_PROPERTIES);
        super.setPluginApplicaitonProperties(pluginProperties);
        registerClassName = getPluginApplicationProperty("application.groupid") + "." + getPluginApplicationProperty("application.name");
        LOGGER.debug("Plugin will try to register as:{}", registerClassName);
        super.setPluginProperties(functions.getPropertiesFromFile(this.getClass(), PluginDataHolder.SETTINGS_PROPERTIES));
        super.setPluginCapabilities(functions.getPropertiesFromFile(this.getClass(), PluginDataHolder.CAPABILITIES_PROPERTIES));
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
    }

    @PreDestroy
    private void shutdown() {
        unregister();
    }

    @Schedule(second = "*/10", minute = "*", hour = "*", persistent = false)
    private void timeout(Timer timer) {

        try {

            LOGGER.info("HEARTBEAT timeout running. isRegistered=" + isRegistered + " ,numberOfTriesExecuted=" + numberOfTriesExecuted + " threadId=" + Thread.currentThread().toString());
            if (!isRegistered && numberOfTriesExecuted < MAX_NUMBER_OF_TRIES) {
                LOGGER.info(getRegisterClassName() + " is not registered, trying to register");
                register();
                numberOfTriesExecuted++;
            }
            if (isRegistered) {
                LOGGER.info(getRegisterClassName() + " is registered. Cancelling timer.");
                timer.cancel();
            } else if (numberOfTriesExecuted >= MAX_NUMBER_OF_TRIES) {
                LOGGER.info(getRegisterClassName() + " failed to register, maximum number of retries reached.");
            }
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
        }
    }


    @Schedule(minute = "*/3", hour = "*", persistent = false)
    private void connectAndRetrieve() {

        LOGGER.info("HEARTBEAT connectAndRetrieve running. IsEnabled=" + isEnabled + " threadId=" + Thread.currentThread().toString());
        TelnetClient telnet = null;
        PrintStream output = null;
        try {
            if (isIsEnabled()) {
                List<String> dnids = getDnids();
                String url = getSetting("URL");
                String port = getSetting("PORT");
                String user = getSetting("USERNAME");
                String pwd = getSetting("PSW");

                telnet = functions.createTelnetClient(url, Integer.parseInt(port));

                // logon
                BufferedInputStream input = new BufferedInputStream(telnet.getInputStream());
                output = new PrintStream(telnet.getOutputStream());
                functions.readUntil("name:", input);
                functions.write(user, output);
                functions.readUntil("word:", input);
                functions.sendPwd(output, pwd);
                functions.readUntil(">", input);

                downloadAndPutToQueue(input, output, dnids);
            }
        } catch (Throwable t) {
            LOGGER.error(t.toString(), t);
        } finally {
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
    }

    private void register() {
        LOGGER.info("Registering to Exchange Module");
        try {
            String registerServiceRequest = ExchangeModuleRequestMapper.createRegisterServiceRequest(serviceType, capabilityList, settingList);
            messageProducer.sendEventBusMessage(registerServiceRequest, ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
            LOGGER.info("Registering to Exchange Module successfully sent.");
        } catch (JMSException | ExchangeModelMarshallException e) {
            LOGGER.error("Failed to send registration message to {}", ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
        }
    }

    private void unregister() {
        LOGGER.info("Unregistering from Exchange Module");
        try {
            String unregisterServiceRequest = ExchangeModuleRequestMapper.createUnregisterServiceRequest(serviceType);
            messageProducer.sendEventBusMessage(unregisterServiceRequest, ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
        } catch (JMSException | ExchangeModelMarshallException e) {
            LOGGER.error("Failed to send unregistration message to {}", ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
        }
    }

    public String getPluginResponseSubscriptionName() {
        return getRegisterClassName() + "." + getPluginApplicationProperty("application.responseTopicName");
    }

    public String getRegisterClassName() {
        return registerClassName;
    }


    private String getPluginApplicationProperty(String key) {
        try {
            return (String) super.getPluginApplicaitonProperties().get(key);
        } catch (Exception e) {
            LOGGER.error("Failed to getSetting for key: " + key, getRegisterClassName());
            return null;
        }
    }

    public String getSetting(String setting) {
        LOGGER.debug("Trying to get setting {}.{}", registerClassName, setting);
        String settingValue = super.getSettings().get(registerClassName + "." + setting);
        LOGGER.debug("Got setting value for {}.{};{}", registerClassName, setting, settingValue);
        return settingValue;
    }

    public boolean isIsRegistered() {
        return isRegistered;
    }

    public void setIsRegistered(boolean isRegistered) {
        LOGGER.info("setRegistered : " + isRegistered);
        this.isRegistered = isRegistered;
    }

    public boolean isIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void updateSettings(List<SettingType> settings) {
        for (SettingType setting : settings) {
            LOGGER.info("Updating setting: {} = {}", setting.getKey(), setting.getValue());
            getSettings().put(setting.getKey(), setting.getValue());
        }
    }

    /**
     * @return list of DNIDs configured
     */
    private List<String> getDnids() {
        String dnidsSettingValue = getSetting("DNIDS");
        if (StringUtils.isBlank(dnidsSettingValue)) {
            return new ArrayList<>();
        }

        return Arrays.asList(dnidsSettingValue.trim().split(","));
    }


    /**
     * @param msg inmarsat message to send
     */
    private void msgToQue(InmarsatMessage msg) throws InmarsatException {

        MovementBaseType movement = new MovementBaseType();
        movement.setComChannelType(MovementComChannelType.MOBILE_TERMINAL);
        MobileTerminalId mobTermId = new MobileTerminalId();
        IdList dnidId = new IdList();
        dnidId.setType(IdType.DNID);
        dnidId.setValue(Integer.toString(msg.getHeader().getDnid()));
        IdList membId = new IdList();
        membId.setType(IdType.MEMBER_NUMBER);
        membId.setValue(Integer.toString(msg.getHeader().getMemberNo()));

        mobTermId.getMobileTerminalIdList().add(dnidId);
        mobTermId.getMobileTerminalIdList().add(membId);
        movement.setMobileTerminalId(mobTermId);
        movement.setMovementType(MovementTypeType.POS);
        MovementPoint mp = new MovementPoint();
        mp.setAltitude(0.0);
        mp.setLatitude(((PositionReport) msg.getBody()).getLatitude().getAsDouble());
        mp.setLongitude(((PositionReport) msg.getBody()).getLongitude().getAsDouble());
        movement.setPosition(mp);

        movement.setPositionTime(((PositionReport) msg.getBody()).getPositionDate().getDate());
        movement.setReportedCourse((double) ((PositionReport) msg.getBody()).getCourse());
        movement.setReportedSpeed(((PositionReport) msg.getBody()).getSpeed());
        movement.setSource(MovementSourceType.INMARSAT_C);
        movement.setStatus(Integer.toString(((PositionReport) msg.getBody()).getMacroEncodedMessage()));

        SetReportMovementType reportType = new SetReportMovementType();
        reportType.setMovement(movement);
        GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        reportType.setTimestamp(gcal.getTime());
        reportType.setPluginName(getRegisterClassName());
        reportType.setPluginType(PluginType.SATELLITE_RECEIVER);

        if (!sendMovementReportToExchange(reportType)) {
            // if it didnt send so quit return
            return;
        }
        // it send so check if it was a pollrequest
        // in that case update the pollrequest status


        PluginPendingResponseList responseList = inmarsatPollHandler.getPluginPendingResponseList();

        // If the report is a pending poll response, also generate a status update for that poll
        InmarsatPendingResponse ipr = responseList.containsPollTo(dnidId.getValue(), membId.getValue());

        if (ipr != null) {
            LOGGER.info("PendingPollResponse found in list: {}", ipr.getReferenceNumber());
            AcknowledgeType ackType = new AcknowledgeType();
            ackType.setMessage("");
            ackType.setMessageId(ipr.getMsgId());

            PollStatusAcknowledgeType osat = new PollStatusAcknowledgeType();
            osat.setPollId(ipr.getMsgId());
            osat.setStatus(ExchangeLogStatusTypeType.SUCCESSFUL);

            ackType.setPollStatus(osat);
            ackType.setType(AcknowledgeTypeType.OK);

            String iprMessageId = ipr.getUnsentMsgId();
            ackType.setUnsentMessageGuid(iprMessageId);

            try {
                String s = ExchangePluginResponseMapper.mapToSetPollStatusToSuccessfulResponse(getApplicationName(), ackType, ipr.getMsgId());
                messageProducer.sendModuleMessage(s, ModuleQueue.EXCHANGE);
                boolean b = responseList.removePendingPollResponse(ipr);
                LOGGER.debug("Pending poll response removed: {}", b);
            } catch (ExchangeModelMarshallException ex) {
                LOGGER.debug("ExchangeModelMarshallException", ex);
            } catch (JMSException jex) {
                LOGGER.debug("JMSException", jex);
            }
        }


        LOGGER.debug("Sending movement to Exchange");
    }

    public String getApplicationName() {
        try {
            return (String) super.getPluginApplicaitonProperties().get("application.name");
        } catch (Exception e) {
            LOGGER.error("Failed to getSetting for key: application.name", getRegisterClassName());
            return null;
        }
    }

    private boolean sendMovementReportToExchange(SetReportMovementType reportType) {
        try {
            String text = ExchangeModuleRequestMapper.createSetMovementReportRequest(reportType, "TWOSTAGE", null, DateUtils.nowUTC().toDate(), null, PluginType.SATELLITE_RECEIVER, "TWOSTAGE", null);
            String messageId = messageProducer.sendModuleMessage(text, ModuleQueue.EXCHANGE);
            LOGGER.debug("Sent to exchange - text:{}, id:{}", text, messageId);
            return true;
        } catch (ExchangeModelMarshallException e) {
            LOGGER.error("Couldn't map movement to setreportmovementtype", e);
            return false;
        } catch (JMSException e) {
            LOGGER.error("couldn't send movement", e);
            return false;
        }
    }

    public AcknowledgeTypeType setReport(ReportType report) {
        return AcknowledgeTypeType.NOK;
    }

    /**
     * Set the config values for the twostage
     *
     * @param settings the settings
     * @return AcknowledgeTypeType
     */
    public AcknowledgeTypeType setConfig(SettingListType settings) {
        LOGGER.info(getRegisterClassName() + ".setConfig()");
        try {
            for (KeyValueType values : settings.getSetting()) {
                LOGGER.debug("Setting [ " + values.getKey() + " : " + values.getValue() + " ]");
                getSettings().put(values.getKey(), values.getValue());
            }
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            LOGGER.error("Failed to set config in {}", getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }
    }

    /**
     * Start the twostage. Use this to enable functionality in the twostage
     *
     * @return AcknowledgeTypeType
     */
    public AcknowledgeTypeType start() {
        LOGGER.info(getRegisterClassName() + ".start()");
        try {
            setIsEnabled(Boolean.TRUE);
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            setIsEnabled(Boolean.FALSE);
            LOGGER.error("Failed to start {}", getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }
    }

    /**
     * Start the twostage. Use this to disable functionality in the twostage
     *
     * @return AcknowledgeTypeType
     */
    public AcknowledgeTypeType stop() {
        LOGGER.info(getRegisterClassName() + ".stop()");
        try {
            setIsEnabled(Boolean.FALSE);
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            setIsEnabled(Boolean.TRUE);
            LOGGER.error("Failed to stop {}", getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }
    }


    private void downloadAndPutToQueue(BufferedInputStream input, PrintStream output, List<String> dnids) {

        for (String dnid : dnids) {
            try {
                List<byte[]> messages = download(input, output, dnid);
                for (byte[] message : messages) {
                    InmarsatMessage[] inmarsatMessagesPerOceanRegion = inmarsatInterpreter.byteToInmMessage(message);
                    if ((inmarsatMessagesPerOceanRegion != null) && (inmarsatMessagesPerOceanRegion.length > 0)) {
                        int n = inmarsatMessagesPerOceanRegion.length;
                        for (int i = 0; i < n; i++) {
                            try {
                                msgToQue(inmarsatMessagesPerOceanRegion[i]);
                            } catch (InmarsatException e) {
                                LOGGER.error("Positiondate not found in " + inmarsatMessagesPerOceanRegion[i].toString(), e);
                            }
                        }
                    }
                }
            } catch (TelnetException e) {
                LOGGER.error(e.toString(), e);
                continue;
            }
        }
    }


    private List<byte[]> download(BufferedInputStream input, PrintStream output, String dnid) throws TelnetException {

        List<byte[]> result = new ArrayList<>();

        LOGGER.info("Download for :{} ", dnid);
        for (int oceanRegion = 0; oceanRegion < 4; oceanRegion++) {
            try {
                String cmd = "DNID " + dnid + " " + oceanRegion;
                functions.write(cmd, output);
                byte[] bos = readUntilDownload(">", input);
                result.add(bos);
            } catch (NullPointerException | IOException ex) {
                LOGGER.error("Error when communicating with Telnet", ex);
            }
        }
        LOGGER.info("OK");
        return result;
    }

    private byte[] readUntilDownload(String pattern, InputStream in) throws TelnetException, IOException {

        StringBuilder sb = new StringBuilder();
        byte[] contents = new byte[1024];
        int bytesRead;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        do {
            bytesRead = in.read(contents);
            if (bytesRead > 0) {
                bos.write(contents, 0, bytesRead);
                String s = new String(contents, 0, bytesRead);
                sb.append(s);
                String currentString = sb.toString();
                if (currentString.trim().endsWith(pattern)) {
                    bos.flush();
                    return bos.toByteArray();
                } else {
                    functions.containsFault(currentString);
                }
            }
        } while (bytesRead >= 0);

        bos.flush();
        throw new TelnetException("Unknown download response from Inmarsat-C LES Telnet @  : " + Arrays.toString(bos.toByteArray()));
    }


}
