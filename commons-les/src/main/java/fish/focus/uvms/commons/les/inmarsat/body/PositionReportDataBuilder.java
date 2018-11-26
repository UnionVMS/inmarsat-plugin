package fish.focus.uvms.commons.les.inmarsat.body;

public class PositionReportDataBuilder {
	private int dataReportFormat;
	private Position latPosition;
	private Position longPosition;
	private int mem;
	private PositionDate positionDate;

	private SpeedAndCourse speedAndCourse;


	public PositionReportDataBuilder setDataReportFormat(int dataReportFormat) {
		this.dataReportFormat = dataReportFormat;
		return this;
	}

	public PositionReportDataBuilder setLatPosition(Position latPosition) {
		this.latPosition = latPosition;
		return this;
	}

	public PositionReportDataBuilder setLongPosition(Position longPosition) {
		this.longPosition = longPosition;
		return this;
	}

	public PositionReportDataBuilder setMem(int mem) {
		this.mem = mem;
		return this;
	}

	public PositionReportDataBuilder setPositionDate(PositionDate positionDate) {
		this.positionDate = positionDate;
		return this;
	}

	public PositionReportDataBuilder setSpeedAndCourse(SpeedAndCourse speedAndCourse) {
		this.speedAndCourse = speedAndCourse;
		return this;
	}

	public PositionReportData createPositionReportData() {
		return new PositionReportData(dataReportFormat, latPosition, longPosition, mem, positionDate, speedAndCourse);
	}
}
