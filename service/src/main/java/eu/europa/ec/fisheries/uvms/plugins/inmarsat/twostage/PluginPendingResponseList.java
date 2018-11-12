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

import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmPendingResponse;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.StartupBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/** */
@LocalBean
public class PluginPendingResponseList {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginPendingResponseList.class);
    private static final String FILE_NAME = "pending.ser";
    private ArrayList<InmPendingResponse> pending;

    @EJB
    private StartupBean startUp;

    @PostConstruct
    public void loadPendingPollResponse() {
        pending = new ArrayList<>();
        File f = new File(startUp.getPollPath(), FILE_NAME);
        try (FileInputStream fis = new FileInputStream(f);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            //noinspection unchecked
            pending = (ArrayList<InmPendingResponse>) ois.readObject();
            if (pending != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Read {} pending responses", pending.size());
                    for (InmPendingResponse element : pending) {
                        LOGGER.debug("Refnr: {}", element.getReferenceNumber());
                    }
                }
            } else {
                pending = new ArrayList<>();
            }
        } catch (IOException ex) {
            LOGGER.debug("IOExeption:", ex);
        } catch (ClassNotFoundException ex) {
            LOGGER.debug("ClassNotFoundException", ex);
        }
    }

    public void addPendingPollResponse(InmPendingResponse resp) {
        if (pending != null) {
            pending.add(resp);
            LOGGER.debug("Pending response added");
        }
    }

    public boolean removePendingPollResponse(InmPendingResponse resp) {
        if (pending != null) {
            LOGGER.debug("Trying to remove pending poll response");
            return pending.remove(resp);
        }
        return false;
    }

    public List<InmPendingResponse> getPendingPollResponses() {
        //noinspection unchecked
        return (ArrayList<InmPendingResponse>) pending.clone();
    }

    public boolean containsPendingPollResponse(InmPendingResponse resp) {
        return pending != null && pending.contains(resp);
    }

    public InmPendingResponse continsPollTo(String dnid, String memberId) {
        for (InmPendingResponse element : pending) {
            if (element.getDnId().equalsIgnoreCase(dnid)
                    && element.getMembId().equalsIgnoreCase(memberId)) {
                return element;
            }
        }
        return null;
    }

    @PreDestroy
    public void writePendingPollResponse() {
        File f = new File(startUp.getPollPath(), FILE_NAME);

        try (FileOutputStream fos = new FileOutputStream(f, false);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(pending);
            oos.flush();
            LOGGER.debug("Wrote {} pending responses", pending.size());
        } catch (IOException ex) {
            LOGGER.debug("IOExeption", ex);
        }
    }
}
