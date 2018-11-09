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

import eu.europa.ec.fisheries.uvms.plugins.inmarsat.StartupBean;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.exception.TelnetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/** */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class DownLoadService {

    @EJB
    private Connect connect;

    @EJB
    private StartupBean startUp;

    private static final Logger LOGGER = LoggerFactory.getLogger(DownLoadService.class);

    @Asynchronous
    public Future<Map<String, String>> download(String path, List<String> dnids) {
        Map<String, String> responses = new HashMap<>();
        for (String dnid : dnids) {
            try {
                String response = download(path, dnid);
                LOGGER.debug("Download returned: {}", response);
                responses.put(dnid, response);
            } catch (TelnetException e) {
                LOGGER.error("Exception while downloading: {}", e.getMessage());
            }
        }

        return new AsyncResult<>(responses);
    }

    private String download(String path, String dnid) throws TelnetException {
        LOGGER.debug("Download invoked with DNID = {}", dnid);
        return connect.connect(null,path,startUp.getSetting("URL"),startUp.getSetting("PORT"),startUp.getSetting("USERNAME"),startUp.getSetting("PSW"),dnid);
    }
}
