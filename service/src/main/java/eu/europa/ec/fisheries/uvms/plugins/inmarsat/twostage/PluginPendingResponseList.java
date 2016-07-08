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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import org.slf4j.LoggerFactory;

/**
 **/
@Singleton
@Startup
@DependsOn({"RetriverBean"})
public class PluginPendingResponseList {

    final static org.slf4j.Logger LOG = LoggerFactory.getLogger(PluginPendingResponseList.class);
    private final String fileName = "pending.ser";
    private ArrayList<InmPendingResponse> pending;

    @EJB
    RetriverBean startUp;

    @PostConstruct
    public void loadPendingPollResponse() {
        pending = new ArrayList<InmPendingResponse>();
        ObjectInputStream ois = null;
        FileInputStream fis = null;
        try {
            File f = new File(startUp.getPollPath(), fileName);
            fis = new FileInputStream(f);
            ois = new ObjectInputStream(fis);
            pending = (ArrayList<InmPendingResponse>) ois.readObject();
            if (pending != null) {
                LOG.debug("Read " + pending.size() + " peding responses");
                for (InmPendingResponse element : pending) {
                    LOG.debug("Refnr: " + element.getReferenceNumber());
                }
            } else {
                pending = new ArrayList<InmPendingResponse>();
            }
        } catch (IOException ex) {
            LOG.debug("IOExeption: "+ex.getMessage());
        } catch (ClassNotFoundException ex) {
            LOG.debug("ClassNotFoundException", ex);
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ex) {
                LOG.debug("IOExeption", ex);
            }
        }
    }

    public void addPendingPollResponse(InmPendingResponse resp) {
        if(pending!=null){
            pending.add(resp);
            LOG.debug("Pending response added");
        }
    }

    public boolean removePendingPollResponse(InmPendingResponse resp) {
        if(pending!=null){
            LOG.debug("Trying to remove pending poll response");
            return pending.remove(resp);
        }
        return false;
    }

    public ArrayList<InmPendingResponse> getPendingPollResponses() {
        return (ArrayList<InmPendingResponse>) pending.clone();
    }

    public boolean containsPendingPollResponse(InmPendingResponse resp) {
        if(pending!=null)
            return pending.contains(resp);  
        return false;
    }

    public InmPendingResponse continsPollTo(String dnid, String memberId) {
        for (InmPendingResponse element : pending) {
            if (element.dnId.equalsIgnoreCase(dnid) && element.membId.equalsIgnoreCase(memberId)) {
                return element;
            }
        }
        return null;
    }

    @PreDestroy
    public void writePendingPollResponse() {
        ObjectOutputStream oos = null;
        FileOutputStream fos = null;
        try {
            File f = new File(startUp.getPollPath(), fileName);
            fos = new FileOutputStream(f, false);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(pending);
            oos.flush();
            LOG.debug("Wrote " + pending.size() + " pending responses");
        } catch (IOException ex) {
            LOG.debug("IOExeption", ex);
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ex) {
                LOG.debug("IOExeption", ex);
            }
        }
    }
}