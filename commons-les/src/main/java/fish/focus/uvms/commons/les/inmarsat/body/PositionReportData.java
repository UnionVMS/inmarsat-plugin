package fish.focus.uvms.commons.les.inmarsat.body;

public class PositionReportData {

	private final int dataReportFormat;
	private final Position latPosition;
	private final Position longPosition;
	private final int mem;
	private final PositionDate positionDate;
	private final SpeedAndCourse speedAndCourse;

	PositionReportData(int dataReportFormat, Position latPosition, Position longPosition, int mem,
			PositionDate positionDate, SpeedAndCourse speedAndCourse) {
		this.dataReportFormat = dataReportFormat;
		this.latPosition = latPosition;
		this.longPosition = longPosition;
		this.mem = mem;
		this.positionDate = positionDate;
		this.speedAndCourse = speedAndCourse;

	}

	public int getDataReportFormat() {
		return dataReportFormat;
	}

	public int getLatHemi() {
		if (latPosition == null) {
			return 0;
		}

		return latPosition.getHemi();
	}

	public int getLatDeg() {
		if (latPosition == null) {
			return 0;
		}

		return latPosition.getDeg();
	}

	public int getLatMin() {

		if (latPosition == null) {
			return 0;
		}
		return latPosition.getMin();
	}

	public int getLatMinFrac() {

		if (latPosition == null) {
			return 0;
		}
		return latPosition.getMinFrac();
	}

	public int getLongHemi() {
		if (longPosition == null) {
			return 0;
		}
		return longPosition.getHemi();
	}

	public int getLongDeg() {
		if (longPosition == null) {
			return 0;
		}
		return longPosition.getDeg();
	}

	public int getLongMin() {

		if (longPosition == null) {
			return 0;
		}
		return longPosition.getMin();
	}

	public int getLongMinFrac() {
		if (longPosition == null) {
			return 0;
		}
		return longPosition.getMinFrac();
	}

	public int getMem() {
		return mem;
	}

	public int getMonthRes() {
		if (positionDate == null) {
			return 0;
		}
		return positionDate.getMonthRes();
	}

	public int getDay() {
		if (positionDate == null) {
			return 0;
		}
		return positionDate.getDay();
	}

	public int getHour() {
		if (positionDate == null) {
			return 0;
		}
		return positionDate.getHour();
	}

	public int getMinute() {
		if (positionDate == null) {
			return 0;
		}
		return positionDate.getMinute();
	}

	public double getSpeed() {
		if (speedAndCourse == null) {
			return 0;
		}
		return speedAndCourse.getSpeed();
	}

	public int getCourse() {
		if (speedAndCourse == null) {
			return 0;
		}
		return speedAndCourse.getCourse();
	}

	public PositionDate getPositionDate() {
		return positionDate;
	}

	public SpeedAndCourse getSpeedAndCourse() {
		return speedAndCourse;
	}

}
