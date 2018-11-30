package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.uvms.plugins.inmarsat.telnetserversimulator.CommandDownLoad;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.telnetserversimulator.TelnetSession;
import org.apache.commons.net.telnet.TelnetClient;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.*;
import java.util.concurrent.ConcurrentMap;


@RunWith(Arquillian.class)
public class TestInmarsatPlugin extends _BuildTestDeployment {

    @Inject
    private InmarsatPlugin startupBean;


    String host = "localhost";
    int port = 9090;
    String username = "Allan";
    String password = "KVACK";



    @Test
    @OperateOnDeployment("normal")
    public void testDNID() throws TelnetException {
        try (TelnetSession c = new TelnetSession(host, port).connect()) {



            c.waitFor("name:");
            c.println(username);
            c.waitFor("word");
            c.println(password);
            String response = c.response();
            if (!response.equals(">")) {
                System.out.println(response);
            }

            CommandDownLoad commandDownload = new CommandDownLoad();

            String queryFor = "10745";

            InputStream in = c.getInputStream();
            OutputStream out = c.getOutputStream();
            long s = System.currentTimeMillis();

            byte[] reponse = commandDownload.sendDownloadCommand(out, in,  queryFor);

            System.out.println(new String(response));

            c.println("quit");
            //c.waitFor("bye");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }







    private String sendPollCommand(PrintStream out, InputStream in) throws TelnetException, IOException {

        String prompt = ">";
        String cmd = "DNID 12345 1";
        write(cmd, out);
        String ret = readUntil(prompt, in);
        return ret;
    }




    @Test
    @OperateOnDeployment("normal")
    public void testRegisterAndUnregister() {

        boolean NO_EXCEPTION_MEANS_THAT_REGISTER_AND_UNREGISTER_ARE_REACHED_AND_MARSHALL_UNMARSHALL_WORKS = true;
        Assert.assertTrue(NO_EXCEPTION_MEANS_THAT_REGISTER_AND_UNREGISTER_ARE_REACHED_AND_MARSHALL_UNMARSHALL_WORKS);
    }

    @Test
    @OperateOnDeployment("normal")
    public void testSetAndGetSetting() {
        String dnids = startupBean.getSetting("DNIDS");
        Assert.assertEquals("1", dnids);
    }

    @Test
    @OperateOnDeployment("normal")
    public void testSetAndGetSettings() {
        ConcurrentMap<String, String> settings = startupBean.getSettings();
        Assert.assertEquals(5, settings.size());
    }

    @Test
    @OperateOnDeployment("normal")
    public void testSomeProps() {

        startupBean.setIsEnabled(false);
        boolean isEnabled = startupBean.isIsEnabled();
        Assert.assertFalse(isEnabled);
        startupBean.setIsEnabled(true);
        isEnabled = startupBean.isIsEnabled();
        Assert.assertTrue(isEnabled);


        startupBean.setIsRegistered(false);
        boolean isRegistered = startupBean.isIsRegistered();
        Assert.assertFalse(isRegistered);
        startupBean.setIsRegistered(true);
        isRegistered = startupBean.isIsRegistered();
        Assert.assertTrue(isRegistered);

        startupBean.setWaitingForResponse(false);
        boolean waiting = startupBean.isWaitingForResponse();
        Assert.assertFalse(waiting);
        startupBean.setWaitingForResponse(true);
        waiting = startupBean.isWaitingForResponse();
        Assert.assertTrue(waiting);

        String registerClassName = startupBean.getRegisterClassName();
        Assert.assertNotNull(registerClassName);

    }





    /*
    private void pollTest() {
        CommandType command = new CommandType();
        command.setCommand(CommandTypeType.POLL);
        command.setPluginName(getPLuginApplicationProperty("application.name"));

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

        InmarsatPoll p = new InmarsatPoll();
        p.setPollType(poll);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Pollcommand: {} ", p.asCommand());
        }
        command.setPoll(poll);
        setCommand(command);


    }
    */


    private String readUntil(String pattern, InputStream in) throws TelnetException, IOException {

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
                    return "ERROR";
                }
            }
        } while (bytesRead >= 0);

        throw new TelnetException("Unknown response from Inmarsat-C LES Telnet @ "  + sb.toString());
    }

    private void write(String value, PrintStream out) {

        out.println(value);
        out.flush();
    }

    private void sendPwd(PrintStream output, String pwd) {

        output.print(pwd + "\r\n");
        output.flush();
    }




}
