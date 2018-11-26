package fish.focus.uvms.commons.les.inmarsat;

import fish.focus.uvms.commons.les.inmarsat.header.HeaderStruct;
import fish.focus.uvms.commons.les.inmarsat.header.HeaderType;
import fish.focus.uvms.commons.les.inmarsat.body.PositionReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

public class InmarsatMessage {
	private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatMessage.class);
	private InmarsatHeader header;
	private InmarsatBody body;

	/**
	 * Create message
	 *
	 * @param header the header
	 * @param body the body
	 * @throws InmarsatException if not valid header or body
	 */
	public InmarsatMessage(InmarsatHeader header, InmarsatBody body) throws InmarsatException {
		this.header = header;
		this.body = body;
		if (header == null || body == null || !validate()) {
			throw new InmarsatException("Not a valid message");
		}
	}

	/**
	 * Parses bytes and returns the the first message
	 *
	 * @param byteMessages the fist message
	 * @throws InmarsatException if not valid message
	 */
	public InmarsatMessage(byte[] byteMessages) throws InmarsatException {

		for (int index = 0; index < byteMessages.length; index++) {
			if (InmarsatHeader.isStartOfMessage(byteMessages, index)) {
				HeaderType headerType = HeaderType.fromInt(byteMessages[index + HeaderStruct.POS_TYPE]);
				if (headerType == null) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Message doesn't contain a valid headertype, messagehex: {}",
								InmarsatUtils.bytesArrayToHexString(byteMessages));
					}
					throw new InmarsatException("Message doesn't contain a valid headertype");
				}

				byte[] headerBytes = Arrays.copyOfRange(byteMessages, index, index + headerType.getHeaderLength());
				header = InmarsatHeader.createHeader(headerBytes);
				if (HeaderType.DNID == headerType) {
					int startOfBody = index + headerType.getHeaderLength();
					byte[] bodyBytes =
							Arrays.copyOfRange(byteMessages, startOfBody, startOfBody + header.getDataLength());
					this.body = PositionReport.createPositionReport(bodyBytes);

					return;// return first message
				}
			}
		}
	}

	public InmarsatHeader getHeader() {
		return header;
	}

	public InmarsatBody getBody() {
		return body;
	}

	public String getMessageAsHexString() {
		return header.getHeaderAsHexString() + body.getBodyAsHexString();
	}

	public boolean validate() {
		return header.validate() && body.validate();
	}

	@Override
	public String toString() {
		return "InmarsatMessage-" + header + ";" + body;
	}
}
