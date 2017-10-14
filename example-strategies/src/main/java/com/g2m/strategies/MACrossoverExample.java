package com.g2m.strategies.examples;

import java.util.ArrayList;
import java.util.List;

import com.g2m.services.strategybuilder.Strategy;
import com.g2m.services.strategybuilder.StrategyComponent;
import com.g2m.services.tradingservices.analytics.AggregateAnalytics;
import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Tick;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.tradingservices.enums.OrderAction;
import com.g2m.services.variables.entities.MovingAverage;

/**
 * Added 6/28/2015.
 * 
 * @author Grant Seward
 */
@StrategyComponent
public class MACrossoverExample extends Strategy {
	
	double prevMA20 = 0.0;
	double prevMA50 = 0.0;
	
	
	public static void main(String... args) {
		Strategy.initialize(MACrossoverExample.class);
		
	}

	@Override
	protected void run() {
		
		// Add the files we want to test with. Any number of files can be in the directory that
		// you specify 
		List<String> fileNames = new ArrayList<String>();
		String tickLocs = System.getProperty("user.home") + "/Github/g2m-algotrader/resources/ticks/";
		fileNames.add(tickLocs);
		
		// Add the bar sizes that should be used for this strategy.
		List<BarSize> barSizes = new ArrayList<BarSize>();
		barSizes.add(BarSize._1_MIN);
		
		// Register the files and the bar sizes
		registerSecuritiesFromFileBackTest(barSizes, fileNames);
		
		setOrgName("G2M");
		
		//Start the back test
		startBacktest();
	}

	@Override
	protected void tickReceived(Tick tick) {
	
	}

	@Override
	protected void barCreated(Bar bar) {
		MovingAverage ma20 = getVariables(
				MovingAverage.createParameters(bar.getSecurity().getKey(), BarSize._1_MIN, 20), 
				bar.getDateTime());
		MovingAverage ma50 = getVariables(
				MovingAverage.createParameters(bar.getSecurity().getKey(), BarSize._1_MIN, 50), 
				bar.getDateTime());
		
		double currentMA20 = ma20.getMovingAverage();
		double currentMA50 = ma50.getMovingAverage();
		
		// Check to see if the current moving average 20 is greater than the current moving average 50
		// and also make sure that the previous moving average 20 is less than the previous moving average 50
		// Final check to see if the previous moving average 20 does not equal 0.0 because we don't want
		// the first time that the moving average is created to trigger this rule.
		if(currentMA20 > currentMA50 && prevMA20 < prevMA50 && prevMA20 != 0.0){
			
			// This strategy will always have an open position. For the very first time that the
			// moving averages cross over, buy 100K, from then on out, we'll reverse the postion 
			// by closing the existing postion and opening a new position
			
			if(!positionOpen(bar)){
				placeMarketOrder(bar, 100000, OrderAction.BUY);
			}
			else if (positionOpen(bar)){
				closePosition(bar);
				placeMarketOrder(bar, 100000, OrderAction.BUY);
			}
				
		}
		
		// Opposite logic of above
		else if(currentMA20 < currentMA50 && prevMA20 > prevMA50 && prevMA20 != 0.0){
			if(!positionOpen(bar)){
				placeMarketOrder(bar, 100000, OrderAction.SELL);
			}
			else if (positionOpen(bar)){
				closePosition(bar);
				placeMarketOrder(bar, 100000, OrderAction.SELL);
			}
		}
		
		// Set the previous moving average equal to the current moving average. The next time
		// that a bar is created, the current moving average will be the previous moving average.
		prevMA20 = currentMA20;
		prevMA50 = currentMA50;
	}
	
	@Override
	protected void backtestComplete() {
		AggregateAnalytics aa = getAnalytics().getAggregateAnalytics();
		StringBuilder sb = new StringBuilder();
		sb.append(aa.getIndividualTradePrintOut());
		sb.append(System.lineSeparator());
		sb.append("*********************");
		sb.append(System.lineSeparator());
		sb.append(aa.getExtremeTradePrintOut());
		sb.append(System.lineSeparator());
		sb.append("*********************");
		sb.append(System.lineSeparator());
		sb.append(aa.getAnalticsResults());
		sb.append(getEquityAndCurrentProfit(startingBalance));
		System.out.println(sb.toString());
		aa.createEquityGraph(this.getClass().getSimpleName());
		aa.writeAnalysisToFile(sb.toString());

	
	}
}
