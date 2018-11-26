package fish.focus.uvms.commons.les.inmarsat.header;

/**
 * 1: InmarsatMessage received using 7 bit, the message content is printable<br>
 * 2: InmarsatMessage received using transparent data, message content might be binary data
 */
public enum HeaderDataPresentation {
	IA5(1), // InmarsatMessage received using 7 bit, the message content is printable
	TRANS_DATA(2); // InmarsatMessage received using transparent data, message content might be binary data

	private final int value;

	HeaderDataPresentation(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static HeaderDataPresentation fromInt(int code) {
		for (HeaderDataPresentation type : HeaderDataPresentation.values()) {
			if (type.value == code) {
				return type;
			}
		}

		return null;
	}
}
