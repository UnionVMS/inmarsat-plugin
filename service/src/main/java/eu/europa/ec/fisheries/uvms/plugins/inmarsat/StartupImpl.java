package eu.europa.ec.fisheries.uvms.plugins.inmarsat;


import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.KeyValueType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollTypeType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.CapabilityListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.ServiceType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import eu.europa.ec.fisheries.uvms.exchange.model.constant.ExchangeModelConstants;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.exception.TelnetException;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.mapper.ServiceMapper;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.producer.PluginMessageProducer;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.service.PluginService;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.twostage.Connect;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.twostage.DownLoadCacheDeliveryBean;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;
import javax.ejb.Timer;
import javax.jms.JMSException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Future;

@Startup
@Singleton
public class StartupImpl extends PluginDataHolder implements StartupBean {


    private static final Logger LOGGER = LoggerFactory.getLogger(StartupImpl.class);

    private static final int MAX_NUMBER_OF_TRIES = 20;
    private boolean isRegistered = false;
    private boolean isEnabled = false;
    private boolean waitingForResponse = false;
    private int numberOfTriesExecuted = 0;
    private String registerClassName;

    @EJB
    private PluginMessageProducer messageProducer;

    @EJB
    private Connect connect;

    @EJB
    private DownLoadCacheDeliveryBean deliveryBean;

    @EJB
    private PluginService pluginService;


    private CapabilityListType capabilityList;
    private SettingListType settingList;
    private ServiceType serviceType;


    private final Map<String, Future> connectFutures = new HashMap<>();



    private Future deliverFuture = null;
    private String cachePath = null;
    private String pollPath = null;


    @PostConstruct
    public void startup() {
        createDirectories();

        // This must be loaded first!!! Not doing that will end in dire problems later on!
        super.setPluginApplicaitonProperties(getPropertiesFromFile(PluginDataHolder.PLUGIN_PROPERTIES));
        registerClassName = getPLuginApplicationProperty("application.groupid") + "." + getPLuginApplicationProperty("application.name");
        LOGGER.debug("Plugin will try to register as:{}", registerClassName);
        // These can be loaded in any order
        super.setPluginProperties(getPropertiesFromFile(PluginDataHolder.SETTINGS_PROPERTIES));
        super.setPluginCapabilities(getPropertiesFromFile(PluginDataHolder.CAPABILITIES_PROPERTIES));
        ServiceMapper.mapToMapFromProperties(super.getSettings(), super.getPluginProperties(), getRegisterClassName());
        ServiceMapper.mapToMapFromProperties(super.getCapabilities(), super.getPluginCapabilities(), null);

        capabilityList = ServiceMapper.getCapabilitiesListTypeFromMap(super.getCapabilities());
        settingList = ServiceMapper.getSettingsListTypeFromMap(super.getSettings());

        serviceType =
                ServiceMapper.getServiceType(getRegisterClassName(),"Thrane&Thrane","inmarsat plugin for the Thrane&Thrane API",PluginType.SATELLITE_RECEIVER,getPluginResponseSubscriptionName(),"INMARSAT_C");
        register();
        LOGGER.debug("Settings updated in plugin {}", registerClassName);
        for (Map.Entry<String, String> entry : super.getSettings().entrySet()) {
            LOGGER.debug("Setting: KEY: {} , VALUE: {}", entry.getKey(), entry.getValue());
        }

        LOGGER.info("PLUGIN STARTED");


    }


    @PreDestroy
    public void shutdown() {
        unregister();
    }

    @Schedule(second = "*/10", minute = "*", hour = "*", persistent = false)
    public void timeout(Timer timer) {
        if (!isRegistered && numberOfTriesExecuted < MAX_NUMBER_OF_TRIES) {
            LOGGER.info(getRegisterClassName() + " is not registered, trying to register");
            register();
            numberOfTriesExecuted++;
        }
        if (isRegistered) {
            LOGGER.info(getRegisterClassName() + " is registered. Cancelling timer.");
            timer.cancel();
        } else if (numberOfTriesExecuted >= MAX_NUMBER_OF_TRIES) {
            LOGGER.info( getRegisterClassName() + " failed to register, maximum number of retries reached.");
        }
    }

    private void register() {
        LOGGER.info("Registering to Exchange Module");
        setWaitingForResponse(true);
        try {
            String registerServiceRequest = ExchangeModuleRequestMapper.createRegisterServiceRequest(serviceType, capabilityList, settingList);
            messageProducer.sendEventBusMessage(registerServiceRequest, ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
        } catch (JMSException | ExchangeModelMarshallException e) {
            LOGGER.error("Failed to send registration message to {}",ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
            setWaitingForResponse(false);
        }
    }

    private void unregister() {
        LOGGER.info("Unregistering from Exchange Module");
        try {
            String unregisterServiceRequest =ExchangeModuleRequestMapper.createUnregisterServiceRequest(serviceType);
            messageProducer.sendEventBusMessage(unregisterServiceRequest, ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
        } catch (JMSException | ExchangeModelMarshallException e) {
            LOGGER.error("Failed to send unregistration message to {}",ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
        }
    }

    public String getPluginResponseSubscriptionName() {
        return getRegisterClassName()+ "." + getPLuginApplicationProperty("application.responseTopicName");
    }

    public String getResponseTopicMessageName() {
        return getPLuginApplicationProperty("application.groupid")+ "."+ getPLuginApplicationProperty("application.name");
    }

    public String getRegisterClassName() {
        return registerClassName;
    }

    public String getApplicaionName() {
        return getPLuginApplicationProperty("application.name");
    }

    public String getPLuginApplicationProperty(String key) {
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


    public String getCachePath() {
        return cachePath;
    }

    public String getPollPath() {
        return pollPath;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
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

    @Schedule(minute = "*/3", hour = "*", persistent = false)
    public void connectAndRetrive() {
        if (isIsEnabled()) {
            List<String> dnids = getDownloadDnids();
            Future<Map<String, String>> future = download(getCachePath(), dnids);

            for (String dnid : dnids) {
                connectFutures.put(dnid, future);
            }
        }
    }

    @Schedule(minute = "*/5", hour = "*", persistent = false)
    public void parseAndDeliver() {
        if (isIsEnabled() && (deliverFuture == null || deliverFuture.isDone())) {
            try {
                deliverFuture = deliveryBean.parseAndDeliver(getCachePath());
            } catch (IOException e) {
                LOGGER.error("Couldn't deliver ");
            }
        } else {
            LOGGER.debug("deliverFuture is not null and busy");
        }
    }

    public void pollTest() {
        CommandType command = new CommandType();
        command.setCommand(CommandTypeType.POLL);
        command.setPluginName(getPLuginApplicationProperty("application.name"));

        command.setTimestamp(new Date());

        PollType poll = new PollType();
        poll.setPollId("123");
        poll.setPollTypeType(PollTypeType.POLL);
        KeyValueType kv = new KeyValueType();
        kv.setKey("DNID");
        kv.setValue("10745");
        poll.getPollReceiver().add(kv);

        KeyValueType kv1 = new KeyValueType();
        kv1.setKey("MEMBER_NUMBER");
        kv1.setValue("255");
        poll.getPollReceiver().add(kv1);

        KeyValueType kv2 = new KeyValueType();
        kv2.setKey("SERIAL_NUMBER");
        kv2.setValue("426509712");
        poll.getPollReceiver().add(kv2);

        InmPoll p = new InmPoll();
        p.setPollType(poll);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Pollcommand: {} ", p.asCommand());
        }
        command.setPoll(poll);
        pluginService.setCommand(command);
    }

    /**
     * @return returns DNIDs available for download
     */
    public List<String> getDownloadDnids() {
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
    public List<String> getDnids() {
        String dnidsSettingValue = getSetting("DNIDS");
        if (StringUtils.isBlank(dnidsSettingValue)) {
            return new ArrayList<>();
        }

        return Arrays.asList(dnidsSettingValue.trim().split(","));
    }


    // from filehandlerbean
    public Properties getPropertiesFromFile(String fileName) {
        Properties props = new Properties();
        try {
            InputStream inputStream =
                    StartupImpl.class.getClassLoader().getResourceAsStream(fileName);
            props.load(inputStream);
        } catch (IOException e) {
            LOGGER.debug("Properties file failed to load");
        }
        return props;
    }



    @Asynchronous
    public Future<Map<String, String>> download(String path, List<String> dnids) {
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

    public String download(String path, String dnid) throws TelnetException {
        LOGGER.debug("Download invoked with DNID = {}", dnid);
        return connect.connect(null,path, getSetting("URL"),getSetting("PORT"),getSetting("USERNAME"),getSetting("PSW"),dnid);
    }



    public String sendPoll(PollType poll, String path, String url, String port, String username, String psw, String dnids) throws TelnetException {
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

    public String sendConfigurationPoll(PollType poll) throws TelnetException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // Extract refnr from LES response
    private String parseResponse(String response) {
        String s = response.substring(response.indexOf("number"));
        return s.replaceAll("[^0-9]", ""); // returns 123
    }







}
