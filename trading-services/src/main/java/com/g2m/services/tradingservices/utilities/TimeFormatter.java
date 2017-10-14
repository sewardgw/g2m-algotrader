package com.g2m.services.tradingservices.utilities;

import java.util.concurrent.TimeUnit;

public class TimeFormatter {

	public TimeFormatter() {
		// TODO Auto-generated constructor stub
	}
	
	public static String getTimeDifferenceString(Long timeDiff) {
		return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(timeDiff),
				TimeUnit.MILLISECONDS.toMinutes(timeDiff) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeDiff)),
				TimeUnit.MILLISECONDS.toSeconds(timeDiff) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes
						(timeDiff)));
	}


}
