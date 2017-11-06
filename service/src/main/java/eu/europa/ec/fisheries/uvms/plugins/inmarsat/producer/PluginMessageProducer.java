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
package eu.europa.ec.fisheries.uvms.plugins.inmarsat.producer;

import eu.europa.ec.fisheries.uvms.commons.message.impl.JMSUtils;
import eu.europa.ec.fisheries.uvms.exchange.model.constant.ExchangeModelConstants;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.constants.ModuleQueue;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PluginMessageProducer {

  private Queue exchangeQueue;
  private Topic eventBus;
  private ConnectionFactory connectionFactory;

  private static final Logger LOGGER = LoggerFactory.getLogger(PluginMessageProducer.class);

  @PostConstruct
  public void resourceLookup() {
    connectionFactory = JMSUtils.lookupConnectionFactory();
    exchangeQueue = JMSUtils.lookupQueue(ExchangeModelConstants.EXCHANGE_MESSAGE_IN_QUEUE);
    eventBus = JMSUtils.lookupTopic(ExchangeModelConstants.PLUGIN_EVENTBUS);
  }

  public void sendResponseMessage(String text, TextMessage requestMessage) throws JMSException {

    try (Connection connection = connectionFactory.createConnection();
        final Session session = JMSUtils.connectToQueue(connection)) {

      TextMessage message = session.createTextMessage();
      message.setJMSDestination(requestMessage.getJMSReplyTo());
      message.setJMSCorrelationID(requestMessage.getJMSMessageID());
      message.setText(text);

      session.createProducer(requestMessage.getJMSReplyTo()).send(message);
      getProducer(session, requestMessage.getJMSReplyTo()).send(message);

    } catch (JMSException e) {
      LOGGER.error("[ Error when sending jms message. {}] {}", text, e.getMessage());
      throw new JMSException(e.getMessage());
    }
  }

  public String sendModuleMessage(String text, ModuleQueue queue) throws JMSException {
    try (Connection connection = connectionFactory.createConnection();
        final Session session = JMSUtils.connectToQueue(connection)) {

      TextMessage message = session.createTextMessage();
      message.setText(text);

      switch (queue) {
        case EXCHANGE:
          getProducer(session, exchangeQueue).send(message);
          break;
        default:
          LOGGER.error("[ Sending Queue is not implemented ]");
          break;
      }
      LOGGER.debug("SendMessage-queue:{}, message:{}", queue, message);
      return message.getJMSMessageID();
    } catch (JMSException e) {
      LOGGER.error("[ Error when sending data source message. {}] {}", text, e.getMessage());
      throw new JMSException(e.getMessage());
    }
  }

  public String sendEventBusMessage(String text, String serviceName) throws JMSException {
    try (Connection connection = connectionFactory.createConnection();
        final Session session = JMSUtils.connectToQueue(connection)) {

      TextMessage message = session.createTextMessage();
      message.setText(text);
      message.setStringProperty(ExchangeModelConstants.SERVICE_NAME, serviceName);

      getProducer(session, eventBus).send(message);

      return message.getJMSMessageID();
    } catch (JMSException e) {
      LOGGER.error("[ Error when sending message. {}] {}", text, e.getMessage());
      throw new JMSException(e.getMessage());
    }
  }

  private MessageProducer getProducer(Session session, Destination destination)
      throws JMSException {
    MessageProducer producer = session.createProducer(destination);
    producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    producer.setTimeToLive(60000L);
    return producer;
  }
}
