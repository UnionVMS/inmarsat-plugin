package fish.focus.uvms.commons.les.inmarsat.body;

public class Position {
	private final int hemisphere;
	private final int degree;
	private final int minute;
	private final int minuteFraction;

	public Position(int hemisphere, int degree, int minute, int minuteFraction) {
		this.hemisphere = hemisphere;
		this.degree = degree;
		this.minute = minute;
		this.minuteFraction = minuteFraction;
	}

	public int getHemi() {
		return hemisphere;
	}

	public int getDeg() {
		return degree;
	}

	public int getMin() {
		return minute;
	}

	public int getMinFrac() {
		return minuteFraction;
	}

	public Double getAsDouble() {
		double minAndMinFraction = (double) minute + ((double) minuteFraction / 100);
		Double d = (double) degree + (minAndMinFraction / 60);

		if (hemisphere > 0) {
			d *= -1.0; // West or South
		}
		return d;
	}

	@Override
	public String toString() {
		return getAsDouble().toString();
	}
}

