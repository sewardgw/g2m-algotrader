package com.g2m.services.tradingservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.shared.utilities.DateTimeUtility;
import com.g2m.services.tradingservices.caches.BarCache;
import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Bar.BarBuilder;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.orders.MarketOrder;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.entities.Tick;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.tradingservices.enums.OrderAction;
import com.g2m.services.tradingservices.utilities.SecurityTradingTime;

/**
 * Added 4/2/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class BarPublisher {
	
	@Autowired
	private BarCache barCache;
	private Map<SecurityKey, Map<BarSize, BarPublisherData>> barPublishersData;
	private Set<SecurityKey> securityBarStartTime;
	private TickSubscriber tickSubscriber;
	long milliInDay = 24*60*60*1000;
	long twoHoursInMilliseconds = 2*60*60*1000;
	BarSize smallestBarSize = null;
	private static boolean fillCacheOnly = false;

	public BarPublisher() {
		barPublishersData = new HashMap<SecurityKey, Map<BarSize, BarPublisherData>>();
		tickSubscriber = new TicksForBarsSubscriber();
		securityBarStartTime = new HashSet<SecurityKey>();

	}

	public void clear() {
		barPublishersData.clear();
	}

	public Set<SecurityKey> getSecurityKeys() {
		return barPublishersData.keySet();
	}

	public void addBarSubscriber(SecurityKey securityKey, BarSize barSize, BarSubscriber barSubscriber) {
		getBarPublisherData(securityKey, barSize).addBarSubscriber(barSubscriber);

		// Each time a new bar size is registered we'll see if we can 
		// set the smallestBarSize to a lower value. The smallestBarSize
		// can change whether we're testing with historical bar data 
		// (which has a min of 1 min from IB) or if we're testing with 
		// historical tick data (which can have a min of 1 second)
		
		if (smallestBarSize == null)
			smallestBarSize = barSize;
		else if (smallestBarSize.getSecondsInBar() > barSize.getSecondsInBar())
			smallestBarSize = barSize;
	}

	private void tickReceived(Tick tick) {
		if (!barPublishersData.containsKey(tick.getSecurity().getKey()) 
				|| SecurityTradingTime.tickBeginsInOffTradingHours(tick)) {
			return;
		}

		// TODO set the initial bar window start time here
		// Check to see if the key set has already been added to the securityBarStartTime
		// hashmap, if is has, then we know that the intial start times have already been
		// set. If it has not been added, then we need to set the initial start times.
		if (!securityBarStartTime.contains(tick.getSecurity().getKey())){
			setInitialBarStartTimes(tick);
			securityBarStartTime.add(tick.getSecurity().getKey());
		}

		BarPublisherData barPublisherData = barPublishersData.get(tick.getSecurity().getKey()).get(smallestBarSize);
		
		barPublisherData.addTickToBuffer(tick);
		if (barPublisherData.isTickBufferFull()) {
			publishBarFromTicks(barPublisherData, false);
		}

	}

	private void setInitialBarStartTimes(Tick tick) {
		// Create a new map that will be used to store the start time for the current bar
		// for each bar size for a given security key. We will update these every time that
		// a new bar is created
		for (BarSize barSize : barPublishersData.get(tick.getSecurity().getKey()).keySet()){
			Long currTime = tick.getDateTime().getTime();
			barPublishersData.get(tick.getSecurity().getKey()).get(barSize).setBarWindowStart(currTime);
		}
	}

	public void flushBufferedBars() {
		for (SecurityKey securityKey : barPublishersData.keySet()) {
			for (BarSize barSize : barPublishersData.get(securityKey).keySet()) {
				BarPublisherData barPublisherData = barPublishersData.get(securityKey).get(barSize);
				if (!barPublisherData.isTickBufferEmpty()) {
					publishBarFromTicks(barPublishersData.get(securityKey).get(barSize), true);
				}
			}
		}
	}

	private void publishBarFromTicks(BarPublisherData barPublisherData, boolean flushBuffer) {
		Bar bar = barPublisherData.createBarFromBuffer(flushBuffer);
		newBarCreated(bar, barPublisherData);
	}

	// Every time a new bar is created, regardless of whether it was created from
	// ticks or from other bars, three steps are taken:
	// 1) Save the bar to the cache
	// 2) Publish the bar to the subscribers
	// 3) Check to see if any larger bars can be created and repeat
	private void newBarCreated(Bar bar, BarPublisherData barPublisherData){
		if(smallestBarSize.equals(bar.getBarSize())){
			List<Bar> missedBars = barsMissedFromSporadicTicks(barPublisherData); 
			if(!missedBars.isEmpty()){
				for(Bar missedBar : missedBars)
					if(SecurityTradingTime.barBeginsDuringTradingHours(missedBar))
						saveAndPublishBar(missedBar, barPublisherData);
			}
		}
		
		if(SecurityTradingTime.barBeginsDuringTradingHours(bar))
			saveAndPublishBar(bar, barPublisherData);
	}

	private void saveAndPublishBar(Bar bar, BarPublisherData barPublisherData) {
		barCache.save(bar);
		for (BarSubscriber barSubscriber : barPublisherData.getBarSubscribers()) {
			if(!fillCacheOnly)
				barSubscriber.barCreated(bar);
			else
				barSubscriber.barCreatedFillCacheOnly(bar);
		}
		List<BarSize> barsToBuild = canCreateLargerBar(bar, barPublisherData.getSecurity().getKey());
		if (!barsToBuild.isEmpty()){
			for(BarSize barSize : barsToBuild){
				BarPublisherData largerBarPublisherData = barPublishersData.get(barPublisherData.getSecurity().getKey()).get(barSize);
				Bar largerBar = largerBarPublisherData.createLargerBarFromSmallerBars(
						largerBarPublisherData.getSecurity().getKey() ,bar.getBarSize(), barSize);
				newBarCreated(largerBar, largerBarPublisherData);						
			}
		}
	}

	private List<Bar> barsMissedFromSporadicTicks(BarPublisherData barPublisherData) {

		List<Bar> barsMissedFromTicks = new ArrayList<Bar>();
		
		Bar lastBar = barCache.getMostRecentBar(barPublisherData.getSecurity().getKey(), barPublisherData.getBarSize());
		
		if(lastBar != null){
			long lastBarEndTime = lastBar.getDateTime().getTime() + (lastBar.getBarSize().getSecondsInBar()*1000);
			long recentBarStartTime = barPublisherData.getBarWindowStart();
			int expectedBarsBetween = SecurityTradingTime.getExpectedNumberBarsMissed(lastBar, recentBarStartTime); 
					//(int) ((recentBarStartTime - lastBarEndTime) / (barPublisherData.getBarSize().getSecondsInBar()*1000));
			if (expectedBarsBetween > 0){
	//			System.out.println(expectedBarsBetween + " Bars created from being missed");
				for(int i = 0; i < expectedBarsBetween; i++)
					barsMissedFromTicks.add(0, barPublisherData.createMissingBarFromLastValue(
							lastBar.getClose(), lastBar.getDateTime().getTime(), expectedBarsBetween-i));
			}
			return barsMissedFromTicks;
		} 
		
		return Collections.emptyList();
	}

	private List<BarSize> canCreateLargerBar(Bar bar, SecurityKey securityKey) {
		
		List<BarSize> eligibleBarSizes = BarSize.getNextBarSizes(bar.getBarSize());
		List<BarSize> barSizesToUpdate = new ArrayList<BarSize>();
		for (BarSize barSize : eligibleBarSizes){
			// Bars are always built from the lowest interval and are built upwards 
			// Here we check the time of the lower level bar that was created for a given security
			// And if the lower level bar start time is <= to the expected end time for the larger
			// Bar then we know to create the larger bar. 
			// NOTE: The end time for the larger bar should ALWAYS match the start of the smaller bar
			
			if(barPublishersData.get(securityKey).get(barSize).getBarWindowEnd() <= 
					bar.getDateTime().getTime() ){
				barSizesToUpdate.add(barSize);
			}
		}

		if (barSizesToUpdate.isEmpty())
			return Collections.emptyList();

		return barSizesToUpdate;
	}

	private BarPublisherData getBarPublisherData(SecurityKey securityKey, BarSize barSize) {
		if (!barPublishersData.containsKey(securityKey)) {
			barPublishersData.put(securityKey, new HashMap<BarSize, BarPublisherData>());
		}

		if (!barPublishersData.get(securityKey).containsKey(barSize)) {
			barPublishersData.get(securityKey).put(barSize, new BarPublisherData(securityKey, barSize));
		}

		return barPublishersData.get(securityKey).get(barSize);
	}

	public TickSubscriber getTickSubscriber() {
		return tickSubscriber;
	}

	private class TicksForBarsSubscriber implements TickSubscriber {
		public void tickReceived(Tick tick) {
			BarPublisher.this.tickReceived(tick);
		}

		@Override
		public void tickReceivedFillCacheOnly(Tick tick) {
			
			
		}
	}



	public class BarPublisherData {
		private final Security security;
		private final BarSize barSize;
		private List<BarSubscriber> barSubscribers;
		private List<Tick> tickBuffer;
		private long barWindowStart;
		private long barWindowEnd;

		public BarPublisherData(SecurityKey securityKey, BarSize barSize) {
			this.security = SecurityRegistry.get(securityKey);
			this.barSize = barSize;
			barSubscribers = new LinkedList<BarSubscriber>();
			tickBuffer = new LinkedList<Tick>();
		}

		public Security getSecurity(){
			return security;
		}

		public BarSize getBarSize(){
			return barSize;
		}

		public boolean isTickBufferEmpty() {
			return tickBuffer.isEmpty() || 0 == barWindowStart;
		}

		public void setBarWindowStart(Long currentTime){
			//System.out.println("CURRENT TIME: " + new Date(currentTime));
			barWindowStart = SecurityTradingTime.getNextBarWindowStart(currentTime, barSize, security);
			//System.out.println("NEW BAR WINDOW START " + new Date(barWindowStart));
			setBarWindowEnd();
		}

		public boolean isTickBufferFull() {
			if (isTickBufferEmpty()) {
				return false;
			}
			return (getLastTickInBuffer().getDateTime().getTime() > getBarWindowEnd());
		}

		private long getBarWindowEnd() {
			return barWindowEnd;
		}
		
		private void setBarWindowEnd(){
			barWindowEnd = barWindowStart + barSize.getSecondsInBar() * 1000;
		}

		private long getBarWindowStart() {
			return barWindowStart;
		}

		public void addTickToBuffer(Tick tick) {
			tickBuffer.add(tick);

			// should only get executed once, when the very first tick is added
			// every other time there should be at least one tick in the buffer
			// before addTickToBuffer is called; see clearTickBuffer()
			if (1 == tickBuffer.size()) {
				setBarWindowStart(
						getFirstTickInBuffer().getDateTime().getTime());
			}
		}

		// Added so that during the very first bar creation the start time doesn't
		// occur until the millisecond that syncs up evenly as though the stream
		// of bars began at exactly 6pm each day. 
		protected long getNearestPossibleBarStartTime(long currTime, BarSize barSize) {
			long remainder;
			long distFromNearestStart;

			remainder = milliInDay - (currTime % milliInDay);
			if (remainder != 0){
				distFromNearestStart = (barSize.getSecondsInBar()*1000) - (remainder % (barSize.getSecondsInBar()*1000));

				// The industry calculates pivot points from 6pm - 6pm so we offset the number of 
				// hours used to calculate the 1 day bar size by 2 hours
				if(barSize == BarSize._1_DAY)
					return currTime - distFromNearestStart - twoHoursInMilliseconds;
				else
					return currTime - distFromNearestStart;

			} else{
				if(barSize == BarSize._1_DAY)
					return currTime -  twoHoursInMilliseconds;
				else
					return currTime;  
			}
		}


		private void updateBarWindowStart() {
			
			do {
				//setBarWindowStart(getBarWindowEnd());
				setBarWindowStart(getLastTickInBuffer().getDateTime().getTime());
			} while (tickBuffer.get(0).getDateTime().getTime() > getBarWindowEnd());
		}

		private void updateBarWindowStartCreatingBarFromBars() {	
			setBarWindowStart(getBarWindowEnd() + 1);
		}


		public Bar createBarFromBuffer(boolean flushBuffer) {
			double high = 0;
			double low = Double.MAX_VALUE;
			long totalVolume = 0;
			int lastTickIndex = tickBuffer.size() - (flushBuffer ? 0 : 1);

			for (int i = 0; i < lastTickIndex; i++) {
				Tick tick = tickBuffer.get(i);

				// Added the check to see if the last price is equal to 0.0, if so then 
				// The instrument never has a last traded price (i.e. Forex) and 
				// We need to take the average of the bid and the ask for a given tick
				if (tick.getLastPrice() > high && tick.getLastPrice() != 0.0)
					high = tick.getLastPrice();
				else if (tick.getLastPrice() == 0.0 && (tick.getAskPrice() + tick.getBidPrice()) / 2 > high)
					high = (tick.getAskPrice() + tick.getBidPrice()) / 2;

				if (tick.getLastPrice() < low  && tick.getLastPrice() != 0.0)
					low = tick.getLastPrice();
				else if (tick.getLastPrice() == 0.0 && (tick.getAskPrice() + tick.getBidPrice()) / 2 < low)
					low = (tick.getAskPrice() + tick.getBidPrice()) / 2;

				totalVolume += (long) tick.getVolume();
			}
			double open = 0.0;
			double close = 0.0;

			if (getFirstTickInBuffer().getLastPrice() != 0.0)
				open = getFirstTickInBuffer().getLastPrice();
			else
				open = (getFirstTickInBuffer().getBidPrice() + getFirstTickInBuffer().getAskPrice()) / 2;

			if((flushBuffer ? getLastTickInBuffer().getLastPrice() : getLastTickInBar().getLastPrice()) != 0.0)
				close = (flushBuffer ? getLastTickInBuffer().getLastPrice() : getLastTickInBar().getLastPrice()); 
			else
				close = (flushBuffer ? ((getLastTickInBuffer().getBidPrice() + getLastTickInBuffer().getAskPrice())/2) : 
					((getLastTickInBar().getBidPrice() + getLastTickInBar().getAskPrice()) / 2 ));

			clearTickBuffer(flushBuffer);
			if (!flushBuffer) {
				//System.out.println(getLastTickInBuffer().getDateTime());
				updateBarWindowStart();
			}
			Bar bar = createBarFromValues(high, low, open, close, totalVolume);
			return bar;
		}

		public Bar createLargerBarFromSmallerBars(SecurityKey securityKey,
				BarSize smallBarSize, BarSize newBarSize) {
			double high = 0;
			double low = Double.MAX_VALUE;
			double open = 0.0;
			double close = 0.0;
			long totalVolume = 0;
			int numBarsNeeded = numSmallBarsNeededForLargeBar(smallBarSize, newBarSize);
			List<Bar> barsToCreateBar = barCache.getMostRecentBars(
					securityKey, smallBarSize, numBarsNeeded);

			int barCnt = 1;
			for (Bar bar : barsToCreateBar) {

				if(bar.getHigh() > high)
					high = bar.getHigh();
				if(bar.getLow() < low)
					low = bar.getLow();
				if(barCnt == 1)
					open = bar.getOpen();
				if(barCnt == barsToCreateBar.size())
					close = bar.getClose();

				totalVolume += bar.getVolume();
				barCnt++;
			}

			
			Bar newBar = createBarFromValues(high, low, open, close, totalVolume);
			updateBarWindowStartCreatingBarFromBars();

			return newBar;
		}

		public Bar createMissingBarFromLastValue(double lastVal, long lastBarEndTime, int numBarsPriorToMostRecent) {
			long barStartTime = lastBarEndTime + ((numBarsPriorToMostRecent-1) * this.getBarSize().getSecondsInBar()*1000);
			//barStartTime = SecurityTradingTime.getNextBarWindowStart(barStartTime, barSize, security);
			Bar newBar = createMissingBar(lastVal, 0, barStartTime);
			return newBar;
		}

		private int numSmallBarsNeededForLargeBar(BarSize smallBarSize,BarSize newBarSize) {
			return newBarSize.getSecondsInBar() / smallBarSize.getSecondsInBar();
		}

		private Bar createBarFromValues(double high, double low, double open, double close, long totalVolume) {
			BarBuilder builder = new BarBuilder();
			builder.setSecurity(security);
			builder.setDateTime(new Date(barWindowEnd));
			builder.setStartDateTime(new Date(barWindowStart));
			builder.setHigh(high);
			builder.setLow(low);
			builder.setOpen(open);
			builder.setClose(close);
			builder.setVolume(totalVolume);
			builder.setBarSize(barSize);
			return builder.build();
		}
		
		private Bar createMissingBar(double lastVal, long totalVolume, long barWindowStart) {
			BarBuilder builder = new BarBuilder();
			builder.setSecurity(security);
			builder.setDateTime(new Date(barWindowStart + barSize.getSecondsInBar()*1000));
			builder.setStartDateTime(new Date(barWindowStart ));
			builder.setHigh(lastVal);
			builder.setLow(lastVal);
			builder.setOpen(lastVal);
			builder.setClose(lastVal);
			builder.setVolume(totalVolume);
			builder.setBarSize(barSize);
			return builder.build();
		}

		private Tick getFirstTickInBuffer() {
			return tickBuffer.get(0);
		}

		private Tick getLastTickInBuffer() {
			return tickBuffer.get(tickBuffer.size() - 1);
		}

		/**
		 * The last tick in the bar (created from the current buffer) is the second to last tick.
		 * The assumption here is that the last tick was just outside of the window for the bar and
		 * thus is what triggerd the creation of the bar. The last tick doesn't belong to the bar
		 * and must be kept out.
		 */
		// Was having some issue where it was immediately throwing an exception for index
		// out of bounds. Adding this removed the exception.
		private Tick getLastTickInBar() {
			if(tickBuffer.size() > 1)
				return tickBuffer.get(tickBuffer.size() - 2);
			else
				return tickBuffer.get(tickBuffer.size() - 1);
		}

		/**
		 * Clear all but the last tick in the buffer. The last tick will be the tick that triggered
		 * the bar, i.e. it came at or past the bar window thus it doesn't belong with the current
		 * bar.
		 */
		private void clearTickBuffer(boolean flushBuffer) {
			if (flushBuffer) {
				tickBuffer.clear();
			} else {
				Tick lastTick = tickBuffer.get(tickBuffer.size() - 1);
				tickBuffer.clear();
				tickBuffer.add(lastTick);
			}
		}

		private Date getDateTimeForBar() {
//			if (BarSize._1_DAY == barSize) {
//				// TODO this will need to be fixed, need to think about what the logic should be
//				return DateTimeUtility.getMidnightEST(getFirstTickInBuffer().getDateTime());
//			} else {
				return new Date(getBarWindowStart());
//			}
		}

		public void addBarSubscriber(BarSubscriber barSubscriber) {
			barSubscribers.add(barSubscriber);
		}

		public List<BarSubscriber> getBarSubscribers() {
			return barSubscribers;
		}
	}



	public static void setFillBarCacheOnlyNotPublish(boolean fillCacheOnly) {
		BarPublisher.fillCacheOnly = fillCacheOnly;
	}
}
