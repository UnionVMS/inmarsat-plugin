package eu.europa.ec.fisheries.uvms.plugins.inmarsat.telnetserversimulator;

import eu.europa.ec.fisheries.uvms.plugins.inmarsat.TelnetException;

import java.io.*;

public class CommandHelper {

    public void write(String value, OutputStream out) throws IOException {
        out.write(value.getBytes());
        out.flush();
    }

    public byte[] readUntil(String pattern, InputStream in)
            throws TelnetException, IOException {
        String[] faultPatterns =
                {"????????", "[Connection to 41424344 aborted: error status 0]", "Illegal address parameter."};
        StringBuffer stringbuffer = new StringBuffer();
        byte[] bytebuffer = new byte[1024];
        int bytesRead = 0;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        do {
            bytesRead = in.read(bytebuffer);
            if (bytesRead > 0) {
                String s = new String(bytebuffer, 0, bytesRead);
                stringbuffer.append(s);
                stream.write(bytebuffer, 0, bytesRead);
                if (stringbuffer.toString().trim().endsWith(pattern)) {
                    stream.flush();
                    return stream.toByteArray();
                } else {
                    for (String faultPattern : faultPatterns) {
                        if (stringbuffer.toString().trim().contains(faultPattern)) {
                            throw new TelnetException("Error while reading");
                        }
                    }
                }
            }
        } while (bytesRead >= 0);
        throw new TelnetException(
                "Unknown response from Inmarsat-C LES Telnet @ ");
    }
}
