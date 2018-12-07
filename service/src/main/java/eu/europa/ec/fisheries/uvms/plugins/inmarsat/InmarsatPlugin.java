package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.KeyValueType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.ReportType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;

import javax.ejb.Local;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/** Main reason for having Interface is to be able to Mock it away so we can write test code
 *  There is also a class (PluginAckEventBusListener) left that actually inject it and run some method aganist it
 *  Otherwise it is ment to be a selfgoing watch (sort of . . . .)
 *
 */

@Local
public interface InmarsatPlugin {


    String getPluginResponseSubscriptionName();
    void setIsRegistered(boolean isRegistered);
    void updateSettings(List<SettingType> settings);
    String getSetting(String setting);
    ConcurrentMap<String, String> getSettings();
    void setIsEnabled(boolean isEnabled);
    boolean isIsEnabled();
    boolean isIsRegistered();
    String getRegisterClassName();


    AcknowledgeTypeType setReport(ReportType report);
    AcknowledgeTypeType setCommand(CommandType command);
    AcknowledgeTypeType setConfig(SettingListType settings) ;
    AcknowledgeTypeType start() ;
    AcknowledgeTypeType stop() ;





}
