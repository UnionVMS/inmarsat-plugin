package fish.focus.uvms.commons.les.inmarsat;


import fish.focus.uvms.commons.les.inmarsat.header.HeaderDataPresentation;
import fish.focus.uvms.commons.les.inmarsat.header.HeaderStruct;
import fish.focus.uvms.commons.les.inmarsat.header.HeaderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;

public class InmarsatFileHandler {
	public static final String ERROR_DIR_NAME = "error";
	public static final String SUSPECT_DIR_NAME = "suspect";

	private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatFileHandler.class);
	private static final byte[] HEADER_PATTERN = ByteBuffer.allocate(4).put((byte) InmarsatDefinition.API_SOH)
			.put(InmarsatDefinition.API_LEAD_TEXT.getBytes()).array();
	private static final int PATTERN_LENGTH = HEADER_PATTERN.length;
	private final Path downloadDir;
	private final Path errorDir;
	private final Path suspectDir;

	public InmarsatFileHandler(Path downloadDir) {
		this.downloadDir = downloadDir;
		this.errorDir = Paths.get(downloadDir.toString(), ERROR_DIR_NAME);
		this.suspectDir = Paths.get(downloadDir.toString(), SUSPECT_DIR_NAME);
	}

	/**
	 * Create a list of files with its inmarsat messages from supplied directory
	 *
	 * @return Map of file and messages parsed from the supplied directory
	 * @throws IOException if the directory couldn't be read
	 */
	public Map<Path, InmarsatMessage[]> createMessages() throws IOException {
		String pattern = new String(HEADER_PATTERN);
		List<Path> fileNames = listFiles(downloadDir);

		Map<Path, InmarsatMessage[]> output = new HashMap<>();

		if (fileNames != null) {

			for (Path file : fileNames) {
				LOGGER.debug("Handling file {}", file);
				InmarsatMessage[] inmarsatMessages;
				byte[] fileBytes;
				try {
					fileBytes = Files.readAllBytes(file);
				} catch (IOException ioe) {
					LOGGER.error("File could not be read :{}", file, ioe);
					continue;
				}

				String fileStr = new String(fileBytes);
				if (fileStr.contains(pattern)) {
					inmarsatMessages = byteToInmMessage(fileBytes, file);
					output.put(file, inmarsatMessages);
				} else {
					if(fileStr.contains("Failed: No message(s).")){
						LOGGER.error("File is not a valid Inmarsat message -> \"{}\" <- deleted.", fileStr);
						Files.deleteIfExists(file);
					} else {
						LOGGER.warn("Suspect file detected. Moved to folder suspect. ", fileStr);
						moveFileToDir(file, suspectDir);
					}
				}
			}
		}

		return output;
	}

	public InmarsatMessage[] byteToInmMessage(final byte[] fileBytes, Path file) {
		byte[] bytes = insertMissingData(fileBytes);

		ArrayList<InmarsatMessage> messages = new ArrayList<>();

		if (bytes == null || bytes.length <= PATTERN_LENGTH) {
			LOGGER.error("File is not a valid Inmarsat Message: {} - moved", file.getFileName());
			moveFileToDir(file, errorDir);

			return new InmarsatMessage[] {};
		}
		boolean errorInfile = false;
		// Parse bytes for messages
		for (int i = 0; i < (bytes.length - PATTERN_LENGTH); i++) {
			// Find message
			if (InmarsatHeader.isStartOfMessage(bytes, i)) {
				InmarsatMessage message;
				byte[] messageBytes = Arrays.copyOfRange(bytes, i, bytes.length);
				try {
					message = new InmarsatMessage(messageBytes);
				} catch (InmarsatException e) {
					LOGGER.error(
							"Something wrong with file {} and it will be moved to error dir, contents: {}, error msg:{}",
							file.getFileName().toString(), InmarsatUtils.bytesArrayToHexString(messageBytes),
							e.getMessage());
					errorInfile = true;
					continue;
				}

				if (message.validate()) {
					messages.add(message);
				} else {
					LOGGER.info("Message in file {} rejected:{}", file.getFileName(), message);
				}
			}
		}
		if (errorInfile) {
			moveFileToDir(file, errorDir);
		}

		return messages.toArray(new InmarsatMessage[0]); // "new InmarsatMessage[0]" is used instead of "new
															// Inmarsat[messages.size()]" to get better performance
	}


	public List<Path> listFiles(Path dir) throws IOException {
		List<Path> result = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.dat")) {
			for (Path entry : stream) {
				result.add(entry);
			}

		} catch (DirectoryIteratorException ex) {
			// I/O error encountered during the iteration, the cause is an IOException
			LOGGER.info("No dir {} , or unable to readBytesFromFile", dir);
			throw ex.getCause();
		}
		return result;
	}


	public void moveFileToDir(Path fromFile, Path toDir) {
		try {
			if (!Files.exists(toDir)) {
				Files.createDirectory(toDir);
			}
			Files.move(fromFile, Paths.get(toDir.toString(), fromFile.getFileName().toString()),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			LOGGER.error("The file: {} could not be moved to dir {} ", new Object[] {fromFile, toDir}, e);
		}
	}

	/**
	 * Header sent doesn't always adhere to the byte contract.. This method tries to insert fix the missing parts..
	 *
	 * @param input bytes that might contain miss some bytes
	 * @return message with fixed bytes
	 */
	public byte[] insertMissingData(byte[] input) {
		byte[] output = insertMissingMsgRefNo(input);
		output = insertMissingStoredTime(output);
		output = insertMissingMemberNo(output);

		if (LOGGER.isDebugEnabled() && (input.length < output.length)) {
			LOGGER.debug("Message fixed: {} -> {}", InmarsatUtils.bytesArrayToHexString(input),
					InmarsatUtils.bytesArrayToHexString(output));
		}
		return output;

	}

	private byte[] insertMissingMsgRefNo(final byte[] contents) {
		byte[] input = contents.clone();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		// Missing last MsgRefNo- insert #00 in before presentation..
		boolean insert = false;
		int insertPosition = 0;
		for (int i = 0; i < input.length; i++) {
			// Find SOH
			if (InmarsatHeader.isStartOfMessage(input, i)) {
				byte[] header = Arrays.copyOfRange(input, i, input.length);
				HeaderType headerType = InmarsatHeader.getType(header);

				if (headerType.getHeaderStruct().isPresentation()) {
					HeaderDataPresentation presentation = InmarsatHeader.getDataPresentation(header);

					if (presentation == null) {
						LOGGER.debug("Presentation is not correct so we add 00 to msg ref no");
						insert = true;
						insertPosition = i + HeaderStruct.POS_REF_NO_END;
					}
				}
			}
			if (insert && (insertPosition == i)) {
				insert = false;
				insertPosition = 0;
				output.write((byte) 0x00);
			}
			output.write(input[i]);
		}
		return output.toByteArray();

	}

	private byte[] insertMissingStoredTime(byte[] contents) {
		// Missing Date byte (incorrect date..)? - insert #00 in date first position
		byte[] input = contents.clone();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		boolean insert = false;
		int insertPosition = 0;

		for (int i = 0; i < input.length; i++) {
			// Find SOH
			if (InmarsatHeader.isStartOfMessage(input, i)) {
				byte[] header = Arrays.copyOfRange(input, i, input.length);
				HeaderType headerType = InmarsatHeader.getType(header);

				Date headerDate = InmarsatHeader.getStoredTime(header);

				if (headerDate.after(Calendar.getInstance(InmarsatDefinition.API_TIMEZONE).getTime())) {
					LOGGER.debug("Stored time is not correct so we add 00 to in first position");
					insert = true;
					insertPosition = i + headerType.getHeaderStruct().getPositionStoredTime();
				}

			}
			if (insert && (insertPosition == i)) {
				insert = false;
				insertPosition = 0;
				output.write((byte) 0x00);
			}
			output.write(input[i]);
		}
		return output.toByteArray();
	}

	private byte[] insertMissingMemberNo(byte[] contents) {

		// Missing Member number - insert #FF before EOH
		// continue from previous cleaned data
		byte[] input = contents.clone();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		boolean insert = false;
		int insertPosition = 0;

		for (int i = 0; i < input.length; i++) {
			// Find SOH
			if (InmarsatHeader.isStartOfMessage(input, i)) {
				int headerLength = input[i + HeaderStruct.POS_HEADER_LENGTH];
				int expectedEOHPosition = i + headerLength - 1;
				// Check if memberNo exits
				if ((expectedEOHPosition >= input.length)
						|| ((input[expectedEOHPosition - 1] == (byte) InmarsatDefinition.API_EOH)
								&& input[expectedEOHPosition] != (byte) InmarsatDefinition.API_EOH)) {
					insert = true;
					insertPosition = expectedEOHPosition - 1;
				}

			}
			// Find EOH
			if (insert && (input[i] == (byte) InmarsatDefinition.API_EOH) && (insertPosition == i)) {
				LOGGER.debug("Message is missing member no");
				output.write((byte) 0xFF);
				insert = false;
				insertPosition = 0;

			}
			output.write(input[i]);
		}
		return output.toByteArray();
	}
}
