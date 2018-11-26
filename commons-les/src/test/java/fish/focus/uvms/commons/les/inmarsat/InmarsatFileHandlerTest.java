package fish.focus.uvms.commons.les.inmarsat;

import fish.focus.uvms.commons.les.inmarsat.header.HeaderStruct;
import fish.focus.uvms.commons.les.inmarsat.header.HeaderType;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InmarsatFileHandlerTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatFileHandlerTest.class);
	private final String start = "444E494420313037343520310D0A52657472696576696E6720444E494420646174612E2E2E0A";
	private final String end = "0A3E20";
	private final String headerDnid = "015426540116890b08000155140036372455c307e702".toUpperCase();
	private final String headerDnidNoMemberNo = "015426540116890b08000155140036372455c30702".toUpperCase();
	private final String headerDnidFFMemberNo = "015426540116890b08000155140036372455c307FF02".toUpperCase();
	private final String bodyPositionReportPart1 = "4969384c89ef1e7c".toUpperCase();
	private final String bodyPositionReportPart2 = "402f00000000000000000000".toUpperCase();
	private final String tempDir = "/tmp";
	private final Path downloadDir = Paths.get(tempDir, "/dummyuvmsjunittest");
	private final InmarsatFileHandler ifd = new InmarsatFileHandler(downloadDir);

	@SuppressWarnings("FieldCanBeLocal")
	private final boolean deleteDownloadDir = false;
	private Path file1;
	private Path file2;
	private Path file3;

	@Before
	public void setup() throws IOException {
		// Clear downloaddir...
		// noinspection ConstantConditions
		if (deleteDownloadDir) {

			Files.walkFileTree(downloadDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}
			});
		}

		// Create 2 dummy dat files
		file1 = Paths.get(downloadDir.toString(), "dummyuvmsjunittest1.dat");
		file2 = Paths.get(downloadDir.toString(), "/dummyuvmsjunittest2.dat");
		file3 = Paths.get(downloadDir.toString(), "/dummyuvmsjunittest3.dat");
		Path fileMoved = Paths.get(downloadDir.toString(), "error", "dummyuvmsjunittest1.dat");
		// Just in case we delete them..
		if (Files.exists(file1)) {
			Files.delete(file1);
		}
		if (Files.exists(file2)) {
			Files.delete(file2);
		}
		if (Files.exists(file3)) {
			Files.delete(file3);
		}

		if (Files.exists(fileMoved)) {
			Files.delete(fileMoved);
		}


		Files.createDirectories(file1.getParent());

	}

	@Test
	public void insertMissingMsgRef() throws Exception {
		byte[] message =
				ifd.insertMissingData(InmarsatUtils.hexStringToByteArray("01542654011658790d024414209820a959f929FF02"));
		assertEquals((byte) 0x00, message[HeaderStruct.POS_REF_NO_END]);
		assertEquals(HeaderType.DNID.getHeaderLength(), message.length);
		assertEquals("01542654011658790d00024414209820a959f929FF02".toUpperCase(),
				InmarsatUtils.bytesArrayToHexString(message));
	}

	@Test
	public void insertMissingMsgRefAndDataLength() throws Exception {
		byte[] message =
				ifd.insertMissingData(InmarsatUtils.hexStringToByteArray("01542654011658790d024414209820a959f92902"));
		assertEquals((byte) 0x00, message[HeaderStruct.POS_REF_NO_END]);
		assertEquals((byte) 0xFF, message[20]);
		assertEquals(HeaderType.DNID.getHeaderLength(), message.length);
		assertEquals("01542654011658790d00024414209820a959f929FF02".toUpperCase(),
				InmarsatUtils.bytesArrayToHexString(message));
	}

	@Test
	public void insertMissingStoredTimeByte() throws Exception {
		byte[] message =
				ifd.insertMissingData(InmarsatUtils.hexStringToByteArray("01542654011690960B00024414001CA959F929FF02"));
		assertEquals((byte) 0x00, message[HeaderType.DNID.getHeaderStruct().getPositionStoredTime()]);
		Date storedTime = InmarsatHeader.getStoredTime(message);
		Calendar expected = Calendar.getInstance();
		expected.clear();
		expected.setTimeZone(InmarsatDefinition.API_TIMEZONE);
		expected.set(2017, Calendar.SEPTEMBER, 1, 8, 36, 16);
		assertTrue(expected.getTime().compareTo(storedTime) == 0);
		assertEquals(HeaderType.DNID.getHeaderLength(), message.length);
		assertEquals("01542654011690960B0002441400001CA959F929FF02".toUpperCase(),
				InmarsatUtils.bytesArrayToHexString(message));
	}

	@Test
	public void insertMissing_MsgRef_StoredDate_DataLength() throws Exception {
		byte[] message =
				ifd.insertMissingData(InmarsatUtils.hexStringToByteArray("01542654011690960B024414001CA959F92902"));
		assertEquals((byte) 0x00, message[HeaderStruct.POS_REF_NO_END]);
		assertEquals((byte) 0x00, message[HeaderType.DNID.getHeaderStruct().getPositionStoredTime()]);
		Date storedTime = InmarsatHeader.getStoredTime(message);
		Calendar expected = Calendar.getInstance();
		expected.clear();
		expected.setTimeZone(InmarsatDefinition.API_TIMEZONE);
		expected.set(2017, Calendar.SEPTEMBER, 1, 8, 36, 16);
		assertTrue(expected.getTime().compareTo(storedTime) == 0);
		assertEquals((byte) 0xFF, message[20]);
		assertEquals(HeaderType.DNID.getHeaderLength(), message.length);
		assertEquals("01542654011690960B0002441400001CA959F929FF02".toUpperCase(),
				InmarsatUtils.bytesArrayToHexString(message));
	}

	@Test
	public void insertMissingMemberNo() throws Exception {
		// Header with no member no
		byte[] message = ifd.insertMissingData(InmarsatUtils.hexStringToByteArray(headerDnidNoMemberNo));
		assertEquals((byte) 0xFF, message[20]);
		assertEquals(HeaderType.DNID.getHeaderLength(), message.length);
		assertEquals(headerDnidFFMemberNo, InmarsatUtils.bytesArrayToHexString(message));
	}


	@Test
	public void insertMissingMemberNoHasFF() throws Exception {
		// Header with default FF
		byte[] message = ifd.insertMissingData(InmarsatUtils.hexStringToByteArray(headerDnidFFMemberNo));
		assertEquals((byte) 0xFF, message[20]);
		assertEquals(HeaderType.DNID.getHeaderLength(), message.length);
		assertEquals(headerDnidFFMemberNo, InmarsatUtils.bytesArrayToHexString(message));
	}

	@Test
	public void insertMissingMemberNoHasMemberNo() throws Exception {
		// Header with member number but not FF
		byte[] message = ifd.insertMissingData(InmarsatUtils.hexStringToByteArray(headerDnid));
		assertEquals(HeaderType.DNID.getHeaderLength(), message.length);
		assertEquals(headerDnid, InmarsatUtils.bytesArrayToHexString(message));
	}

	@Test
	public void insertMissingMemberNoManyHeaderWithWithout() throws Exception {
		// Headers with/without member number
		String inputMes = headerDnid + headerDnidFFMemberNo + headerDnidNoMemberNo + headerDnidFFMemberNo + headerDnid
				+ headerDnidNoMemberNo;
		String expected = headerDnid + headerDnidFFMemberNo + headerDnidFFMemberNo + headerDnidFFMemberNo + headerDnid
				+ headerDnidFFMemberNo;
		byte[] message = ifd.insertMissingData(InmarsatUtils.hexStringToByteArray(inputMes));
		assertEquals(expected, InmarsatUtils.bytesArrayToHexString(message));

	}

	@Test
	public void insertMissingMemberNoWithMemberNoAndEOFInMessage() throws Exception {
		// Headers with/without member number
		String inputMes = "01542654011634630E0002440800511AA959F9290C02808A160000010000";
		byte[] message = ifd.insertMissingData(InmarsatUtils.hexStringToByteArray(inputMes));
		assertEquals(inputMes, InmarsatUtils.bytesArrayToHexString(message));

	}


	@Test
	public void createMessagesFromPathInfo() throws Exception {
		Map<Path, InmarsatMessage[]> messagesFromPath = ifd.createMessages();
		int index = 0, size = messagesFromPath.size();

		for (Map.Entry<Path, InmarsatMessage[]> entry : messagesFromPath.entrySet()) {
			index++;
			LOGGER.info("File {}/{} {}", index, size, entry.getKey().getFileName());
			int i = 0;
			int length = entry.getValue().length;
			for (InmarsatMessage message : entry.getValue()) {
				i++;
				LOGGER.info("Message {}/{} {}", i, length, message);
			}
		}
	}


	@Test
	public void createMessagesFromPath() throws Exception {
		String file1_msg1 = headerDnid + bodyPositionReportPart1 + bodyPositionReportPart2;
		String file2_msg1 = "01542654011634630E0002440800511AA959F9290C02808A160000010000";
		String file2_msg2 = "015426540116EF630800024408005A1AA959F92902808A170000010000"; // missing member no!
		String file2_msg2FF = "015426540116EF630800024408005A1AA959F929FF02808A170000010000"; // member no FF
		String file2_msg3 = "015426540116EF630800024408005A1AA959F9290B02808A170000010000";

		String file3_msg1 = "01542654011734630E0002440800511AA959F9290C02808A160000010000"; // Not valid
		String file3_msg2 = "015426540116EF630800024408005A1AA959F92902808A170000010000"; // missing member no!
		String file3_msg2FF = "015426540116EF630800024408005A1AA959F929FF02808A170000010000"; // member no FF
		String file3_msg3 = "015426540116EF630800024408005A1AA959F9290B02808A170000010000";


		Files.write(file1, InmarsatUtils.hexStringToByteArray(start), StandardOpenOption.CREATE_NEW);
		Files.write(file1, InmarsatUtils.hexStringToByteArray(file1_msg1), StandardOpenOption.APPEND);
		Files.write(file1, InmarsatUtils.hexStringToByteArray(end), StandardOpenOption.APPEND);

		Files.write(file2, InmarsatUtils.hexStringToByteArray(start), StandardOpenOption.CREATE_NEW);
		Files.write(file2, InmarsatUtils.hexStringToByteArray(file2_msg1), StandardOpenOption.APPEND);
		Files.write(file2, InmarsatUtils.hexStringToByteArray(file2_msg2), StandardOpenOption.APPEND);
		Files.write(file2, InmarsatUtils.hexStringToByteArray(file2_msg3), StandardOpenOption.APPEND);
		Files.write(file2, InmarsatUtils.hexStringToByteArray(end), StandardOpenOption.APPEND);

		Files.write(file3, InmarsatUtils.hexStringToByteArray(start), StandardOpenOption.CREATE_NEW);
		Files.write(file3, InmarsatUtils.hexStringToByteArray(file3_msg1), StandardOpenOption.APPEND);
		Files.write(file3, InmarsatUtils.hexStringToByteArray(file3_msg2), StandardOpenOption.APPEND);
		Files.write(file3, InmarsatUtils.hexStringToByteArray(file3_msg3), StandardOpenOption.APPEND);
		Files.write(file3, InmarsatUtils.hexStringToByteArray(end), StandardOpenOption.APPEND);
		Map<Path, InmarsatMessage[]> messagesFromPath = ifd.createMessages();
		assertEquals(3, messagesFromPath.size());
		assertTrue(messagesFromPath.containsKey(file1));
		assertTrue(messagesFromPath.containsKey(file2));
		assertTrue(messagesFromPath.containsKey(file3));
		int found = 0;
		for (Map.Entry<Path, InmarsatMessage[]> entry : messagesFromPath.entrySet()) {
			if (entry.getKey().equals(file1)) {
				assertEquals(file1_msg1, entry.getValue()[0].getMessageAsHexString());
				found++;
			}
			if (entry.getKey().equals(file2)) {
				assertEquals(file2_msg1, entry.getValue()[0].getMessageAsHexString());
				assertEquals(file2_msg2FF, entry.getValue()[1].getMessageAsHexString());
				assertEquals(file2_msg3, entry.getValue()[2].getMessageAsHexString());
				found++;
			}
			if (entry.getKey().equals(file3)) {
				// file3_msg1 not valid and should not be parsed----
				assertEquals(file3_msg2FF, entry.getValue()[0].getMessageAsHexString());
				assertEquals(file3_msg3, entry.getValue()[1].getMessageAsHexString());
				found++;
			}

		}
		assertTrue(found == 3);
	}

	@Test
	public void byteToInmMessage() throws Exception {
		String iMessageHex = start + headerDnidFFMemberNo + bodyPositionReportPart1 + bodyPositionReportPart2;
		InmarsatMessage iMessage = new InmarsatMessage(InmarsatUtils.hexStringToByteArray(iMessageHex));
		assertEquals(headerDnidFFMemberNo, iMessage.getHeader().getHeaderAsHexString());
		assertEquals(bodyPositionReportPart1 + bodyPositionReportPart2, iMessage.getBody().getBodyAsHexString());
		assertTrue(iMessage.validate());
	}

	@Test
	public void listFiles() throws Exception {
		Files.write(file1, "1".getBytes(), StandardOpenOption.CREATE_NEW);
		Files.write(file2, "2".getBytes(), StandardOpenOption.CREATE_NEW);

		List<Path> listOfFiles = ifd.listFiles(downloadDir);
		assertEquals(2, listOfFiles.size());

		assertTrue(listOfFiles.contains(file1));
		assertTrue(listOfFiles.contains(file2));

	}

	@Test
	public void moveFile() throws Exception {
		Files.write(file1, "1".getBytes(), StandardOpenOption.CREATE_NEW);
		Files.write(file2, "2".getBytes(), StandardOpenOption.CREATE_NEW);

		ifd.moveFileToDir(file1, Paths.get(tempDir, InmarsatFileHandler.ERROR_DIR_NAME));
		assertTrue(
				Files.exists(Paths.get(tempDir, InmarsatFileHandler.ERROR_DIR_NAME, file1.getFileName().toString())));

	}
	@Test
	public void moveFileToSuspect() throws Exception {
		Files.write(file1, "1".getBytes(), StandardOpenOption.CREATE_NEW);
		Files.write(file2, "2".getBytes(), StandardOpenOption.CREATE_NEW);

		ifd.moveFileToDir(file1, Paths.get(tempDir, InmarsatFileHandler.SUSPECT_DIR_NAME));
		assertTrue(
				Files.exists(Paths.get(tempDir, InmarsatFileHandler.SUSPECT_DIR_NAME, file1.getFileName().toString())));

	}

}
