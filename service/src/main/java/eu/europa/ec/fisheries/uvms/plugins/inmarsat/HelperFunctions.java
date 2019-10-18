package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.uvms.plugins.inmarsat.data.InmarsatSocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

@Stateless
public class HelperFunctions {

    private static final Logger LOGGER = LoggerFactory.getLogger("InmarsatPlugin");

    private static final String[] faultPatterns = {
            "Illegal poll type parameter",
            "????????",
            "[Connection to 41424344 aborted: error status 0]",
            "Illegal address parameter.",
            "Failed: Cannot reach the mobile",

            "Cannot reach the mobile",
            "Cannot reach the mobile(s)",
            "No legal address",
            "No data to send",
            "Number of bytes too large",
            "Time string longer than the allowed 39 characters",
            "No DNID file found",
            "Passwords doesn't match",
            "Illegal ocean region parameter",
            "No DNID in mobile's ocean region",
            "Mobile not in ocean region",
            "Illegal field value",
            "Memory shortage",
            "Messagestore full",
            "Unknown type of address",
            "Illegal address",
            "Illegal repetition code",
            "Illegal poll type",
            "Sequencenumber table is full",
            "Option not supported",
            "User is barred",
            "Network is barred",
            "Service is barred",
            "Unknown DNID",
            "Illegal destination",
            "Onestage access is barred",
            "Twostage access is barred",
            "Unknown mailbox",
            "DNID file is full",
            "DNID file is empty",
            "Unknown message",
            "Unknown ENID",
            "No matching message",
            "Message is being processed. Try again later",
            "Message has been rerouted",
            "Message cannot be deleted",
            "Unknown user",
            "Update of userinformation failed",
            "Message has been delivered",
            "Message has been aborted",
            "Message has been deleted",
            "To much data, please be more specific",
            "No message(s)",
            "The service is disabled",
            "Invalid time",
            "Missing user acknowledgment",
            "Traffic limit exceeded, Try again later",
            "Unknown command",
            "Sorry, you have no access to this service",
            "Sorry, you have no access to unreserved data reporting",
            "Sorry, you have no access to DNID management",
            "Sorry, you have no access to multi addressing",
            "Sorry, you have no access to this service",
            "Sorry, this service is only for registered users.",
            "Illegal parameter in view command",
            "Too many commands during this session. Reconnect and try again",
            "Illegal reference number in address command" ,
            "Illegal parameter in delete command",
            "Illegal address parameter",
            "Illegal service code parameter",
            "Illegal repetition code parameter",
            "Illegal priority parameter",
            "Illegal ocean region parameter",
            "Illegal egc parameters",
            "Illegal parameters in program command",
            "Area polls is not allowed for P6=11",
            "Sorry, serial number required",
            "Only a individual poll is allowed for downloading DNID",
            "Illegal member no in download command",
            "Sorry, serial number required",
            "Illegal command type",
            "Illegal ocean region parameter",
            "Illegal poll type parameter",
            "Illegal DNID in poll command parameter",
            "Illegal response type parameter",
            "Illegal member number (0 - 255)",
            "Login incorrect",
            "Command failed",
            "You must enter some text before issuing the '.S' command."
    };

    public Properties getPropertiesFromFile(Class clazz, String fileName) {
        Properties props = new Properties();
        try {
            InputStream inputStream = clazz.getClassLoader().getResourceAsStream(fileName);
            props.load(inputStream);
        } catch (IOException e) {
            LOGGER.debug("Properties file failed to load");
        }
        return props;
    }

    public byte[] readStream(InputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int bytesRead = 0;
        while ((bytesRead = in.read(buffer)) > 0) {
            bos.write(buffer, 0, bytesRead);
        }
        return bos.toByteArray();
    }

    public void write(String value, PrintStream out) {
        out.println(value + "\r\n");
        out.flush();
    }

    public String readUntil(String pattern, InputStream in) throws InmarsatSocketException, IOException {
        StringBuilder sb = new StringBuilder();
        byte[] contents = new byte[1024];
        int bytesRead;

        do {
            bytesRead = in.read(contents);
            if (bytesRead > 0) {
                String s = new String(contents, 0, bytesRead);
                sb.append(s);
                String currentString = sb.toString();
                if (currentString.trim().endsWith(pattern)) {
                    return currentString;
                } else {
                    containsFault(currentString);
                }
            }
        } while (bytesRead >= 0);

        throw new InmarsatSocketException("Unknown response from Inmarsat-C LES Telnet @   (readUntil) : " + sb.toString());
    }

    public void containsFault(String currentString) throws InmarsatSocketException {
        for (String faultPattern : faultPatterns) {
            if (currentString.trim().contains(faultPattern)) {
                throw new InmarsatSocketException("Error while reading from Inmarsat-C LES Telnet @ " + ": " + currentString);
            }
        }
    }

    public void sendPwd(PrintStream output, String pwd) {
        output.print(pwd + "\r\n");
        output.flush();
    }

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
}
