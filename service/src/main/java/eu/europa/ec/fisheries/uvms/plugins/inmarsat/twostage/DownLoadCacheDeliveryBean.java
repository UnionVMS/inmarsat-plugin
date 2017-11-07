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

import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.PollStatusAcknowledgeType;
import eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.IdList;
import eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.IdType;
import eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.MobileTerminalId;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementComChannelType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementPoint;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementTypeType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.SetReportMovementType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.schema.exchange.v1.ExchangeLogStatusTypeType;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmPendingResponse;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.StartupBean;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.constants.ModuleQueue;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.producer.PluginMessageProducer;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.service.ExchangeService;
import fish.focus.uvms.commons.les.inmarsat.InmarsatException;
import fish.focus.uvms.commons.les.inmarsat.InmarsatFileHandler;
import fish.focus.uvms.commons.les.inmarsat.InmarsatMessage;
import fish.focus.uvms.commons.les.inmarsat.header.body.PositionReport;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.jms.JMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
@Singleton
public class DownLoadCacheDeliveryBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(DownLoadCacheDeliveryBean.class);
  @EJB private PluginPendingResponseList pendingPollResponsList;
  @EJB private PluginMessageProducer pluginMessageProducer;
  @EJB private ExchangeService service;
  @EJB private StartupBean startBean;

  /**
   * @param path the directory to parse inmarsat files
   * @return future
   * @throws IOException @see {@link InmarsatFileHandler#createMessages()}
   */
  @Asynchronous
  public Future<String> parseAndDeliver(String path) throws IOException {

    InmarsatFileHandler fileHandler = new InmarsatFileHandler(Paths.get(path));
    Map<Path, InmarsatMessage[]> messagesFromPath = fileHandler.createMessages();

    for (Map.Entry<Path, InmarsatMessage[]> entry : messagesFromPath.entrySet()) {
      // Send message to Que
      for (InmarsatMessage message : entry.getValue()) {
        try {
          msgToQue(message);
        } catch (InmarsatException e) {
          LOGGER.error("Could not send to msg que for message: {}", message, e);
        }
      }
      // Send ok so move file
      LOGGER.info("File: {} processed and moved to handled", entry.getKey());
      fileHandler.moveFileToDir(
          entry.getKey(), Paths.get(entry.getKey().getParent().toString(), "handled"));
    }
    return new AsyncResult<>("Done");
  }

  /**
   * @param msg inmarsat message to send
   * @throws InmarsatException if stored date could be set.
   */
  private void msgToQue(InmarsatMessage msg) throws InmarsatException {
    MovementBaseType movement = new MovementBaseType();

    movement.setComChannelType(MovementComChannelType.MOBILE_TERMINAL);
    MobileTerminalId mobTermId = new MobileTerminalId();

    IdList dnidId = new IdList();
    dnidId.setType(IdType.DNID);
    dnidId.setValue(Integer.toString(msg.getHeader().getDnid()));

    IdList membId = new IdList();
    membId.setType(IdType.MEMBER_NUMBER);
    membId.setValue(Integer.toString(msg.getHeader().getMemberNo()));

    mobTermId.getMobileTerminalIdList().add(dnidId);
    mobTermId.getMobileTerminalIdList().add(membId);

    movement.setMobileTerminalId(mobTermId);

    movement.setMovementType(MovementTypeType.POS);

    MovementPoint mp = new MovementPoint();
    mp.setAltitude(0.0);
    mp.setLatitude(((PositionReport) msg.getBody()).getLatitude().getAsDouble());
    mp.setLongitude(((PositionReport) msg.getBody()).getLongitude().getAsDouble());
    movement.setPosition(mp);

    movement.setPositionTime(((PositionReport) msg.getBody()).getPositionDate().getDate());

    movement.setReportedCourse((double) ((PositionReport) msg.getBody()).getCourse());

    movement.setReportedSpeed(((PositionReport) msg.getBody()).getSpeed());

    movement.setSource(MovementSourceType.INMARSAT_C);

    movement.setStatus(Integer.toString(((PositionReport) msg.getBody()).getMacroEncodedMessage()));

    SetReportMovementType reportType = new SetReportMovementType();
    reportType.setMovement(movement);
    GregorianCalendar gcal =
        (GregorianCalendar) GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
    reportType.setTimestamp(gcal.getTime());
    reportType.setPluginName(startBean.getRegisterClassName());
    reportType.setPluginType(PluginType.SATELLITE_RECEIVER);

    service.sendMovementReportToExchange(reportType);

    // If there is a pending poll response, also generate a status update for that poll
    InmPendingResponse ipr =
        pendingPollResponsList.continsPollTo(dnidId.getValue(), membId.getValue());
    if (ipr != null) {
      LOGGER.info("PendingPollResponse found in list: " + ipr.getReferenceNumber());
      AcknowledgeType ackType = new AcknowledgeType();
      ackType.setMessage("");
      ackType.setMessageId(ipr.getMsgId());

      PollStatusAcknowledgeType osat = new PollStatusAcknowledgeType();
      osat.setPollId(ipr.getMsgId());
      osat.setStatus(ExchangeLogStatusTypeType.SUCCESSFUL);

      ackType.setPollStatus(osat);
      ackType.setType(AcknowledgeTypeType.OK);

      String s;
      try {
        s =
            ExchangePluginResponseMapper.mapToSetPollStatusToSuccessfulResponse(
                startBean.getApplicaionName(), ackType, ipr.getMsgId());
        pluginMessageProducer.sendModuleMessage(s, ModuleQueue.EXCHANGE);
        boolean b = pendingPollResponsList.removePendingPollResponse(ipr);
        LOGGER.debug("Pending poll response removed: {}", b);
      } catch (ExchangeModelMarshallException ex) {
        LOGGER.debug("ExchangeModelMarshallException", ex);
      } catch (JMSException jex) {
        LOGGER.debug("JMSException", jex);
      }
    }

    LOGGER.debug("Sending momvement to Exchange");
  }
}
