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

import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.StartupBean;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import eu.europa.ec.fisheries.uvms.plugins.inmarsat.exception.TelnetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 **/
@LocalBean
@Stateless
public class PollService {
    @EJB
    StartupBean startUp;
    
    @EJB
    Connect connect;
    
    final static Logger LOG = LoggerFactory.getLogger(PollService.class);
    
    
    public String sendPoll(PollType poll, String path) throws TelnetException {
        LOG.info("sendPoll invoked");
        String s = connect.connect(poll,path,startUp.getSetting("URL"), startUp.getSetting("PORT"), startUp.getSetting("USERNAME"), startUp.getSetting("PSW"), startUp.getSetting("DNIDS"));
        LOG.info("sendPoll returned: "+s);
        if(s != null) {
            s = parseResponse(s);
        } else {
            throw new TelnetException("Connect returned null response");
        }
        return s;
    }

    public String sendConfigurationPoll(PollType poll) throws TelnetException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    //Extract refnr from LES response
    public String parseResponse(String response){
        String s  = response.substring(response.indexOf("number"));
        return s.replaceAll("[^0-9]", ""); // returns 123
    }
}