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
}

