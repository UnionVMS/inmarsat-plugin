package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.KeyValueType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollTypeType;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmarsatPlugin;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmarsatPoll;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat._TransactionalTests;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.concurrent.ConcurrentMap;


@RunWith(Arquillian.class)
public class TestRetrieverBeanIT extends _TransactionalTests {

   // private JMSHelper jmsHelper = new JMSHelper();
    private ConcurrentMap<String, String> settingsAsString ;

    @Inject
    private InmarsatPlugin startupBean;


    @Before
    public void before(){
        startupBean.getSettings().put("DNID-123","DNID-123");
        startupBean.getSettings().put("123456","123456");
        startupBean.getSettings().put("ABC","ABC");
        startupBean.getSettings().put("DNIDS","DNIDS");
        settingsAsString = startupBean.getSettings();
    }

    @Test
    public void testSetAndGetSetting() {
        String dnids = startupBean.getSetting("DNIDS");
        Assert.assertEquals("DNIDS", dnids);
    }

    @Test
    public void testSetAndGetSettings() {
        ConcurrentMap<String, String> dnids = startupBean.getSettings();
        Assert.assertEquals(settingsAsString, dnids);
    }

    @Test
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
