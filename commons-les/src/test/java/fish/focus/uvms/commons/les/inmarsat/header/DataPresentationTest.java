package fish.focus.uvms.commons.les.inmarsat.header;

import fish.focus.uvms.commons.les.inmarsat.header.HeaderDataPresentation;
import org.junit.Test;
import static fish.focus.uvms.commons.les.inmarsat.header.HeaderDataPresentation.*;
import static org.junit.Assert.*;

public class DataPresentationTest {
	@Test
	public void getValue() throws Exception {
		assertEquals(1, IA5.getValue());
		assertEquals(2, TRANS_DATA.getValue());
	}

	@Test
	public void fromInt() throws Exception {
		assertEquals(IA5, HeaderDataPresentation.fromInt(IA5.getValue()));
		assertNull(HeaderDataPresentation.fromInt(3));
	}

}
