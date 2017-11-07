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
package eu.europa.ec.fisheries.uvms.plugins.inmarsat.mapper;

import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.CapabilityListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.CapabilityType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.CapabilityTypeType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.ServiceType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class ServiceMapper {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceMapper.class);

  private ServiceMapper() {
    throw new IllegalStateException("Utility class");
  }

  public static ServiceType getServiceType(
      String serviceClassName,
      String twostageDisplayName,
      String description,
      PluginType twostageType,
      String responseMessageName,
      String satelliteType) {

    if (responseMessageName == null) {
      throw new IllegalArgumentException("Response message must be provided!");
    }

    if (serviceClassName == null) {
      throw new IllegalArgumentException("ServiceClassName message must be provided!");
    }

    ServiceType serviceType = new ServiceType();
    serviceType.setDescription(description);
    serviceType.setName(twostageDisplayName);
    serviceType.setServiceClassName(serviceClassName);
    serviceType.setServiceResponseMessageName(responseMessageName);
    serviceType.setPluginType(twostageType);
    serviceType.setSatelliteType(satelliteType);
    return serviceType;
  }

  public static SettingListType getSettingsListTypeFromMap(ConcurrentMap<String, String> settings) {
    SettingListType settingListType = new SettingListType();
    for (Map.Entry<String, String> tmp : settings.entrySet()) {
      SettingType setting = new SettingType();
      setting.setKey(tmp.getKey());
      setting.setValue(tmp.getValue());
      settingListType.getSetting().add(setting);
    }
    return settingListType;
  }

  public static CapabilityListType getCapabilitiesListTypeFromMap(
      ConcurrentMap<String, String> capabilities) {
    CapabilityListType capabilityListType = new CapabilityListType();
    for (Map.Entry<String, String> tmp : capabilities.entrySet()) {
      CapabilityType setting = new CapabilityType();

      try {
        setting.setType(CapabilityTypeType.valueOf(tmp.getKey()));
      } catch (Exception e) {
        LOGGER.error("Error when parsing to Enum type from String KEY: {}", tmp.getKey());
      }

      setting.setValue(tmp.getValue());
      capabilityListType.getCapability().add(setting);
    }
    return capabilityListType;
  }

  public static void mapToMapFromProperties(
      ConcurrentMap<String, String> map, Properties props, String registerClassName) {

    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      if (entry.getKey().getClass().isAssignableFrom(String.class)) {
        String key = (String) entry.getKey();
        if (registerClassName != null) {
          key = registerClassName.concat("." + key);
        }
        map.put(key, (String) entry.getValue());
      }
    }
  }
}
