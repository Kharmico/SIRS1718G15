package utils;

import java.text.ParseException;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public final class DateUtil {

	private DateUtil() {}
	
	public static String getTimestamp() {
		DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ss");
		DateTime date = new DateTime().now();
		return dateFormat.print(date);
	}
	
	public static DateTime convertDate(String timestamp) throws ParseException {
		DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ss");
		return dateFormat.parseDateTime(timestamp);
	}
	
	
	public static boolean checkStinkDays(DateTime date, int quantity) throws ParseException {
		DateTime now = new DateTime();
		
		if(now.isBefore(date)) {
			return false;
		}
		
		return Days.daysBetween(now, date).getDays() >= quantity;
	}
	
	public static boolean checkFreshnessMinutes(DateTime date, int quantity) throws ParseException {
		DateTime now = new DateTime();
		
		if(now.isBefore(date)) {
			return false;
		}
		
		return Minutes.minutesBetween(now, date).getMinutes() <= quantity;
	}
}
