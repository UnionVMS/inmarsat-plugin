package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.ReportType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.message.PluginMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@Startup
@Singleton
public class InmarsatPluginMock extends PluginDataHolder implements InmarsatPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatPluginMock.class);
    private static final int MAX_NUMBER_OF_TRIES = 20;
    private final Map<String, Future> connectFutures = new HashMap<>();



    @Inject
    private PluginMessageProducer messageProducer;

    @Inject
    private InmarsatConnection connect ;



    boolean isEnabled = false;
    boolean isRegistered = false;
    boolean waiting = false;
    private int numberOfTriesExecuted = 0;
    private String registerClassName = null;



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
        LOGGER.info("updateSettings " + settings.toString());

    }

    @PostConstruct
    public void startup() {
        LOGGER.info("startup ");

    }

    @PreDestroy
    public void shutdown() {
        unregister();
    }


    @Override
    public String getPluginResponseSubscriptionName() {
        LOGGER.info("");

        return null;
    }

    public String getResponseTopicMessageName() {
        LOGGER.info("getPluginResponseSubscriptionName");

        return null;
    }

    @Override
    public String getRegisterClassName() {
        return registerClassName;
    }

    @Override
    public AcknowledgeTypeType setReport(ReportType report) {
        return null;
    }

    @Override
    public AcknowledgeTypeType setCommand(CommandType command) {
        return null;
    }

    @Override
    public AcknowledgeTypeType setConfig(SettingListType settings) {
        return null;
    }

    @Override
    public AcknowledgeTypeType start() {
        return null;
    }

    @Override
    public AcknowledgeTypeType stop() {
        return null;
    }

    public String getApplicaionName() {
        return "registerClassName";
    }

    public String getPLuginApplicationProperty(String key) {
        LOGGER.info("getPLuginApplicationProperty");

        return null;
    }

    public String getCachePath() {
        LOGGER.info("getCachePath");

        return null;
    }

    public String getPollPath() {

        LOGGER.info("getPollPath");

        return null;
    }

    public void createDirectories() {
        LOGGER.info("createDirectories");
    }


    public void pollTest() {
        LOGGER.info("pollTest");

    }

    public List<String> getDownloadDnids() {
        List<String> downloadDnids = new ArrayList<>();
        downloadDnids.add("DNID-123");
        downloadDnids.add("123456");
        downloadDnids.add("ABC");
        return downloadDnids;
    }

    public List<String> getDnids() {

        LOGGER.info("getDnids");


        return null;
    }

    public String sendPoll(PollType poll, String path, String url, String port, String username, String psw, String dnids) throws TelnetException {
        LOGGER.info("sendPoll");
        return "";
    }


    private void register() {
        LOGGER.info("Registering to Exchange Module");

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


}
