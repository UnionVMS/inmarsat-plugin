package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.uvms.commons.message.api.MessageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
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

    private static final Logger LOG = LoggerFactory.getLogger(InmarsatMessageListener.class);


    @Override
    public void onMessage(Message message) {

        byte[] payload = null;

        try {
            if (message instanceof BytesMessage) {
                payload = message.getBody(byte[].class);
                if(payload != null){
                    String msg = bytesArrayToHexString(payload);
                    LOG.info(msg);

                    // interpret and post to exchange . . .


                }
            }
        } catch (JMSException e) {
            LOG.error(e.toString(), e);
        }
    }


    public String bytesArrayToHexString(byte[] bytes) {

        char[] hexArray = "0123456789ABCDEF".toCharArray();

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }



}
