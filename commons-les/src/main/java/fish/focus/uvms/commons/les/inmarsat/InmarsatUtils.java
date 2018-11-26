package fish.focus.uvms.commons.les.inmarsat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class InmarsatUtils {
	private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

	private InmarsatUtils() {}

	// Returns digit at pos
	public static int digitAt(int input, int pos) {
		int i = (int) (input % (Math.pow(10, pos)));
		int j = (int) (i / Math.pow(10, (double) pos - 1));
		return Math.abs(j); // abs handles negative input
	}

	public static String bytesArrayToHexString(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static byte[] int2ByteArray(int i, int bytes) {
		byte[] bytesAll = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(i).array();
		return Arrays.copyOf(bytesAll, bytes);

	}

	/**
	 * Get an byte array by binary string
	 *
	 * @param binaryString the string representing a byte (must be multiple of 8 bits)
	 * @return an byte array
	 * @throws InmarsatException if binary string isn't multiple of 8 bits
	 */
	public static byte[] binaryStringToByteArray(String binaryString) throws InmarsatException {
		int splitSize = 8;

		if (binaryString.length() % splitSize == 0) {
			int index = 0;
			int position = 0;

			byte[] resultByteArray = new byte[binaryString.length() / splitSize];
			StringBuilder text = new StringBuilder(binaryString);

			while (index < text.length()) {
				String binaryStringChunk = text.substring(index, Math.min(index + splitSize, text.length()));
				Integer byteAsInt = Integer.parseInt(binaryStringChunk, 2);
				resultByteArray[position] = byteAsInt.byteValue();
				index += splitSize;
				position++;
			}
			return resultByteArray;
		} else {
			throw new InmarsatException("Cannot convert binary string to byte[], because of the input length. '"
					+ binaryString + "' % 8 != 0");

		}
	}

	public static byte[] hexStringToByteArray(String hexString) {
		int len = hexString.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
					+ Character.digit(hexString.charAt(i + 1), 16));
		}
		return data;
	}

	public static String intToBinary(int n, int numOfBits) {
		StringBuilder binary = new StringBuilder();
		for (int i = 0; i < numOfBits; ++i, n /= 2) {
			switch (n % 2) {
				case 0:
					binary.insert(0, "0");
					break;
				case 1:
				default:
					binary.insert(0, "1");
			}
		}

		return binary.toString();
	}

	public static String byteToZeroPaddedString(byte b) {
		return intToBinary(InmarsatUtils.byteToUnsignedInt(b), 8);
	}

	public static int byteToUnsignedInt(byte b) {
		return b & 0xFF;
	}

	public static int toUnsignedShort(final byte[] bytes, int from, int to) {
		return readUnsignedShort(Arrays.copyOfRange(bytes, from, to + 1));
	}

	public static long toUnsignedInt(final byte[] bytes, int from, int to) {
		return readUnsignedInt(Arrays.copyOfRange(bytes, from, to + 1));
	}


	public static int toUnsignedShort(final byte[] bytes) {
		return readUnsignedShort(bytes);
	}


	public static long toUnsignedInt(final byte[] bytes) {
		return readUnsignedInt(bytes);
	}

	/**
	 * Read an unsigned short (2 bytes) from the given byte array (MSB last)
	 *
	 * @param bytes The bytes to read from
	 * @return The short as a int
	 */
	private static int readUnsignedShort(byte[] bytes) {
		return (((bytes[1] & 0xff) << 8) | (bytes[0] & 0xff));
	}


	/**
	 * Read an unsigned integer from the given byte array (MSB last)
	 *
	 * @param bytes The bytes to read from
	 * @return The integer as a long
	 */
	private static long readUnsignedInt(byte[] bytes) {
		return (((bytes[3] & 0xffL) << 24) | ((bytes[2] & 0xffL) << 16) | ((bytes[1] & 0xffL) << 8)
				| (bytes[0] & 0xffL));
	}
}
