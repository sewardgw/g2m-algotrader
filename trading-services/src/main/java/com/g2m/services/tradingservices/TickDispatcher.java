package com.g2m.services.tradingservices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.caches.TickCache;
import com.g2m.services.tradingservices.entities.PendingTick;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.Tick;
import com.g2m.services.tradingservices.enums.SecurityType;

/**
 * Added 3/29/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class TickDispatcher {
	@Autowired
	private TickCache tickCache;
	private Map<SecurityKey, List<TickSubscriber>> tickSubscriberMap;
	private long milliOneDay = 86400000;
	private static boolean fillCacheOnly = false;

	public TickDispatcher() {
		tickSubscriberMap = new HashMap<SecurityKey, List<TickSubscriber>>();
	}

	public void addTickSubscriber(SecurityKey securityKey, TickSubscriber tickSubscriber) {
		if (!tickSubscriberMap.containsKey(securityKey)) {
			tickSubscriberMap.put(securityKey, new ArrayList<TickSubscriber>());
		}
		tickSubscriberMap.get(securityKey).add(tickSubscriber);
	}

	public Set<SecurityKey> getSecurityKeys() {
		return tickSubscriberMap.keySet();
	}

	public void dispatchPendingTicks(List<PendingTick> pendingTicks) {
		for (PendingTick pendingTick : pendingTicks) {
			Tick tick = pendingTick.createTick();
			dispatchTick(tick);
		}
	}

	public void dispatchTick(Tick tick) {
		if(isValidTick(tick)){
			tickCache.save(tick);
			
			// We keep track of conversion ratios so that all strategies can easily tap into them
			// if using Forex securities
			if(tick.getSecurity().getSecurityType().equals(SecurityType.CASH))
				ForexConverter.addUSDVal(tick);
			
			SecurityKey securityKey = tick.getSecurity().getKey();
			if (null != tickSubscriberMap.get(securityKey)) {
				for (TickSubscriber tickSubscriber : tickSubscriberMap.get(securityKey)) {
					if(!fillCacheOnly)
						tickSubscriber.tickReceived(tick);
					else
						tickSubscriber.tickReceivedFillCacheOnly(tick);
				}
			}
		}
	}

	private boolean isValidTick(Tick tick) {
		if(tick.getSecurity().getSecurityType().equals(SecurityType.CASH))
			return isValidCashTick(tick);
		if(tick.getSecurity().getSecurityType().equals(SecurityType.STOCK))
			return isValidStockTick(tick);
		if(tick.getSecurity().getSecurityType().equals(SecurityType.FUTURE))
			return isValidFutureTick(tick);
		System.out.println("TICK IS NOT VALID. UPDATE TICK DISPATCHER: " + tick);
		return false;
	}

	private boolean isValidFutureTick(Tick tick) {
		// Dont allow ticks through that occur between 5:00pm and 6:00pm
		if(tick.getDateTime().getTime() % milliOneDay >= 75600000 
				&& tick.getDateTime().getTime() % milliOneDay <= 79200000)
			return false;

		// Dont allow ticks through that have a price of $0 or less
		if(tick.getLastPrice() <= 0)
			return false;
		return true;
	}

	private boolean isValidStockTick(Tick tick) {
		// Dont allow ticks through that occur between 5:00pm and 6:00pm
		if(tick.getDateTime().getTime() % milliOneDay <= 43200000 
				|| tick.getDateTime().getTime() % milliOneDay >= 75600000)
			return false;

		// Dont allow ticks through that have a price of $0 or less
		if(tick.getLastPrice() <= 0)
			return false;
		return true;
	}

	private boolean isValidCashTick(Tick tick) {
		// Dont allow ticks through that occur between 5:00pm and 5:15pm
		if(tick.getDateTime().getTime() % milliOneDay >= 75600000 
				&& tick.getDateTime().getTime() % milliOneDay <= 76500000)
			return false;

		// Dont allow ticks through that have a price of $0 or less
		if(tick.getLastPrice() <= 0)
			return false;
		return true;
	}
	
	public static void setFillTickCacheAndDontDispatch(boolean fillCacheOnly){
		TickDispatcher.fillCacheOnly = fillCacheOnly;
	}
}
