package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Formatter;

public final class BufferUtil {

	public static int readInputStreamWithTimeout(InputStream is, byte[] b, int timeoutMillis)
			throws IOException  {
		int bufferOffset = 0;
		long maxTimeMillis = System.currentTimeMillis() + timeoutMillis;
		while (System.currentTimeMillis() < maxTimeMillis && bufferOffset < b.length) {
			int readLength = java.lang.Math.min(is.available(),b.length-bufferOffset);
			// can alternatively use bufferedReader, guarded by isReady():
			int readResult = is.read(b, bufferOffset, readLength);
			if (readResult == -1) break;
			bufferOffset += readResult;
		}
		return bufferOffset;
	}

	public static String toHexString(byte[] bytes) {
		Formatter formatter = new Formatter();

		for (byte b : bytes) {
			formatter.format("%02x", b);
		}

		return formatter.toString();
	}

	public static byte[] pad(byte[] padInput, int padSize) {
		if (padSize <= 0){
			throw new IllegalArgumentException("padSize must be a positive number");
		}
		int inputLen = padInput.length;

		//byte value to put in each padded byte
		byte padNum = (byte) padSize;

		//creates an array of aligned size
		byte[] paddedArray = new byte[padInput.length + padSize];

		//copies the given input to the beginning of the aligned array
		System.arraycopy(padInput, 0, paddedArray, 0, padInput.length);

		//add padSize bytes with the byte value of the number of bytes to add
		for(int i=0; i<padSize; i++){
			paddedArray[inputLen + i] = padNum;
		}
		return paddedArray;
	}

	public static byte[] removePad(byte[] paddedInput) {
		//get the number of padding bytes
		int numPadBytes = paddedInput[paddedInput.length-1];
		//size of the original array
		int originalSize = paddedInput.length - numPadBytes;

		//copy the array without the padding to a new array and return it
		byte[] original = new byte[originalSize];
		System.arraycopy(paddedInput, 0, original, 0, originalSize);
		return original;
	}

}

