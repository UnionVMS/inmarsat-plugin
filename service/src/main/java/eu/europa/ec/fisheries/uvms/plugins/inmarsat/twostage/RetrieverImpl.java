package eu.europa.ec.fisheries.uvms.plugins.inmarsat.twostage;

import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.KeyValueType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollTypeType;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmPoll;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.StartupBean;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.service.PluginService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;


@Singleton
@Startup
@DependsOn({"StartupBean"})
public class RetrieverImpl implements RetrieverBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetrieverBean.class);
    private final Map<String, Future> connectFutures = new HashMap<>();
    @EJB
    DownLoadService downloadService;
    @EJB
    StartupBean startUp;
    @EJB
    PollService pollService;
    @EJB
    private DownLoadCacheDeliveryBean deliveryBean;
    @EJB
    private PluginService pluginService;
    private Future deliverFuture = null;
    private String cachePath = null;
    private String pollPath = null;

    @PostConstruct
    public void startup() {
        createDirectories();
    }

    public String getCachePath() {
        return cachePath;
    }

    public String getPollPath() {
        return pollPath;
    }

    /*
        [2015-12-11 16:11] Joakim Johansson:
    SERIAL_NUMBER
    [2015-12-11 16:11] Joakim Johansson:
    MOBILE_TERMINAL_ID
    [2015-12-11 16:11] Joakim Johansson:
    CONNECT_ID
    [2015-12-11 16:12] Joakim Johansson:
    LES_SERVICE_NAME
    [2015-12-11 16:12] Joakim Johansson:
    LES_NAME

        */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void createDirectories() {
        File f = new File(startUp.getPLuginApplicationProperty("application.logfile"));
        File dirCache = new File(f.getParentFile(), "cache");
        File dirPoll = new File(f.getParentFile(), "poll");
        if (!dirCache.exists()) {
            dirCache.mkdir();
        }
        if (!dirPoll.exists()) {
            dirPoll.mkdir();
        }
        cachePath = dirCache.getAbsolutePath() + File.separator;
        pollPath = dirPoll.getAbsolutePath() + File.separator;
    }

    @Schedule(minute = "*/3", hour = "*", persistent = false)
    public void connectAndRetrive() {
        if (startUp.isIsEnabled()) {
            List<String> dnids = getDownloadDnids();
            Future<Map<String, String>> future = downloadService.download(getCachePath(), dnids);

            for (String dnid : dnids) {
                connectFutures.put(dnid, future);
            }
        }
    }

    @Schedule(minute = "*/5", hour = "*", persistent = false)
    public void parseAndDeliver() {
        if (startUp.isIsEnabled() && (deliverFuture == null || deliverFuture.isDone())) {
            try {
                deliverFuture = deliveryBean.parseAndDeliver(getCachePath());
            } catch (IOException e) {
                LOGGER.error("Couldn't deliver ");
            }
        } else {
            LOGGER.debug("deliverFuture is not null and busy");
        }
    }

    public void pollTest() {
        CommandType command = new CommandType();
        command.setCommand(CommandTypeType.POLL);
        command.setPluginName(startUp.getPLuginApplicationProperty("application.name"));

        command.setTimestamp(new Date());

        PollType poll = new PollType();
        poll.setPollId("123");
        poll.setPollTypeType(PollTypeType.POLL);
        KeyValueType kv = new KeyValueType();
        kv.setKey("DNID");
        kv.setValue("10745");
        poll.getPollReceiver().add(kv);

        KeyValueType kv1 = new KeyValueType();
        kv1.setKey("MEMBER_NUMBER");
        kv1.setValue("255");
        poll.getPollReceiver().add(kv1);

        KeyValueType kv2 = new KeyValueType();
        kv2.setKey("SERIAL_NUMBER");
        kv2.setValue("426509712");
        poll.getPollReceiver().add(kv2);

        InmPoll p = new InmPoll();
        p.setPollType(poll);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Pollcommand: {} ", p.asCommand());
        }
        command.setPoll(poll);
        pluginService.setCommand(command);
    }

    /**
     * @return returns DNIDs available for download
     */
    public List<String> getDownloadDnids() {
        List<String> downloadDnids = new ArrayList<>();
        List<String> dnidList = getDnids();
        for (String dnid : dnidList) {
            Future existingFuture = connectFutures.get(dnid);
            if (!downloadDnids.contains(dnid) && (existingFuture == null || existingFuture.isDone())) {
                downloadDnids.add(dnid);
            }
        }

        return downloadDnids;
    }

    /**
     * @return list of DNIDs configured
     */
    public  List<String> getDnids() {
        String dnidsSettingValue = startUp.getSetting("DNIDS");
        if (StringUtils.isBlank(dnidsSettingValue)) {
            return new ArrayList<>();
        }

        return Arrays.asList(dnidsSettingValue.trim().split(","));
    }



}
