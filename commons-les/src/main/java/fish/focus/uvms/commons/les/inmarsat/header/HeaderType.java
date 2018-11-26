package fish.focus.uvms.commons.les.inmarsat.header;

/**
 * API InmarsatHeader type
 */
public enum HeaderType {
	//@formatter:off
     DNID(    1, new HeaderStructBuilder().disableAll().enablePresentation().enableSatIdAndLesId().enableDataLength().enableDnid().enableMemberNo().createHeaderStruct())
    ,DNID_MSG(2, new HeaderStructBuilder().disableAll().enablePresentation().enableSatIdAndLesId().enableDataLength().enableDnid().enableMesMobNo().createHeaderStruct())
    ,MSG(     3, new HeaderStructBuilder().disableAll().enablePresentation().enableSatIdAndLesId().enableDataLength().enableMesMobNo().createHeaderStruct())
    ,PDN(     4, new HeaderStructBuilder().disableAll().enableDelivery().enableMesMobNo().createHeaderStruct())
    ,NDN(     5, new HeaderStructBuilder().disableAll().enableFailure().enableDelivery().enableMesMobNo().createHeaderStruct());
    //@formatter:on

	private final int value;

	private final HeaderStruct headerStruct;

	HeaderType(int value, HeaderStruct headerStruct) {
		this.value = value;
		this.headerStruct = headerStruct;
	}

	public static HeaderType fromInt(int code) {
		for (HeaderType type : HeaderType.values()) {
			if (type.value == code) {
				return type;
			}
		}

		return null;
	}

	public int getValue() {
		return value;
	}

	public HeaderStruct getHeaderStruct() {
		return headerStruct;
	}

	public int getHeaderLength() {
		return headerStruct.getHeaderLength();
	}

}
