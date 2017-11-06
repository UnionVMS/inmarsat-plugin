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
package eu.europa.ec.fisheries.uvms.plugins.inmarsat.twostage;

import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.KeyValueType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollTypeType;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmPoll;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.StartupBean;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.service.PluginService;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
@Singleton
@Startup
@DependsOn({"StartupBean"})
public class RetriverBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(RetriverBean.class);
  private final Map<String, Future> connectFutures = new HashMap<>();
  @EJB DownLoadService downloadService;
  @EJB StartupBean startUp;
  @EJB PollService pollService;
  @EJB private DownLoadCacheDeliveryBean deliveryBean;
  @EJB private PluginService pluginService;
  private Future deliverFuture = null;
  private String cachePath = null;
  private String pollPath = null;

  @PostConstruct
  public void startup() {
    createDirectories();
  }

  private String getCachePath() {
    return cachePath;
  }

  public String getPollPath() {
    return pollPath;
  }

  /*
      [2015-12-11 16:11] Joakim Johansson:
  SERIAL_NUMBER
  [2015-12-11 16:11] Joakim Johansson:
  MOBILE_TERMINAL_ID
  [2015-12-11 16:11] Joakim Johansson:
  CONNECT_ID
  [2015-12-11 16:12] Joakim Johansson:
  LES_SERVICE_NAME
  [2015-12-11 16:12] Joakim Johansson:
  LES_NAME

      */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void createDirectories() {
    File f = new File(startUp.getPLuginApplicationProperty("application.logfile"));
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
    if (startUp.isIsEnabled()) {
      List<String> dnids = getDownloadDnids();
      Future<Map<String, String>> future = downloadService.download(getCachePath(), dnids);

      for (String dnid : dnids) {
        connectFutures.put(dnid, future);
      }
    }
  }

  @Schedule(minute = "*/5", hour = "*", persistent = false)
  public void parseAndDeliver() {
    /* try{
       pollTest();
    } catch (DatatypeConfigurationException ex) {
        java.util.logging.Logger.getLogger(RetriverBean.class.getName()).log(Level.SEVERE, null, ex);
    }
    */
    if (startUp.isIsEnabled() && (deliverFuture == null || deliverFuture.isDone())) {
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
    command.setPluginName(startUp.getPLuginApplicationProperty("application.name"));

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
    LOGGER.info("Pollcommand: " + p.asCommand());
    //        pollService.sendPoll(poll,getPollPath());
    command.setPoll(poll);
    pluginService.setCommand(command);
  }

  /** @return returns DNIDs available for download */
  private List<String> getDownloadDnids() {
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

  /** @return list of DNIDs configured */
  private List<String> getDnids() {
    String dnidsSettingValue = startUp.getSetting("DNIDS");
    if (StringUtils.isBlank(dnidsSettingValue)) {
      return new ArrayList<>();
    }

    return Arrays.asList(dnidsSettingValue.trim().split(","));
  }
}
