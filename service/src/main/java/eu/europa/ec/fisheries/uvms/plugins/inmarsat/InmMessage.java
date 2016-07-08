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
package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 **/
public class InmMessage implements InmMessageIntf {

    final static Logger LOG = LoggerFactory.getLogger(InmMessage.class);
    private byte[] message = null;
    private InmHeader header = null;
    private InmBody body = null;
    

    public InmMessage(byte[] bArr) {
        setMessage(bArr);
    }

    private void setMessage(byte[] bArr) {
        this.message = bArr;
        header = new InmHeader(message);
        try {
            
            if (header.validate()) {
                int start = header.getHeaderLength();
                int end = header.getMessageLength();
            
                body = new InmBody(Arrays.copyOfRange(message, start, start+end));
                
                //For debug
                /*
                byte[] report = Arrays.copyOfRange(message, 0, start+end);
                for (int i = 0; i < report.length; i++) {
                    int num = Integer.parseInt(String.format("%02X ", report[i]).trim(), 16);
                    String tmp = Integer.toBinaryString(num);
                    LOG.debug(i + "\t" + report[i] + "\t" + String.format("%02X ", report[i])+"\t" + String.format("%8s", tmp).replace(' ', '0'));
                }
                printReport();
                */
            }

        } catch (Exception e) {
            LOG.debug("setMessage header probably failed",e);
        }
    }

    public InmHeader getHeader() {
        return this.header;
    }

    public InmBody getBody() {
        return this.body;
    }

    public void printReport() {
        LOG.debug("ApiHeader: " + getHeader().getheaderType());
        LOG.debug("Header length: " + getHeader().getHeaderLength());
        LOG.debug("Message Length: " + getHeader().getMessageLength());
        LOG.debug("MessageRef: " + getHeader().getMsgRefNr());
        LOG.debug("SatId: " + getHeader().getSatId());
        LOG.debug("Stored time: " + getHeader().getStoredTime());
        LOG.debug("Dnid: " + getHeader().getDnid());
        LOG.debug("MemberId: " + getHeader().getMemberId());
        LOG.debug("ReportDateFormat: " + getBody().getDataReportFormat());
        LOG.debug("Latidude: " + getBody().getLatitude());
        LOG.debug("Longitude: " + getBody().getLongitude());
        LOG.debug("Memcode: " + getBody().getMemCode());
        LOG.debug("DayOfMonth: " + getBody().getDayOfMonth());
        LOG.debug("Hour: " + getBody().getHour());
        LOG.debug("Minutes: " + getBody().getMinutes());
        LOG.debug("Speed: " + getBody().getSpeed());
        LOG.debug("Course: " + getBody().getCourse());
    }

    @Override
    public boolean validate() throws Exception {
        return header.validate();
    }

}