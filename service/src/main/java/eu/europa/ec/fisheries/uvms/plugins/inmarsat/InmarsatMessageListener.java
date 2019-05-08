package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.uvms.commons.message.api.MessageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;


@MessageDriven(mappedName = MessageConstants.QUEUE_ASSET_EVENT, activationConfig = {
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "UVMSPluginFailedReport"),
        @ActivationConfigProperty(propertyName = "destinationJndiName", propertyValue = "jms/queue/UVMSPluginFailedReport"),
        @ActivationConfigProperty(propertyName = "connectionFactoryJndiName", propertyValue = "ConnectionFactory")
})

public class InmarsatMessageListener implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatMessageListener.class);



    @Override
    public void onMessage(Message message) {

        LOGGER.info("GOT A MESSAGE HURRA !!!!!!!!!!!!!!!!!!!!!!");


    }
}
