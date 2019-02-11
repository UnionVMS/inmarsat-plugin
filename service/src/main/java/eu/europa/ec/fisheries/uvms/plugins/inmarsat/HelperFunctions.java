package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

@Stateless
public class HelperFunctions {

    private static final Logger LOGGER = LoggerFactory.getLogger("InmarsatPlugin");

    private static final String[] faultPatterns = {
            "Illegal poll type parameter",
            "????????",
            "[Connection to 41424344 aborted: error status 0]",
            "Illegal address parameter.",
            "Failed: Cannot reach the mobile",
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
        out.println(value);
        out.flush();
    }


    public String readUntil(String pattern, InputStream in) throws TelnetException, IOException {

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

        throw new TelnetException("Unknown response from Inmarsat-C LES Telnet @   (readUntil) : " + sb.toString());
    }

    public void containsFault(String currentString) throws TelnetException {

        for (String faultPattern : faultPatterns) {
            if (currentString.trim().contains(faultPattern)) {
                throw new TelnetException("Error while reading from Inmarsat-C LES Telnet @ " + ": " + currentString);
            }
        }
    }

    public TelnetClient createTelnetClient(String url, int port) throws IOException {
        TelnetClient telnet = new TelnetClient();
        telnet.connect(url, port);
        return telnet;
    }

    public void sendPwd(PrintStream output, String pwd) {
        output.print(pwd + "\r\n");
        output.flush();
    }


}
