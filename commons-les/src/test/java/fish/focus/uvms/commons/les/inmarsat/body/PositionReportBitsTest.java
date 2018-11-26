package fish.focus.uvms.commons.les.inmarsat.body;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

public class PositionReportBitsTest {
	@Test
	public void getValue() throws Exception {
		Assert.assertEquals(16, PositionReportBits.COURSE.getValue());
	}

	@Test
	public void fromInt() throws Exception {
		Assert.assertEquals(PositionReportBits.LAT_MIN_FRAC,
				PositionReportBits.fromInt(PositionReportBits.LAT_MIN_FRAC.getValue()));
		Assert.assertNotEquals(PositionReportBits.LAT_MIN_FRAC,
				PositionReportBits.fromInt(PositionReportBits.LONG_MIN_FRAC.getValue()));
		assertNull(MemCode.fromInt(99));
	}
}
