package com.g2m.services.shared.utilities;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author Michael Borromeo
 */
public class DateTimeUtility {
	public static final String EST_TIME_ZONE = "America/New_York";
	public static Calendar midnightEST = Calendar.getInstance(TimeZone.getTimeZone(EST_TIME_ZONE));
	
	public static Date getMidnightEST(Date date) {
		midnightEST.setTime(date);
		midnightEST.set(Calendar.MINUTE, 0);
		midnightEST.set(Calendar.HOUR, 0);
		midnightEST.set(Calendar.SECOND, 0);
		midnightEST.set(Calendar.AM_PM, Calendar.AM);

		return midnightEST.getTime();
	}
}
