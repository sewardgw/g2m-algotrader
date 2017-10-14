package com.g2m.services.tradingservices.analytics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.enums.WinLoss;
import com.g2m.services.tradingservices.utilities.TimeFormatter;

/**
 * Added 6/20/2015.
 * 
 * @author michaelborromeo
 */
public class AggregateAnalytics {
	private double profit;
	private Map<WinLoss, Integer> winLossCount;
	private Map<Security, Double> securityProfit; 
	private double totalComissions;
	private int totalTrades;
	private static List<PositionAnalytics> positionalAnalyticsList;
	private static String url;
	private static String fileName;
	private PositionAnalytics largestWin;
	private PositionAnalytics largestLoss;
	private PositionAnalytics longestTrade;
	private PositionAnalytics shortestTrade;
	private double totalWins;
	private double totalLosses;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd'-'HH:mm:ss");
	private long totalPositionTime;

	private AggregateAnalytics() {
		winLossCount = new HashMap<WinLoss, Integer>();
		securityProfit = new HashMap<Security, Double>();
		resetWinLossCount();
		positionalAnalyticsList = new ArrayList<PositionAnalytics>();
	}

	private void resetWinLossCount() {
		winLossCount.put(WinLoss.WIN, 0);
		winLossCount.put(WinLoss.LOSS, 0);
		winLossCount.put(WinLoss.UNKNOWN, 0);
		winLossCount.put(WinLoss.NOPROFIT, 0);
	}

	private PositionAnalytics getLargestWin(){
		return largestWin;
	}

	private PositionAnalytics getLargestLoss(){
		return largestLoss;
	}

	private PositionAnalytics getLongestTrade(){
		return longestTrade;
	}

	private PositionAnalytics getShortestTrade(){
		return shortestTrade;
	}

	public double getProfit() {
		return profit;
	}

	public double getTotalWins(){
		return totalWins;
	}

	public double getTotalLosses(){
		return totalLosses;
	}

	public double getAvgWin(){
		return getTotalWins() / getWinLossCount().get(WinLoss.WIN);
	}

	public double getAvgLoss(){
		return getTotalLosses() / getWinLossCount().get(WinLoss.LOSS);
	}

	public double getAvgWinToLossProfitRatio(){
		return Math.abs(getAvgWin() / getAvgLoss());
	}

	public double getWinToLossCountRatio(){
		double win =  getWinLossCount().get(WinLoss.WIN);
		double loss = getWinLossCount().get(WinLoss.LOSS);
		return (win / loss);
	}

	public double getTotalDollarsOfProfitPerDollarOfLoss(){
		return getAvgWinToLossProfitRatio() * getWinToLossCountRatio();
	}

	public Map<WinLoss, Integer> getWinLossCount() {
		return winLossCount;
	}

	public double getTotalComissions() {
		return totalComissions;
	}

	public int getTotalTrades() {
		return totalTrades;
	}

	public long getTotalHoldTime(){
		return totalPositionTime;
	}

	public long getAverageHoldTime(){
		if (getTotalTrades() > 0)
			return Math.round(getTotalHoldTime() / getTotalTrades());
		return 0;
	}

	public double getWinPercentage(){
		return Math.round(100*((double)getWinLossCount().get(WinLoss.WIN)/ (double)(getTotalTrades() / 2)));
	}

	public String getIndividualTradePrintOut(){
		StringBuilder sb = new StringBuilder();
		String seperator = "----------------------------------";
		sb.append(System.lineSeparator());
		for(PositionAnalytics pa : positionalAnalyticsList){
			sb.append(seperator);
			sb.append(System.lineSeparator());
			sb.append(pa.getTradeOutput());
			sb.append(System.lineSeparator());
		}	
		sb.append(seperator);
		return sb.toString();
	}

	public void createEquityGraph(String title){
		if(!positionalAnalyticsList.isEmpty()){
			new Thread(new Runnable() {
				public void run(){
					AnalyticsGraphs ag = new AnalyticsGraphs();
					ag.createEquityGraph(positionalAnalyticsList, title, url);
				}
			}).start();
		}
	}

	public String getExtremeTradePrintOut(){
		if (getLargestWin() != null && getLargestLoss() != null 
				&& getLongestTrade() != null && getShortestTrade() != null){
			StringBuilder sb = new StringBuilder();
			sb.append("------------MAX TRADE PROFIT------------");
			sb.append(System.lineSeparator());
			sb.append(largestWin.getTradeOutput());
			sb.append(System.lineSeparator());
			sb.append("------------MAX TRADE LOSS------------");
			sb.append(System.lineSeparator());
			sb.append(largestLoss.getTradeOutput());
			sb.append(System.lineSeparator());
			sb.append("------------LONGEST TRADE------------");
			sb.append(System.lineSeparator());
			sb.append(longestTrade.getTradeOutput());
			sb.append(System.lineSeparator());
			sb.append("------------SHORTEST TRADE------------");
			sb.append(System.lineSeparator());
			sb.append(shortestTrade.getTradeOutput());
			sb.append(System.lineSeparator());
			sb.append("---------------------------------------");
			return sb.toString();
		} else {
			return "NO COMPLETED TRADES YET";
		}
		
	}
	
	public String getProfitBySecurity(){
		if(!securityProfit.keySet().isEmpty()){
			StringBuilder sb = new StringBuilder();
			for(Security sec : securityProfit.keySet()){
				sb.append(sec.getSymbol() + "." + sec.getCurrency() + ": ");
				sb.append(securityProfit.get(sec));
				sb.append(System.lineSeparator());
			}
			return sb.toString();
		}
		return null;
	}
	
	public String getAnalticsResults(){
		StringBuilder sb = new StringBuilder();
		sb.append("ANALYTICS - Total Trades: " + (getTotalTrades() / 2));
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Average Trade Time: " + TimeFormatter.getTimeDifferenceString(getAverageHoldTime()));
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Win Loss Count: " + getWinLossCount());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Average WIN: " + getAvgWin());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Average LOSS: " + getAvgLoss());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - WIN : LOSS Profit Ratio: " + getAvgWinToLossProfitRatio());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - WIN : LOSS Count Ratio: " + getWinToLossCountRatio());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - WIN Percentage: " + getWinPercentage());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Dollar of Profit Per Dollar of Loss: " + getTotalDollarsOfProfitPerDollarOfLoss());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Total Profit (from Analytics): " + getProfit());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Total Comissions (from Analytics): " + getTotalComissions());
		sb.append(System.lineSeparator());
		return sb.toString();
	}

	public static class AggregateAnalyticsBuilder {
		private AggregateAnalytics aggregateAnalytics;

		public AggregateAnalyticsBuilder() {
			aggregateAnalytics = new AggregateAnalytics();
		}

		public void updateFromPositionAnalytics(List<PositionAnalytics> positionAnalyticsList) {

			aggregateAnalytics.profit = 0;
			aggregateAnalytics.resetWinLossCount();
			aggregateAnalytics.totalComissions = 0;
			aggregateAnalytics.totalTrades = 0;
			aggregateAnalytics.largestLoss = null;
			aggregateAnalytics.largestWin = null;
			aggregateAnalytics.longestTrade = null;
			aggregateAnalytics.shortestTrade = null;
			aggregateAnalytics.totalComissions = 0;
			aggregateAnalytics.totalLosses = 0;
			aggregateAnalytics.totalWins = 0;
			aggregateAnalytics.totalPositionTime = 0;
			aggregateAnalytics.totalTrades = 0;


			positionalAnalyticsList.clear();
			for (PositionAnalytics positionAnalytics : positionAnalyticsList) {
				if (positionAnalytics.getOpenedDateTime() != null){
					positionalAnalyticsList.add(positionAnalytics);

					aggregateAnalytics.winLossCount.put(positionAnalytics.getWinLoss(),
							aggregateAnalytics.winLossCount.get(positionAnalytics.getWinLoss()) + 1);

					aggregateAnalytics.totalTrades += positionAnalytics.getTradeCount();

					if(positionAnalytics.getClosedDateTime() != null)
						aggregateAnalytics.totalPositionTime += 
						positionAnalytics.getClosedDateTime().getTime() - positionAnalytics.getOpenedDateTime().getTime();

					aggregateAnalytics.totalComissions += positionAnalytics.getComissions();

					// If the trade has been completed, we'll capture it's profit and use it
					// to set the extreme values
					if(!positionAnalytics.getWinLoss().equals(WinLoss.UNKNOWN)){
						aggregateAnalytics.profit += positionAnalytics.getRealizedProfit();
						// Here we set the largest win, largest loss, longest trade and shortest trade
						setExtremePositionAnalyticsValues(positionAnalytics);
						// Add the profit for each of the securities to be able to segregate them
						if(!aggregateAnalytics.securityProfit.containsKey(positionAnalytics.getSecurity()))
								aggregateAnalytics.securityProfit.put(positionAnalytics.getSecurity(), positionAnalytics.getRealizedProfit());
						else {
							double currVal = aggregateAnalytics.securityProfit.get(positionAnalytics.getSecurity());
							currVal += positionAnalytics.getRealizedProfit();
							aggregateAnalytics.securityProfit.put(positionAnalytics.getSecurity(), currVal);
						}
							
					}
					// We aggregate the avg win per win and the avg loss per loss
					if(positionAnalytics.getWinLoss() == WinLoss.WIN)
						aggregateAnalytics.totalWins += positionAnalytics.getRealizedProfit();
					else if (positionAnalytics.getWinLoss() == WinLoss.LOSS)
						aggregateAnalytics.totalLosses += positionAnalytics.getRealizedProfit();
				}
			} 
		}

		private void setExtremePositionAnalyticsValues(
				PositionAnalytics pa) {
			if(pa.getClosedDateTime() != null){
				if(aggregateAnalytics.getLargestWin() != null){
					if(pa.getRealizedProfit() > aggregateAnalytics.getLargestWin().getRealizedProfit())
						aggregateAnalytics.largestWin = pa;
				} else {
					aggregateAnalytics.largestWin = pa;
				}
				if(aggregateAnalytics.getLargestLoss() != null){
					if(pa.getRealizedProfit() < aggregateAnalytics.getLargestLoss().getRealizedProfit())
						aggregateAnalytics.largestLoss = pa;
				} else {
					aggregateAnalytics.largestLoss = pa;
				}
				if(aggregateAnalytics.getLongestTrade() != null){
					if(pa.getClosedDateTime().getTime() - pa.getOpenedDateTime().getTime() > 
					aggregateAnalytics.getLongestTrade().getClosedDateTime().getTime() -  
					aggregateAnalytics.getLongestTrade().getOpenedDateTime().getTime())
						aggregateAnalytics.longestTrade = pa;
				} else {
					aggregateAnalytics.longestTrade = pa;
				}
				if (aggregateAnalytics.getShortestTrade() != null){
					if(pa.getClosedDateTime().getTime() - pa.getOpenedDateTime().getTime() < 
							aggregateAnalytics.getShortestTrade().getClosedDateTime().getTime() -  
							aggregateAnalytics.getShortestTrade().getOpenedDateTime().getTime())
						aggregateAnalytics.shortestTrade = pa;
				} else {
					aggregateAnalytics.shortestTrade = pa;
				}
			}

		}

		public AggregateAnalytics build() {
			return aggregateAnalytics;
		}
	}

	public void writeAnalysisToFile(String s){

		new Thread(new Runnable() {
			public void run(){
				String newFileName = fileName + System.currentTimeMillis();
				File f = new File(url, newFileName);
				if(!f.exists()){
					try {
						f.createNewFile();

					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				try (Writer writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(url + newFileName, false),"utf-8"))) {
					writer.append(s);
					writer.close();

				} catch (IOException e) {
					e.printStackTrace();
				}	
			}
		}).start();


	}

	public void setOutputUrlAndFile(String s) {
		url = s;
		fileName = "G2M-ANALYTICS_OUTPUT";
	}
}
