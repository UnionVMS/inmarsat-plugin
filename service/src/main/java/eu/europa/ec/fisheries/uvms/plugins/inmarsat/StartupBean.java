package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.movement.v1.SetReportMovementType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.exception.TelnetException;

import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Timer;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

@Local
public interface StartupBean {

    @Asynchronous
    Future<Map<String, String>> download(String path, List<String> dnids) ;
   String download(String path, String dnid) throws TelnetException;




        /**********************************************
         *  from PluginDataHolder                     *
         **********************************************/

    String getSetting(String setting);

    boolean isWaitingForResponse();

    void setWaitingForResponse(boolean waitingForResponse);

    boolean isIsRegistered();

    void setIsRegistered(boolean isRegistered);

    boolean isIsEnabled();

    void setIsEnabled(boolean isEnabled);

    void updateSettings(List<SettingType> settings);

    ConcurrentMap<String, String> getSettings();

    ConcurrentMap<String, String> getCapabilities();

    ConcurrentMap<String, SetReportMovementType> getCachedMovement();

    Properties getPluginApplicaitonProperties();

    void setPluginApplicaitonProperties(Properties twostageApplicaitonProperties);

    Properties getPluginProperties();

    void setPluginProperties(Properties twostageProperties);

    Properties getPluginCapabilities();

    void setPluginCapabilities(Properties twostageCapabilities);

    void startup();


    /**********************************************
     *  from StartupBean                          *
     **********************************************/

    void shutdown();

    void timeout(Timer timer);

    String getPluginResponseSubscriptionName();

    String getResponseTopicMessageName();

    String getRegisterClassName();

    String getApplicaionName();

    String getPLuginApplicationProperty(String key);


    /**********************************************
     *  from RetrieverBean                        *
     **********************************************/

     String getCachePath();

     String getPollPath();

     void createDirectories();

     void connectAndRetrive();

     void parseAndDeliver();

     void pollTest();

    List<String> getDownloadDnids();

    List<String> getDnids();




    String sendPoll(PollType poll, String path, String url, String port, String username, String psw, String dnids) throws TelnetException ;


}
