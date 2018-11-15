package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.common.v1.*;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollTypeType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.CapabilityListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.ServiceType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import eu.europa.ec.fisheries.schema.exchange.v1.ExchangeLogStatusTypeType;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.message.PluginMessageProducer;
import org.apache.commons.lang3.StringUtils;
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
import java.util.concurrent.Future;

@Startup
@Singleton
public class InmarsatPluginMock extends PluginDataHolder implements InmarsatPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatPluginMock.class);
    private static final int MAX_NUMBER_OF_TRIES = 20;
    private static final String FILE_NAME = "pending.ser";
    private ArrayList<InmarsatPendingResponse> pending;
    private String pollPath = null;
    private Future deliverFuture = null;
    private String cachePath = null;

    private final Map<String, Future> connectFutures = new HashMap<>();

    @Inject
    private InmarsatPlugin startupService;

    @Inject
    private PluginMessageProducer messageProducer;

    @Inject
    private InmarsatConnection connect ;

    boolean isEnabled = false;
    boolean isRegistered = false;
    boolean waiting = false;
    private int numberOfTriesExecuted = 0;
    private String registerClassName = null;
    private CapabilityListType capabilityList;
    private SettingListType settingList;
    private ServiceType serviceType;

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

        serviceType = ServiceMapper.getServiceType(getRegisterClassName(),"Thrane&Thrane","inmarsat plugin for the Thrane&Thrane API", PluginType.SATELLITE_RECEIVER,getPluginResponseSubscriptionName(),"INMARSAT_C");
        register();
        LOGGER.debug("Settings updated in plugin {}", registerClassName);
        for (Map.Entry<String, String> entry : super.getSettings().entrySet()) {
            LOGGER.debug("Setting: KEY: {} , VALUE: {}", entry.getKey(), entry.getValue());
        }

        loadPendingPollResponse();
        LOGGER.info("PLUGIN STARTED");
    }


    /* OBS THE TIMERS ARE MUCH MORE FREQUENT THAN IN THE REAL PLUGIN */

    @Schedule(second = "*/3", minute = "*", hour = "*", persistent = false)
    public void connectAndRetrive() {
        if (isIsEnabled()) {
            List<String> dnids = getDownloadDnids();
            Future<Map<String, String>> future = download(getCachePath(), dnids);
            // is null
        }
    }

    @Schedule(second = "*/2", minute = "*", hour = "*", persistent = false)
    public void parseAndDeliver() {
        LOGGER.info("parseAndDeliver");

    }

    @Schedule(second = "*/1", minute = "*", hour = "*", persistent = false)
    public void timeout(Timer timer) {
        if (!isRegistered && numberOfTriesExecuted < MAX_NUMBER_OF_TRIES) {
            LOGGER.info(getRegisterClassName() + " is not registered, trying to register");
            register();
            numberOfTriesExecuted++;
        }
        if (isRegistered) {
            timer.cancel();
        } else if (numberOfTriesExecuted >= MAX_NUMBER_OF_TRIES) {
        }
    }

    public Future<Map<String, String>> download(String path, List<String> dnids) {
        return null;
    }

    public String download(String path, String dnid) throws TelnetException {
        return null;
    }

    @Override
    public String getSetting(String setting) {
        return super.getSettings().get(setting);
    }

    @Override
    public boolean isWaitingForResponse() {
        return waiting;
    }

    @Override
    public void setWaitingForResponse(boolean waitingForResponse) {
        this.waiting = waitingForResponse;
    }


    @Override
    public boolean isIsRegistered() {
        return isRegistered;
    }

    @Override
    public void setIsRegistered(boolean isRegistered) {
        this.isRegistered = isRegistered;
    }

    @Override
    public boolean isIsEnabled() {
        return isEnabled;
    }

    @Override
    public void setIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;

    }

    @Override
    public void updateSettings(List<SettingType> settings) {
        for (SettingType setting : settings) {
            getSettings().put(setting.getKey(), setting.getValue());
        }
    }


    @PreDestroy
    public void shutdown() {
        unregister();
    }


    @Override
    public String getPluginResponseSubscriptionName() {
        return getRegisterClassName()+ "." + getPLuginApplicationProperty("application.responseTopicName");
    }

    private String getResponseTopicMessageName() {
        return getPLuginApplicationProperty("application.groupid")+ "."+ getPLuginApplicationProperty("application.name");
    }

    @Override
    public String getRegisterClassName() {
        return registerClassName;
    }

    @Override
    public AcknowledgeTypeType setReport(ReportType report) {
        return AcknowledgeTypeType.NOK;
    }

    @Override
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

    @Override
    public AcknowledgeTypeType setConfig(SettingListType settings) {
        return null;
    }

    @Override
    public AcknowledgeTypeType start() {
        try {
            setIsEnabled(Boolean.TRUE);
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            setIsEnabled(Boolean.FALSE);
            return AcknowledgeTypeType.NOK;
        }
    }

    @Override
    public AcknowledgeTypeType stop() {
        try {
            setIsEnabled(Boolean.FALSE);
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            setIsEnabled(Boolean.TRUE);
            return AcknowledgeTypeType.NOK;
        }
    }

    public String getApplicaionName() {
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

    private String getCachePath() {
        return cachePath;
    }

    private String getPollPath() {
        return pollPath;
    }


    public void createDirectories() {
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



    public List<String> getDownloadDnids() {
        List<String> downloadDnids = new ArrayList<>();
        downloadDnids.add("DNID-123");
        downloadDnids.add("123456");
        downloadDnids.add("ABC");
        return downloadDnids;
    }

    public List<String> getDnids() {
        String dnidsSettingValue = getSetting("DNIDS");
        if (StringUtils.isBlank(dnidsSettingValue)) {
            return new ArrayList<>();
        }
        return Arrays.asList(dnidsSettingValue.trim().split(","));
    }

    public String sendPoll(PollType poll, String path, String url, String port, String username, String psw, String dnids) throws TelnetException {
        LOGGER.info("sendPoll");
        return "";
    }


    private void register() {
        // simulate one fail;
        if (numberOfTriesExecuted < 2) {
            return;
        }
        setIsRegistered(true);
    }

    private void unregister() {
        LOGGER.info("Unregistering from Exchange Module");
        setIsRegistered(false);
        numberOfTriesExecuted = 0;
    }

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

    private void addPendingPollResponse(InmarsatPendingResponse resp) {

        if (pending != null) {
            pending.add(resp);
            LOGGER.debug("Pending response added");
        }
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
            messageProducer.sendModuleMessage(s, ModuleQueue.EXCHANGE);
            LOGGER.debug(
                    "Poll response {} sent to exchange with status: {}",
                    ipr.getMsgId(),
                    ExchangeLogStatusTypeType.PENDING);

        } catch (ExchangeModelMarshallException ex) {
            LOGGER.debug("ExchangeModelMarshallException", ex);
        } catch (JMSException jex) {
            LOGGER.debug("JMSException", jex);
        }
    }



}
