package fish.focus.uvms.commons.les.inmarsat;


import fish.focus.uvms.commons.les.inmarsat.header.HeaderDataPresentation;
import fish.focus.uvms.commons.les.inmarsat.header.HeaderStruct;
import fish.focus.uvms.commons.les.inmarsat.header.HeaderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.LocalBean;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.*;

@LocalBean
public class InmarsatInterpreter {

	private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatInterpreter.class);
	private static final byte[] HEADER_PATTERN = ByteBuffer.allocate(4).put((byte) InmarsatDefinition.API_SOH)
			.put(InmarsatDefinition.API_LEAD_TEXT.getBytes()).array();
	private static final int PATTERN_LENGTH = HEADER_PATTERN.length;


	public InmarsatMessage[] byteToInmMessage(final byte[] fileBytes) {
		byte[] bytes = insertMissingData(fileBytes);

		ArrayList<InmarsatMessage> messages = new ArrayList<>();
		if (bytes == null || bytes.length <= PATTERN_LENGTH) {
			LOGGER.error("File is not a valid Inmarsat Message: {}", new String(fileBytes));
			return new InmarsatMessage[] {};
		}
		// Parse bytes for messages
		for (int i = 0; i < (bytes.length - PATTERN_LENGTH); i++) {
			// Find message
			if (InmarsatHeader.isStartOfMessage(bytes, i)) {
				InmarsatMessage message;
				byte[] messageBytes = Arrays.copyOfRange(bytes, i, bytes.length);
				try {
					message = new InmarsatMessage(messageBytes);
				} catch (InmarsatException e) {
					LOGGER.error("Error in file Inmarsat Message: {}", new String(fileBytes));
					continue;
				}

				if (message.validate()) {
					messages.add(message);
				} else {
					LOGGER.info("Message rejected: {}", new String(fileBytes));
				}
			}
		}
		return messages.toArray(new InmarsatMessage[0]); // "new InmarsatMessage[0]" is used instead of "new
		// Inmarsat[messages.size()]" to get better performance
	}

	/**
	 * Header sent doesn't always adhere to the byte contract.. This method tries to insert fix the missing parts..
	 *
	 * @param input bytes that might contain miss some bytes
	 * @return message with fixed bytes
	 */
	private byte[] insertMissingData(byte[] input) {
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
