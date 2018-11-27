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
package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollTypeType;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmarsatPoll.OceanRegion;
import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.LocalBean;
import java.io.*;
import java.net.SocketException;

@LocalBean
public class InmarsatPollConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatPollConnection.class);
    private static final String[] faultPatterns = {
            "????????", "[Connection to 41424344 aborted: error status 0]", "Illegal address parameter."
    };


    public String poll(PollType poll, String url, String port, String user, String pwd, String dnids) throws TelnetException {

        String response = null;
        TelnetClient telnet = null;
        try {
            telnet = new TelnetClient();
            telnet.connect(url, Integer.parseInt(port));

            BufferedInputStream input = new BufferedInputStream(telnet.getInputStream());
            PrintStream output = new PrintStream(telnet.getOutputStream());
            readUntil("name:", input,  url, port);
            write(user, output);
            readUntil("word:", input,  url, port);
            sendPwd(output, pwd);
            readUntil(">", input,  url, port);
            response = sendPollCommand(poll, output, input,   url, port);

        } catch (SocketException ex) {
            LOGGER.error("Error when communicating with Telnet", ex);
            throw new TelnetException(ex);
        } catch (IOException ex) {
            LOGGER.error("Error when communicating with Telnet", ex);
            throw new TelnetException(ex);
        } catch (NullPointerException ex) {
            LOGGER.error(ex.toString(), ex);
            throw new TelnetException(ex);
        } finally {
            if (telnet != null && telnet.isConnected()) {
                try {
                    telnet.disconnect();
                } catch (IOException e) {
                    // intentionally left blank
                }
            }
        }
        return response;
    }

    private void sendPwd(PrintStream output, String pwd) {

        output.print(pwd + "\r\n");
        output.flush();
    }

    private void write(String value, PrintStream out) {

        out.println(value);
        out.flush();
        LOGGER.debug("write:{}", value);
    }

    private String buildCommand(PollType poll, OceanRegion oceanRegion) {

        InmarsatPoll inmPoll = new InmarsatPoll();
        inmPoll.setPollType(poll);
        inmPoll.setOceanRegion(oceanRegion);
        if (poll.getPollTypeType() == PollTypeType.CONFIG) inmPoll.setAck(InmarsatPoll.AckType.TRUE);
        return inmPoll.asCommand();
    }



    /**
     * Sends one or more poll commands, one for each ocean region, until a reference number is
     * received.
     *
     * @return result of first successful poll command, or null if poll failed on every ocean region
     */
    private String sendPollCommand(PollType poll, PrintStream out, InputStream in, String url, String port) throws TelnetException, IOException {

        for (OceanRegion oceanRegion : OceanRegion.values()) {
            String result = sendPollCommand(poll, out, in, oceanRegion, url, port);
            if (result.contains("Reference number")) {
                return result;
            }
        }
        return null;
    }

    private String sendPollCommand(PollType poll, PrintStream out, InputStream in, OceanRegion oceanRegion, String url, String port) throws TelnetException, IOException {

        String prompt = ">";
        String cmd = buildCommand(poll, oceanRegion);
        String ret;
        write(cmd, out);
        ret = readUntil("Text:", in, url, port);
        write(".S", out);
        ret += readUntil(prompt, in, url, port);
        return ret;
    }

    private String readUntil(String pattern, InputStream in, String url, String port) throws TelnetException, IOException {

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
                    containsFault(currentString, url, port);
                }
            }
        } while (bytesRead >= 0);

        throw new TelnetException("Unknown response from Inmarsat-C LES Telnet @ " + url + ":" + port + ": " + sb.toString());
    }


    private void containsFault(String currentString, String url, String port) throws TelnetException {

        for (String faultPattern : faultPatterns) {
            if (currentString.trim().contains(faultPattern)) {
                LOGGER.error(
                        "Error while reading from Inmarsat-C LES Telnet @ {}:{}: {}", url, port, currentString);
                throw new TelnetException(
                        "Error while reading from Inmarsat-C LES Telnet @ " + url + ":" + port + ": " + currentString);
            }
        }
    }









}
