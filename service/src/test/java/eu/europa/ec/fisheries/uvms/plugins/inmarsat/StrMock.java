package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.exception.TelnetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import javax.ejb.Singleton;
import javax.ejb.Timer;

@javax.ejb.Startup
@Singleton
public class StrMock extends PluginDataHolder  implements StartupBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrMock.class);

     List<String>  dnids = Arrays.asList("DNID-123", "123456", "ABC");


    @Override
       public  Future<Map<String, String>> download(String path, List<String> dnids) {
        return null;
    }

    @Override
    public String download(String path, String dnid) throws TelnetException {
        return null;
    }

    @Override
    public String getSetting(String setting) {

        LOGGER.info("getSetting " + setting);
        return "ASETTING";
    }

    @Override
    public boolean isWaitingForResponse() {

        LOGGER.info("isWaitingForResponse");
        return false;
    }

    @Override
    public void setWaitingForResponse(boolean waitingForResponse) {

        LOGGER.info("setWaitingForResponse " + waitingForResponse);
        return ;
    }


    @Override
    public boolean isIsRegistered() {

        LOGGER.info("isIsRegistered");
        return false;
    }

    @Override
    public void setIsRegistered(boolean isRegistered) {
        LOGGER.info("setIsRegistered " + isRegistered);

    }

    @Override
    public boolean isIsEnabled() {
        LOGGER.info("isIsEnabled");

        return false;
    }

    @Override
    public void setIsEnabled(boolean isEnabled) {
        LOGGER.info("setIsEnabled " + isEnabled);

    }

    @Override
    public void updateSettings(List<SettingType> settings) {
        LOGGER.info("updateSettings " +  settings.toString());

    }

    @Override
    public void startup() {
       LOGGER.info("startup "  );

    }

    @Override
    public void shutdown() {
        LOGGER.info("startup");

    }

    @Override
    public void timeout(Timer timer) {
        LOGGER.info("timeout");

    }

    @Override
    public String getPluginResponseSubscriptionName() {
        LOGGER.info("");

        return null;
    }

    @Override
    public String getResponseTopicMessageName() {
        LOGGER.info("getPluginResponseSubscriptionName");

        return null;
    }

    @Override
    public String getRegisterClassName() {

        LOGGER.info("getRegisterClassName");

        return null;
    }

    @Override
    public String getApplicaionName() {
        LOGGER.info("getApplicaionName");

        return null;
    }

    @Override
    public String getPLuginApplicationProperty(String key) {
        LOGGER.info("getPLuginApplicationProperty");

        return null;
    }

    @Override
    public String getCachePath() {
        LOGGER.info("getCachePath");

        return null;
    }

    @Override
    public String getPollPath() {

        LOGGER.info("getPollPath");

        return null;
    }

    @Override
    public void createDirectories() {
        LOGGER.info("createDirectories");


    }

    @Override
    public void connectAndRetrive() {
        LOGGER.info("connectAndRetrive");

    }

    @Override
    public void parseAndDeliver() {
        LOGGER.info("parseAndDeliver");

    }

    @Override
    public void pollTest() {
        LOGGER.info("pollTest");

    }

    @Override
    public List<String> getDownloadDnids() {

        LOGGER.info("getDownloadDnids");


        return null;
    }

    @Override
    public List<String> getDnids() {

        LOGGER.info("getDnids");


        return null;
    }

    @Override
    public String sendPoll(PollType poll, String path, String url, String port, String username, String psw, String dnids) throws TelnetException {
        LOGGER.info("sendPoll");
        return "";
    }
}
