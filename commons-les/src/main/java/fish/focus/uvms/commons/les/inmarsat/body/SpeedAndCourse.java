package fish.focus.uvms.commons.les.inmarsat.body;

public class SpeedAndCourse {

	private final double speed;
	private final int course;

	public SpeedAndCourse(double speed, int course) {

		this.speed = speed;
		this.course = course;
	}

	public int getCourse() {
		return course;
	}

	public double getSpeed() {
		return speed;
	}

}
