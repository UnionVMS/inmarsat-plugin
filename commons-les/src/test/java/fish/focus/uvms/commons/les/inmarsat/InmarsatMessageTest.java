package fish.focus.uvms.commons.les.inmarsat;

import fish.focus.uvms.commons.les.inmarsat.body.*;
import fish.focus.uvms.commons.les.inmarsat.header.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import java.util.Arrays;
import java.util.Calendar;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InmarsatMessageTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void validateValidMessage() throws InmarsatException {
		HeaderData headerData = new HeaderDataBuilder().setType(HeaderType.DNID).setRefno(1)
				.setStoredTime(Calendar.getInstance().getTime()).setDataPresentation(HeaderDataPresentation.TRANS_DATA)
				.createHeaderData();
		InmarsatHeader inmarsatHeader = InmarsatHeader.createHeader(headerData);
		PositionReportData bodyData = new PositionReportDataBuilder().setLatPosition(new Position(0, 12, 2, 20))
				.setSpeedAndCourse(new SpeedAndCourse(12.0, 90)).createPositionReportData();
		InmarsatBody inmarsatBody = PositionReport.createPositionReport(bodyData, true);

		InmarsatMessage message = new InmarsatMessage(inmarsatHeader, inmarsatBody);
		assertTrue(message.validate());

	}

	@Test(expected = InmarsatException.class)
	public void validateNonValidHeader() throws InmarsatException {
		HeaderData headerData = new HeaderDataBuilder().setType(HeaderType.DNID).setRefno(1)
				.setStoredTime(Calendar.getInstance().getTime()).setDataPresentation(HeaderDataPresentation.TRANS_DATA)
				.createHeaderData();
		PositionReportData bodyData = new PositionReportDataBuilder().setLatPosition(new Position(0, 12, 2, 20))
				.setSpeedAndCourse(new SpeedAndCourse(12.0, 90)).createPositionReportData();
		InmarsatBody inmarsatBody = PositionReport.createPositionReport(bodyData, true);

		InmarsatHeader iHeaderClone = InmarsatHeader.createHeader(headerData);
		byte[] headerClone = iHeaderClone.header.clone();
		headerClone[HeaderStruct.POS_HEADER_LENGTH] = 34;
		iHeaderClone.header = headerClone;
		InmarsatMessage message = new InmarsatMessage(iHeaderClone, inmarsatBody);
		assertFalse(message.validate());
	}

	@Test(expected = InmarsatException.class)
	public void validateNonValidBody() throws InmarsatException {
		HeaderData headerData = new HeaderDataBuilder().setType(HeaderType.DNID).setRefno(1)
				.setStoredTime(Calendar.getInstance().getTime()).setDataPresentation(HeaderDataPresentation.TRANS_DATA)
				.createHeaderData();
		InmarsatHeader inmarsatHeader = InmarsatHeader.createHeader(headerData);

		PositionReportData bodyData = new PositionReportDataBuilder().setLatPosition(new Position(0, 12, 2, 20))
				.setSpeedAndCourse(new SpeedAndCourse(12.0, 90)).createPositionReportData();

		InmarsatBody inmarsatBodyClone = PositionReport.createPositionReport(bodyData, true);
		inmarsatBodyClone.body = Arrays.copyOf(inmarsatBodyClone.body, inmarsatBodyClone.body.length - 2);
		InmarsatMessage message = new InmarsatMessage(inmarsatHeader, inmarsatBodyClone);
		assertFalse(message.validate());

	}

	@Test
	public void gettersTest() throws InmarsatException {
		HeaderData headerData = new HeaderDataBuilder().setType(HeaderType.DNID).setRefno(1)
				.setStoredTime(Calendar.getInstance().getTime()).setDataPresentation(HeaderDataPresentation.TRANS_DATA)
				.createHeaderData();
		InmarsatHeader inmarsatHeader = InmarsatHeader.createHeader(headerData);
		PositionReportData bodyData = new PositionReportDataBuilder().setLatPosition(new Position(0, 12, 2, 20))
				.setSpeedAndCourse(new SpeedAndCourse(12.0, 90)).createPositionReportData();
		InmarsatBody inmarsatBody = PositionReport.createPositionReport(bodyData, true);

		InmarsatMessage message = new InmarsatMessage(inmarsatHeader, inmarsatBody);
		assertTrue(message.getBody().validate());
		assertTrue(message.getHeader().validate());

	}
}
