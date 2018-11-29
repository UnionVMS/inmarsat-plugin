package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Startup
@Singleton
public class SocketServer {

    @PostConstruct
    private void startup() {

        System.out.println("hepp");

    }

    @PreDestroy
    private void shutdown() {
        System.out.println("hopp");
    }




}
