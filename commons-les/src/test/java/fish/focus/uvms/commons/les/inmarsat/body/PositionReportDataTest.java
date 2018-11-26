package fish.focus.uvms.commons.les.inmarsat.body;

import fish.focus.uvms.commons.les.inmarsat.InmarsatException;
import org.junit.Test;
import static org.junit.Assert.*;

public class PositionReportDataTest {

	@Test
	public void createPositionReportData() throws InmarsatException {
		Position latitude = new Position(0, 22, 10, 12);
		Position longitude = new Position(1, 23, 34, 25);
		int memCode = 1;
		SpeedAndCourse speedAndCourse = new SpeedAndCourse(10.2, 67);
		PositionDate positionDate = new PositionDate(31, 23, 22);

		PositionReportData data = new PositionReportData(1, latitude, longitude, memCode, positionDate, speedAndCourse);

		assertEquals(1, data.getDataReportFormat());

		assertEquals(latitude.getHemi(), data.getLatHemi());
		assertEquals(latitude.getDeg(), data.getLatDeg());
		assertEquals(latitude.getMin(), data.getLatMin());
		assertEquals(latitude.getMinFrac(), data.getLatMinFrac());

		assertEquals(longitude.getHemi(), data.getLongHemi());
		assertEquals(longitude.getDeg(), data.getLongDeg());
		assertEquals(longitude.getMin(), data.getLongMin());
		assertEquals(longitude.getMinFrac(), data.getLongMinFrac());

		assertEquals(memCode, data.getMem());

		assertEquals(positionDate.getDay(), data.getPositionDate().getDay());
		assertEquals(positionDate.getHour(), data.getPositionDate().getHour());
		assertEquals(positionDate.getMinute(), data.getPositionDate().getMinute());

		assertEquals(speedAndCourse.getSpeed(), data.getSpeedAndCourse().getSpeed(), 0.0);
		assertEquals(speedAndCourse.getSpeed(), data.getSpeed(), 0.0);
		assertEquals(speedAndCourse.getCourse(), data.getSpeedAndCourse().getCourse());
		assertEquals(speedAndCourse.getCourse(), data.getCourse());
		assertNull(data.getPositionDate().getExtraDate());



	}
}
