package fish.focus.uvms.commons.les.inmarsat;

import org.junit.Test;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

public class InmarsatExceptionTest {

	@Test(expected = InmarsatException.class)
	public void constructorWithNoCause() throws InmarsatException {
		String message = "Test";
		InmarsatException ie = new InmarsatException(message);
		assertEquals(message, ie.getMessage());
		assertNull(ie.getCause());
		throw ie;
	}

	@Test(expected = InmarsatException.class)
	public void constructorWithCause() throws InmarsatException {
		String message = "Test";
		String causeMsg = "Cause";
		InmarsatException ie = new InmarsatException(message, new IllegalArgumentException(causeMsg));
		assertEquals(message, ie.getMessage());
		assertEquals(causeMsg, ie.getCause().getMessage());
		throw ie;
	}


}
