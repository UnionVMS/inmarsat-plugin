package fish.focus.uvms.commons.les.inmarsat.body;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

public class MemCodeTest {
	@Test
	public void getValue() throws Exception {
		Assert.assertEquals(0x45, MemCode.ANTENNA_BLOCKED_OR_JAMMED.getValue());
	}

	@Test
	public void fromInt() throws Exception {
		Assert.assertEquals(MemCode.IO_PIN_EVENT, MemCode.fromInt(MemCode.IO_PIN_EVENT.getValue()));
		assertNull(MemCode.fromInt(4));
	}
}
