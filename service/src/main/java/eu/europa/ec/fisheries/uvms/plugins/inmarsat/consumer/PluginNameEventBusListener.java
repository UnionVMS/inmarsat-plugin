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
package eu.europa.ec.fisheries.uvms.plugins.inmarsat.consumer;

import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.PollStatusAcknowledgeType;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.*;
import eu.europa.ec.fisheries.schema.exchange.v1.ExchangeLogStatusTypeType;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.JAXBMarshaller;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmarsatMessageRetriever;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmarsatPlugin;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmarsatPollHandler;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.message.PluginMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven(mappedName = "jms:/jms/topic/EventBus", activationConfig = {
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "eu.europa.ec.fisheries.uvms.plugins.inmarsat"),
        @ActivationConfigProperty(propertyName = "clientId", propertyValue = "eu.europa.ec.fisheries.uvms.plugins.inmarsat"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "ServiceName='eu.europa.ec.fisheries.uvms.plugins.inmarsat'"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "EventBus"),
        @ActivationConfigProperty(propertyName = "connectionFactoryJndiName", propertyValue = "jms:/ConnectionFactory"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic")
})
public class PluginNameEventBusListener implements MessageListener {



    private static final Logger LOGGER = LoggerFactory.getLogger(PluginNameEventBusListener.class);

    @Inject
    private PluginMessageProducer messageProducer;

    @Inject
    private InmarsatPlugin startup;

    @Inject
    private InmarsatPollHandler inmarsatPollHandler;

    @Inject
    private InmarsatMessageRetriever inmarsatMessageRetriever;


    @Override
    public void onMessage(Message inMessage) {

        LOGGER.debug(
                "Eventbus listener for inmarsat-c (MessageConstants.PLUGIN_SERVICE_CLASS_NAME): {}",
                startup.getRegisterClassName());

        TextMessage textMessage = (TextMessage) inMessage;

        try {

            PluginBaseRequest request =
                    JAXBMarshaller.unmarshallTextMessage(textMessage, PluginBaseRequest.class);

            String responseMessage = null;

            switch (request.getMethod()) {
                case SET_CONFIG:
                    SetConfigRequest setConfigRequest = JAXBMarshaller.unmarshallTextMessage(textMessage, SetConfigRequest.class);
                    AcknowledgeTypeType setConfig = startup.setConfig(setConfigRequest.getConfigurations());
                    inmarsatMessageRetriever.setConfig(setConfigRequest.getConfigurations());
                    AcknowledgeType setConfigAck = ExchangePluginResponseMapper.mapToAcknowlegeType(textMessage.getJMSMessageID(), setConfig);
                    responseMessage = ExchangePluginResponseMapper.mapToSetConfigResponse(startup.getRegisterClassName(), setConfigAck);
                    break;
                case SET_COMMAND:
                    SetCommandRequest setCommandRequest = JAXBMarshaller.unmarshallTextMessage(textMessage, SetCommandRequest.class);
                    AcknowledgeTypeType setCommand = inmarsatPollHandler.setCommand(setCommandRequest.getCommand());
                    AcknowledgeType setCommandAck = ExchangePluginResponseMapper.mapToAcknowlegeType(textMessage.getJMSMessageID(), setCommand);
                    setCommandAck.setUnsentMessageGuid(setCommandRequest.getCommand().getUnsentMessageGuid());
                    PollStatusAcknowledgeType pollAck = new PollStatusAcknowledgeType();
                    if(setCommand.equals(AcknowledgeTypeType.OK)) {
                        pollAck.setStatus(ExchangeLogStatusTypeType.PENDING);
                    }else{
                        pollAck.setStatus(ExchangeLogStatusTypeType.FAILED);
                    }
                    pollAck.setPollId(setCommandRequest.getCommand().getPoll().getPollId());
                    setCommandAck.setPollStatus(pollAck);
                    responseMessage = ExchangePluginResponseMapper.mapToSetCommandResponse(startup.getRegisterClassName(), setCommandAck);
                    break;
                case SET_REPORT:
                    SetReportRequest setReportRequest = JAXBMarshaller.unmarshallTextMessage(textMessage, SetReportRequest.class);
                    AcknowledgeTypeType setReport = startup.setReport(setReportRequest.getReport());
                    AcknowledgeType setReportAck = ExchangePluginResponseMapper.mapToAcknowlegeType(textMessage.getJMSMessageID(), setReport);
                    responseMessage = ExchangePluginResponseMapper.mapToSetReportResponse(startup.getRegisterClassName(), setReportAck);
                    break;
                case START:
                    JAXBMarshaller.unmarshallTextMessage(textMessage, StartRequest.class);
                    AcknowledgeTypeType start =  inmarsatMessageRetriever.start();
                    AcknowledgeType startAck = ExchangePluginResponseMapper.mapToAcknowlegeType(textMessage.getJMSMessageID(), start);
                    responseMessage = ExchangePluginResponseMapper.mapToStartResponse(startup.getRegisterClassName(), startAck);
                    break;
                case STOP:
                    JAXBMarshaller.unmarshallTextMessage(textMessage, StopRequest.class);
                    AcknowledgeTypeType stop = inmarsatMessageRetriever.stop();
                    AcknowledgeType stopAck = ExchangePluginResponseMapper.mapToAcknowlegeType(textMessage.getJMSMessageID(), stop);
                    responseMessage = ExchangePluginResponseMapper.mapToStopResponse(startup.getRegisterClassName(), stopAck);
                    break;
                case PING:
                    JAXBMarshaller.unmarshallTextMessage(textMessage, PingRequest.class);
                    responseMessage = ExchangePluginResponseMapper.mapToPingResponse(inmarsatMessageRetriever.isEnabled(), inmarsatMessageRetriever.isEnabled());
                    break;
                default:
                    LOGGER.error("Not supported method");
                    break;
            }

            messageProducer.sendResponseMessage(responseMessage, textMessage);

        } catch (RuntimeException e) {
            LOGGER.error(
                    "[ Error when receiving message in inmarsat-c " + startup.getRegisterClassName() + " ]", e);
        } catch (JMSException ex) {
            LOGGER.error(
                    "[ Error when handling JMS message in inmarsat-c " + startup.getRegisterClassName() + " ]",
                    ex);
        }
    }




}
