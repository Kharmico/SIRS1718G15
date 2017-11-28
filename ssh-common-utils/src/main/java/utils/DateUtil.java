package utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public final class DateUtil {

	private DateUtil() {}
	
	public static String getTimestamp() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		return dateFormat.format(date);
	}
	
	public static Date convertDate(String timestamp) throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		return dateFormat.parse(timestamp);
	}
	
	
	public static boolean checkStinkDays(Date date, int quantity) {
		Date now = new Date();
		
		long timeout = TimeUnit.MILLISECONDS.convert(quantity, TimeUnit.DAYS);
		long diff = now.getTime() - date.getTime();
		
		return   diff >= timeout;
	}
	
	public static boolean checkFreshnessMinutes(Date date, int quantity) {
		Date now = new Date();
		
		if(now.before(date)) {
			return false;
		}
		
		long timeout = TimeUnit.MILLISECONDS.convert(quantity, TimeUnit.MINUTES);
		long diff = now.getTime() - date.getTime();
		
		
		return   diff <= timeout;
	}
}
