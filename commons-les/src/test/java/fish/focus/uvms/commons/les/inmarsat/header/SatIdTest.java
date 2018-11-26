package fish.focus.uvms.commons.les.inmarsat.header;

import fish.focus.uvms.commons.les.inmarsat.header.SatId;
import org.junit.Test;
import static fish.focus.uvms.commons.les.inmarsat.header.SatId.*;
import static org.junit.Assert.*;

public class SatIdTest {
	@Test
	public void getValue() throws Exception {
		assertEquals(1, AORE.getValue());
	}

	@Test
	public void fromInt() throws Exception {
		assertEquals(IOR, SatId.fromInt(IOR.getValue()));
		assertNull(SatId.fromInt(4));
	}

}
