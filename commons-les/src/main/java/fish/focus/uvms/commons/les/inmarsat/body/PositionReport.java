package fish.focus.uvms.commons.les.inmarsat.body;

import fish.focus.uvms.commons.les.inmarsat.InmarsatBody;
import fish.focus.uvms.commons.les.inmarsat.InmarsatConfig;
import fish.focus.uvms.commons.les.inmarsat.InmarsatException;
import fish.focus.uvms.commons.les.inmarsat.InmarsatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Position Report Packet Format
 * 
 * <pre>
 * The position report format used for the Maritime or Land Mobile Transceivers is defined by Inmarsat.
 * Each position report can contain additional information such as detailed date
 * information or status of the I/O pins. The position reports have this general format.
 * Each element except the first can be disabled. Each element will follow directly after the previous,
 * even if some elements are disabled. If only the &lt;speed and course&amp;ramp are enabled it will
 * follow directly after &lt;position and date&gt;.
 * In the position reports with MEM-code IO-Report (71) the &lt;I/O status&gt; will always be included.
 * In position reports with MEM-code Enter Zone (72) the &lt;ZoneNo&gt; will always be included.
 * In position reports with MEM-code Above Speed Limits (89) the &lt;Speed and course&gt; will always be included.
 *
 * The Standard Inmarsat Maritime Position Report includes:
 * Position   (Accuracy of 74 meters)
 * Type of Report  (Mem-Code)
 * Time Stamp   (Info of Day, Hour and Minute)
 * Speed   (Accuracy of 0.2 knots)
 * Course   (Accuracy of 1 degree)
 * </pre>
 */
public class PositionReport extends InmarsatBody {
	public static final int DATA_PACKET_1_BYTES = 8;
	public static final int DATA_PACKET_2_BYTES = 12;

	private static final Logger LOGGER = LoggerFactory.getLogger(PositionReport.class);


	private PositionReport() {
		super();
	}

	/**
	 * @param body for a position report
	 * @return a PositionReport
	 * @throws InmarsatException if not a valid body
	 */
	public static PositionReport createPositionReport(byte[] body) throws InmarsatException {
		PositionReport posReport = new PositionReport();
		posReport.body = body;
		if (!posReport.validate()) {
			LOGGER.debug("Not a valid Position report body: {}", body);
			throw new InmarsatException("Not a valid Position report body");
		}
		return posReport;
	}


	/**
	 * @param positionReportData for a position report
	 * @param includePackage2 if package 2 is included
	 * @return a PositionReport
	 * @throws InmarsatException if not a valid body
	 */
	public static PositionReport createPositionReport(PositionReportData positionReportData, boolean includePackage2)
			throws InmarsatException {
		PositionReport posReport = new PositionReport();

		byte[] body = new byte[includePackage2 ? DATA_PACKET_1_BYTES + DATA_PACKET_2_BYTES : DATA_PACKET_1_BYTES];
		String sb = InmarsatUtils.intToBinary(positionReportData.getDataReportFormat(),
				PositionReportBits.DATA_REPORT_FORMAT.getNoOfBytes())
				+ InmarsatUtils.intToBinary(positionReportData.getLatHemi(),
						PositionReportBits.LAT_HEMISPHERE.getNoOfBytes())
				+ InmarsatUtils.intToBinary(positionReportData.getLatDeg(), PositionReportBits.LAT_DEG.getNoOfBytes())
				+ InmarsatUtils.intToBinary(positionReportData.getLatMin(), PositionReportBits.LAT_MIN.getNoOfBytes())
				+ InmarsatUtils.intToBinary(positionReportData.getLatMinFrac() / 4,
						PositionReportBits.LAT_MIN_FRAC.getNoOfBytes())
				+ InmarsatUtils.intToBinary(positionReportData.getLongHemi(),
						PositionReportBits.LONG_HEMISPHERE.getNoOfBytes())
				+ InmarsatUtils.intToBinary(positionReportData.getLongDeg(), PositionReportBits.LONG_DEG.getNoOfBytes())
				+ InmarsatUtils.intToBinary(positionReportData.getLongMin(), PositionReportBits.LONG_MIN.getNoOfBytes())
				+ InmarsatUtils.intToBinary(positionReportData.getLongMinFrac() / 4,
						PositionReportBits.LONG_MIN_FRAC.getNoOfBytes())
				+ InmarsatUtils.intToBinary(positionReportData.getMem(),
						PositionReportBits.MACRO_ENC_DATA.getNoOfBytes())
				+ InmarsatUtils.intToBinary(positionReportData.getMonthRes(),
						PositionReportBits.MONTH_NOT_USED.getNoOfBytes())
				+ InmarsatUtils.intToBinary(positionReportData.getDay(), PositionReportBits.DAY_OF_MONTH.getNoOfBytes())
				+ InmarsatUtils.intToBinary(positionReportData.getHour(), PositionReportBits.HOUR.getNoOfBytes())
				+ InmarsatUtils.intToBinary(positionReportData.getMinute(), PositionReportBits.MINUTES.getNoOfBytes());
		if (includePackage2) {
			sb = sb + InmarsatUtils.intToBinary((int) (positionReportData.getSpeed() * 5),
					PositionReportBits.SPEED.getNoOfBytes())
					+ InmarsatUtils.intToBinary(positionReportData.getCourse(),
							PositionReportBits.COURSE.getNoOfBytes())
					+ InmarsatUtils.intToBinary(0, PositionReportBits.RESERVED_BY_INMARSAT.getNoOfBytes())
					+ InmarsatUtils.intToBinary(0, PositionReportBits.FREE_FOR_USERDATA.getNoOfBytes());

		}

		posReport.body = InmarsatUtils.binaryStringToByteArray(sb);
		if (!posReport.validate()) {
			LOGGER.debug("Not a valid Position report body: {}", body);
			throw new InmarsatException("Not a valid Position report body");
		}

		return posReport;
	}

	@Override
	public boolean validate() {
		if ((body.length != DATA_PACKET_1_BYTES) && body.length != (DATA_PACKET_1_BYTES + DATA_PACKET_2_BYTES)) {
			LOGGER.debug("Position report data length not valid: {}", body);
			return false;
		}
		// validate body data
		try {
			getPositionDate();
		} catch (InmarsatException ie) {
			return false;
		}


		return true;
	}

	public int getDataReportFormat() {
		return Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[0]).substring(0, 2));
	}

	/**
	 * North/South indication. Set to 0 for North or 1 for South. 1 bit
	 *
	 * @return North or South hemisphere
	 */
	public int getLatitudeHemisphere() {
		return Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[0]).substring(2, 3));
	}

	/**
	 * Lat. Degrees (7 bits)
	 *
	 * @return The degrees of Latitude, North or South. 1° is 60 minutes
	 */
	public int getLatitudeDegrees() {
		String part1 = InmarsatUtils.byteToZeroPaddedString(body[0]).substring(3);
		String part2 = InmarsatUtils.byteToZeroPaddedString(body[1]).substring(0, 2);
		return Integer.parseInt(part1 + part2, 2);

	}

	/**
	 * Lat. Minutes (6 bits)
	 *
	 * @return The integer part of the Minutes of latitude. 1 latitude minute is 1 nautical mile (~1852 meters)
	 */
	public int getLatitudeMinutes() {
		return Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[1]).substring(2), 2);

	}

	/**
	 * Lat. Min. Fractional part (5 bits)
	 *
	 * @return The fractional part of the Minutes of latitude in units of 0.04 of a Minute i.e. ~74 meters.
	 */
	public int getLatitudeMinuteFractions() {
		return Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[2]).substring(0, 5), 2) * 4;
	}

	/**
	 * Longitude hemisphere (H) (1 bit):
	 *
	 * @return East/West indication. Set to 0 for East or 1 for West
	 */
	public int getLongitudeHemisphere() {
		return Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[2]).substring(5, 6));
	}

	/**
	 * Lon. Degrees (8 bits)
	 *
	 * @return The degrees of Longitude, East or West
	 */
	public int getLongitudeDegrees() {
		String part1 = InmarsatUtils.byteToZeroPaddedString(body[2]).substring(6);
		String part2 = InmarsatUtils.byteToZeroPaddedString(body[3]).substring(0, 6);
		return Integer.parseInt(part1 + part2, 2);
	}

	/**
	 * Lon. Minutes (6 bits)
	 *
	 * @return The integer part of the Minutes of longitude.
	 */
	public int getLongitudeMinutes() {
		String part1 = InmarsatUtils.byteToZeroPaddedString(body[3]).substring(6);
		String part2 = InmarsatUtils.byteToZeroPaddedString(body[4]).substring(0, 4);
		return Integer.parseInt(part1 + part2, 2);
	}

	/**
	 * Lon. Min. Fractional (5 bits)
	 *
	 * @return The fractional part of the Minutes of longitude in units of 0.04 of a Minute.
	 */
	public int getLongitudeMinuteFractions() {
		String part1 = InmarsatUtils.byteToZeroPaddedString(body[4]).substring(4);
		String part2 = InmarsatUtils.byteToZeroPaddedString(body[5]).substring(0, 1);
		return Integer.parseInt(part1 + part2, 2) * 4;

	}

	/**
	 * Macro Encoded Msg (MEM) (7 bits): Message (MEM) number is a code that identifies the reason for sending the
	 * report (such as regular position reporting or a report triggered by some specific event).
	 *
	 * @return Message (MEM) number
	 */
	public int getMacroEncodedMessage() {
		return Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[5]).substring(1), 2);
	}

	/**
	 * Reserved (1 bit)
	 *
	 * @return Set to zero
	 */
	public int getMonthReserved() {
		return Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[6]).substring(0, 1), 2);
	}

	/**
	 * Day (5 bits)
	 *
	 * @return Value: 0 - 31 (Day of the month)
	 */
	public int getDayOfMonth() {
		return Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[6]).substring(1, 6), 2);
	}

	/**
	 * Hour (5bit)
	 *
	 * @return Value: 0 - 23 (Hour of the day)
	 */
	public int getHour() {
		String part1 = InmarsatUtils.byteToZeroPaddedString(body[6]).substring(6);
		String part2 = InmarsatUtils.byteToZeroPaddedString(body[7]).substring(0, 3);
		return Integer.parseInt(part1 + part2, 2);
	}

	/**
	 * Minutes (5 bits) (Minutes divided by two)
	 *
	 * @return Value: 0 - 29 (Minute within the hour given in units of 2 minutes)
	 */
	public int getMinutes() {
		return Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[7]).substring(3), 2);
	}

	/*
	 * ################## SECOND PACKAGE ############################################ # For landmobile The Landmobile
	 * Position includes as default two reserved bytes in the optional packet.
	 *
	 * # For Maritime The Maritime Position includes by default speed, course and a reserved field in the 2 packet. The
	 * formats of the speed and course fields are like this
	 * ##############################################################################
	 */

	/**
	 * Speed (8bit) (second package) Speed is coded as a one byte unsigned binary number with a resolution of 0.2 knots.
	 * If no valid data is available at the MES, the field should be set to "FFH".
	 *
	 * @return speed
	 */
	public double getSpeed() {
		if ((body.length == DATA_PACKET_1_BYTES) || (body[8] == (byte) 0xFF)) {
			return 0;
		}

		return ((double) InmarsatUtils.byteToUnsignedInt(body[8])) / 5;
	}

	/**
	 * Course in 1 degree resolution (9bit)
	 *
	 * @return Course (0-359 degrees)
	 */
	public int getCourse() {
		if (body.length == DATA_PACKET_1_BYTES) {
			return 0;
		}

		String courseBin = InmarsatUtils.byteToZeroPaddedString(body[9])
				.concat(InmarsatUtils.byteToZeroPaddedString(body[10]).substring(0, 1));
		return Integer.parseInt(courseBin, 2);
	}

	/**
	 * Detailed date information
	 *
	 * @return {@link PositionDate.PositionDateExtra} detailed date from extra package
	 */
	public PositionDate.PositionDateExtra getPositionDateExtra() throws InmarsatException {
		if (body.length == DATA_PACKET_1_BYTES && !InmarsatConfig.getInstance().isExtraDataEnabled()) {
			return null; // No extra date
		}

		switch (InmarsatConfig.getInstance().getExtraDataFormat()) {
			case 1:
				// Date format 1
				int month = Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[12]).substring(0, 4), 2);
				int year = Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[12]).substring(4)
						.concat(InmarsatUtils.byteToZeroPaddedString(body[13]).substring(0, 3)), 2);

				try {
					return new PositionDate.PositionDateExtra(year, month);
				} catch (InmarsatException e) {
					// Ignore handled by returning null
				}
				break;
			case 2:
				// Date format 2
				year = Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[12]).substring(1), 2);
				month = Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[13]).substring(0, 4), 2);
				int day = Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[13]).substring(4)
						.concat(InmarsatUtils.byteToZeroPaddedString(body[14]).substring(0, 1)), 2);
				int hour = Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[14]).substring(1, 6), 2);
				int min = Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[14]).substring(6)
						.concat(InmarsatUtils.byteToZeroPaddedString(body[15]).substring(0, 4)), 2);

				try {
					return new PositionDate.PositionDateExtra(2, year, month, day, hour, min);
				} catch (InmarsatException e) {
					// Ignore handled by returning null
				}

				break;
			case 3:
				// Date format 3
				year = Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[12]).substring(1)
						.concat(InmarsatUtils.byteToZeroPaddedString(body[13]).substring(0, 5)), 2);
				month = Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[13]).substring(5)
						.concat(InmarsatUtils.byteToZeroPaddedString(body[14]).substring(0, 1)), 2);
				day = Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[14]).substring(1, 6), 2);
				hour = Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[14]).substring(6)
						.concat(InmarsatUtils.byteToZeroPaddedString(body[15]).substring(0, 3)), 2);
				min = Integer.parseInt(InmarsatUtils.byteToZeroPaddedString(body[15]).substring(3)
						.concat(InmarsatUtils.byteToZeroPaddedString(body[16]).substring(0, 1)), 2);

				if (PositionDate.PositionDateExtra.validate(3, year, month, day, hour, min)) {
					return new PositionDate.PositionDateExtra(3, year, month, day, hour, min);
				}
				break;
			default:
				return null;
		}

		return null;
	}


	public Position getLatitude() {
		return new Position(getLatitudeHemisphere(), getLatitudeDegrees(), getLatitudeMinutes(),
				getLatitudeMinuteFractions());
	}

	public Position getLongitude() {
		return new Position(getLongitudeHemisphere(), getLongitudeDegrees(), getLongitudeMinutes(),
				getLongitudeMinuteFractions());
	}

	/**
	 * Position date
	 *
	 * @return the position date
	 * @throws InmarsatException @see {@link PositionDate#PositionDate(int, int, int, PositionDate.PositionDateExtra)}
	 */
	public PositionDate getPositionDate() throws InmarsatException {
		return new PositionDate(getDayOfMonth(), getHour(), getMinutes(), getPositionDateExtra());
	}


	@Override
	public String toString() {
		try {
			return "Body=" + getBodyAsHexString() + ";type=" + getDataReportFormat() + ";latitude=" + getLatitude()
					+ ";longitude=" + getLongitude() + ";memcode=" + getMacroEncodedMessage() + ";date="
					+ getPositionDate() + ";speed=" + getSpeed() + ";course=" + getCourse();
		} catch (InmarsatException e) {
			LOGGER.warn("Not a valid date-{},{}", getBodyAsHexString(), e.getMessage());
		}
		return "Body=" + getBodyAsHexString() + ";type=" + getDataReportFormat() + ";latitude=" + getLatitude()
				+ ";longitude=" + getLongitude() + ";memcode=" + getMacroEncodedMessage() + ";date=NOTVALID;speed="
				+ getSpeed() + ";course=" + getCourse();
	}
}

