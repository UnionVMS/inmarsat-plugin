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
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmMessage;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmPendingResponse;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.StartupBean;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.constants.ModuleQueue;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.producer.PluginMessageProducer;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.service.ExchangeService;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Vector;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.jms.JMSException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 **/
@Singleton
public class DownLoadCacheDeliveryBean {

    final static Logger LOG = LoggerFactory.getLogger(DownLoadCacheDeliveryBean.class);
    private int PATTERN_LENGTH;
    private final byte[] BYTE_PATTERN = {1, 84, 38, 84};
   
    private InmMessage[] fileMessages;

    @EJB
    ExchangeService service;

    @EJB
    StartupBean startBean;
    
    @EJB
    PluginPendingResponseList pendingPollResponsList;
    
    @EJB 
    PluginMessageProducer pluginMessageProducer;
    
   
    @Asynchronous
    public Future<String> parseAndDeliver(String path) {
        String pattern = new String(BYTE_PATTERN);
        String[] sArr = listCacheFiles(path);
        fileMessages = null;
        if (sArr != null) {
            for (int i = 0; i < sArr.length; i++) {
                byte[] bArr = read(path,sArr[i]);
                String fileStr = new String(bArr);
                if (fileStr.indexOf(pattern) >= 0) {
                    fileMessages = byteToInmMessge(bArr, fileMessages);
                } else {
                    File f = new File(path + sArr[i]);
                    LOG.info("File: " + sArr[i] + " deleted");
                    f.delete();
                }
            }
            if (fileMessages != null) {
                LOG.info("fileMessages total in directory: " + fileMessages.length);
                for (int j = 0; j < fileMessages.length; j++) {
                    try{
                        msgToQue(fileMessages[j]);
                    }catch(DatatypeConfigurationException dce){
                    }
                }
                for(int k=0;k<sArr.length;k++){
                    File f = new File(path + sArr[k]);
                    if(f.exists()){
                        LOG.info("File: " + sArr[k] + "processed and deleted");
                        f.delete(); 
                    }
                }
            }
        }
        return new AsyncResult<String>("Done");
    }

    private void msgToQue(InmMessage msg) throws DatatypeConfigurationException {
        msg.printReport();
        MovementBaseType movement = new MovementBaseType();
        
        movement.setComChannelType(MovementComChannelType.MOBILE_TERMINAL);
        MobileTerminalId mobTermId = new MobileTerminalId();

        IdList dnidId = new IdList();
        dnidId.setType(IdType.DNID);
        dnidId.setValue("" + msg.getHeader().getDnid());

        IdList membId = new IdList();
        membId.setType(IdType.MEMBER_NUMBER);
        membId.setValue("" + msg.getHeader().getMemberId());

        mobTermId.getMobileTerminalIdList().add(dnidId);
        mobTermId.getMobileTerminalIdList().add(membId);

        movement.setMobileTerminalId(mobTermId);

        movement.setMovementType(MovementTypeType.POS);

        MovementPoint mp = new MovementPoint();
        mp.setAltitude(0.0);
        mp.setLatitude(msg.getBody().getLatitude());
        mp.setLongitude(msg.getBody().getLongitude());
        movement.setPosition(mp);

        movement.setPositionTime(msg.getBody().getPositionDate());

        movement.setReportedCourse(msg.getBody().getCourse());

        movement.setReportedSpeed(msg.getBody().getSpeed());

        movement.setSource(MovementSourceType.INMARSAT_C);

        movement.setStatus("" + msg.getBody().getMemCode());

        SetReportMovementType reportType = new SetReportMovementType();
        reportType.setMovement(movement);
        GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance();
        reportType.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));
        reportType.setPluginName(startBean.getRegisterClassName());
        reportType.setPluginType(PluginType.SATELLITE_RECEIVER);

        service.sendMovementReportToExchange(reportType);
        

        //If there is a pending poll response, also generate a status update for that poll
        InmPendingResponse ipr = pendingPollResponsList.continsPollTo(dnidId.getValue(), membId.getValue());
        if(ipr!=null){
            LOG.info("PendingPollResponse found in list: "+ipr.getReferenceNumber());
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
                s = ExchangePluginResponseMapper.mapToSetPollStatusToSuccessfulResponse(startBean.getApplicaionName(), ackType, ipr.getMsgId());
                pluginMessageProducer.sendModuleMessage(s, ModuleQueue.EXCHANGE);
                boolean b = pendingPollResponsList.removePendingPollResponse(ipr);
                LOG.debug("Pending poll response removed: "+b);
            } catch (ExchangeModelMarshallException ex) {
                LOG.debug("ExchangeModelMarshallException",ex);
            } catch(JMSException jex){
                LOG.debug("JMSException",jex);
            }
        }
        
        LOG.debug("Sending momvement to Exchange");
    }
    //Fix for bug
    public byte[] insertId(byte[] contents) {
        ByteArrayOutputStream output_ = new ByteArrayOutputStream();
        int found = -1;

        for (int i = 0; i < (contents.length - BYTE_PATTERN.length); i++) {
            //Find message
            if (contents[i] == BYTE_PATTERN[0] && contents[i + 1] == BYTE_PATTERN[1]
                    && contents[i + 2] == BYTE_PATTERN[2] && contents[i + 3] == BYTE_PATTERN[3]) {
                found = i;
            }
            if (found >= 0 && found + 20 == i && contents[i] == (byte) 2) {
                output_.write((byte) 255);
                found = -1;
            }
            output_.write(contents[i]);
        }
        return output_.toByteArray();
    }
    private InmMessage[] byteToInmMessge(byte[] fileArray, InmMessage[] dirMessages) {
        byte[]fileBytes = insertId(fileArray);
        //byte[]fileBytes = fileArray;
        InmMessage[] messages = null;
        Vector v = new Vector();
        PATTERN_LENGTH = BYTE_PATTERN.length;
        //Parse file content for messages
        if (fileBytes != null && fileBytes.length > PATTERN_LENGTH) {
            for (int i = 0; i < (fileBytes.length - PATTERN_LENGTH); i++) {
                //Find message
                if (fileBytes[i] == BYTE_PATTERN[0] && fileBytes[i + 1] == BYTE_PATTERN[1]
                        && fileBytes[i + 2] == BYTE_PATTERN[2] && fileBytes[i + 3] == BYTE_PATTERN[3]) {

                    //For debug
                   /* 
                    byte[] report = Arrays.copyOfRange(fileBytes, i, i+42);
                     for (int J = 0; J < report.length; J++) {
                     int num = Integer.parseInt(String.format("%02X ", report[J]).trim(), 16);
                     String tmp = Integer.toBinaryString(num);
                     LOG.debug(J + "\t" + report[J] + "\t" + String.format("%02X ", report[J])+"\t" + String.format("%8s", tmp).replace(' ', '0'));
                     }
                    */ 
                    InmMessage iMes;
                    iMes = new InmMessage(Arrays.copyOfRange(fileBytes, i, fileBytes.length));

                    try {
                        if (iMes.validate()) {
                            v.add(iMes);
                        } else {
                            LOG.debug("Message rejected");
                        }
                    } catch (Exception e) {
                        LOG.debug("InmarsatMessages failed Validation", e);
                    }

                }
            }
        } else {
            LOG.info("FileByte is null or fileSize to small");
        }

        if (dirMessages != null) {
            v.addAll(Arrays.asList(dirMessages));
        }
        LOG.info("Vector size: " + v.size());
        messages = new InmMessage[v.size()];
        v.copyInto(messages);

        return messages;
    }

    private String[] listCacheFiles(String _path) {
        File dir = new File(_path);
        String[] sArr = null;
        FilenameFilter datFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                return lowercaseName.endsWith(".dat");
            }
        };
        if (dir.exists() && dir.isDirectory() && dir.canRead()) {
            sArr = dir.list(datFilter);
            for (int i = 0; i < sArr.length; i++) {
                LOG.debug("File " + i + " in dir: " + sArr[i]);
            }
        } else {
            LOG.info("No dir " + _path + ", or unable to read");
        }
        return sArr;
    }

    private byte[] read(String _path, String aInputFileName) {

        LOG.info("Parsing in binary file named : " + aInputFileName);
        File file = new File(_path + aInputFileName);
        byte[] result = new byte[(int) file.length()];
        if (result != null && result.length > 0) {
            try {
                InputStream input = null;
                try {
                    int totalBytesRead = 0;
                    input = new BufferedInputStream(new FileInputStream(file));
                    while (totalBytesRead < result.length) {
                        int bytesRemaining = result.length - totalBytesRead;
                        //input.read() returns -1, 0, or more :
                        int bytesRead = input.read(result, totalBytesRead, bytesRemaining);
                        if (bytesRead > 0) {
                            totalBytesRead = totalBytesRead + bytesRead;
                        }
                    }
                } finally {
                    LOG.info("Closing input stream");
                    input.close();
                }
            } catch (FileNotFoundException ex) {
                LOG.error("File not found");
            } catch (IOException ex) {
                LOG.error("IOException", ex);
            } catch (NullPointerException ex) {
                LOG.error("Nullpointerexception", ex);
            }
        }
        return result;
    }
}