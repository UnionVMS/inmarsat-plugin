package fish.focus.uvms.commons.les.inmarsat.header;

import org.junit.Test;
import static org.junit.Assert.*;

public class HeaderStructTest {


	@Test
	public void isPresentation() throws Exception {
		HeaderStruct headerStruct = new HeaderStructBuilder().createHeaderStruct();
		assertTrue(headerStruct.isPresentation());

		headerStruct = new HeaderStructBuilder().disablePresentation().createHeaderStruct();
		assertFalse(headerStruct.isPresentation());
	}

	@Test
	public void isFailure() throws Exception {
		HeaderStruct headerStruct = new HeaderStructBuilder().createHeaderStruct();
		assertTrue(headerStruct.isFailure());

		headerStruct = new HeaderStructBuilder().disableFailure().createHeaderStruct();
		assertFalse(headerStruct.isFailure());
	}

	@Test
	public void isDelivery() throws Exception {
		HeaderStruct headerStruct = new HeaderStructBuilder().createHeaderStruct();
		assertTrue(headerStruct.isDelivery());

		headerStruct = new HeaderStructBuilder().disableDelivery().createHeaderStruct();
		assertFalse(headerStruct.isDelivery());
	}

	@Test
	public void isSatIdAndLesId() throws Exception {
		HeaderStruct headerStruct = new HeaderStructBuilder().createHeaderStruct();
		assertTrue(headerStruct.isSatIdAndLesId());

		headerStruct = new HeaderStructBuilder().disableSatIdAndLesId().createHeaderStruct();
		assertFalse(headerStruct.isSatIdAndLesId());
	}

	@Test
	public void isDataLength() throws Exception {
		HeaderStruct headerStruct = new HeaderStructBuilder().createHeaderStruct();
		assertTrue(headerStruct.isDataLength());

		headerStruct = new HeaderStructBuilder().disableDataLength().createHeaderStruct();
		assertFalse(headerStruct.isDataLength());
	}

	@Test
	public void isDnid() throws Exception {
		HeaderStruct headerStruct = new HeaderStructBuilder().createHeaderStruct();
		assertTrue(headerStruct.isDnid());

		headerStruct = new HeaderStructBuilder().disableDnid().createHeaderStruct();
		assertFalse(headerStruct.isDnid());
	}

	@Test
	public void isMemberNo() throws Exception {
		HeaderStruct headerStruct = new HeaderStructBuilder().createHeaderStruct();
		assertTrue(headerStruct.isMemberNo());

		headerStruct = new HeaderStructBuilder().disableMemberNo().createHeaderStruct();
		assertFalse(headerStruct.isMemberNo());
	}

	@Test
	public void isMesMobNo() throws Exception {
		HeaderStruct headerStruct = new HeaderStructBuilder().createHeaderStruct();
		assertTrue(headerStruct.isMesMobNo());

		headerStruct = new HeaderStructBuilder().disableMesMobNo().createHeaderStruct();
		assertFalse(headerStruct.isMesMobNo());
	}


	@Test
	public void getLength() throws Exception {

		HeaderStruct headerStruct = new HeaderStructBuilder().createHeaderStruct();
		assertEquals(getAllLength(), headerStruct.getHeaderLength());

		headerStruct = new HeaderStructBuilder().disableMesMobNo().createHeaderStruct();
		assertEquals(getAllLength() - HeaderByte.MES_MOB_NO.getNoOfBytes(), headerStruct.getHeaderLength());

		headerStruct = new HeaderStructBuilder().disablePresentation().disableFailure().disableDelivery()
				.disableSatIdAndLesId().disableDataLength().disableDnid().disableMemberNo().disableMesMobNo()
				.createHeaderStruct();
		assertEquals(getBaseLength(), headerStruct.getHeaderLength());

		headerStruct = new HeaderStructBuilder().disableMesMobNo().disableFailure().createHeaderStruct();
		assertEquals(getAllLength() - HeaderByte.MES_MOB_NO.getNoOfBytes() - HeaderByte.FAILURE_RESON.getNoOfBytes(),
				headerStruct.getHeaderLength());

	}

	@Test
	public void getLengthAllEnabled() throws Exception {

		HeaderStruct headerStruct = new HeaderStructBuilder().enableAll().createHeaderStruct();
		assertEquals(getAllLength(), headerStruct.getHeaderLength());

	}

	private int getAllLength() {
		return getBaseLength() + HeaderByte.PRESENTATION.getNoOfBytes() + HeaderByte.FAILURE_RESON.getNoOfBytes()
				+ HeaderByte.DELIVERY_ATTEMPTS.getNoOfBytes() + HeaderByte.SATID_AND_LESID.getNoOfBytes()
				+ HeaderByte.DATA_LENGTH.getNoOfBytes() + HeaderByte.DNID.getNoOfBytes()
				+ HeaderByte.MEMBER_NO.getNoOfBytes() + HeaderByte.MES_MOB_NO.getNoOfBytes();
	}

	private int getBaseLength() {
		return HeaderByte.START_OF_HEADER.getNoOfBytes() + HeaderByte.LEAD_TEXT.getNoOfBytes()
				+ HeaderByte.TYPE.getNoOfBytes() + HeaderByte.HEADER_LENGTH.getNoOfBytes()
				+ HeaderByte.MSG_REF_NO.getNoOfBytes() + HeaderByte.STORED_TIME.getNoOfBytes()
				+ HeaderByte.END_OF_HEADER.getNoOfBytes();
	}


	@Test
	public void getPositionStartOfHeader() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().createHeaderStruct();
		assertEquals(HeaderByte.START_OF_HEADER.getNoOfBytes(),
				headerStruct.getPositionStartOfHeader() + HeaderByte.START_OF_HEADER.getNoOfBytes());

	}

	@Test
	public void getPositionLeadText() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().createHeaderStruct();
		assertEquals(headerStruct.getPositionStartOfHeader() + HeaderByte.START_OF_HEADER.getNoOfBytes(),
				headerStruct.getPositionLeadText());
	}

	@Test
	public void getPositionType() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().createHeaderStruct();
		assertEquals(headerStruct.getPositionLeadText() + HeaderByte.LEAD_TEXT.getNoOfBytes(),
				headerStruct.getPositionType());

	}

	@Test
	public void getPositionHeaderLength() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().createHeaderStruct();
		assertEquals(headerStruct.getPositionType() + HeaderByte.TYPE.getNoOfBytes(),
				headerStruct.getPositionHeaderLength().intValue());

	}

	@Test
	public void getPositionMsgRefNo() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().createHeaderStruct();
		assertEquals(headerStruct.getPositionHeaderLength() + HeaderByte.HEADER_LENGTH.getNoOfBytes(),
				headerStruct.getPositionMsgRefNo().intValue());

	}

	@Test
	public void getPositionPresentation() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().enablePresentation().createHeaderStruct();
		assertEquals(headerStruct.getPositionMsgRefNo() + HeaderByte.MSG_REF_NO.getNoOfBytes(),
				headerStruct.getPositionPresentation().intValue());

	}

	@Test
	public void getPositionFailureReason() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().enableFailure().createHeaderStruct();
		assertEquals(headerStruct.getPositionMsgRefNo() + HeaderByte.MSG_REF_NO.getNoOfBytes(),
				headerStruct.getPositionFailureReason().intValue());
	}

	@Test
	public void getPositionDeliveryAttempt() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().enableDelivery().createHeaderStruct();
		assertEquals(headerStruct.getPositionMsgRefNo() + HeaderByte.MSG_REF_NO.getNoOfBytes(),
				headerStruct.getPositionDeliveryAttempts().intValue());
	}

	@Test
	public void getPositionSatIdAndLesId() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().enableSatIdAndLesId().createHeaderStruct();
		assertEquals(headerStruct.getPositionMsgRefNo() + HeaderByte.MSG_REF_NO.getNoOfBytes(),
				headerStruct.getPositionSatIdAndLesId().intValue());
	}

	@Test
	public void getPositionSatIdAndLesIdNotEnabled() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().createHeaderStruct();
		assertNull(headerStruct.getPositionSatIdAndLesId());
	}

	@Test
	public void getPositionDataLength() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().enableDataLength().createHeaderStruct();
		assertEquals(headerStruct.getPositionMsgRefNo() + HeaderByte.MSG_REF_NO.getNoOfBytes(),
				headerStruct.getPositionDataLength().intValue());
	}

	@Test
	public void getPositionStoredTime() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().createHeaderStruct();
		assertEquals(headerStruct.getPositionMsgRefNo() + HeaderByte.MSG_REF_NO.getNoOfBytes(),
				headerStruct.getPositionStoredTime().intValue());
	}

	@Test
	public void getPositionDnid() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().enableDnid().createHeaderStruct();
		assertEquals(headerStruct.getPositionStoredTime() + HeaderByte.STORED_TIME.getNoOfBytes(),
				headerStruct.getPositionDnid().intValue());
	}

	@Test
	public void getPositionMemberNo() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().enableMemberNo().createHeaderStruct();
		assertEquals(headerStruct.getPositionStoredTime() + HeaderByte.STORED_TIME.getNoOfBytes(),
				headerStruct.getPositionMemberNo().intValue());
	}

	@Test
	public void getPositionMesMobNo() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().enableMesMobNo().createHeaderStruct();
		assertEquals(headerStruct.getPositionStoredTime() + HeaderByte.STORED_TIME.getNoOfBytes(),
				headerStruct.getPositionMesMobNo().intValue());
	}

	@Test
	public void getPositionEndOfHeader() {
		HeaderStruct headerStruct = new HeaderStructBuilder().disableAll().createHeaderStruct();
		assertEquals(headerStruct.getPositionStoredTime() + HeaderByte.STORED_TIME.getNoOfBytes(),
				headerStruct.getPositionEndOfHeader().intValue());
	}
}
