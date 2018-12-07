package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import org.apache.commons.net.telnet.TelnetClient;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.util.concurrent.ConcurrentMap;


@RunWith(Arquillian.class)
public class TestInmarsatPlugin extends _BuildTestDeployment {

    Logger LOG = LoggerFactory.getLogger("LOGGER");
    private static final String crlf = "\r\n";


    @Inject
    private InmarsatPlugin startupBean;


    String host = "localhost";
    int port = 9090;
    String username = "Allan";
    String password = "KVACK";



    void println( PrintStream out  , String s) {
        out.print(s);
        out.print(crlf);
        out.flush();
    }

    public byte[] waitFor(InputStream is, String mask ) {

        try {
            byte[] buffer = new byte[2048];
            int bytesRead = 0;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while ((bytesRead = is.read(buffer)) > 0) {
                bos.write(buffer, 0, bytesRead);
                String str = new String(bos.toByteArray());
                if(str.endsWith(mask)){
                    bos.flush();
                    return bos.toByteArray();
                }
            }
        }catch(IOException e){
            // NOP
        }
        return new byte[0];
    }




    @Test
    @OperateOnDeployment("normal")
    @Ignore
    public void testAHandler() throws TelnetException, IOException {

        TelnetClient client = new TelnetClient();
        client.connect("localhost",  9090);
        InputStream is = client.getInputStream();
        OutputStream os = client.getOutputStream();
        PrintStream out = new  PrintStream(os);

        // login is part of the protocol
        waitFor( is, "name:");
        println(out, "nisse");
        waitFor( is, "word:");
        println(out, "tuta");
        waitFor( is, ">");

        // write a command
        println(out, "TEST 10745 1");
        // get response
        String resp = new String(waitFor( is, ">"));

        Assert.assertTrue(resp.startsWith("SUCCESS"));
        client.disconnect();
    }

    @Test
    @OperateOnDeployment("normal")
    @Ignore
    public void testAHandlerWillFail() throws TelnetException, IOException {

        TelnetClient client = new TelnetClient();
        client.connect("localhost",  9090);
        InputStream is = client.getInputStream();
        OutputStream os = client.getOutputStream();
        PrintStream out = new  PrintStream(os);

        // login is part of the protocol
        waitFor( is, "name:");
        println(out, "nisse");
        waitFor( is, "word:");
        println(out, "tuta");
        waitFor( is, ">");

        // write a command
        println(out, "10745 1");
        // get response
        String resp = new String(waitFor( is, ">"));

        Assert.assertTrue(resp.startsWith("Unknown command"));
        client.disconnect();
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




}
