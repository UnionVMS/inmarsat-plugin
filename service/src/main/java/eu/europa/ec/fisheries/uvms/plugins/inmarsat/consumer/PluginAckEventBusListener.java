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

import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginFault;
import eu.europa.ec.fisheries.schema.exchange.registry.v1.ExchangeRegistryBaseRequest;
import eu.europa.ec.fisheries.schema.exchange.registry.v1.RegisterServiceResponse;
import eu.europa.ec.fisheries.schema.exchange.registry.v1.UnregisterServiceResponse;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.JAXBMarshaller;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmarsatPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;


/*
       <message-driven>
            <ejb-name>PluginAckEventBusListenerinmarsat</ejb-name>
            <ejb-class>eu.europa.ec.fisheries.uvms.plugins.inmarsat.consumer.PluginAckEventBusListener</ejb-class>
            <mapped-name>jms:/jms/topic/EventBus</mapped-name>
            <messaging-type>javax.jms.MessageListener</messaging-type>
            <activation-config>
                <activation-config-property>
                    <activation-config-property-name>subscriptionName</activation-config-property-name>
                    <activation-config-property-value>eu.europa.ec.fisheries.uvms.plugins.inmarsat.PLUGIN_RESPONSE</activation-config-property-value>
                </activation-config-property>
                <activation-config-property>
                    <activation-config-property-name>clientId</activation-config-property-name>
                    <activation-config-property-value>eu.europa.ec.fisheries.uvms.plugins.inmarsat.PLUGIN_RESPONSE</activation-config-property-value>
                </activation-config-property>
                <activation-config-property>
                    <activation-config-property-name>messageSelector</activation-config-property-name>
                    <activation-config-property-value>ServiceName='eu.europa.ec.fisheries.uvms.plugins.inmarsat.PLUGIN_RESPONSE'</activation-config-property-value>
                </activation-config-property>
                <activation-config-property>
                    <activation-config-property-name>subscriptionDurability</activation-config-property-name>
                    <activation-config-property-value>Durable</activation-config-property-value>
                </activation-config-property>
                <activation-config-property>
                    <activation-config-property-name>destination</activation-config-property-name>
                    <activation-config-property-value>EventBus</activation-config-property-value>
                </activation-config-property>
                <activation-config-property>
                    <activation-config-property-name>connectionFactoryJndiName</activation-config-property-name>
                    <activation-config-property-value>jms:/ConnectionFactory</activation-config-property-value>
                </activation-config-property>
                <activation-config-property>
                    <activation-config-property-name>destinationType</activation-config-property-name>
                    <activation-config-property-value>javax.jms.Topic</activation-config-property-value>
                </activation-config-property>
            </activation-config>
        </message-driven>

        */


@MessageDriven(mappedName="jms:/jms/topic/EventBus", activationConfig =  {
        @ActivationConfigProperty(propertyName = "subscriptionName",          propertyValue = "eu.europa.ec.fisheries.uvms.plugins.inmarsat.PLUGIN_RESPONSE"),
        @ActivationConfigProperty(propertyName = "clientId",                  propertyValue = "eu.europa.ec.fisheries.uvms.plugins.inmarsat.PLUGIN_RESPONSE"),
        @ActivationConfigProperty(propertyName = "messageSelector",           propertyValue = "ServiceName='eu.europa.ec.fisheries.uvms.plugins.inmarsat.PLUGIN_RESPONSE'"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability",    propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "destination",               propertyValue = "EventBus"),
        @ActivationConfigProperty(propertyName = "connectionFactoryJndiName", propertyValue = "jms:/ConnectionFactory"),
        @ActivationConfigProperty(propertyName = "destinationType",           propertyValue = "javax.jms.Topic")
})
public class PluginAckEventBusListener implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginAckEventBusListener.class);

    @Inject
    private InmarsatPlugin startupService;

    @Override
    public void onMessage(Message inMessage) {

        LOGGER.info("Eventbus listener for twostage at selector: {} got a message", startupService.getPluginResponseSubscriptionName());
        TextMessage textMessage = (TextMessage) inMessage;

        try {

            ExchangeRegistryBaseRequest request = tryConsumeRegistryBaseRequest(textMessage);

            if (request == null) {
                PluginFault fault = JAXBMarshaller.unmarshallTextMessage(textMessage, PluginFault.class);
                handlePluginFault(fault);
            } else {
                switch (request.getMethod()) {
                    case REGISTER_SERVICE:
                        RegisterServiceResponse registerResponse = JAXBMarshaller.unmarshallTextMessage(textMessage, RegisterServiceResponse.class);
                        startupService.setWaitingForResponse(Boolean.FALSE);
                        switch (registerResponse.getAck().getType()) {
                            case OK:
                                LOGGER.info("Register OK");
                                startupService.setIsRegistered(Boolean.TRUE);
                                startupService.updateSettings(registerResponse.getService().getSettingList().getSetting());
                                break;
                            case NOK:
                                LOGGER.info("Register NOK: " + registerResponse.getAck().getMessage());
                                startupService.setIsRegistered(Boolean.FALSE);
                                break;
                            default:
                                LOGGER.error("[ Type not supperted: ]" + request.getMethod());
                        }
                        break;
                    case UNREGISTER_SERVICE:
                        UnregisterServiceResponse unregisterResponse = JAXBMarshaller.unmarshallTextMessage(textMessage, UnregisterServiceResponse.class);
                        switch (unregisterResponse.getAck().getType()) {
                            case OK:
                                LOGGER.info("Unregister OK");
                                break;
                            case NOK:
                                LOGGER.info("Unregister NOK");
                                break;
                            default:
                                LOGGER.error("[ Ack type not supported ] ");
                                break;
                        }
                        break;
                    default:
                        LOGGER.error("Not supported method");
                        break;
                }
            }
        } catch (ExchangeModelMarshallException | NullPointerException e) {
            LOGGER.error("[ Error when receiving message in twostage ]", e);
        }
    }

    private void handlePluginFault(PluginFault fault) {
        LOGGER.error(
                startupService.getPluginResponseSubscriptionName() + " received fault " + fault.getCode() + " : " + fault.getMessage());
    }

    private ExchangeRegistryBaseRequest tryConsumeRegistryBaseRequest(TextMessage textMessage) {
        try {
            return JAXBMarshaller.unmarshallTextMessage(textMessage, ExchangeRegistryBaseRequest.class);
        } catch (ExchangeModelMarshallException e) {
            return null;
        }
    }
}
