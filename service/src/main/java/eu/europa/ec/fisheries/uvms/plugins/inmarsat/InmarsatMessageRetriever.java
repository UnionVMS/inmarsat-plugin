package eu.europa.ec.fisheries.uvms.plugins.inmarsat;


import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.KeyValueType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.ReportType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import fish.focus.uvms.commons.les.inmarsat.InmarsatDefinition;
import fish.focus.uvms.commons.les.inmarsat.InmarsatHeader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.jms.Queue;
import javax.jms.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Startup
@Singleton
public class InmarsatMessageRetriever {


    private static final byte[] HEADER_PATTERN = ByteBuffer.allocate(4).put((byte) InmarsatDefinition.API_SOH).put(InmarsatDefinition.API_LEAD_TEXT.getBytes()).array();
    private static final int PATTERN_LENGTH = HEADER_PATTERN.length;


    // API Misc. definitions
    public static final int API_SOH = 1;
    public static final int API_EOH = 2;
    public static final String API_LEAD_TEXT = "T&T";


    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatMessageRetriever.class);
    private static final String INMARSAT_MESSAGES = "jms/queue/UVMSInmarsatMessages";



    @Resource(mappedName = "java:/" + INMARSAT_MESSAGES)
    private Queue inmarsatMessages;

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;


    public static final String PLUGIN_PROPERTIES = "plugin.properties";
    public static final String SETTINGS_PROPERTIES = "settings.properties";
    public static final String CAPABILITIES_PROPERTIES = "capabilities.properties";
    private ConcurrentMap<String, String> settings = null;
    private ConcurrentMap<String, String> capabilities = null;
    private Properties twostageApplicaitonProperties;
    private boolean isEnabled = false;

//    @Inject
//    private PluginMessageProducer messageProducer;

    @Inject
    private HelperFunctions functions;


    public void mapToMapFromProperties(ConcurrentMap<String, String> map, Properties props, String registerClassName) {

        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            if (entry.getKey().getClass().isAssignableFrom(String.class)) {
                String key = (String) entry.getKey();
                if (registerClassName != null) {
                    key = registerClassName.concat("." + key);
                }
                map.put(key, (String) entry.getValue());
            }
        }
    }


    private void initialize() {

        isEnabled = false;
        settings = new ConcurrentHashMap<>();
        capabilities = new ConcurrentHashMap<>();
        twostageApplicaitonProperties = functions.getPropertiesFromFile(this.getClass(), PLUGIN_PROPERTIES);
        Properties twostageProperties = functions.getPropertiesFromFile(this.getClass(), SETTINGS_PROPERTIES);
        Properties twostageCapabilities = functions.getPropertiesFromFile(this.getClass(), CAPABILITIES_PROPERTIES);
        mapToMapFromProperties(settings, twostageProperties, getRegisterClassName());
        mapToMapFromProperties(capabilities, twostageCapabilities, null);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Settings updated in plugin {}", getRegisterClassName());
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                LOGGER.debug("Setting: KEY: {} , VALUE: {}", entry.getKey(), entry.getValue());
            }
        }
    }


    @PostConstruct
    private void startup() {
        initialize();
        LOGGER.info("Inmarsat retriever plugin created");
    }

    @PreDestroy
    private void shutdown() {
        LOGGER.info("Inmarsat retriever plugin destroyed");
    }


    //    @Schedule(second = "15", minute = "*", hour = "*", persistent = false)
    @Schedule(minute = "*/3", hour = "*", persistent = false)
    private void retrieveMessages() {

        LOGGER.info("HEARTBEAT retrieveMessages running. IsEnabled=" + isEnabled + " threadId=" + Thread.currentThread().toString());
        TelnetClient telnet = null;
        PrintStream output = null;
        try {
            if (isEnabled()) {
                List<String> dnids = getDnids();
                String url = getSetting("URL");
                String port = getSetting("PORT");
                String user = getSetting("USERNAME");
                String pwd = getSetting("PSW");

                telnet = functions.createTelnetClient(url, Integer.parseInt(port));

                // logon
                BufferedInputStream input = new BufferedInputStream(telnet.getInputStream());
                output = new PrintStream(telnet.getOutputStream());
                functions.readUntil("name:", input);
                functions.write(user, output);
                functions.readUntil("word:", input);
                functions.sendPwd(output, pwd);
                functions.readUntil(">", input);

                for (String dnid : dnids) {
                    String status = "NOK";
                    LOGGER.info("Trying to download for :{}", dnid);
                    for (int oceanRegion = 0; oceanRegion < 4; oceanRegion++) {
                        try {
                            String cmd = "DNID " + dnid + " " + oceanRegion;
                            functions.write(cmd, output);
                            try {
                                byte[] bos = readUntil(">", input);
                                if (bos != null && bos.length > 0) {

                                    String messageControl = new String(bos);
                                    int pos = messageControl.indexOf("T&T");
                                    if(pos < 0){
                                        status = "OK";
                                        continue;
                                    }
                                    if (post(bos)) {
                                        status = "OK";
                                    } else {
                                        status = "NOK could not post to queue";
                                    }
                                }
                            } catch (TelnetException tex) {
                                LOGGER.info("Possible reason : Vessel probably not in that Ocean Region " + String.valueOf(oceanRegion), tex);
                            }
                        } catch (NullPointerException ex) {
                            LOGGER.error("Error when communicating with Telnet", ex);
                            status = "NOK Error when communicating with Telnet";
                        }
                    }
                    LOGGER.info(status);
                }
            }
        } catch (Throwable t) {
            LOGGER.error(t.toString(), t);
        } finally {
            if (output != null) {
                output.print("QUIT \r\n");
                output.flush();
            }
            if ((telnet != null) && (telnet.isConnected())) {
                try {
                    telnet.disconnect();
                } catch (IOException e) {
                    // OK
                }
            }
        }
    }


    /**
     * check if stream contains a 1T&T
     *
     * @param bytes
     * @return
     */
    private boolean verifyMessage(byte[] bytes) {
        if ((bytes == null) || (bytes.length < 4)) return false;
        boolean retval = false;

        for (int i = 0; i < (bytes.length - PATTERN_LENGTH); i++) {
            if (InmarsatHeader.isStartOfMessage(bytes, i)) {
                retval = true;
            }
        }
        return retval;
    }

    private boolean post(byte[] msg) {


        if (connectionFactory == null) {
            LOGGER.error("No factory. Cannot send messages to queue");
            return false;
        }

        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, 1);
             MessageProducer producer = session.createProducer(inmarsatMessages);
        ) {
            BytesMessage message = session.createBytesMessage();
            message.setStringProperty("messagesource", "INMARSAT_C");
            message.writeBytes(msg);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(message);
            String msgId = message.getJMSMessageID();
            LOGGER.info("Added to internal queue " + msgId);
            return true;
        } catch (JMSException e) {
            LOGGER.error(e.toString(), e);
            return false;
        }
    }

    private byte[] readUntil(String pattern, BufferedInputStream in) throws TelnetException {

        StringBuilder sb = new StringBuilder();
        byte[] contents = new byte[4096];
        int bytesRead;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            while ((bytesRead = in.read(contents)) > 0) {
                bos.write(contents, 0, bytesRead);
                String s = new String(contents, 0, bytesRead);
                sb.append(s);
                String currentString = sb.toString().trim();
                if (currentString.endsWith(pattern)) {
                    bos.flush();
                    return bos.toByteArray();
                } else {
                    functions.containsFault(currentString);
                }
            }
            bos.flush();
            return new byte[0];

        } catch (IOException ioe) {
            LOGGER.info(ioe.toString(), ioe);
            return new byte[0];
        }

    }


    public String getPluginResponseSubscriptionName() {
        return getRegisterClassName() + "." + getPluginApplicationProperty("application.responseTopicName");
    }

    public String getRegisterClassName() {
        return getPluginApplicationProperty("application.groupid") + "." + getPluginApplicationProperty("application.name");
    }


    private String getPluginApplicationProperty(String key) {
        try {
            return (String) twostageApplicaitonProperties.get(key);
        } catch (Exception e) {
            LOGGER.error("Failed to getSetting for key: " + key, getRegisterClassName());
            return null;
        }
    }

    public String getSetting(String setting) {
        LOGGER.debug("Trying to get setting {}.{}", getRegisterClassName(), setting);
        String settingValue = settings.get(getRegisterClassName() + "." + setting);
        LOGGER.debug("Got setting value for {}.{};{}", getRegisterClassName(), setting, settingValue);
        return settingValue;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void updateSettings(List<SettingType> settings) {
        for (SettingType setting : settings) {
            LOGGER.info("Updating setting: {} = {}", setting.getKey(), setting.getValue());
            this.settings.put(setting.getKey(), setting.getValue());
        }
    }

    /**
     * @return list of DNIDs configured
     */
    private List<String> getDnids() {
        String dnidsSettingValue = getSetting("DNIDS");
        if (StringUtils.isBlank(dnidsSettingValue)) {
            return new ArrayList<>();
        }

        return Arrays.asList(dnidsSettingValue.trim().split(","));
    }


    /**
     * @param msg inmarsat message to send
     */
    private void msgToQue(Object msg) throws RuntimeException {

    }

    public String getApplicationName() {
        try {
            return (String) twostageApplicaitonProperties.get("application.name");
        } catch (Exception e) {
            LOGGER.error("Failed to getSetting for key: application.name", getRegisterClassName());
            return null;
        }
    }


    public AcknowledgeTypeType setReport(ReportType report) {
        return AcknowledgeTypeType.NOK;
    }

    /**
     * Set the config values for the twostage
     *
     * @param settings the settings
     * @return AcknowledgeTypeType
     */
    public AcknowledgeTypeType setConfig(SettingListType settings) {
        LOGGER.info(getRegisterClassName() + ".setConfig()");
        try {
            for (KeyValueType values : settings.getSetting()) {
                LOGGER.debug("Setting [ " + values.getKey() + " : " + values.getValue() + " ]");
                this.settings.put(values.getKey(), values.getValue());
            }
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            LOGGER.error("Failed to set config in {}", getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }
    }

    /**
     * Start the twostage. Use this to enable functionality in the twostage
     *
     * @return AcknowledgeTypeType
     */
    public AcknowledgeTypeType start() {
        LOGGER.info(getRegisterClassName() + ".start()");
        try {
            setEnabled(Boolean.TRUE);
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            setEnabled(Boolean.FALSE);
            LOGGER.error("Failed to start {}", getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }
    }

    /**
     * Start the twostage. Use this to disable functionality in the twostage
     *
     * @return AcknowledgeTypeType
     */
    public AcknowledgeTypeType stop() {
        LOGGER.info(getRegisterClassName() + ".stop()");
        try {
            setEnabled(Boolean.FALSE);
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            setEnabled(Boolean.TRUE);
            LOGGER.error("Failed to stop {}", getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }
    }


}
