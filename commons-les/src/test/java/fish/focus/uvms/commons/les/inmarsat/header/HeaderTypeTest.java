package fish.focus.uvms.commons.les.inmarsat.header;

import org.junit.Test;
import static fish.focus.uvms.commons.les.inmarsat.header.HeaderType.DNID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class HeaderTypeTest {
	@Test
	public void getValue() throws Exception {
		assertEquals(1, DNID.getValue());


	}

	@Test
	public void getLength() throws Exception {
		assertEquals(22, DNID.getHeaderLength());
	}


	@Test
	public void fromInt() throws Exception {
		assertEquals(HeaderType.DNID, HeaderType.fromInt(1));
		assertNotEquals(HeaderType.DNID, HeaderType.fromInt(2));
	}

}
