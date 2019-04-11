package fish.focus.uvms.commons.les.inmarsat.header;

import fish.focus.uvms.commons.les.inmarsat.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class HeaderDnidTest {

	private final byte[] header;
	private final InmarsatHeader headerDnid;
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	private final int dataLength;
	private final HeaderDataPresentation dataPresentation;
	private final int dnid;
	private final int memberNo;
	private final String storedTime;
	private final int lesId;


	public HeaderDnidTest(String headerHex, int dataLength, HeaderDataPresentation dataPresentation, int dnid,
			String storedTime, int memberNo, int lesId) throws InmarsatException {
		header = InmarsatUtils.hexStringToByteArray(headerHex);
		headerDnid = InmarsatHeader.createHeader(header);
		this.dataLength = dataLength;
		this.dataPresentation = dataPresentation;
		this.dnid = dnid;
		this.storedTime = storedTime;
		this.memberNo = memberNo;
		this.lesId = lesId;

	}

	@Parameterized.Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{"015426540116890b08000155140036372455c307e702", 20, HeaderDataPresentation.IA5,        1987, "2015-04-07 21:59:50", 231, 121},
				{"015426540116890b08000255160036372455c307e702", 22, HeaderDataPresentation.TRANS_DATA, 1987, "2015-04-07 21:59:50", 231, 121},
				{"015426540116890b08000255160036372455c307e702", 22, HeaderDataPresentation.TRANS_DATA, 1987, "2015-04-07 21:59:50", 231, 121},
				{"015426540116890b08000255010036372855c307e702",  1, HeaderDataPresentation.TRANS_DATA, 1987, "2015-04-10 22:48:54", 231, 121},
		});
	}

	@Test
	public void testHeaderWithNoEOH(){

		byte[] msg = new byte[] {1,84,38,84,1,22,-100,21,6,0,2,-60,20,0,-59,-97,-117,92,-125,42,78,106,-72,47,-91,11,61,-104,0,-121,-128,0,53,-128,0,0,0,0,0,0,13,10,62,32};

		InmarsatInterpreter interpreter = new InmarsatInterpreter();
		byte[]  fixedHeader = interpreter.insertMissingData(msg);
		assertTrue(msg.length  + 1 == fixedHeader.length);

		// 0154265401169C15060002C41400C59F8B5C832A4E6AB82FA50B3D980087800035800000000000000D0A3E20
		// 015402265401169C15060002C41400C59F8B5C832A4E6AB82FA50B3D980087800035800000000000000D0A3E20
		//
	}



	@Test
	public void createHeader() {
		assertNotNull(headerDnid);
		assertEquals(HeaderType.DNID, headerDnid.getType());
		assertEquals(HeaderType.DNID.getHeaderLength(), headerDnid.getHeaderLength());
	}

	@Test(expected = InmarsatException.class)
	public void createHeaderNegative() throws InmarsatException {
		byte[] cloneHeader = header.clone();
		cloneHeader[HeaderStruct.POS_TYPE] = 8;
		InmarsatHeader cloneHeaderDnid = InmarsatHeader.createHeader(cloneHeader);
		assertFalse(cloneHeaderDnid.validate());
	}

	@Test
	public void getSatIdAndLesId() {
		assertEquals(lesId, headerDnid.getSatIdAndLesId());
	}


	@Test
	public void getDataLength() {
		assertEquals(dataLength, headerDnid.getDataLength());
	}

	@Test
	public void getStoredTime() throws ParseException {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		assertEquals(sdf.parse(storedTime), headerDnid.getStoredTime());
	}

	@Test
	public void getDataPresentation() {
		assertEquals(dataPresentation, headerDnid.getDataPresentation());
	}

	@Test
	public void getDnid() {
		assertEquals(dnid, headerDnid.getDnid());
	}

	@Test
	public void getMemberNo() {
		assertEquals(memberNo, headerDnid.getMemberNo());
	}

}
