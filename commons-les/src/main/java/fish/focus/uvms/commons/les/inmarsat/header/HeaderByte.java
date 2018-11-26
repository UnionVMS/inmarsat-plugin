package fish.focus.uvms.commons.les.inmarsat.header;

public enum HeaderByte {
	//@formatter:off
    START_OF_HEADER(1,1)
   ,LEAD_TEXT(2,3)
   ,TYPE(3,1)
   ,HEADER_LENGTH(4,1)
   ,MSG_REF_NO(5,4)
   ,PRESENTATION(6,1)
   ,FAILURE_RESON(7,1)
   ,DELIVERY_ATTEMPTS(8,1)
   ,SATID_AND_LESID(9,1)
   ,DATA_LENGTH(10,2)
   ,STORED_TIME(11,4)
   ,DNID(12,2)
   ,MEMBER_NO(13,1)
   ,MES_MOB_NO(14,4)
   ,END_OF_HEADER(15,1);
    //@formatter:on

	private final int value;
	private final int noOfBytes;

	HeaderByte(int value, int noOfBytes) {
		this.value = value;
		this.noOfBytes = noOfBytes;
	}

	public static HeaderByte fromInt(int value) {
		for (HeaderByte entry : HeaderByte.values()) {
			if (entry.value == value) {
				return entry;
			}
		}

		return null;
	}

	public int getValue() {
		return value;
	}

	public int getNoOfBytes() {
		return noOfBytes;
	}
}
