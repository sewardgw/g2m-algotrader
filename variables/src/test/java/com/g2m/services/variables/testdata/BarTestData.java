package com.g2m.services.variables.testdata;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Bar.BarBuilder;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.enums.BarSize;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
public class BarTestData {
	public static final Date START_DATETIME = new GregorianCalendar(2014, 1, 1, 0, 0, 0).getTime();

	/**
	 * The test bars created here will have their open/close/high/low values set to the sequence
	 * number in which they come. E.g. The second bar generated will have its open/close/high/low
	 * values set to 2.
	 */
	public static List<Bar> createTestBars(Security security, BarSize barSize, int count) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(START_DATETIME);
		List<Bar> bars = new ArrayList<Bar>();
		BarBuilder barBuilder = new BarBuilder();
		for (int i = 1; i <= count; i++) {
			barBuilder.setSecurity(security);
			barBuilder.setBarSize(barSize);
			barBuilder.setDateTime(calendar.getTime());
			barBuilder.setHigh(i);
			barBuilder.setLow(i);
			barBuilder.setOpen(i);
			barBuilder.setClose(i);
			barBuilder.setVolume(i);
			bars.add(barBuilder.build());
			calendar.add(Calendar.SECOND, barSize.getSecondsInBar());
		}
		return bars;
	}
}
