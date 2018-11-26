package fish.focus.uvms.commons.les.inmarsat.body;

public enum MemCode {
	//@formatter:off

    NORMAL_REPORT(0x0B)
    , POWER_CONTROL_UP(0x40)
    , POWER_CONTROL_DOWN(0x42)
    , ANTENNA_DISCONNECTED(0x44)
    , ANTENNA_BLOCKED_OR_JAMMED(0x45)
    , STORED_POSITION(0x46)
    , IO_PIN_EVENT(0x47)
    , ZONE_ENTER(0x48)
    , ZONE_LEAVE(0x49)
    , SLEEP_ENTER(0x50)
    , SLEEP_IN(0x51)
    , SLEEP_LEAVE(0x52)
    , SLEEP_ENTER_FIX_TIME(0x53)
    , MANUAL_POSITION(0x58)
    , SPEED_ABOVE_LIMIT(0x59)
    , SPEED_BELOW_LIMIT(0x5A)
    , GPS_BLOCKED_OR_JAMMED(0x5B);

    //@formatter:on

	private final int value;


	MemCode(int value) {
		this.value = value;
	}

	public static MemCode fromInt(int code) {
		for (MemCode type : MemCode.values()) {
			if (type.value == code) {
				return type;
			}
		}

		return null;
	}

	public int getValue() {
		return value;
	}
}
