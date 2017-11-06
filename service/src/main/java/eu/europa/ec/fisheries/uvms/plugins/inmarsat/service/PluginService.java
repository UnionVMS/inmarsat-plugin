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
package eu.europa.ec.fisheries.uvms.plugins.inmarsat.service;

import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.KeyValueType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.PollStatusAcknowledgeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.ReportType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollTypeType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingListType;
import eu.europa.ec.fisheries.schema.exchange.v1.ExchangeLogStatusTypeType;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmPendingResponse;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.StartupBean;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.constants.ModuleQueue;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.exception.TelnetException;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.producer.PluginMessageProducer;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.twostage.PluginPendingResponseList;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.twostage.PollService;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.twostage.RetriverBean;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.jms.JMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
@LocalBean
@Stateless
public class PluginService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PluginService.class);
  @EJB private StartupBean startupBean;
  @EJB private PollService pollService;
  @EJB private RetriverBean retriverBean;
  @EJB private PluginPendingResponseList responseList;
  @EJB private PluginMessageProducer pluginMessageProducer;

  /**
   * TODO implement
   *
   * @param report todo
   * @return NOK
   */
  public AcknowledgeTypeType setReport(ReportType report) {
    return AcknowledgeTypeType.NOK;
  }

  public AcknowledgeTypeType setCommand(CommandType command) {
    LOGGER.info(
        startupBean.getRegisterClassName() + ".setCommand(" + command.getCommand().name() + ")");
    LOGGER.debug("timestamp: " + command.getTimestamp());
    PollType poll = command.getPoll();
    if (poll != null && CommandTypeType.POLL.equals(command.getCommand())) {
      String result;
      if (PollTypeType.POLL == poll.getPollTypeType()) {
        try {
          result = pollService.sendPoll(poll, retriverBean.getPollPath());
          LOGGER.debug("POLL returns: " + result);
          // Register Not acknowledge response

          // Register response as pending
          InmPendingResponse ipr = new InmPendingResponse();
          ipr.setPollType(poll);
          ipr.setMsgId(poll.getPollId());
          ipr.setReferenceNumber(Integer.parseInt(result));
          List<KeyValueType> pollReciver = poll.getPollReceiver();
          for (KeyValueType element : pollReciver) {
            if (element.getKey().equalsIgnoreCase("MOBILE_TERMINAL_ID")) {
              ipr.setMobTermId(element.getValue());
            } else if (element.getKey().equalsIgnoreCase("DNID")) {
              ipr.setDnId(element.getValue());
            } else if (element.getKey().equalsIgnoreCase("MEMBER_NUMBER")) {
              ipr.setMembId(element.getValue());
            }
          }
          ipr.setStatus(InmPendingResponse.StatusType.PENDING);
          responseList.addPendingPollResponse(ipr);

          // Send status update to exchange
          sentStatusToExchange(ipr);
        } catch (TelnetException e) {
          LOGGER.error("Error while sending poll: {}", e.getMessage());
          return AcknowledgeTypeType.NOK;
        }
      } else if (PollTypeType.CONFIG == poll.getPollTypeType()) {
        // TODO - Should this be removed?
      }
    }
    return AcknowledgeTypeType.NOK;
  }

  private void sentStatusToExchange(InmPendingResponse ipr) {

    AcknowledgeType ackType = new AcknowledgeType();
    ackType.setMessage("");
    ackType.setMessageId(ipr.getMsgId());

    PollStatusAcknowledgeType osat = new PollStatusAcknowledgeType();
    osat.setPollId(ipr.getMsgId());
    osat.setStatus(ExchangeLogStatusTypeType.PENDING);

    ackType.setPollStatus(osat);
    ackType.setType(AcknowledgeTypeType.OK);

    String s;
    try {
      s =
          ExchangePluginResponseMapper.mapToSetPollStatusToSuccessfulResponse(
              startupBean.getApplicaionName(), ackType, ipr.getMsgId());
      pluginMessageProducer.sendModuleMessage(s, ModuleQueue.EXCHANGE);
      LOGGER.debug(
          "Poll response "
              + ipr.getMsgId()
              + " sent to exchange with status: "
              + ExchangeLogStatusTypeType.PENDING.value());
    } catch (ExchangeModelMarshallException ex) {
      LOGGER.debug("ExchangeModelMarshallException", ex);
    } catch (JMSException jex) {
      LOGGER.debug("JMSException", jex);
    }
  }

  /**
   * Set the config values for the twostage
   *
   * @param settings the settings
   * @return AcknowledgeTypeType
   */
  public AcknowledgeTypeType setConfig(SettingListType settings) {
    LOGGER.info(startupBean.getRegisterClassName() + ".setConfig()");
    try {
      for (KeyValueType values : settings.getSetting()) {
        LOGGER.debug("Setting [ " + values.getKey() + " : " + values.getValue() + " ]");
        startupBean.getSettings().put(values.getKey(), values.getValue());
      }
      return AcknowledgeTypeType.OK;
    } catch (Exception e) {
      LOGGER.error("Failed to set config in {}", startupBean.getRegisterClassName());
      return AcknowledgeTypeType.NOK;
    }
  }

  /**
   * Start the twostage. Use this to enable functionality in the twostage
   *
   * @return AcknowledgeTypeType
   */
  public AcknowledgeTypeType start() {
    LOGGER.info(startupBean.getRegisterClassName() + ".start()");
    try {
      startupBean.setIsEnabled(Boolean.TRUE);
      return AcknowledgeTypeType.OK;
    } catch (Exception e) {
      startupBean.setIsEnabled(Boolean.FALSE);
      LOGGER.error("Failed to start {}", startupBean.getRegisterClassName());
      return AcknowledgeTypeType.NOK;
    }
  }

  /**
   * Start the twostage. Use this to disable functionality in the twostage
   *
   * @return AcknowledgeTypeType
   */
  public AcknowledgeTypeType stop() {
    LOGGER.info(startupBean.getRegisterClassName() + ".stop()");
    try {
      startupBean.setIsEnabled(Boolean.FALSE);
      return AcknowledgeTypeType.OK;
    } catch (Exception e) {
      startupBean.setIsEnabled(Boolean.TRUE);
      LOGGER.error("Failed to stop {}", startupBean.getRegisterClassName());
      return AcknowledgeTypeType.NOK;
    }
  }
}
