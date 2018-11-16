package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentMap;


@RunWith(Arquillian.class)
public class TestRetrieverBeanIT extends _BuildTestDeployment {

    @Inject
    private InmarsatPlugin startupBean;

    @Test
    @OperateOnDeployment("normal")
    public void test() {
        String dnids = startupBean.getSetting("DNIDS");
        Assert.assertEquals("1", dnids);
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





}
