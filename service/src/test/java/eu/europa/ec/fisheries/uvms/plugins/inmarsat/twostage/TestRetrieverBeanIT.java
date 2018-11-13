package eu.europa.ec.fisheries.uvms.plugins.inmarsat.twostage;

import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmarsatPlugin;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat._TransactionalTests;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import java.util.concurrent.ConcurrentMap;


@RunWith(Arquillian.class)
public class TestRetrieverBeanIT extends _TransactionalTests {


    ConcurrentMap<String, String> settingsAsString ;

    @EJB
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
    public void testEnabled() {

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
        Assert.assertNull(registerClassName);




    }







}
