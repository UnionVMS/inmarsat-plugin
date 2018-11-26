package fish.focus.uvms.commons.les.inmarsat.header;

import org.junit.Test;
import static fish.focus.uvms.commons.les.inmarsat.header.HeaderByte.*;
import static org.junit.Assert.*;

public class HeaderByteTest {
	@Test
	public void getValue() throws Exception {
		assertEquals(10, DATA_LENGTH.getValue());
		assertEquals(5, MSG_REF_NO.getValue());
	}

	@Test
	public void fromInt() throws Exception {
		assertEquals(DATA_LENGTH, HeaderByte.fromInt(DATA_LENGTH.getValue()));
		assertNull(HeaderByte.fromInt(33));
	}

}
