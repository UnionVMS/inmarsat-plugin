package fish.focus.uvms.commons.les.inmarsat;

import fish.focus.uvms.commons.les.inmarsat.header.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class InmarsatHeaderTest {

	private final byte[] header;
	private final String headerHexString;

	private final HeaderData headerData;

	private final InmarsatHeader iHeader;

	public InmarsatHeaderTest(String headerHex, HeaderData headerData) throws InmarsatException {
		header = InmarsatUtils.hexStringToByteArray(headerHex);
		headerHexString = headerHex;
		iHeader = InmarsatHeader.createHeader(header);
		this.headerData = headerData;

	}

	@Parameterized.Parameters
	public static Collection<Object[]> data() throws ParseException {

		return Arrays.asList(new Object[][] {{"015426540116890b08000255140036372455c307e70",
				new HeaderDataBuilder().setType(HeaderType.DNID).setRefno(527241)
						.setDataPresentation(HeaderDataPresentation.TRANS_DATA).setSatIdAndLesId(121).setDataLength(20)
						.setStoredTime("2015-04-07 19:59:50").setDnid(1987).setMemNo(231).createHeaderData()},
				{"01542654021993c30c00024c20003f37364ade403521591d0",
						new HeaderDataBuilder().setType(HeaderType.DNID_MSG).setRefno(836499)
								.setDataPresentation(HeaderDataPresentation.TRANS_DATA).setSatIdAndLesId(112)
								.setDataLength(0x20).setStoredTime("2009-06-15 11:57:51").setDnid(0x40de)
								.setMesNo(492380469).createHeaderData()},
				{"01542654051593c30c0063023f37364a3521591d0",
						new HeaderDataBuilder().setType(HeaderType.NDN).setRefno(0x000cc393).setFailureReason(0x63)
								.setDeliveryAttempts(0x02).setStoredTime("2009-06-15 11:57:51").setMesNo(0x1d592135)
								.createHeaderData()},
				{"0154265401163AA80C00024414003369AD59F929FF0",
						new HeaderDataBuilder().setType(HeaderType.DNID).setRefno(829498)
								.setDataPresentation(HeaderDataPresentation.TRANS_DATA).setSatIdAndLesId(14)
								.setDataLength(20).setStoredTime("2017-09-04 14:54:43").setDnid(10745).setMemNo(255)
								.createHeaderData()}


		});
	}


	@Test
	public void isValidHeader() {
		assertTrue(InmarsatHeader.validate(header));
	}


	@Test
	public void isValidHeaderNegativeEOH() {
		byte[] cloneHeader = header.clone();
		cloneHeader[cloneHeader.length - 1] = 0x1;
		iHeader.header = cloneHeader;
		assertFalse(iHeader.validate());
	}


	@Test
	public void isValidHeaderNegativeSOH() {
		byte[] cloneHeader = header.clone();
		cloneHeader[0] = 0x2;
		iHeader.header = cloneHeader;
		assertFalse(iHeader.validate());

	}

	@Test
	public void isValidHeaderNegativeLength() {
		byte[] cloneHeader = header.clone();
		cloneHeader[HeaderStruct.POS_HEADER_LENGTH] = 7;
		iHeader.header = cloneHeader;
		assertFalse(iHeader.validate());
	}


	@Test
	public void isValidHeaderNegativeLeadText() {
		byte[] cloneHeader = header.clone();
		cloneHeader[HeaderStruct.POS_LEAD_TEXT_0] = 0x55; // "U&T"
		iHeader.header = cloneHeader;
		assertFalse(iHeader.validate());
	}

	@Test
	public void isValidHeaderNegativeType() {
		byte[] cloneHeader = header.clone();
		cloneHeader[HeaderStruct.POS_TYPE] = 8;
		iHeader.header = cloneHeader;
		assertFalse(iHeader.validate());
	}

	@Test
	public void getStartOfHeader() {
		assertEquals(InmarsatDefinition.API_SOH, iHeader.getStartOfHeader());
		assertEquals(InmarsatDefinition.API_SOH, iHeader.header[HeaderStruct.POS_START_OF_HEADER_POS]);
	}

	@Test
	public void getLeadText() {
		assertEquals(InmarsatDefinition.API_LEAD_TEXT, new String(iHeader.getLeadText()));
	}

	@Test
	public void getType() {
		assertEquals(headerData.getType(), iHeader.getType());
	}

	@Test
	public void getHeaderLength() {
		assertEquals(headerData.getType().getHeaderLength(), iHeader.getHeaderLength());
	}

	@Test
	public void getRefNo() {
		assertEquals(headerData.getRefno(), iHeader.getRefNo());
	}

	@Test
	public void getDataPresentation() {
		assertEquals(headerData.getDataPresentation(), iHeader.getDataPresentation());
	}

	@Test
	public void getFailureReason() {
		assertEquals(headerData.getFailureReason(), iHeader.getFailureReason());
	}

	@Test
	public void getDeliveryAttempts() {
		assertEquals(headerData.getDeliveryAttempts(), iHeader.getDeliveryAttempts());
	}

	@Test
	public void getSatIdAndLesId() {
		assertEquals(headerData.getSatIdAndLesId(), iHeader.getSatIdAndLesId());
	}

	@Test
	public void getSatId() {
		Integer position = iHeader.getType().getHeaderStruct().getPositionSatIdAndLesId();
		if (headerData.getSatIdAndLesId() > 0) {
			SatId idExpected = SatId.fromInt(
					InmarsatUtils.digitAt(headerData.getSatIdAndLesId(), headerData.getSatIdAndLesId() >= 100 ? 3 : 2));
			assertEquals(idExpected, iHeader.getSatId(position));
		} else {
			assertNull(position);
		}

	}

	@Test
	public void getLesId() {
		Integer position = iHeader.getType().getHeaderStruct().getPositionSatIdAndLesId();
		if (headerData.getSatIdAndLesId() > 0) {

			int idExpected = headerData.getSatIdAndLesId() % (headerData.getSatIdAndLesId() >= 100 ? 100 : 10);
			assertEquals(idExpected, iHeader.getLesId(position));
		} else {
			assertNull(position);
		}

	}

	@Test
	public void getDataLength() {

		assertEquals(headerData.getDataLength(), iHeader.getDataLength());
	}

	@Test
	public void getStoredTime() throws ParseException {
		assertEquals(headerData.getStoredTime(), iHeader.getStoredTime());
	}

	@Test
	public void getDnid() {
		assertEquals(headerData.getDnid(), iHeader.getDnid());
	}

	@Test
	public void getMemberNo() {
		assertEquals(headerData.getMemNo(), iHeader.getMemberNo());
	}

	@Test
	public void getMesMobNo() {
		assertEquals(headerData.getMesNo(), iHeader.getMesMobNo());
	}

	@Test
	public void getEndOfHeader() {
		assertEquals(InmarsatDefinition.API_EOH, iHeader.getEndOfHeader());
		assertEquals(InmarsatDefinition.API_EOH, iHeader.header[iHeader.getHeaderLength() - 1]);
	}

	@Test
	public void getHeaderAsHexString() {
		assertEquals(headerHexString.toUpperCase(), iHeader.getHeaderAsHexString());
	}


}

