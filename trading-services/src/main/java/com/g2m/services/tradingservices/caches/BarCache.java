package com.g2m.services.tradingservices.caches;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.enums.BarSize;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class BarCache {
	private int barGroupMaxSize = 500;
	private Map<String, BarGroup> barGroupMap;

	private BarCache() {
		barGroupMap = new HashMap<String, BarGroup>();
	}

	public int getBarGroupCount(SecurityKey securityKey, BarSize barSize) {
		String barHash = createBarHash(securityKey, barSize);
		if (!barGroupMap.containsKey(barHash)) {
			return 0;
		}
		return barGroupMap.get(barHash).getBarCount();
	}

	public List<Bar> getBars(SecurityKey securityKey, BarSize barSize, Date beforeDateTime, int count) {
		String barHash = createBarHash(securityKey, barSize);
		if (!barGroupMap.containsKey(barHash)) {
			return Collections.emptyList();
		}
		return barGroupMap.get(barHash).getBars(securityKey, barSize, beforeDateTime, count);
	}

	public List<Bar> getMostRecentBars(SecurityKey securityKey, BarSize barSize, int numBars) {
		String barHash = createBarHash(securityKey, barSize);
		if (!barGroupMap.containsKey(barHash)) {
			return Collections.emptyList();
		}
		return barGroupMap.get(barHash).getMostRecentNBars(securityKey, barSize, numBars);
	}

	public Bar getMostRecentBar(SecurityKey securityKey, BarSize barSize) {
		String barHash = createBarHash(securityKey, barSize);
		if (!barGroupMap.containsKey(barHash)) {
			return null;
		}
		return barGroupMap.get(barHash).getMostRecentNBars(securityKey, barSize, 1).get(0);
	}

	public Bar getSecondToLastBar(SecurityKey securityKey, BarSize barSize) {
		String barHash = createBarHash(securityKey, barSize);
		if (!barGroupMap.containsKey(barHash)) {
			return null;
		}
		
		List<Bar> bars = barGroupMap.get(barHash).getMostRecentNBars(securityKey, barSize, 2);
		if(bars.isEmpty())
			return null;
		
		return barGroupMap.get(barHash).getMostRecentNBars(securityKey, barSize, 2).get(1);
	}


	public void save(Bar bar) {
		String barHash = createBarHash(bar.getSecurity().getKey(), bar.getBarSize());
		if (!barGroupMap.containsKey(barHash)) {
			barGroupMap.put(barHash, new BarGroup());
		}
		barGroupMap.get(barHash).save(bar);
	}

	public void clear() {
		barGroupMap.clear();
	}

	private String createBarHash(SecurityKey securityKey, BarSize barSize) {
		StringBuilder builder = new StringBuilder();
		builder.append("securityKey=").append(securityKey.toString()).append("&");
		builder.append("barSize=").append(barSize.toString());
		return builder.toString();
	}

	public int getBarGroupMaxSize() {
		return barGroupMaxSize;
	}

	public void setBarGroupMaxSize(int valueListMaxSize) {
		this.barGroupMaxSize = valueListMaxSize;
	}

	private class BarGroup {
		private Map<Date, Bar> barsByDateTimeMap;
		private Queue<Date> dateTimeQueue;

		public BarGroup() {
			barsByDateTimeMap = new TreeMap<Date, Bar>(Collections.reverseOrder());
			dateTimeQueue = new LinkedList<Date>();
		}

		public int getBarCount() {
			return dateTimeQueue.size();
		}

		public List<Bar> getBars(SecurityKey securityKey, BarSize barSize, Date beforeDateTime, int count) {
			if (count > barsByDateTimeMap.size()) {
				return Collections.emptyList();
			}
			int remainingBars = barsByDateTimeMap.size();
			List<Bar> values = new ArrayList<Bar>();
			for (Date barDateTime : barsByDateTimeMap.keySet()) {
				if (count - values.size() > remainingBars) {
					return Collections.emptyList();
				}
				if (beforeDateTime.after(barDateTime) || beforeDateTime.equals(barDateTime)) {
					values.add(0, barsByDateTimeMap.get(barDateTime));
					if (count <= values.size()) {
						return values;
					}
				}
				remainingBars--;
			}
			return values;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public List<Bar> getMostRecentNBars(SecurityKey securityKey, BarSize barSize, int N) {

			List<Bar> values = new ArrayList<Bar>();
			Collection<Date> keyValueDates = barsByDateTimeMap.keySet();
			List<Date> sortedDates = new ArrayList(new TreeSet(keyValueDates)); 
			Collections.reverse(sortedDates);
			if(sortedDates.size() < N)
				return Collections.EMPTY_LIST;
			for (Date barDateTime : sortedDates) {			
				values.add(0, barsByDateTimeMap.get(barDateTime));
				if (N == values.size()) {
					return values;
				}
			}
			return values;
		}

		public void save(Bar bar) {
			barsByDateTimeMap.put(bar.getDateTime(), bar);
			dateTimeQueue.add(bar.getDateTime());
			pruneOldValues();
		}

		private void pruneOldValues() {
			while (barGroupMaxSize < dateTimeQueue.size()) {
				barsByDateTimeMap.remove(dateTimeQueue.poll());
			}
		}
	}
}
