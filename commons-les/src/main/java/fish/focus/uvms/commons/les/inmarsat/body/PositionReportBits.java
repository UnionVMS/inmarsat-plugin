package fish.focus.uvms.commons.les.inmarsat.body;

public enum PositionReportBits {
	//@formatter:off
     DATA_REPORT_FORMAT(1,2)
    ,LAT_HEMISPHERE(2,1)
    ,LAT_DEG(3,7)
    ,LAT_MIN(4,6)
    ,LAT_MIN_FRAC(5,5)
    ,LONG_HEMISPHERE(6,1)
    ,LONG_DEG(7,8)
    ,LONG_MIN(8,6)
    ,LONG_MIN_FRAC(9,5)
    ,MACRO_ENC_DATA(10,7)
    ,MONTH_NOT_USED(11,1)
    ,DAY_OF_MONTH(12,5)
    ,HOUR(13,5)
    ,MINUTES(14,5)
    ,SPEED(15,8)
    ,COURSE(16,9)
    ,RESERVED_BY_INMARSAT(17,15)
    ,FREE_FOR_USERDATA(18,8*8);

    //@formatter:on

	private final int value;
	private final int noOfBytes;

	PositionReportBits(int value, int noOfBytes) {
		this.value = value;
		this.noOfBytes = noOfBytes;
	}

	public int getNoOfBytes() {
		return noOfBytes;
	}

	public static PositionReportBits fromInt(int value) {
		for (PositionReportBits pr : PositionReportBits.values()) {
			if (pr.value == value) {
				return pr;
			}
		}
		return null;
	}

	public int getValue() {
		return value;
	}
}
