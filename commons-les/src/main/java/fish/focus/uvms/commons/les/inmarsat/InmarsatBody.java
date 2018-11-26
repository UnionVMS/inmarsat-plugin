package fish.focus.uvms.commons.les.inmarsat;

public abstract class InmarsatBody {
	protected byte[] body = null;

	public String getBodyAsHexString() {
		return InmarsatUtils.bytesArrayToHexString(body);
	}

	public abstract boolean validate();
}
