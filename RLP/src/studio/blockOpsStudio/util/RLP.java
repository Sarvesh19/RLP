package studio.blockOpsStudio.util;

import static org.bouncycastle.util.Arrays.concatenate;
import static org.bouncycastle.util.BigIntegers.asUnsignedByteArray;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class RLP {

	public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	/**
	 * [0x80] If a string is 0-55 bytes long, the RLP encoding consists of a
	 * single byte with value 0x80 plus the length of the string followed by the
	 * string. The range of the first byte is thus [0x80, 0xb7].
	 */
	private static final int OFFSET_SHORT_LIST = 0xc0;

	private static final int SIZE_THRESHOLD = 56;
	private static final double MAX_ITEM_LENGTH = Math.pow(256, 8);

	/**
	 * [0x80] If a string is 0-55 bytes long, the RLP encoding consists of a
	 * single byte with value 0x80 plus the length of the string followed by the
	 * string. The range of the first byte is thus [0x80, 0xb7].
	 */
	private static final int OFFSET_SHORT_ITEM = 0x80;

	/* ******************************************************
	 * ENCODING * *****************************************************
	 */

	/**
	 * Turn Object into its RLP encoded equivalent of a byte-array Support for
	 * String, Integer, BigInteger and Lists of any of these types.
	 * 
	 * @param input
	 *            as object or List of objects
	 * @return byte[] RLP encoded
	 */
	public static byte[] encode(Object input) {
		Value val = new Value(input);
		if (val.isList()) {
			List<Object> inputArray = val.asList();

			if (inputArray.isEmpty()) {
				return encodeLength(inputArray.size(), OFFSET_SHORT_LIST);
			}
			byte[] output = EMPTY_BYTE_ARRAY;
			for (Object object : inputArray) {
				output = concatenate(output, encode(object));
			}
			byte[] prefix = encodeLength(output.length, OFFSET_SHORT_LIST);
			return concatenate(prefix, output);
		} else {
			byte[] inputAsBytes = toBytes(input);
			if (inputAsBytes.length == 1 && (inputAsBytes[0] & 0xff) <= 0x80) { // ff
				// is
				// byte
				// and
				// this
				// is
				// for
				// 1
				// byte
				// length
				return inputAsBytes;
			} else {
				byte[] firstByte = encodeLength(inputAsBytes.length,
						OFFSET_SHORT_ITEM);// 0x80+ length +input data
				return concatenate(firstByte, inputAsBytes);
			}
		}
	}

	/**
	 * Integer limitation goes up to 2^31-1 so length can never be bigger than
	 * MAX_ITEM_LENGTH
	 */
	public static byte[] encodeLength(int length, int offset) {// 0x80+length
		if (length < SIZE_THRESHOLD) {
			byte firstByte = (byte) (length + offset); //
			return new byte[] { firstByte };
		} else if (length < MAX_ITEM_LENGTH) {
			byte[] binaryLength;
			if (length > 0xFF)
				binaryLength = intToBytesNoLeadZeroes(length);
			else
				binaryLength = new byte[] { (byte) length };
			byte firstByte = (byte) (binaryLength.length + offset
					+ SIZE_THRESHOLD - 1);
			return concatenate(new byte[] { firstByte }, binaryLength);
		} else {
			throw new RuntimeException("Input too long");
		}
	}

	public static String byteAryToHex(byte[] byteAry) {
		final StringBuilder builder = new StringBuilder();
		for (byte b : byteAry) {
			builder.append(String.format("%02x", b));
		}
		return builder.toString();
	}

	public static void main(String[] args) {
		// List<Object> list = new ArrayList<Object>();
		// list.add("cat");
		// list.add("dog");
		// System.out.println(list);
		String[] list = new String[] { "cat", "dog" };
		Object[] test = new Object[] {
				new Object[] { new Object[] {}, new Object[] {} },
				new Object[] {} };
		Object[] test2 = new Object[] {
				new Object[] {},
				new Object[] { new Object[] {} },
				new Object[] { new Object[] {},
						new Object[] { new Object[] {} } } };
		byte[] v = encode(list);

		byte[] listoflist = encode(test);

		byte[] listlll = encode(test2);

		System.out.println(byteAryToHex(v));

		System.out.println("-------");

		System.out.println(byteAryToHex(listoflist));

		System.out.println("-------");

		System.out.println(byteAryToHex(listlll));
	}

	public static byte[] intToBytesNoLeadZeroes(int val) {

		long v = System.nanoTime();
		if (val == 0)
			return EMPTY_BYTE_ARRAY;

		int lenght = 0;

		int tmpVal = val;
		while (tmpVal != 0) {
			tmpVal = tmpVal >>> 8;
			++lenght;
		}

		byte[] result = new byte[lenght];

		int index = result.length - 1;
		while (val != 0) {

			result[index] = (byte) (val & 0xFF);
			val = val >>> 8;
			index -= 1;
		}
		System.out.println(System.nanoTime() - v);
		return result;
	}

	/*
	 * Utility function to convert Objects into byte arrays
	 */
	private static byte[] toBytes(Object input) {
		if (input instanceof byte[]) {
			return (byte[]) input;
		} else if (input instanceof String) {
			String inputString = (String) input;
			return inputString.getBytes();
		} else if (input instanceof Long) {
			Long inputLong = (Long) input;
			return (inputLong == 0) ? EMPTY_BYTE_ARRAY
					: asUnsignedByteArray(BigInteger.valueOf(inputLong));
		} else if (input instanceof Integer) {
			Integer inputInt = (Integer) input;
			return (inputInt == 0) ? EMPTY_BYTE_ARRAY
					: asUnsignedByteArray(BigInteger.valueOf(inputInt));
		} else if (input instanceof BigInteger) {
			BigInteger inputBigInt = (BigInteger) input;
			return (inputBigInt.equals(BigInteger.ZERO)) ? EMPTY_BYTE_ARRAY
					: asUnsignedByteArray(inputBigInt);
		}
		// else if (input instanceof Value) {
		// Value val = (Value) input;
		// return toBytes(val.asObj());
		// }
		throw new RuntimeException(
				"Unsupported type: Only accepting String, Integer and BigInteger for now");
	}

}
