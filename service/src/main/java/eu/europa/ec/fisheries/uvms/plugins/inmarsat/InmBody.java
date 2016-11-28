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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import eu.europa.ec.fisheries.uvms.exchange.model.util.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.IllegalFieldValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 **/

public class InmBody implements InmMessageIntf{
    final static Logger LOG = LoggerFactory.getLogger(InmBody.class);
    private byte[] body=null;

    public InmBody(byte[] message){
        setBody(message);
    }
    
    private void setBody(byte[] message){
        if(message!=null ){
            this.body=message;
        }
        else{
            LOG.debug("Unable to set InmBody byte[] is null" );
        }
    }
    public int getDataReportFormat() {
        int num = Integer.parseInt(String.format("%02X ", body[0]).trim(), 16);
        String s = Integer.toBinaryString(num);
        s = String.format("%8s", s).replace(' ', '0');
        int i = Integer.parseInt(s.substring(0, 2), 2);
        return i;
    }

    public Double getLatitude() {
        char hemisphere = 'N';
        int num = Integer.parseInt(String.format("%02X ", body[0]).trim(), 16);
        String tmp = Integer.toBinaryString(num);
        String s = String.format("%8s", tmp).replace(' ', '0');

        num = Integer.parseInt(String.format("%02X ", body[1]).trim(), 16);
        tmp = Integer.toBinaryString(num);
        s += String.format("%8s", tmp).replace(' ', '0');

        num = Integer.parseInt(String.format("%02X ", body[2]).trim(), 16);
        tmp = Integer.toBinaryString(num);
        s += String.format("%8s", tmp).replace(' ', '0');

        int i = Integer.parseInt(s.substring(2, 3), 2);
        if (i > 0) {
            hemisphere = 'S';
        }

        int latDeg = Integer.parseInt(s.substring(3, 10), 2);
        int latMin = Integer.parseInt(s.substring(10, 16), 2);
        int latFrac = Integer.parseInt(s.substring(16, 21), 2) * 4;
        double latMinFrac = (double)latMin + ((double)latFrac / 100);
        Double d = (double)latDeg+(latMinFrac/60);
        if (i > 0) {
            d*=-1.0;
        }
        return d;
    }

    public Double getLongitude() {
        char hemisphere = 'E';
        int num = Integer.parseInt(String.format("%02X ", body[2]).trim(), 16);
        String tmp = Integer.toBinaryString(num);
        String s = String.format("%8s", tmp).replace(' ', '0');

        num = Integer.parseInt(String.format("%02X ", body[3]).trim(), 16);
        tmp = Integer.toBinaryString(num);
        s += String.format("%8s", tmp).replace(' ', '0');

        num = Integer.parseInt(String.format("%02X ", body[4]).trim(), 16);
        tmp = Integer.toBinaryString(num);
        s += String.format("%8s", tmp).replace(' ', '0');

        num = Integer.parseInt(String.format("%02X ", body[5]).trim(), 16);
        tmp = Integer.toBinaryString(num);
        s += String.format("%8s", tmp).replace(' ', '0');

        int i = Integer.parseInt(s.substring(5, 6), 2);
        if (i > 0) {
            hemisphere = 'W';
        }

        int lonDeg = Integer.parseInt(s.substring(6, 14), 2);
        int lonMin = Integer.parseInt(s.substring(14, 20), 2);
        int lonFrac = Integer.parseInt(s.substring(20, 25), 2) * 4;

        double lonMinFrac = (double)lonMin + ((double)lonFrac / 100);
        Double d = (double)lonDeg+(lonMinFrac/60);
        if (i > 0) {
            d*=-1.0;
        }
        return d;
    }

    public int getMemCode() {
        int num = Integer.parseInt(String.format("%02X ", body[5]).trim(), 16);
        String tmp = Integer.toBinaryString(num);
        String s = String.format("%8s", tmp).replace(' ', '0');

        int i = Integer.parseInt(s.substring(1), 2);
        return i;
    }

    public int getDayOfMonth() {
        int num = Integer.parseInt(String.format("%02X ", body[6]).trim(), 16);
        String tmp = Integer.toBinaryString(num);
        String s = String.format("%8s", tmp).replace(' ', '0');
        int i = Integer.parseInt(s.substring(1, 6), 2);
        return i;
    }

    public int getHour() {
        int num = Integer.parseInt(String.format("%02X ", body[6]).trim(), 16);
        String tmp = Integer.toBinaryString(num);
        String s = String.format("%8s", tmp).replace(' ', '0');

        num = Integer.parseInt(String.format("%02X ", body[7]).trim(), 16);
        tmp = Integer.toBinaryString(num);
        s += String.format("%8s", tmp).replace(' ', '0');

        int i = Integer.parseInt(s.substring(6, 11), 2);
        return i;
    }

    public int getMinutes() {
        int num = Integer.parseInt(String.format("%02X ", body[7]).trim(), 16);
        String tmp = Integer.toBinaryString(num);
        String s = String.format("%8s", tmp).replace(' ', '0');
        int i = Integer.parseInt(s.substring(3), 2);
        return (i * 2);
    }

    public DateTime getPositionDate() {
        DateTime now = new DateTime(DateTimeZone.UTC);
        return getPositionDate(now);
    }

    DateTime getPositionDate(DateTime now) {
        int year = now.getYear();
        int month = now.getMonthOfYear();
        int d = getDayOfMonth();
        int h = getHour();
        int m = getMinutes();

        DateTime dateTime;
        try {
            // Date is in current month
            dateTime = new DateTime(year, month, d, h, m, DateTimeZone.UTC);
            // Date is in previous month
            if (dateTime.isAfter(now)) {
                dateTime = dateTime.minusMonths(1);
            }
        } catch (IllegalFieldValueException e) {
            // Date is in previous month, and day of month is > days of current month
            if (month != 1) {
                dateTime = new DateTime(year, month - 1, d, h, m, DateTimeZone.UTC);
            }
            // Date is in previous month and previous year (current month is january)
            else {
                dateTime = new DateTime(year - 1, 12, d, h, m, DateTimeZone.UTC);
            }
        }

        return dateTime;

    }
    
    public Double getSpeed() {
        String hex = String.format("%02X ", body[8]);
        return (double) (Integer.parseInt(hex.trim(), 16) * 0.2);
    }

    public Double getCourse() {
        int num = Integer.parseInt(String.format("%02X ", body[9]).trim(), 16);
        String tmp = Integer.toBinaryString(num);
        String s = String.format("%8s", tmp).replace(' ', '0');

        num = Integer.parseInt(String.format("%02X ", body[10]).trim(), 16);
        tmp = Integer.toBinaryString(num);
        s += String.format("%8s", tmp).replace(' ', '0');

        int i = Integer.parseInt(s.substring(0, 9), 2);
        return (double)i;
    }
    
    @Override
    public boolean validate() throws Exception {
        return true;
    }
    
}