/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.CapabilityListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.ServiceType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import eu.europa.ec.fisheries.uvms.exchange.model.constant.ExchangeModelConstants;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.mapper.ServiceMapper;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.producer.PluginMessageProducer;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.service.FileHandlerBean;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.jms.JMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Startup
public class StartupBean extends PluginDataHolder {

  private static final Logger LOGGER = LoggerFactory.getLogger(StartupBean.class);

  private static final int MAX_NUMBER_OF_TRIES = 20;
  private boolean isRegistered = false;
  private boolean isEnabled = false;
  private boolean waitingForResponse = false;
  private int numberOfTriesExecuted = 0;
  private String registerClassName;

  @EJB private PluginMessageProducer messageProducer;

  @EJB private FileHandlerBean fileHandler;

  private CapabilityListType capabilityList;
  private SettingListType settingList;
  private ServiceType serviceType;

  @PostConstruct
  public void startup() {

    // This must be loaded first!!! Not doing that will end in dire problems later on!
    super.setPluginApplicaitonProperties(
        fileHandler.getPropertiesFromFile(PluginDataHolder.PLUGIN_PROPERTIES));
    registerClassName =
        getPLuginApplicationProperty("application.groupid")
            + "."
            + getPLuginApplicationProperty("application.name");

    LOGGER.debug("Plugin will try to register as:{}", registerClassName);
    // These can be loaded in any order
    super.setPluginProperties(
        fileHandler.getPropertiesFromFile(PluginDataHolder.SETTINGS_PROPERTIES));
    super.setPluginCapabilities(
        fileHandler.getPropertiesFromFile(PluginDataHolder.CAPABILITIES_PROPERTIES));

    ServiceMapper.mapToMapFromProperties(
        super.getSettings(), super.getPluginProperties(), getRegisterClassName());
    ServiceMapper.mapToMapFromProperties(
        super.getCapabilities(), super.getPluginCapabilities(), null);

    capabilityList = ServiceMapper.getCapabilitiesListTypeFromMap(super.getCapabilities());
    settingList = ServiceMapper.getSettingsListTypeFromMap(super.getSettings());

    serviceType =
        ServiceMapper.getServiceType(
            getRegisterClassName(),
            "Thrane&Thrane",
            "inmarsat plugin for the Thrane&Thrane API",
            PluginType.SATELLITE_RECEIVER,
            getPluginResponseSubscriptionName(),
            "INMARSAT_C");

    register();

    LOGGER.debug("Settings updated in plugin {}", registerClassName);
    for (Entry<String, String> entry : super.getSettings().entrySet()) {
      LOGGER.debug("Setting: KEY: {} , VALUE: {}", entry.getKey(), entry.getValue());
    }

    LOGGER.info("PLUGIN STARTED");
  }

  @PreDestroy
  public void shutdown() {
    unregister();
  }

  @Schedule(second = "*/5", minute = "*", hour = "*", persistent = false)
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
      LOGGER.info(
          getRegisterClassName() + " failed to register, maximum number of retries reached.");
    }
  }

  private void register() {
    LOGGER.info("Registering to Exchange Module");
    setWaitingForResponse(true);
    try {
      String registerServiceRequest =
          ExchangeModuleRequestMapper.createRegisterServiceRequest(
              serviceType, capabilityList, settingList);
      messageProducer.sendEventBusMessage(
          registerServiceRequest, ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
    } catch (JMSException | ExchangeModelMarshallException e) {
      LOGGER.error(
          "Failed to send registration message to {}",
          ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
      setWaitingForResponse(false);
    }
  }

  private void unregister() {
    LOGGER.info("Unregistering from Exchange Module");
    try {
      String unregisterServiceRequest =
          ExchangeModuleRequestMapper.createUnregisterServiceRequest(serviceType);
      messageProducer.sendEventBusMessage(
          unregisterServiceRequest, ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
    } catch (JMSException | ExchangeModelMarshallException e) {
      LOGGER.error(
          "Failed to send unregistration message to {}",
          ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
    }
  }

  public String getPluginResponseSubscriptionName() {
    return getRegisterClassName()
        + "."
        + getPLuginApplicationProperty("application.responseTopicName");
  }

  public String getResponseTopicMessageName() {
    return getPLuginApplicationProperty("application.groupid")
        + "."
        + getPLuginApplicationProperty("application.name");
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
}
