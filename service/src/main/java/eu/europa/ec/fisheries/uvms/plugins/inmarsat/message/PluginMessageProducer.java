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
package eu.europa.ec.fisheries.uvms.plugins.inmarsat.message;

import eu.europa.ec.fisheries.uvms.commons.message.impl.JMSUtils;
import eu.europa.ec.fisheries.uvms.exchange.model.constant.ExchangeModelConstants;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.ModuleQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.jms.*;

import static eu.europa.ec.fisheries.uvms.plugins.inmarsat.ModuleQueue.EXCHANGE;

@LocalBean
public class PluginMessageProducer {

    public static final String INMARSAT_FAILED_REPORT_QUEUE = "jms/queue/UVMSInmarsatCFailedReport";


    private static final Logger LOGGER = LoggerFactory.getLogger(PluginMessageProducer.class);
    private Queue exchangeQueue;
    private Topic eventBus;
    private ConnectionFactory connectionFactory;
    private Queue inmarsatFailedReportQueue;

    @PostConstruct
    public void resourceLookup() {
        connectionFactory = JMSUtils.lookupConnectionFactory();
        exchangeQueue = JMSUtils.lookupQueue(ExchangeModelConstants.EXCHANGE_MESSAGE_IN_QUEUE);
        eventBus = JMSUtils.lookupTopic(ExchangeModelConstants.PLUGIN_EVENTBUS);
        inmarsatFailedReportQueue = JMSUtils.lookupQueue(INMARSAT_FAILED_REPORT_QUEUE);
    }

    public void sendResponseMessage(String text, TextMessage requestMessage) throws JMSException {

        try (Connection connection = connectionFactory.createConnection();
             Session session = JMSUtils.connectToQueue(connection)) {

            TextMessage message = session.createTextMessage();
            message.setJMSDestination(requestMessage.getJMSReplyTo());
            message.setJMSCorrelationID(requestMessage.getJMSMessageID());
            message.setText(text);

            sendMessage(session, requestMessage.getJMSReplyTo(), message);

        } catch (JMSException e) {
            LOGGER.error("[ Error when sending jms message. {}] {}", text, e.getMessage());
            throw new JMSException(e.getMessage());
        }
    }

    public String sendModuleMessage(String text, ModuleQueue queue) throws JMSException {
        try (Connection connection = connectionFactory.createConnection();
             Session session = JMSUtils.connectToQueue(connection)) {

            TextMessage message = session.createTextMessage();
            message.setText(text);

            if (EXCHANGE == queue) {
                sendMessage(session, exchangeQueue, message);
            } else {
                LOGGER.error("[ Sending Queue is not implemented ]");
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
             Session session = JMSUtils.connectToQueue(connection)) {

            TextMessage message = session.createTextMessage();
            message.setText(text);
            message.setStringProperty(ExchangeModelConstants.SERVICE_NAME, serviceName);

            sendMessage(session, eventBus, message);

            return message.getJMSMessageID();
        } catch (JMSException e) {
            LOGGER.error("[ Error when sending message. {}] {}", text, e.getMessage());
            throw new JMSException(e.getMessage());
        }
    }

    public String  sendFailedReportMessage(byte[] inmarsatReport) throws JMSException {

        try (Connection connection = connectionFactory.createConnection();
             Session session = JMSUtils.connectToQueue(connection)) {

            BytesMessage message = session.createBytesMessage();
            message.writeBytes(inmarsatReport);

            sendMessage(session, inmarsatFailedReportQueue, message);

            return message.getJMSMessageID();
        } catch (JMSException e) {
            throw e;
        }

    }


    private void sendMessage(Session session, Destination destination, TextMessage message)
            throws JMSException {
        try (MessageProducer messageProducer = session.createProducer(destination)) {
            messageProducer.setDeliveryMode(DeliveryMode.PERSISTENT);
            messageProducer.send(message);
        }
    }

    private void sendMessage(Session session, Destination destination, BytesMessage message)
            throws JMSException {
        try (MessageProducer messageProducer = session.createProducer(destination)) {
            messageProducer.setDeliveryMode(DeliveryMode.PERSISTENT);
            messageProducer.send(message);
        }
    }

}
