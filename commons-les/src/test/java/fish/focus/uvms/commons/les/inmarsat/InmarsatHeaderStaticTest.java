package fish.focus.uvms.commons.les.inmarsat;

import fish.focus.uvms.commons.les.inmarsat.header.HeaderDataBuilder;
import fish.focus.uvms.commons.les.inmarsat.header.HeaderDataPresentation;
import fish.focus.uvms.commons.les.inmarsat.header.HeaderType;
import fish.focus.uvms.commons.les.inmarsat.header.SatId;
import org.junit.Test;
import java.text.ParseException;
import static org.junit.Assert.assertEquals;

public class InmarsatHeaderStaticTest {

	@Test
	public void createSatIdAndLesIdInt() throws Exception {
		assertEquals(0b01001100, InmarsatHeader.createSatIdAndLesIdByte(112));
		assertEquals((byte) 0b10111111, InmarsatHeader.createSatIdAndLesIdByte(263));
		assertEquals((byte) 0b11100000, InmarsatHeader.createSatIdAndLesIdByte(332));
	}


	@Test
	public void createSatIdAndLesIdByte() throws Exception {
		assertEquals(0b01001100, InmarsatHeader.createSatIdAndLesIdByte(SatId.AORE, 12));
		assertEquals((byte) 0b11111111, InmarsatHeader.createSatIdAndLesIdByte(SatId.IOR, 63));
		assertEquals((byte) 0b10100000, InmarsatHeader.createSatIdAndLesIdByte(SatId.POR, 32));
	}

	@Test
	public void createHeaderFromHeaderData() throws InmarsatException, ParseException {
		assertEquals("015426540116890b08000255140036372455c307e702".toUpperCase(),
				InmarsatHeader.createHeader(new HeaderDataBuilder().setType(HeaderType.DNID).setRefno(527241)
						.setDataPresentation(HeaderDataPresentation.TRANS_DATA).setSatIdAndLesId(121).setDataLength(20)
						.setStoredTime("2015-04-07 19:59:50").setDnid(1987).setMemNo(231).createHeaderData())
						.getHeaderAsHexString());

		assertEquals("01542654021993c30c00024c20003f37364ade403521591d02".toUpperCase(),
				InmarsatHeader.createHeader(new HeaderDataBuilder().setType(HeaderType.DNID_MSG).setRefno(836499)
						.setDataPresentation(HeaderDataPresentation.TRANS_DATA).setSatIdAndLesId(112).setDataLength(32)
						.setStoredTime("2009-06-15 11:57:51").setDnid(16606).setMesNo(492380469).createHeaderData())
						.getHeaderAsHexString());


	}

}
