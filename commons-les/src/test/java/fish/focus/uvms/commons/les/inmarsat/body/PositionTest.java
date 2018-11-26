package fish.focus.uvms.commons.les.inmarsat.body;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PositionTest {
	@Test
	public void getAsDouble() throws Exception {
		Position position = new Position(1, 2, 3, 4);
		assertEquals("Position should be correct", -(2 + (3 + (double) 4 / 100) / 60), position.getAsDouble(), 0.000);

		position = new Position(0, 56, 59, 99);
		assertEquals("Position should be correct", +(56 + (59 + (double) 99 / 100) / 60), position.getAsDouble(),
				0.000);
		position = new Position(0, 56, 59, 101);
		assertEquals("Position should be correct", +(56 + (59 + (double) 101 / 100) / 60), position.getAsDouble(),
				0.000);
		position = new Position(0, 56, 61, 101);
		assertEquals("Position should be correct", +(56 + (61 + (double) 101 / 100) / 60), position.getAsDouble(),
				0.000);

	}

}
