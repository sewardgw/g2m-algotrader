package com.g2m.services.tradingservices.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

//import org.hamcrest.collection.IsMapContaining;
import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Tick;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.tradingservices.enums.SecurityType;

@Component
public class SecurityTradingTime {

	private static long milliOneDay = 86400000;
	private static long milliOneWeek = 86400000*7;
	
	private static long cashMarketOpen = 338400000;
	private static long cashMarketClose = 165600000;
	
	private static long futureMarketOpen = 345600000;
	private static long futureMarketClose = 165600000;
	
	private static long stockMarketDailyOpen = 46800000;
	private static long stockMarketDailyClose = 75600000;
	private static long stockMarketWeeklyOpen = 392400000;
	private static long stockMarketWeeklyClose = 162000000;
	
	private static long currentCashEarlyClose = 0;
	private static long currentCashEarlyReOpen = 0;
	private static long lastCashEarlyClose = 0;
	private static long lastCashEarlyReOpen = 0;

	private static long currentFutureEarlyClose = 0;
	private static long currentFutureEarlyReOpen = 0;
	private static long lastFutureEarlyClose = 0;
	private static long lastFutureEarlyReOpen = 0;
	
	private static long currentStockEarlyClose = 0;
	private static long currentStockEarlyReOpen = 0;
	private static long lastStockEarlyClose = 0;
	private static long lastStockEarlyReOpen = 0;
	//private static File cashHolidayListFile = null; 
	private static BufferedReader reader = null;

	public SecurityTradingTime() {

	}


	public static boolean tickBeginsInOffTradingHours(Tick tick) {
		if(tick.getSecurity().getSecurityType().equals(SecurityType.CASH))
			return cashEventTimeInOffTradingHours(tick.getDateTime().getTime());

		else if(tick.getSecurity().getSecurityType().equals(SecurityType.FUTURE))
			return futureEventTimeInOffTradingHours(tick.getDateTime().getTime());
			
		else if(tick.getSecurity().getSecurityType().equals(SecurityType.STOCK)){
			return stockEventTimeInOffTradingHours(tick.getDateTime().getTime());
		}
		else return true;
	}

	private static boolean stockEventTimeInOffTradingHours(long dateTime) {
		if(isStockMarketClosed(dateTime))
			return true;
		else if(dateTime > currentStockEarlyReOpen)
			return false;//checkForNewEarlyMarketClose(dateTime);
		return false;
	}


	private static boolean futureEventTimeInOffTradingHours(long dateTime) {
		if(isFutureMarketClosed(dateTime))
			return true;
		else if(dateTime > currentFutureEarlyReOpen)
			return false;//checkForNewEarlyMarketClose(dateTime);
		return false;
	}


	private static boolean cashEventTimeInOffTradingHours(long dateTime) {
		if(isCashMarketClosed(dateTime))
			return true;
		else if(dateTime > currentCashEarlyReOpen)
			return false;//checkForNewEarlyMarketClose(dateTime);
		return false;
	}


	private static boolean isCashMarketClosed(long dateTime) {
		if(isCashMarketClosedForWeekend(dateTime))
			return true;
		if(isCashMarketClosedForEarlyMarketClose(dateTime))
			return true;
		return false;
	}

	private static boolean isCashMarketClosedForEarlyMarketClose(long dateTime){
		return dateTime >= currentCashEarlyClose && dateTime < currentCashEarlyReOpen;
	}

	private static boolean isCashMarketClosedForWeekend(long dateTime){
		return cashMarketOpen > dateTime%milliOneWeek && cashMarketClose <= dateTime%milliOneWeek;
	}

	private static boolean isFutureMarketClosed(long dateTime) {
		return isFutureMarketClosedForWeekend(dateTime) || isFutureMarketClosedForEarlyMarketClose(dateTime);
	}

	private static boolean isFutureMarketClosedForEarlyMarketClose(long dateTime){
		return dateTime >= currentFutureEarlyClose && dateTime < currentFutureEarlyReOpen;
	}

	private static boolean isFutureMarketClosedForWeekend(long dateTime){
		return futureMarketOpen > dateTime % milliOneWeek && futureMarketClose <= dateTime % milliOneWeek;
	}

	private static boolean isStockMarketClosed(long dateTime) {
		return isStockMarketClosedForDay(dateTime) || isStockMarketClosedForEarlyMarketClose(dateTime);
	}

	private static boolean isStockMarketClosedForEarlyMarketClose(long dateTime){
		return dateTime >= currentStockEarlyClose && dateTime < currentStockEarlyReOpen;
	}

	private static boolean isStockMarketClosedForDay(long dateTime){
		return dateTime % milliOneDay < stockMarketDailyOpen || dateTime % milliOneDay >= stockMarketDailyClose;
	}
	
	private static boolean isStockMarketClosedForWeekend(long dateTime){
		return stockMarketWeeklyOpen > dateTime % milliOneWeek && stockMarketWeeklyClose  <= dateTime % milliOneWeek;
	}


	public static boolean barBeginsDuringTradingHours(Bar bar) {
		if(bar.getSecurity().getSecurityType().equals(SecurityType.CASH))
			return !isCashMarketClosed(bar.getStartDateTime().getTime());

		else if(bar.getSecurity().getSecurityType().equals(SecurityType.FUTURE))
			return !isFutureMarketClosed(bar.getStartDateTime().getTime());

		else if(bar.getSecurity().getSecurityType().equals(SecurityType.STOCK))
			return !isStockMarketClosed(bar.getStartDateTime().getTime());
			
		else return true;
	}


	public static long getNextBarWindowStart(Long currentTime, BarSize barSize, Security security) {
		if(security.getSecurityType().equals(SecurityType.CASH))
			return adjustCashBarWindowStart(getNextBarWindowStart(currentTime, barSize), barSize.getSecondsInBar()*1000);

		else if(security.getSecurityType().equals(SecurityType.FUTURE))
			return adjustFutureBarWindowStart(getNextBarWindowStart(currentTime, barSize), barSize.getSecondsInBar()*1000);

		else if(security.getSecurityType().equals(SecurityType.STOCK)){
			return adjustStockBarWindowStart(getNextBarWindowStart(currentTime, barSize), barSize.getSecondsInBar()*1000);
			
		}
		return 0;
	}

	private static long getNextBarWindowStart(Long currentTime, BarSize barSize) {
		long remainder;
		long distFromNearestStart;
		long barWindowStart = 0;

		remainder = milliOneDay - (currentTime % milliOneDay);

		if (remainder != 0){
			distFromNearestStart = (barSize.getSecondsInBar()*1000) - (remainder % (barSize.getSecondsInBar()*1000));

			// The industry calculates pivot points from 6pm - 6pm so we offset the number of 
			// hours used to calculate the 1 day bar size by 2 hours
			if(barSize == BarSize._1_DAY)
				barWindowStart = currentTime - distFromNearestStart - (milliOneDay/12);

			barWindowStart =  currentTime - distFromNearestStart;

		} else{
			if(barSize == BarSize._1_DAY)
				barWindowStart = currentTime -   (milliOneDay/12);
			else
				barWindowStart = currentTime;  
		}
		return barWindowStart;
	}
	
	private static long adjustStockBarWindowStart(long originalBarWindowStart, long barDuration) {

		if(isStockMarketClosedForEarlyMarketClose(originalBarWindowStart)){

			// If the cash market is closed due to an early market close,
			// we'll update the start time of the bar to be next time the 
			// market reopens

			long updatedStart = getStockNextDayOpen(currentFutureEarlyReOpen);

			// After updating the start time, we'll double check to make sure that the new
			// updated start time is still within trading hours. If it is within trading
			// hours then we'll return the updated start time. 
			if(!isStockMarketClosedForWeekend(updatedStart))
				return updatedStart;

			// If the updated time is not within trading hours then we'll set the 
			// new bar time to be the market open next week.
			else
				return createStockBarWindowStartNextWeek(stockMarketWeeklyOpen, updatedStart);
		}


		// If the cash market is closed for the weekend when this bar is requesting a start time,
		// then we'll update the start time to be the market open next week.
		else if(isStockMarketClosedForWeekend(originalBarWindowStart)){
			return createStockBarWindowStartNextWeek(stockMarketWeeklyOpen, originalBarWindowStart);
		}

		// If the cash market is not closed and this is not impacted by an early market close
		// the we'll return the original value for the bar window start.
		else {
			return originalBarWindowStart;
		}
	}

	private static long getStockNextDayOpen(long currentOpenTime) {
		return currentOpenTime + (milliOneDay - (milliOneDay - currentOpenTime % milliOneDay)) + stockMarketDailyOpen;
	}


	private static long adjustFutureBarWindowStart(long originalBarWindowStart, long barDuration) {

		if(isFutureMarketClosedForEarlyMarketClose(originalBarWindowStart)){

			// If the cash market is closed due to an early market close,
			// we'll update the start time of the bar to be next time the 
			// market reopens

			long updatedStart = currentFutureEarlyReOpen;

			// After updating the start time, we'll double check to make sure that the new
			// updated start time is still within trading hours. If it is within trading
			// hours then we'll return the updated start time. 
			if(!isFutureMarketClosedForWeekend(updatedStart))
				return updatedStart;

			// If the updated time is not within trading hours then we'll set the 
			// new bar time to be the market open next week.
			else
				return createFutureBarWindowStartNextWeek(futureMarketOpen, updatedStart);
		}


		// If the cash market is closed for the weekend when this bar is requesting a start time,
		// then we'll update the start time to be the market open next week.
		else if(isFutureMarketClosedForWeekend(originalBarWindowStart)){
			return createFutureBarWindowStartNextWeek(futureMarketOpen, originalBarWindowStart);
		}

		// If the cash market is not closed and this is not impacted by an early market close
		// the we'll return the original value for the bar window start.
		else {
			return originalBarWindowStart;
		}
	}

	private static long adjustCashBarWindowStart(long originalBarWindowStart, long barDuration) {

		if(isCashMarketClosedForEarlyMarketClose(originalBarWindowStart)){

			// If the cash market is closed due to an early market close,
			// we'll update the start time of the bar to be next time the 
			// market reopens

			long updatedStart = currentCashEarlyReOpen;

			// After updating the start time, we'll double check to make sure that the new
			// updated start time is still within trading hours. If it is within trading
			// hours then we'll return the updated start time. 
			if(!isCashMarketClosedForWeekend(updatedStart))
				return updatedStart;

			// If the updated time is not within trading hours then we'll set the 
			// new bar time to be the market open next week.
			else
				return createCashBarWindowStartNextWeek(cashMarketOpen, updatedStart);
		}


		// If the cash market is closed for the weekend when this bar is requesting a start time,
		// then we'll update the start time to be the market open next week.
		else if(isCashMarketClosedForWeekend(originalBarWindowStart)){
			return createCashBarWindowStartNextWeek(cashMarketOpen, originalBarWindowStart);
		}

		// If the cash market is not closed and this is not impacted by an early market close
		// the we'll return the original value for the bar window start.
		else {
			return originalBarWindowStart;
		}
	}


	private static long createCashBarWindowStartNextWeek(long weeklyMarketOpen, long updatedStart) {
		return updatedStart + (weeklyMarketOpen - (updatedStart % milliOneWeek));
	}
	
	private static long createFutureBarWindowStartNextWeek(long weeklyMarketOpen, long updatedStart) {
		return updatedStart + (weeklyMarketOpen - (updatedStart % milliOneWeek));
	}
	
	private static long createStockBarWindowStartNextWeek(long weeklyMarketOpen, long updatedStart) {
		return updatedStart + (weeklyMarketOpen - (updatedStart % milliOneWeek));
	}

	public static int getExpectedNumberBarsMissed(Bar lastBar, long currentBarStartTime) {
		if(lastBar.getSecurity().getSecurityType().equals(SecurityType.CASH)){
			return getExpectedNumberCashBarsMissed(lastBar, currentBarStartTime);
		}
		else if(lastBar.getSecurity().getSecurityType().equals(SecurityType.FUTURE)){
			return getExpectedNumberFutureBarsMissed(lastBar, currentBarStartTime);
			
		}
		else if(lastBar.getSecurity().getSecurityType().equals(SecurityType.STOCK)){
			return getExpectedNumberStockBarsMissed(lastBar, currentBarStartTime);
		}
		return 0;
	}

	private static int getExpectedNumberCashBarsMissed(Bar lastBar, long barWindowStart) {
		long lastBarEnd = lastBar.getDateTime().getTime();
		long totalTimeBetween = barWindowStart - lastBarEnd;
		if(totalTimeBetween == 0)
			return 0;

		// If the cash market was closed early, then we need to remove that time
		// when determining if more bars should be created.
		if (wasCashMarketClosedEarly(lastBarEnd, lastBar.getBarSize(), barWindowStart)){
			
			// If the early market close was a predecessor to the weekend, then we also
			// remove the weekend time as well.
			if(lastCashEarlyReOpen % milliOneWeek >= cashMarketClose){
				totalTimeBetween = totalTimeBetween - (lastCashEarlyReOpen - lastCashEarlyClose);  
				totalTimeBetween = totalTimeBetween - (cashMarketOpen - cashMarketClose);
				totalTimeBetween = totalTimeBetween	+ ((lastCashEarlyReOpen % milliOneWeek) - cashMarketClose);
			}
			else
				totalTimeBetween = totalTimeBetween - (currentCashEarlyReOpen  - currentCashEarlyClose);
		}
	
		// If the market was not closed early, but the last bar was created just before the 
		// market closed for the weekend, then remove the weekend time from the duration.
		else if(wasCashMarketClosedForWeekend(lastBarEnd, barWindowStart)){ 
			totalTimeBetween = totalTimeBetween - (cashMarketOpen - cashMarketClose);
		} 
		
		return (int) (totalTimeBetween / (lastBar.getBarSize().getSecondsInBar()*1000));
	}

	
	private static int getExpectedNumberFutureBarsMissed(Bar lastBar, long barWindowStart) {
		long lastBarEnd = lastBar.getDateTime().getTime();
		long totalTimeBetween = barWindowStart - lastBarEnd;
		if(totalTimeBetween == 0)
			return 0;

		// If the cash market was closed early, then we need to remove that time
		// when determining if more bars should be created.
		if (wasFutureMarketClosedEarly(lastBarEnd, lastBar.getBarSize(), barWindowStart)){
			
			// If the early market close was a predecessor to the weekend, then we also
			// remove the weekend time as well.
			if(lastFutureEarlyReOpen % milliOneWeek >= futureMarketClose){
				totalTimeBetween = totalTimeBetween - (lastFutureEarlyReOpen - lastFutureEarlyClose);  
				totalTimeBetween = totalTimeBetween - (futureMarketOpen - futureMarketClose);
				totalTimeBetween = totalTimeBetween	+ ((lastFutureEarlyReOpen % milliOneWeek) - futureMarketClose);
			}
			else
				totalTimeBetween = totalTimeBetween - (currentFutureEarlyReOpen - currentFutureEarlyClose);
		}
	
		// If the market was not closed early, but the last bar was created just before the 
		// market closed for the weekend, then remove the weekend time from the duration.
		else if(wasFutureMarketClosedForWeekend(lastBarEnd, barWindowStart)){ 
			totalTimeBetween = totalTimeBetween - (futureMarketOpen - futureMarketClose);
		} 
		
		return (int) (totalTimeBetween / (lastBar.getBarSize().getSecondsInBar()*1000));
	}
	

	private static int getExpectedNumberStockBarsMissed(Bar lastBar, long barWindowStart) {
		long lastBarEnd = lastBar.getDateTime().getTime();
		long totalTimeBetween = barWindowStart - lastBarEnd;
		if(totalTimeBetween == 0)
			return 0;

		// If the cash market was closed early, then we need to remove that time
		// when determining if more bars should be created.
		if (wasStockMarketClosedEarly(lastBarEnd, lastBar.getBarSize(), barWindowStart)){
			
			// If the early market close was a predecessor to the weekend, then we also
			// remove the weekend time as well.
			if(lastStockEarlyReOpen % milliOneWeek >= stockMarketWeeklyClose){
				totalTimeBetween = totalTimeBetween - (lastStockEarlyReOpen - lastStockEarlyClose);  
				totalTimeBetween = totalTimeBetween - (stockMarketWeeklyOpen - stockMarketWeeklyClose);
				totalTimeBetween = totalTimeBetween	+ ((lastStockEarlyReOpen % milliOneWeek) - stockMarketWeeklyClose);
			}
			else
				totalTimeBetween = totalTimeBetween - (currentStockEarlyReOpen - currentStockEarlyClose);
		}
	
		// If the market was not closed early, but the last bar was created just before the 
		// market closed for the weekend, then remove the weekend time from the duration.
		else if(wasStockMarketClosedForWeekend(lastBarEnd, barWindowStart)){ 
			totalTimeBetween = totalTimeBetween - (stockMarketWeeklyOpen - stockMarketWeeklyClose);
		} 
		
		return (int) (totalTimeBetween / (lastBar.getBarSize().getSecondsInBar()*1000));
	}
	
	private static boolean wasCashMarketClosedForWeekend(long lastBarEnd, long barWindowStart) {
		if(lastBarEnd % milliOneWeek <= cashMarketClose)
			if((barWindowStart+ BarSize._1_HOUR.getSecondsInBar()*1000) % milliOneWeek >= cashMarketOpen)
				return true;
		return false;
		//return lastBarEnd % milliOneWeek <= cashMarketClose && barWindowStart % milliOneWeek >= cashMarketOpen;
	}
	
	private static boolean wasFutureMarketClosedForWeekend(long lastBarEnd, long barWindowStart) {
		return lastBarEnd % milliOneWeek >= futureMarketClose && barWindowStart >= futureMarketOpen;
	}
	
	private static boolean wasStockMarketClosedForWeekend(long lastBarEnd, long barWindowStart) {
		return lastBarEnd % milliOneWeek >= stockMarketWeeklyClose && barWindowStart >= stockMarketWeeklyOpen;
	}


	private static boolean wasCashMarketClosedEarly(long lastBarEnd, BarSize barSize , long currentBarStart) {
		if((lastBarEnd >= currentCashEarlyClose || lastBarEnd + barSize.getSecondsInBar()*1000 >= currentCashEarlyClose)
				&& lastBarEnd < currentCashEarlyReOpen)
			return true;
		else if (lastBarEnd <= lastCashEarlyClose && currentBarStart >= lastCashEarlyReOpen)
			return true;
		else if(lastBarEnd >= lastCashEarlyClose && lastBarEnd - barSize.getSecondsInBar()*1000 < lastCashEarlyClose )
			return true;
		return false;
	}
	

	private static boolean wasFutureMarketClosedEarly(long lastBarEnd, BarSize barSize , long currentBarStart) {
		if((lastBarEnd >= currentFutureEarlyClose || lastBarEnd + barSize.getSecondsInBar()*1000 >= currentFutureEarlyClose)
				&& lastBarEnd < currentFutureEarlyReOpen)
			return true;
		else if (lastBarEnd <= lastFutureEarlyClose && currentBarStart >= lastFutureEarlyReOpen)
			return true;
		else if(lastBarEnd >= lastFutureEarlyClose && lastBarEnd - barSize.getSecondsInBar()*1000 < lastFutureEarlyClose )
			return true;
		return false;
	}


	private static boolean wasStockMarketClosedEarly(long lastBarEnd, BarSize barSize , long currentBarStart) {
		if((lastBarEnd >= currentStockEarlyClose || lastBarEnd + barSize.getSecondsInBar()*1000 >= currentStockEarlyClose)
				&& lastBarEnd < currentStockEarlyReOpen)
			return true;
		else if (lastBarEnd <= lastStockEarlyClose && currentBarStart >= lastStockEarlyReOpen)
			return true;
		else if(lastBarEnd >= lastStockEarlyClose && lastBarEnd - barSize.getSecondsInBar()*1000 < lastStockEarlyClose )
			return true;
		return false;
	}
	
	private static void checkForNewEarlyMarketClose(long dateTime) {
		if(reader == null){
			try {
				String homeLocation = System.getProperty("user.home") + "/BitBucket/g2m-services/resources/CashEarlyMarketClose";
				System.out.println(homeLocation);
				reader = new BufferedReader(new FileReader(homeLocation));
			} catch (FileNotFoundException e) {
				// 
				e.printStackTrace();
			}
		}

		try {
			String line;
			while (null != (line = reader.readLine())) {
				if(!line.contains("//")){
					String[] openCloseTimes = line.split(":");
					if(Long.valueOf(openCloseTimes[1]) > dateTime){
						lastCashEarlyClose = currentCashEarlyClose;
						lastCashEarlyReOpen = currentCashEarlyReOpen;
						
						currentCashEarlyClose = Long.valueOf(openCloseTimes[0]);
						currentCashEarlyReOpen = Long.valueOf(openCloseTimes[1]);
						
						lastFutureEarlyClose = currentFutureEarlyClose;
						lastFutureEarlyReOpen = currentFutureEarlyReOpen;
						
						currentFutureEarlyClose = Long.valueOf(openCloseTimes[0]);
						currentFutureEarlyReOpen = Long.valueOf(openCloseTimes[1]);
						
						lastStockEarlyClose = currentStockEarlyClose;
						lastStockEarlyReOpen = currentStockEarlyReOpen;
						
						currentStockEarlyClose = Long.valueOf(openCloseTimes[0]);
						currentStockEarlyReOpen = Long.valueOf(openCloseTimes[1]);
						break;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
