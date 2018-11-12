package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;

import java.util.List;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;

@Startup
@Singleton
public class StartupMockImpl extends PluginDataHolder  implements StartupBean{

    public void startup() {

    }

    public void shutdown() {

    }

    public void timeout(Timer timer) {

    }

    public String getPluginResponseSubscriptionName() {
        return null;
    }

    public String getResponseTopicMessageName() {
        return null;
    }

    public String getRegisterClassName() {
        return null;
    }

    public String getApplicaionName() {
        return null;
    }

    public String getPLuginApplicationProperty(String key) {
        return null;
    }

    public String getSetting(String setting) {
        return null;
    }

    public boolean isWaitingForResponse() {
        return false;
    }

    public void setWaitingForResponse(boolean waitingForResponse) {

    }

    public boolean isIsRegistered() {
        return true;
    }

    public void setIsRegistered(boolean isRegistered) {

    }

    public boolean isIsEnabled() {
        return true;
    }

    public void setIsEnabled(boolean isEnabled) {

    }

    public void updateSettings(List<SettingType> settings) {

    }
}
