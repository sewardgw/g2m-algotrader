package com.g2m.services.tradingservices.analytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.jfree.ui.RefineryUtilities;

public class AnalyticsGraphs {

	public AnalyticsGraphs() {

	}

	public void createEquityGraph(List<PositionAnalytics> pas, String title, String location){
		ArrayList<IncrementalEquity> equityChanges = sortEquityChanges(pas);
		ChartWrapper cw = new ChartWrapper(title);
		cw.createChartFromEquityUpdates(equityChanges);
		cw.createChart();
		//cw.createChartPanel();
		//cw.pack();
		//cw.setVisible(true);
		cw.save(location);
	}

	private ArrayList<IncrementalEquity> sortEquityChanges(List<PositionAnalytics> pas) {
		HashMap<Long, IncrementalEquity> equityMap = new HashMap<Long, IncrementalEquity>();

		// Add all of the position analytics to a hashmap with the key being
		// the close time for the position. Merge multiple positions that closed
		// at the exact same time
		for(PositionAnalytics pa : pas){
			if(pa.getClosedDateTime() != null){
				IncrementalEquity ie = new IncrementalEquity(pa);
				if(!equityMap.containsKey(pa.getClosedDateTime().getTime()))
					equityMap.put(ie.closeTime, ie);
				else
					equityMap.get(ie.closeTime).mergeIncrementalEquities(ie);
			}
		}

		// Sort the close times
		List<Long> keys = new ArrayList<Long>(equityMap.keySet());
		Collections.sort(keys);

		ArrayList<IncrementalEquity> finalList = new ArrayList<IncrementalEquity>();
		for (long time : keys){
			finalList.add(equityMap.get(time));
		}

		return finalList;
	}

	public class IncrementalEquity{
		long closeTime;
		double realizedProfit;
		double comissionCosts;

		public IncrementalEquity(PositionAnalytics pa){
			closeTime = pa.getClosedDateTime().getTime();
			realizedProfit = pa.getRealizedProfit();
			comissionCosts = pa.getComissions();
		}

		public long getCloseTime(){
			return closeTime;
		}

		public double getRealizedProfit(){
			return realizedProfit;
		}

		public double getComissionCosts(){
			return comissionCosts;
		}

		public void mergeIncrementalEquities(IncrementalEquity ie){
			this.realizedProfit += ie.realizedProfit;
			this.comissionCosts += ie.comissionCosts;
		}
	}


}
