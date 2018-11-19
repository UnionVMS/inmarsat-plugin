package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.common.v1.*;
import eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.IdList;
import eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.IdType;
import eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.MobileTerminalId;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.*;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollTypeType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.CapabilityListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.ServiceType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import eu.europa.ec.fisheries.schema.exchange.v1.ExchangeLogStatusTypeType;
import eu.europa.ec.fisheries.uvms.commons.date.DateUtils;
import eu.europa.ec.fisheries.uvms.commons.message.api.MessageException;
import eu.europa.ec.fisheries.uvms.exchange.model.constant.ExchangeModelConstants;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.message.PluginToEventBusTopicProducer;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.message.PluginToExchangeProducer;
import fish.focus.uvms.commons.les.inmarsat.InmarsatException;
import fish.focus.uvms.commons.les.inmarsat.InmarsatFileHandler;
import fish.focus.uvms.commons.les.inmarsat.InmarsatMessage;
import fish.focus.uvms.commons.les.inmarsat.body.PositionReport;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;
import javax.ejb.Timer;
import javax.inject.Inject;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Future;

@Startup
@Singleton
public class InmarsatPluginMock extends PluginDataHolder implements InmarsatPlugin {


    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatPluginMock.class);

    private static final String FILE_NAME = "pending.ser";
    private static final int MAX_NUMBER_OF_TRIES = 20;
    private boolean isRegistered = false;
    private boolean isEnabled = false;
    private boolean waitingForResponse = false;
    private int numberOfTriesExecuted = 0;
    private String registerClassName;

    private ArrayList<InmarsatPendingResponse> pending;

    @Inject
    private PluginToEventBusTopicProducer evtBusProducer;

    @Inject
    private PluginToExchangeProducer messageProducer;

    @Inject
    private InmarsatConnection connect;

    private CapabilityListType capabilityList;
    private SettingListType settingList;
    private ServiceType serviceType;

    private final Map<String, Future> connectFutures = new HashMap<>();

    private Future deliverFuture = null;
    private String cachePath = null;
    private String pollPath = null;

    @PostConstruct
    private void startup() {

        Properties props = getPropertiesFromFile(PluginDataHolder.PLUGIN_PROPERTIES);
        super.setPluginApplicaitonProperties(props);
        createDirectories();
        registerClassName = getPLuginApplicationProperty("application.groupid") + "." + getPLuginApplicationProperty("application.name");
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

        loadPendingPollResponse();
        LOGGER.info("PLUGIN STARTED");
    }

    @PreDestroy
    private void shutdown() {
        writePendingPollResponse();
        unregister();
    }

    /* OBS THE TIMERS ARE MUCH MORE FREQUENT THAN IN THE REAL PLUGIN */

    @Schedule(second = "*/1", minute = "*", hour = "*", persistent = false)
    private void timeout(Timer timer) {
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
    }

    @Schedule(second = "*/3", minute = "*", hour = "*", persistent = false)
    private void connectAndRetrieve() {
        /*
        if (isIsEnabled()) {
            List<String> dnids = getDownloadDnids();
            Future<Map<String, String>> future = download(getCachePath(), dnids);

            for (String dnid : dnids) {
                connectFutures.put(dnid, future);
            }
        }
        */
        LOGGER.info("connectAndRetrieve");
    }

    @Schedule(second = "*/2", minute = "*", hour = "*", persistent = false)
    private void parseAndDeliver() {
        /*
        if (isIsEnabled() && (deliverFuture == null || deliverFuture.isDone())) {
            try {
                deliverFuture = parseAndDeliver(getCachePath());
            } catch (IOException e) {
                LOGGER.error("Couldn't deliver ");
            }
        } else {
            LOGGER.debug("deliverFuture is not null and busy");
        }
        */
        LOGGER.info("parseAndDeliver");
    }

    private void register() {
        LOGGER.info("Registering to Exchange Module");
        setWaitingForResponse(true);
        try {
            String registerServiceRequest = ExchangeModuleRequestMapper.createRegisterServiceRequest(serviceType, capabilityList, settingList);
            evtBusProducer.sendEventBusMessage(registerServiceRequest, ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
        } catch (ExchangeModelMarshallException | MessageException e) {
            LOGGER.error("Failed to send registration message to {}", ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
            setWaitingForResponse(false);
        }
    }

    private void unregister() {
        LOGGER.info("Unregistering from Exchange Module");
        try {
            String unregisterServiceRequest = ExchangeModuleRequestMapper.createUnregisterServiceRequest(serviceType);
            evtBusProducer.sendEventBusMessage(unregisterServiceRequest, ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
        } catch (ExchangeModelMarshallException | MessageException e) {
            LOGGER.error("Failed to send unregistration message to {}", ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
        }
    }

    public String getPluginResponseSubscriptionName() {
        return getRegisterClassName() + "." + getPLuginApplicationProperty("application.responseTopicName");
    }

    private String getResponseTopicMessageName() {
        return getPLuginApplicationProperty("application.groupid") + "." + getPLuginApplicationProperty("application.name");
    }

    public String getRegisterClassName() {
        return registerClassName;
    }

    private String getApplicaionName() {
        return getPLuginApplicationProperty("application.name");
    }

    private String getPLuginApplicationProperty(String key) {
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

    public boolean isWaitingForResponse() {
        return waitingForResponse;
    }

    public void setWaitingForResponse(boolean waitingForResponse) {
        this.waitingForResponse = waitingForResponse;
    }

    public boolean isIsRegistered() {
        return isRegistered;
    }

    public void setIsRegistered(boolean isRegistered) {
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


    private String getCachePath() {
        return cachePath;
    }

    private String getPollPath() {
        return pollPath;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createDirectories() {

        File f = new File(getPLuginApplicationProperty("application.logfile"));
        File dirCache = new File(f.getParentFile(), "cache");
        File dirPoll = new File(f.getParentFile(), "poll");
        if (!dirCache.exists()) {
            dirCache.mkdir();
        }
        if (!dirPoll.exists()) {
            dirPoll.mkdir();
        }
        cachePath = dirCache.getAbsolutePath() + File.separator;
        pollPath = dirPoll.getAbsolutePath() + File.separator;
    }


    /**
     * @return returns DNIDs available for download
     */
    private List<String> getDownloadDnids() {
        List<String> downloadDnids = new ArrayList<>();
        List<String> dnidList = getDnids();
        for (String dnid : dnidList) {
            Future existingFuture = connectFutures.get(dnid);
            if (!downloadDnids.contains(dnid) && (existingFuture == null || existingFuture.isDone())) {
                downloadDnids.add(dnid);
            }
        }
        return downloadDnids;
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


    // from filehandlerbean
    private Properties getPropertiesFromFile(String fileName) {
        Properties props = new Properties();
        try {
            InputStream inputStream =
                    InmarsatPluginMock.class.getClassLoader().getResourceAsStream(fileName);
            props.load(inputStream);
        } catch (IOException e) {
            LOGGER.debug("Properties file failed to load");
        }
        return props;
    }


    @Asynchronous
    private Future<Map<String, String>> download(String path, List<String> dnids) {
        Map<String, String> responses = new HashMap<>();
        for (String dnid : dnids) {
            try {
                String response = download(path, dnid);
                LOGGER.debug("Download returned: {}", response);
                responses.put(dnid, response);
            } catch (TelnetException e) {
                LOGGER.error("Exception while downloading: {}", e.getMessage());
            }
        }

        return new AsyncResult<>(responses);
    }

    private String download(String path, String dnid) throws TelnetException {
        LOGGER.debug("Download invoked with DNID = {}", dnid);
        return connect.connect(null, path, getSetting("URL"), getSetting("PORT"), getSetting("USERNAME"), getSetting("PSW"), dnid);
    }


    private String sendPoll(PollType poll, String path, String url, String port, String username, String psw, String dnids) throws TelnetException {
        LOGGER.info("sendPoll invoked");
        String s =
                connect.connect(poll, path, url, port, username, psw, dnids);
        LOGGER.info("sendPoll returned:{} ", s);
        if (s != null) {
            s = parseResponse(s);
        } else {
            throw new TelnetException("Connect returned null response");
        }
        return s;
    }

    private String sendConfigurationPoll(PollType poll) throws TelnetException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // Extract refnr from LES response
    private String parseResponse(String response) {
        String s = response.substring(response.indexOf("number"));
        return s.replaceAll("[^0-9]", ""); // returns 123
    }


    /**
     * @param path the directory to parse inmarsat files
     * @return future
     * @throws IOException @see {@link InmarsatFileHandler#createMessages()}
     */
    @Asynchronous
    private Future<String> parseAndDeliver(String path) throws IOException {

        InmarsatFileHandler fileHandler = new InmarsatFileHandler(Paths.get(path));
        Map<Path, InmarsatMessage[]> messagesFromPath = fileHandler.createMessages();
        for (Map.Entry<Path, InmarsatMessage[]> entry : messagesFromPath.entrySet()) {
            // Send message to Que
            for (InmarsatMessage message : entry.getValue()) {
                try {
                    msgToQue(message);
                } catch (InmarsatException e) {
                    LOGGER.error("Could not send to msg que for message: {}", message, e);
                }
            }
            // Send ok so move file
            LOGGER.info("File: {} processed and moved to handled", entry.getKey());
            fileHandler.moveFileToDir(entry.getKey(), Paths.get(entry.getKey().getParent().toString(), "handled"));
        }
        return new AsyncResult<>("Done");
    }

    /**
     * @param msg inmarsat message to send
     * @throws InmarsatException if stored date could be set.
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

        sendMovementReportToExchange(reportType);

        // If there is a pending poll response, also generate a status update for that poll
        InmarsatPendingResponse ipr = continsPollTo(dnidId.getValue(), membId.getValue());

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

            try {
                String s = ExchangePluginResponseMapper.mapToSetPollStatusToSuccessfulResponse(getApplicaionName(), ackType, ipr.getMsgId());
                messageProducer.sendModuleMessage(s, null);
                boolean b = removePendingPollResponse(ipr);
                LOGGER.debug("Pending poll response removed: {}", b);
            } catch (ExchangeModelMarshallException ex) {
                LOGGER.debug("ExchangeModelMarshallException", ex);
            } catch (MessageException jex) {
                LOGGER.debug("JMSException", jex);
            }
        }

        LOGGER.debug("Sending momvement to Exchange");
    }


    private void sendMovementReportToExchange(SetReportMovementType reportType) {
        try {
            String text = ExchangeModuleRequestMapper.createSetMovementReportRequest(reportType, "TWOSTAGE", null, DateUtils.nowUTC().toDate(), null, PluginType.SATELLITE_RECEIVER, "TWOSTAGE", null);
            String messageId = messageProducer.sendModuleMessage(text, null);
            LOGGER.debug("Sent to exchange - text:{}, id:{}", text, messageId);
            getCachedMovement().put(messageId, reportType);
        } catch (ExchangeModelMarshallException e) {
            LOGGER.error("Couldn't map movement to setreportmovementtype");
        } catch (MessageException e) {
            LOGGER.error("couldn't send movement");
            getCachedMovement().put(UUID.randomUUID().toString(), reportType);
        }
    }

    public AcknowledgeTypeType setReport(ReportType report) {
        return AcknowledgeTypeType.NOK;
    }

    public AcknowledgeTypeType setCommand(CommandType command) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "{}.setCommand({})", getRegisterClassName(), command.getCommand().name());
            LOGGER.debug("timestamp: {}", command.getTimestamp());
        }
        PollType poll = command.getPoll();
        if (poll != null && CommandTypeType.POLL.equals(command.getCommand())) {
            String result;
            if (PollTypeType.POLL == poll.getPollTypeType()) {
                try {
                    result = sendPoll(poll, getPollPath(), getSetting("URL"), getSetting("PORT"), getSetting("USERNAME"), getSetting("PSW"), getSetting("DNIDS"));
                    LOGGER.debug("POLL returns: {}", result);
                    // Register Not acknowledge response
                    InmarsatPendingResponse ipr = getInmPendingResponse(poll, result);

                    addPendingPollResponse(ipr);

                    // Send status update to exchange
                    sentStatusToExchange(ipr);
                } catch (TelnetException e) {
                    LOGGER.error("Error while sending poll: {}", e.getMessage());
                    return AcknowledgeTypeType.NOK;
                }
            } else if (PollTypeType.CONFIG == poll.getPollTypeType()) {
                // TODO - Should this be removed?
            }
        }
        return AcknowledgeTypeType.NOK;
    }

    private InmarsatPendingResponse getInmPendingResponse(PollType poll, String result) {
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
        osat.setPollId(ipr.getMsgId());
        osat.setStatus(ExchangeLogStatusTypeType.PENDING);

        ackType.setPollStatus(osat);
        ackType.setType(AcknowledgeTypeType.OK);

        try {
            String s =
                    ExchangePluginResponseMapper.mapToSetPollStatusToSuccessfulResponse(
                            getApplicaionName(), ackType, ipr.getMsgId());
            messageProducer.sendModuleMessage(s, null);
            LOGGER.debug("Poll response {} sent to exchange with status: {}", ipr.getMsgId(), ExchangeLogStatusTypeType.PENDING);
        } catch (ExchangeModelMarshallException ex) {
            LOGGER.debug("ExchangeModelMarshallException", ex);
        } catch (MessageException jex) {
            LOGGER.debug("JMSException", jex);
        }
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

    private void loadPendingPollResponse() {
        pending = new ArrayList<>();
        File f = new File(pollPath, FILE_NAME);
        try (FileInputStream fis = new FileInputStream(f);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            //noinspection unchecked
            pending = (ArrayList<InmarsatPendingResponse>) ois.readObject();
            if (pending != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Read {} pending responses", pending.size());
                    for (InmarsatPendingResponse element : pending) {
                        LOGGER.debug("Refnr: {}", element.getReferenceNumber());
                    }
                }
            } else {
                pending = new ArrayList<>();
            }
        } catch (IOException ex) {
            LOGGER.debug("IOExeption:", ex);
        } catch (ClassNotFoundException ex) {
            LOGGER.debug("ClassNotFoundException", ex);
        }
    }

    private void addPendingPollResponse(InmarsatPendingResponse resp) {

        if (pending != null) {
            pending.add(resp);
            LOGGER.debug("Pending response added");
        }
    }

    private boolean removePendingPollResponse(InmarsatPendingResponse resp) {
        if (pending != null) {
            LOGGER.debug("Trying to remove pending poll response");
            return pending.remove(resp);
        }
        return false;
    }

    private List<InmarsatPendingResponse> getPendingPollResponses() {
        //noinspection unchecked
        return (ArrayList<InmarsatPendingResponse>) pending.clone();
    }

    private boolean containsPendingPollResponse(InmarsatPendingResponse resp) {
        return pending != null && pending.contains(resp);
    }

    private InmarsatPendingResponse continsPollTo(String dnid, String memberId) {
        for (InmarsatPendingResponse element : pending) {
            if (element.getDnId().equalsIgnoreCase(dnid)
                    && element.getMembId().equalsIgnoreCase(memberId)) {
                return element;
            }
        }
        return null;
    }


    private void writePendingPollResponse() {
        File f = new File(pollPath, FILE_NAME);

        try (FileOutputStream fos = new FileOutputStream(f, false);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(pending);
            oos.flush();
            LOGGER.debug("Wrote {} pending responses", pending.size());
        } catch (IOException ex) {
            LOGGER.debug("IOExeption", ex);
        }
    }


}
