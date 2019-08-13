package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.PollStatusAcknowledgeType;
import eu.europa.ec.fisheries.schema.exchange.module.v1.ExchangeModuleMethod;
import eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.IdList;
import eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.IdType;
import eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.MobileTerminalId;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.*;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.schema.exchange.v1.ExchangeLogStatusTypeType;
import eu.europa.ec.fisheries.uvms.commons.message.api.MessageConstants;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.message.PluginMessageProducer;
import fish.focus.uvms.commons.les.inmarsat.InmarsatException;
import fish.focus.uvms.commons.les.inmarsat.InmarsatInterpreter;
import fish.focus.uvms.commons.les.inmarsat.InmarsatMessage;
import fish.focus.uvms.commons.les.inmarsat.body.PositionReport;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.time.Instant;
import java.util.Base64;
import java.util.GregorianCalendar;
import java.util.TimeZone;


@MessageDriven( activationConfig = {
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "UVMSInmarsatMessages"),
        @ActivationConfigProperty(propertyName = "destinationJndiName", propertyValue = "jms/queue/UVMSInmarsatMessages"),
        @ActivationConfigProperty(propertyName = "connectionFactoryJndiName", propertyValue = "ConnectionFactory")
})

public class InmarsatMessageListener implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(InmarsatMessageListener.class);

    @Inject
    private InmarsatPlugin inmarsatPlugin;

    @Inject
    private InmarsatInterpreter inmarsatInterpreter;

    @Inject
    private InmarsatPollHandler inmarsatPollHandler;

    @Inject
    private PluginMessageProducer messageProducer;

    @Inject
    @Metric(name = "inmarsat_incoming", absolute = true)
    Counter inmarsatIncoming;

    @Override
    public void onMessage(Message message) {


        try {
            if (message instanceof BytesMessage) {
                byte[] payload = message.getBody(byte[].class);
                if(payload != null){

                    InmarsatMessage[] inmarsatMessagesPerOceanRegion = inmarsatInterpreter.byteToInmMessage(payload);

                    if ((inmarsatMessagesPerOceanRegion != null) && (inmarsatMessagesPerOceanRegion.length > 0)) {
                        int n = inmarsatMessagesPerOceanRegion.length;
                        for (int i = 0; i < n; i++) {
                            try {
                                msgToQue(inmarsatMessagesPerOceanRegion[i], payload);
                                inmarsatIncoming.inc();
                            } catch (InmarsatException e) {
                                LOG.error("Positiondate not found in " + inmarsatMessagesPerOceanRegion[i].toString(), e);
                            }
                        }
                    }
                }
            }
        } catch (JMSException e) {
            LOG.error(e.toString(), e);
        }
    }


    private void msgToQue(InmarsatMessage msg, byte[] orgiDatFile) throws InmarsatException {

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
        GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        reportType.setTimestamp(gcal.getTime());
        reportType.setPluginName(inmarsatPlugin.getRegisterClassName());
        reportType.setPluginType(PluginType.SATELLITE_RECEIVER);

        reportType.setOriginalIncomingMessage(Base64.getEncoder().encodeToString(orgiDatFile));


        if (!sendMovementReportToExchange(reportType)) {
            // if it didnt send so quit return
            return;
        }


        // it send so check if it was a pollrequest
        // in that case update the pollrequest status


        PluginPendingResponseList responseList = inmarsatPollHandler.getPluginPendingResponseList();

        // If the report is a pending poll response, also generate a status update for that poll
        InmarsatPendingResponse ipr = responseList.containsPollTo(dnidId.getValue(), membId.getValue());

        if (ipr != null) {
            LOG.info("PendingPollResponse found in list: {}", ipr.getReferenceNumber());
            AcknowledgeType ackType = new AcknowledgeType();
            ackType.setMessage("");

            PollStatusAcknowledgeType osat = new PollStatusAcknowledgeType();
            osat.setPollId(ipr.getMsgId());
            osat.setStatus(ExchangeLogStatusTypeType.SUCCESSFUL);

            ackType.setPollStatus(osat);
            ackType.setType(AcknowledgeTypeType.OK);

            String iprMessageId = ipr.getUnsentMsgId();
            ackType.setUnsentMessageGuid(iprMessageId);

            try {
                String s = ExchangePluginResponseMapper.mapToSetPollStatusToSuccessfulResponse(inmarsatPlugin.getApplicationName(), ackType, ipr.getMsgId());
                messageProducer.sendModuleMessage(s, ModuleQueue.EXCHANGE, ExchangeModuleMethod.PLUGIN_SET_COMMAND_ACK.value());
                boolean b = responseList.removePendingPollResponse(ipr);
                LOG.debug("Pending poll response removed: {}", b);
            } catch (RuntimeException ex) {
                LOG.debug("ExchangeModelMarshallException", ex);
            } catch (JMSException jex) {
                LOG.debug("JMSException", jex);
            }
        }

        LOG.debug("Sending movement to Exchange");
    }


    private boolean sendMovementReportToExchange(SetReportMovementType reportType) {
        try {
            String text = ExchangeModuleRequestMapper.createSetMovementReportRequest(reportType, "TWOSTAGE", null, Instant.now(),  PluginType.SATELLITE_RECEIVER, "TWOSTAGE", null);
            String messageId = messageProducer.sendModuleMessage(text, ModuleQueue.EXCHANGE, ExchangeModuleMethod.SET_MOVEMENT_REPORT.value());
            LOG.debug("Sent to exchange - text:{}, id:{}", text, messageId);
            return true;
        } catch (RuntimeException e) {
            LOG.error("Couldn't map movement to setreportmovementtype", e);
            return false;
        } catch (JMSException e) {
            LOG.error("couldn't send movement", e);
            return false;
        }
    }


}
