package fish.focus.uvms.commons.les.inmarsat;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class InmarsatConfigTest {

	@Test
	public void test() throws Exception {
		assertEquals(InmarsatConfig.DEFAULT_EXTRA_DATA_ENABLED, InmarsatConfig.getInstance().isExtraDataEnabled());
		assertEquals(InmarsatConfig.DEFAULT_EXTRA_DATA_FORMAT, InmarsatConfig.getInstance().getExtraDataFormat());
		InmarsatConfig.getInstance().setExtraDataEnabled(true);
		assertEquals(true, InmarsatConfig.getInstance().isExtraDataEnabled());
		InmarsatConfig.getInstance().setExtraDataFormat(2);
		assertEquals(2, InmarsatConfig.getInstance().getExtraDataFormat());

		InmarsatConfig.getInstance().setToDefault();
		assertEquals(InmarsatConfig.DEFAULT_EXTRA_DATA_ENABLED, InmarsatConfig.getInstance().isExtraDataEnabled());
		assertEquals(InmarsatConfig.DEFAULT_EXTRA_DATA_FORMAT, InmarsatConfig.getInstance().getExtraDataFormat());


	}

}
