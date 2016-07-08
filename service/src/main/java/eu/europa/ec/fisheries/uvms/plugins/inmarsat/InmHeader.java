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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 **/
public class InmHeader implements InmMessageIntf{
    final static Logger LOG = LoggerFactory.getLogger(InmHeader.class);
    final static int apiHeaderLength = 22;
    private String headerType;
    private int headerLength;
    private int msgRefNr;
    private int dataPresentation;
    private int satId = -1; //Ocean region
    private int lesId = -1;
    private int msgLength;
    private Date storedTime;
    private int dnid;
    private int membId;
    private byte [] header;
    
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss");
    
    public InmHeader(byte[] message){
        setHeader(message);
    }
    
    private void setHeader(byte[] message) {
        if(message!=null && message.length >apiHeaderLength){
            this.header = Arrays.copyOfRange(message, 0, apiHeaderLength);
        }
    }
    public String getheaderType() {
        headerType = String.format("%02X ", header[4]).trim();
        return headerType;
    }

    public int getHeaderLength() {
        String hex = String.format("%02X ", header[5]);
        return Integer.parseInt(hex.trim(), 16);
    }
    public int getMsgRefNr() {
        String hex = String.format("%02X ", header[9]).trim();
        hex += String.format("%02X ", header[8]).trim();
        hex += String.format("%02X ", header[7]).trim();
        hex += String.format("%02X ", header[6]).trim();
        return Integer.parseInt(hex, 16);
    }
     public int getDataPresentation() {
        String hex = String.format("%02X ", header[10]).trim();
        return Integer.parseInt(hex, 16);
    }
    public int getSatId() {
        int num = Integer.parseInt(String.format("%02X ", header[11]).trim(), 16);
        String s = Integer.toBinaryString(num);
        this.satId = Integer.parseInt(s.substring(0, 2), 2);
        this.lesId = Integer.parseInt(s.substring(2), 2);
        return satId;
    }
    public int getLesId(){
        if(lesId<0)
          getSatId();
        return lesId;
    }
    public int getMessageLength() {
        String hex = String.format("%02X ", header[13]).trim();
        hex += String.format("%02X ", header[12]).trim();
        return Integer.parseInt(hex, 16);
    }
    //Stored at LES
    public Date getStoredTime() {
        String hex = String.format("%02X ", header[17]).trim();
        hex += String.format("%02X ", header[16]).trim();
        hex += String.format("%02X ", header[15]).trim();
        hex += String.format("%02X ", header[14]).trim();
        Long l = Long.parseLong(hex, 16);
        storedTime = new Date(l * 1000);
        return storedTime;
    }
    
    public String getFormattedDate(Date d){
        return sdf.format(d);
    }
    
    public int getDnid() {
        String hex = String.format("%02X ", header[19]).trim();
        hex += String.format("%02X ", header[18]).trim();
        return Integer.parseInt(hex, 16);
    }

    public int getMemberId() {
        String hex = String.format("%02X ", header[20]).trim();
        return Integer.parseInt(hex, 16);
    }
    
    @Override
    public boolean validate() throws Exception {
        byte dot=1;
        byte t = 84;
        byte and = 38;
        byte end = 2;
        boolean ret = false;
        if(header==null || header.length!=apiHeaderLength){
            LOG.debug("header validation failed is either null or to short");
            return ret;
        }
        else if(header[0]!=dot||header[1]!=t||header[2]!=and||header[3]!=t||header[21]!=end){
            LOG.debug("header validation failed format check");
            return ret;
        }
        return true;
    }
}