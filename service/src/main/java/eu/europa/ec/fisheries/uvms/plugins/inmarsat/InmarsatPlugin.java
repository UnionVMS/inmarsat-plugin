package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;

import javax.ejb.Local;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

@Local
public interface InmarsatPlugin {


    String getPluginResponseSubscriptionName();
    void setWaitingForResponse(boolean waitingForResponse);
    void setIsRegistered(boolean isRegistered);
    void updateSettings(List<SettingType> settings);
    String getSetting(String setting);
    ConcurrentMap<String, String> getSettings();
    void setIsEnabled(boolean isEnabled);
    boolean isIsEnabled();
    boolean isIsRegistered();
    boolean isWaitingForResponse();
    String getRegisterClassName();

}
