package eu.europa.ec.fisheries.uvms.plugins.inmarsat;


import eu.europa.ec.fisheries.schema.exchange.movement.v1.SetReportMovementType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Timer;

@Local
public interface StartupBean {


    void startup();

    void shutdown();

    void timeout(Timer timer);


    String getPluginResponseSubscriptionName();

    String getResponseTopicMessageName();

    String getRegisterClassName();

    String getApplicaionName();

    String getPLuginApplicationProperty(String key);

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

}





