package com.g2m.services.tradingservices.caches;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.Tick;

/**
 * Added 5/14/2015.
 * 
 * @author michaelborromeo
 */
@Component
public class TickCache {
	private Map<SecurityKey, LinkedList<Tick>> ticksMap;
	private int maxSize = 1000;

	public TickCache() {
		ticksMap = new HashMap<SecurityKey, LinkedList<Tick>>();
	}

	public void save(Tick tick) {
		if (!ticksMap.containsKey(tick.getSecurity().getKey())) {
			ticksMap.put(tick.getSecurity().getKey(), new LinkedList<Tick>());
		}
		ticksMap.get(tick.getSecurity().getKey()).add(tick);
		pruneOldValues(tick.getSecurity().getKey());
	}

	public List<Tick> getTicks(SecurityKey securityKey, int howManyTicks) {
		LinkedList<Tick> list = ticksMap.get(securityKey);
		if (1 > howManyTicks || list.size() < howManyTicks) {
			return Collections.emptyList();
		}
		return list.subList(list.size() - howManyTicks, list.size());
	}

	public int getTickCount(SecurityKey securityKey) {
		if (ticksMap.containsKey(securityKey)) {
			return ticksMap.get(securityKey).size();
		} else {
			return 0;
		}
	}

	public void clear() {
		ticksMap.clear();
	}

	public Tick getLastTick(SecurityKey securityKey){
		return getTicks(securityKey, 1).get(0);
	}
	
	public double getLastTickPrice(SecurityKey securityKey) {
		List<Tick> lastTicks = getTicks(securityKey, 1);
		if (lastTicks.isEmpty()) {
			return 0;
		}
		return lastTicks.get(0).getLastPrice();
	}

	private void pruneOldValues(SecurityKey securityKey) {
		LinkedList<Tick> ticks = ticksMap.get(securityKey);
		while (maxSize < ticks.size()) {
			ticks.remove();
		}
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}
}
