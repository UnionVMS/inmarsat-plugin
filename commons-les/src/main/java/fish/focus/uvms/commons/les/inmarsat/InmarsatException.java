package fish.focus.uvms.commons.les.inmarsat;

public class InmarsatException extends Exception {

	private static final long serialVersionUID = 1L;

	public InmarsatException(String message) {
		super(message);
	}

	public InmarsatException(String message, Throwable cause) {
		super(message, cause);
	}
}
