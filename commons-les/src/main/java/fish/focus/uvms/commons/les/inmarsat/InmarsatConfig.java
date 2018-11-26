package fish.focus.uvms.commons.les.inmarsat;

public class InmarsatConfig {
	public static final boolean DEFAULT_EXTRA_DATA_ENABLED = false;
	public static final int DEFAULT_EXTRA_DATA_FORMAT = 3;
	private boolean extraDataEnabled = DEFAULT_EXTRA_DATA_ENABLED;
	private int extraDataFormat = DEFAULT_EXTRA_DATA_FORMAT;

	private InmarsatConfig() {}

	public static InmarsatConfig getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void setToDefault() {
		extraDataEnabled = DEFAULT_EXTRA_DATA_ENABLED;
		extraDataFormat = DEFAULT_EXTRA_DATA_FORMAT;
	}

	public boolean isExtraDataEnabled() {
		return extraDataEnabled;
	}

	public void setExtraDataEnabled(boolean extraDataEnabled) {
		this.extraDataEnabled = extraDataEnabled;
	}

	public int getExtraDataFormat() {
		return extraDataFormat;
	}

	public void setExtraDataFormat(int extraDataFormat) {
		this.extraDataFormat = extraDataFormat;
	}

	private static class SingletonHelper {
		private SingletonHelper() {}

		private static final InmarsatConfig INSTANCE = new InmarsatConfig();
	}

}
