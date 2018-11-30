package eu.europa.ec.fisheries.uvms.plugins.inmarsat.telnetserversimulator;

import eu.europa.ec.fisheries.uvms.plugins.inmarsat.TelnetException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class CommandDownLoad {
    private CommandHelper commandHelper = new CommandHelper();

    public byte[] sendDownloadCommand(OutputStream out, InputStream in, String dnid) throws TelnetException, IOException {
        String prompt = ">";
        String cmd = "DNID " + dnid + " 1";
        commandHelper.write(cmd, out);
        return commandHelper.readUntil(prompt, in);
    }

}
