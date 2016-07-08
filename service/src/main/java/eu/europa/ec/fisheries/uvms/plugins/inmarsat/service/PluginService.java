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
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.producer.PluginMessageProducer;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.twostage.PluginPendingResponseList;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.twostage.PollService;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.twostage.RetriverBean;
import java.util.Iterator;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 **/
@LocalBean
@Stateless
public class PluginService {

    @EJB
    StartupBean startupBean;

    @EJB
    PollService pollService;

    @EJB
    RetriverBean retriverBean;

    @EJB
    PluginPendingResponseList responseList;

    @EJB
    PluginMessageProducer pluginMessageProducer;

    final static Logger LOG = LoggerFactory.getLogger(PluginService.class);

    /**
     * TODO implement
     *
     * @param report
     * @return
     */
    public AcknowledgeTypeType setReport(ReportType report) {
        return AcknowledgeTypeType.NOK;
    }

    /**
     *
     * @param command
     * @return
     */
    public AcknowledgeTypeType setCommand(CommandType command) {
        LOG.info(startupBean.getRegisterClassName() + ".setCommand(" + command.getCommand().name() + ")");
        LOG.debug("timestamp: " + command.getTimestamp());
        PollType poll = command.getPoll();
        if (poll != null && CommandTypeType.POLL.equals(command.getCommand())) {
            String result = null;
            if (PollTypeType.POLL == poll.getPollTypeType()) {
                result = pollService.sendPoll(poll, retriverBean.getPollPath());
                LOG.debug("POLL returns: " + result);
                //Register Not acknowledge response
                if (result == null) {
                    return AcknowledgeTypeType.NOK;
                } 
                //Register response as pending
                else {
                    InmPendingResponse ipr = new InmPendingResponse();
                    ipr.setPollType(poll);
                    ipr.setMsgId(poll.getPollId());
                    ipr.setReferenceNumber(Integer.parseInt(result));
                    List<KeyValueType> pollReciver = (List<KeyValueType>) poll.getPollReceiver();
                    for (KeyValueType element : pollReciver) {
                        if (element.getKey().equalsIgnoreCase("MOBILE_TERMINAL_ID")) {
                            ipr.setMobTermId(element.getValue());
                        } else if (element.getKey().equalsIgnoreCase("DNID")) {
                            ipr.setDnId(element.getValue());
                        } else if (element.getKey().equalsIgnoreCase("MEMBER_NUMBER")) {
                            ipr.setMembId(element.getValue());
                        }
                    }
                    ipr.setStatus(InmPendingResponse.Staus_Type.PENDING);
                    responseList.addPendingPollResponse(ipr);

                    //Send status update to exchange
                    sentStatusToExcange(ipr,ExchangeLogStatusTypeType.PENDING);
                }
            } else if (PollTypeType.CONFIG == poll.getPollTypeType()) {

            }

        }
        return AcknowledgeTypeType.OK;
    }

    private void sentStatusToExcange(InmPendingResponse ipr, ExchangeLogStatusTypeType status) {

        AcknowledgeType ackType = new AcknowledgeType();
        ackType.setMessage("");
        ackType.setMessageId(ipr.getMsgId());

        PollStatusAcknowledgeType osat = new PollStatusAcknowledgeType();
        osat.setPollId(ipr.getMsgId());
        osat.setStatus(status);

        ackType.setPollStatus(osat);
        ackType.setType(AcknowledgeTypeType.OK);

        String s;
        try {
            s = ExchangePluginResponseMapper.mapToSetPollStatusToSuccessfulResponse(startupBean.getApplicaionName(), ackType, ipr.getMsgId());
            pluginMessageProducer.sendModuleMessage(s, ModuleQueue.EXCHANGE);
            LOG.debug("Poll response " + ipr.getMsgId() + " sent to exchange with status: " + status.value());
        } catch (ExchangeModelMarshallException ex) {
            LOG.debug("ExchangeModelMarshallException", ex);
        } catch (JMSException jex) {
            LOG.debug("JMSException", jex);
        }

    }

    /**
     * Set the config values for the twostage
     *
     * @param settings
     * @return
     */
    public AcknowledgeTypeType setConfig(SettingListType settings) {
        LOG.info(startupBean.getRegisterClassName() + ".setConfig()");
        try {
            for (KeyValueType values : settings.getSetting()) {
                LOG.debug("Setting [ " + values.getKey() + " : " + values.getValue() + " ]");
                startupBean.getSettings().put(values.getKey(), values.getValue());
            }
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            LOG.error("Failed to set config in {}", startupBean.getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }

    }

    /**
     * Start the twostage. Use this to enable functionality in the twostage
     *
     * @return
     */
    public AcknowledgeTypeType start() {
        LOG.info(startupBean.getRegisterClassName() + ".start()");
        try {
            startupBean.setIsEnabled(Boolean.TRUE);
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            startupBean.setIsEnabled(Boolean.FALSE);
            LOG.error("Failed to start {}", startupBean.getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }

    }

    /**
     * Start the twostage. Use this to disable functionality in the twostage
     *
     * @return
     */
    public AcknowledgeTypeType stop() {
        LOG.info(startupBean.getRegisterClassName() + ".stop()");
        try {
            startupBean.setIsEnabled(Boolean.FALSE);
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            startupBean.setIsEnabled(Boolean.TRUE);
            LOG.error("Failed to stop {}", startupBean.getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }
    }

}