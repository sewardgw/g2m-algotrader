package com.g2m.strategies.examples;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.g2m.services.strategybuilder.HistoricalBarCacheLoader;
import com.g2m.services.strategybuilder.Strategy;
import com.g2m.services.strategybuilder.StrategyComponent;
import com.g2m.services.strategybuilder.enums.EntityPersistMode;
import com.g2m.services.tradingservices.ForexConverter;
import com.g2m.services.tradingservices.analytics.AggregateAnalytics;
import com.g2m.services.tradingservices.analytics.Analytics;
import com.g2m.services.tradingservices.backtest.BacktestAccount;
import com.g2m.services.tradingservices.backtest.BacktestTradingService;
import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Position;
import com.g2m.services.tradingservices.entities.Position.PositionBuilder;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityBuilder;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.orders.MarketOrder;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.entities.Tick;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.tradingservices.enums.OrderAction;
import com.g2m.services.tradingservices.enums.SecurityType;
import com.g2m.services.variables.entities.DayValues;
import com.g2m.services.variables.entities.Macd;
import com.g2m.services.variables.entities.MovingAverage;
import com.g2m.services.variables.entities.Rsi;
import com.g2m.services.variables.entities.TrendLine;



/**
 * Added 6/28/2015.
 * 
 * @author Grant Seward
 */

@StrategyComponent
public class MovingAverage_Example extends Strategy {
	
	// The ForexConverter is used to convert the value of forex
	// positions from the base currency into USD so that all
	// positions can be managed with a similar level of
	// risk from the perspective of USD at risk
	@Autowired
	static ForexConverter fx;

	// This flag is used to determine whether or not the test is being done
	// by an automated tuner
	static boolean automatedTuning;

	static int tickCnt;
	private List<BarSize> barSizeList = new ArrayList<BarSize>();  
	private List<String> fileNames = new ArrayList<String>();
	static Long startTime;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	SimpleDateFormat stf = new SimpleDateFormat("HH:mm:ss");

	HashMap<SecurityKey,List<MovingAverage>> ma30Sec9;
	HashMap<SecurityKey,List<MovingAverage>> ma30Sec20;
	HashMap<SecurityKey,List<MovingAverage>> ma30Sec50;
	HashMap<SecurityKey,List<MovingAverage>> ma30Sec200;

	// The number of variables to be returned within the variable list 
	// when calculating new variables
	static int numVars;

	// This HashMap keeps track of the multiplier that should be used
	// to multiply the baseShares by when opening a new position. As
	// a position is opened, if the previous position was a loss
	// then we will martingale the new positions.
	HashMap<SecurityKey, Integer> martingaleVals;

	// This HashMap stores the current stop loss for each security key
	HashMap<SecurityKey, Double> stopLosses;

	// This HashMap keeps track as to whether or not the ma9 has moved
	// far enough away from the ma20 in order to close the position once
	// the two MovingAverages cross again.
	HashMap<SecurityKey, Boolean> ma9ma20diff;

	// This HashMap keeps track as to whether or not the price has moved
	// far enough away from the ma9 in order to close the position once
	// the price crosses the MA 9 again.
	HashMap<SecurityKey, Boolean> ma9Pricediff;
	
	// This HashMap keep track as to whether or not the MA 50 has moved
	// far enough away form the MA 200 in order to close the position once 
	// the MA 9 crosses the MA 20 again
	HashMap<SecurityKey, Boolean> ma50ma200diff;

	static boolean allowTrades;
	// FOREX - 1 basis point = .0001
	// FOREX - pricing structure is the greater of +/- .2 basis points per trade or $2 
	static int baseShares;

	// The close threshold is used as a moving stop loss. If the price
	// in this strategy crosses the MA in the wrong direction AND it
	// continues some percentage (the close threshold) past the MA
	// then the position is closed.
	static double closeThreshold; //.001;

	// This threshold is is used in order to determine when to take profits.
	// If the MA9 moves some percentage away from the MA 20 then we will 
	// close the position once the MA9 crosses over the MA 20 in order
	// to try and maximize profits
	//	double ma9ma20Thresh = .0005;
	static double ma9ma20Thresh; //6.5E-4 // .0003  //.0005 // .00075;

	// This threshold is how far away the price must move in order to
	// close the position once the price crosses over the MA 9 again
	static double ma9PriceThresh;
	
	// This threshold is for how far away the MA 50 needs to move from the 
	// MA 200 in order to close the position once the MA 9 crosses the MA 20
	static double ma50ma200thresh;

	// When trying to open a position, the strategy will only open a new 
	// position if a trade did not occur in the recent N bars. This variable
	// sets the value for N.
	static int numBarsForLastTradeToOpenPosition; // 8 // 6 // 3

	// When a position is open, this strategy uses 2 distinct sets of logic
	// when determining to close a position. This variable sets the number 
	// of bars that must occur since the position was opened before transitioning
	// from the short-term logic (which is more conservative) to the long-term
	// logic (which is gives more bandwidth for longer runs)
	static int numBarsForLastTradeToClosePosition; // = 1; // 5 // 10

	// This window is used to set the time window (in number of bars)  for
	// retrieving the total number of MA crosses during that time period
	// before the values are pruned and no longer counted. A higher value
	// will return a higher count when getting all MA crosses within 
	// the recent window;
	int windowForMACrosses;

	// The starting balance of the account
	double startingBalance;

	// bar size, which helps calculate the duration between creating
	// the aggregate output.
	static BarSize analyticsBar = BarSize._1_HOUR;
	long lastAnalyticsTime;

	// The location of the properties file to use for this strategy
	static String propsFile;

	// The bar size for the strategy
	BarSize stratBarSize = BarSize._30_SECS;

	// These are the different reasons for closing a position. Theses
	// are only used for analytics
	double priceOppositeMa200;
	double priceMa9Diff;
	double priceLessThanMa9;
	double priceLessThanOpen;
	double Ma50ShortLoss;
	double Ma20ShortProfit;
	double targetReached;
	double ma9gtMa50;

	public static void main(String... args) {
		Strategy.initialize(MovingAverage_Example.class);
	}


	public void automateTuning(String propsFileLocation){
		automatedTuning = true;
		propsFile = propsFileLocation;
		Strategy.initialize(MovingAverage_Example.class);
	}

	@Override
	protected void run() {
        String usrHome = System.getProperty("user.home");
        //String propsLoc = usrHome + "/GitHub/g2m-algotrader/resources/properties/MovingAverage_Example.props";
		//setStrategyProperties(this);
		
		List<BarSize> allBarSizes = new ArrayList<BarSize>();
		allBarSizes.add(stratBarSize);
		
		String tickLocs =  usrHome + "/Github/g2m-algotrader/resources/ticks/";
		fileNames.add(tickLocs);

		registerSecuritiesFromFileBackTest(allBarSizes, fileNames);
		startTime = System.currentTimeMillis();

		// Create the HashMaps that are required for this strategy.
		stopLosses = new HashMap<SecurityKey, Double>();
		ma9ma20diff = new HashMap<SecurityKey, Boolean>();
		martingaleVals = new HashMap<SecurityKey, Integer>();
		ma9Pricediff = new HashMap<SecurityKey, Boolean>();
		ma50ma200diff = new HashMap<SecurityKey, Boolean>();
		
		// Basic pre-strategy setup of objects and values occurs here. In this
		// instance we're setting doing the following: 
		// 	- setting the number of bars for the count of MA crosses,
		// 	- setting initial number of losses to 0 (when > 2 we martingale)
		// 	- setting the flag to false that will be used to close positions when
		//		the MA 9 crosses the MA 20
		for (SecurityKey key : getSecurityKeys()){
			getBarTrader(key).setNumBarsForMACrosses(windowForMACrosses);
			martingaleVals.put(key, 0);
			ma9ma20diff.put(key, false);
			ma9Pricediff.put(key, false);
			ma50ma200diff.put(key, false);
		}

		ma30Sec9 = new HashMap<SecurityKey,List<MovingAverage>>();
		ma30Sec20 = new HashMap<SecurityKey,List<MovingAverage>>();
		ma30Sec50 = new HashMap<SecurityKey,List<MovingAverage>>();
		ma30Sec200 = new HashMap<SecurityKey,List<MovingAverage>>();

		setEntityPersistMode(EntityPersistMode.NONE);
		
		// Default is G2M but this can be changed if needed to change the base directory where to
		// set the output
		setOrgName("Whatever");
		
		// Set the minumum forex amount to 80% of the base equity value
		ForexConverter.setMinForexAmount( (int) Math.round(baseShares));
		startBacktest();
	}


	@Override
	protected void tickReceived(Tick tick) {


		// Only allow trades after a 6pm on Sundays but don't set this to true if it is after noon on Friday.
		if(!allowTrades 
				&& (tick.getDateTime().getTime() % (milliOneDay*7) > 352800000
						|| tick.getDateTime().getTime() % (milliOneDay*7) < 158380000)){
			allowTrades = true;
		}

		// Increase the tick for each tick that comes through. Used for monitoring and reporting.
		tickCnt ++;

		// Each tick that comes through updates the ForexConverter to make sure that 
		// when we place trades that we change the position size where necessary
		// to keep all strategies equal in terms of total wins / losses matching
		// total % change in price
		ForexConverter.addUSDVal(tick);

		// Only used to make sure that the data is actually flowing through. Occasionally
		// IB does not send data when restarting the application.
		if (tickCnt < 5){
			System.out.println(tick);
		} 

		// Set the starting balance relatively early on. This is only used for 
		// analysis and is not currently leveraged in the strategy..
		// ** getAvailableFunds is currently broken, not sure what IB API to use **
		else if (tickCnt == 6){
			startingBalance = getAccount().getAvailableFunds();
		}

		ma30Sec9.put(tick.getSecurity().getKey(), 
				getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), stratBarSize, 9),tick.getDateTime(),numVars));
		Rsi rsi = getVariables(Rsi.createParameters(tick.getSecurity().getKey(), BarSize._1_DAY, 3), tick.getDateTime());
		ma30Sec20.put(tick.getSecurity().getKey(), getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), stratBarSize, 20),tick.getDateTime(),numVars));
		ma30Sec50.put(tick.getSecurity().getKey(), getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), stratBarSize, 50),tick.getDateTime(),numVars));
		ma30Sec200.put(tick.getSecurity().getKey(), getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), stratBarSize, 200),tick.getDateTime(),numVars));
		
		if(rsi.getRsi() > 80)
			placeMarketOrder(tick, 10000, OrderAction.SELL);
		

		// Check to see if a long position exists. When evaluating each individual tick,
		// we'll only look at scenarios where at least N bars have passed since the
		// trade was opened
		if(		allowTrades
				&& longPositionExists(tick) 
				&& tick.getLastPrice() != 0.0 
				&& !ma30Sec200.get(tick.getSecurity().getKey()).isEmpty()
				&& !getBarTrader(tick.getSecurity().getKey())
				.tradePlacedInRecentBars(tick.getDateTime().getTime(), 
						numBarsForLastTradeToClosePosition, 
						stratBarSize)){

			// If the MA 9 becomes > X % from MA 20, then the position becomes eligible to 
			// be closed if the MA 9 crosses MA 20. We wait for the distance between the 
			// MA 9 and the MA 20 to grow so that we don't close the position too early, yet 
			// we still close the position before the momentum shifts too soon.
			double ma9 = ma30Sec9.get(tick.getSecurity().getKey()).get(numVars-1).getMovingAverage();
			double ma20 = ma30Sec20.get(tick.getSecurity().getKey()).get(numVars-1).getMovingAverage();
			double ma50 = ma30Sec50.get(tick.getSecurity().getKey()).get(numVars-1).getMovingAverage();
			double ma200 = ma30Sec200.get(tick.getSecurity().getKey()).get(numVars-1).getMovingAverage();
			
			
			if(ma50 > (ma200 + ma200 * ma50ma200thresh)
					&& getBarTrader(tick.getSecurity().getKey()).getPriorBar(stratBarSize).getClose() != 0.0
					&& ma9 != 0.0
					&& !ma50ma200diff.get(tick.getSecurity().getKey())){
				System.out.println("MA 9 - MA 20: " + ma9 + " - " + ma20);
				ma50ma200diff.put(tick.getSecurity().getKey(), true);
			}

			boolean ma50CrossoverCloseable = ma50ma200diff.get(tick.getSecurity().getKey());

			
			if(tick.getLastPrice() > (ma9 + ma9 * ma9PriceThresh) 
					&& ma9 != 0.0
					&& !ma9Pricediff.get(tick.getSecurity().getKey())){
				System.out.println("Price - MA 9: " + ma9 + " - " + ma20);
				ma9Pricediff.put(tick.getSecurity().getKey(), true);
			}

			boolean maCrossoverCloseable = ma9ma20diff.get(tick.getSecurity().getKey());
			
			
			// If the tick price is on the opposite side of the 200 Moving Average for
			// the given trade, then close the position (when evaluating for a LONG 
			// position we will close the position when the price is below the MA)
			if (tick.getLastPrice() < ma200 - (ma200 * closeThreshold)){
				priceOppositeMa200 ++;
				String reason = "CLOSING POSITION for PRICE BELOW 200 MA";
				updateMartingale(tick.getSecurity().getKey());
				closePosition(tick, reason);
			}

			// When the MA 50 has deviated far enough from the MA 200, close the position when the
			// MA 9 crosses the MA 20 again
			else if (ma50CrossoverCloseable && ma9 < ma20){
				ma9gtMa50 ++;
				String reason = "CLOSING POSITION for MA9 < MA20";
				updateMartingale(tick.getSecurity().getKey());
				closePosition(tick, reason);
			}
			
		}

		else if (allowTrades
				&& shortPositionExists(tick) 
				&& tick.getLastPrice() != 0.0 
				&& !ma30Sec200.get(tick.getSecurity().getKey()).isEmpty()
				&& !getBarTrader(tick.getSecurity().getKey())
				.tradePlacedInRecentBars(tick.getDateTime().getTime(), 
						numBarsForLastTradeToClosePosition, 
						stratBarSize)){

			// If the MA 9 becomes > X % from MA 20, then the position becomes eligible to 
			// be closed if the MA 9 crosses MA 20. We wait for the distance between the 
			// MA 9 and the MA 20 to grow so that we don't close the position too early, yet 
			// we still close the position before the momentum shifts too soon.
			double ma9 = ma30Sec9.get(tick.getSecurity().getKey()).get(numVars-1).getMovingAverage();
			double ma20 = ma30Sec20.get(tick.getSecurity().getKey()).get(numVars-1).getMovingAverage();
			double ma50 = ma30Sec50.get(tick.getSecurity().getKey()).get(numVars-1).getMovingAverage();
			double ma200 = ma30Sec200.get(tick.getSecurity().getKey()).get(numVars-1).getMovingAverage();
			
			if(ma50 < (ma200 - ma200 * ma50ma200thresh)
					&& getBarTrader(tick.getSecurity().getKey()).getPriorBar(stratBarSize).getClose() != 0.0
					&& tick.getLastPrice() != 0.0
					&& !ma50ma200diff.get(tick.getSecurity().getKey()))
				ma50ma200diff.put(tick.getSecurity().getKey(), true);

			boolean ma50CrossoverCloseable = ma50ma200diff.get(tick.getSecurity().getKey());

			
			if(tick.getLastPrice() > (ma9 + ma9 * ma9PriceThresh) 
					&& ma9 != 0.0
					&& !ma9Pricediff.get(tick.getSecurity().getKey())){
				System.out.println("Price - MA 9: " + ma9 + " - " + ma20);
				ma9Pricediff.put(tick.getSecurity().getKey(), true);
			}

			boolean maCrossoverCloseable = ma9ma20diff.get(tick.getSecurity().getKey());

			// If the tick price is on the opposite side of the 200 Moving Average for
			// the given trade, then close the position (when evaluating for a SHORT 
			// position we will close the position when the price is above the MA)
			if (tick.getLastPrice() > ma200 + (ma200 * closeThreshold)){
				priceOppositeMa200 ++;
				String reason = "CLOSING POSITION for PRICE ABOVE 200 MA";
				updateMartingale(tick.getSecurity().getKey());
				closePosition(tick, reason);
			}

			// When the MA 50 has deviated far enough from the MA 200, close the position when the
			// MA 9 crosses the MA 20
			else if (ma50CrossoverCloseable && ma9 > ma20){
				ma9gtMa50 ++;
				String reason = "CLOSING POSITION for MA9 > MA20";
				updateMartingale(tick.getSecurity().getKey());
				closePosition(tick, reason);				
			}
		}

		if(isEndOfWeek(tick)){
			if(allowTrades)
				closeAllPositions("END OF WEEK");
			allowTrades = false;
		}
	}


	@Override
	protected void barCreated(Bar bar) {
		Bar prevBar = getBarTrader(bar.getSecurity().getKey()).getPriorBar(bar.getBarSize());
		Bar prev2Bar = null; 
		List<Bar> barList = getBarCache().getMostRecentBars(bar.getSecurity().getKey(),stratBarSize, 3);
		if(!barList.isEmpty()){
			prev2Bar = barList.get(0);
		}
		// We check four items for this strategy:
		// 1) Make sure all of the variables are able to be used, since this strategy 
		// uses the MA 200 for the 30 second period We know that it will take the 
		// longest to fully populate and we Only need to check that one variable. 
		// 2) Check to make sure that there hasn't been a trade for that security recently
		// 3) Check to see if the price is crossing over the 200 MA often (we want seperation)
		// 4) A position is not already open
		if(		allowTrades
				&& bar.getBarSize().equals(stratBarSize) 
				&& !ma30Sec200.get(bar.getSecurity().getKey()).isEmpty()){
			
			double ma20 = ma30Sec20.get(bar.getSecurity().getKey()).get(numVars-1).getMovingAverage();
			double ma50 = ma30Sec50.get(bar.getSecurity().getKey()).get(numVars-1).getMovingAverage();
			double ma200 = ma30Sec200.get(bar.getSecurity().getKey()).get(numVars-1).getMovingAverage();
			double prevMa200 = ma30Sec200.get(bar.getSecurity().getKey()).get(numVars-2).getMovingAverage();
			
			// ------- GO LONG
			// If the price just moved over the 200 MA then we're eligible for a trade
			// There must not be an existing open position

			if(prevBar.getClose() < prevMa200 
					&& !positionOpen(bar)
					&& !getBarTrader(bar.getSecurity().getKey())
					.tradePlacedInRecentBars(bar.getDateTime().getTime(), 
							numBarsForLastTradeToOpenPosition, 
							bar.getBarSize())){	

				// if the price moved over the 200 MA then we'll update the # of recent crosses
				// This is used above in the final check. The thought here is that if
				// the price is just hovering around the 200 MA and just crossing it 
				// every few minutes without making any large deviations 
				// then we don't want to open up the position until we feel that there 
				// is a suffecient amount of followthrough that will come after
				getBarTrader(bar.getSecurity().getKey())
				.addToRecentMACrosses(bar.getBarSize(), bar.getDateTime().getTime());

				// If the number of recent crosses is low & the position is not already open
				// then we'll open a new position
				if(getBarTrader(bar.getSecurity().getKey())
						.getNumOfRecentMACrosses(stratBarSize, bar.getDateTime().getTime()) < 3){

					int quantityToOpen =  baseSharesMultiplier(bar.getSecurity()); 
					String reason = "OPENING POSITION LONG - martingale val " + martingaleVals.get(bar.getSecurity().getKey());
					placeMarketOrder(bar, quantityToOpen, OrderAction.BUY, reason);

					stopLosses.put(bar.getSecurity().getKey(), prevBar.getOpen());
					ma9ma20diff.put(bar.getSecurity().getKey(), false);
					ma9Pricediff.put(bar.getSecurity().getKey(), false);
					ma50ma200diff.put(bar.getSecurity().getKey(), false);
					
				}
			}

			// ------- GO SHORT
			// See rules for going long, these are all opposites
			else if (prevBar.getClose() > prevMa200
					&& !positionOpen(bar)
					&& !getBarTrader(bar.getSecurity().getKey())
					.tradePlacedInRecentBars(bar.getDateTime().getTime(), 
							numBarsForLastTradeToOpenPosition, 
							bar.getBarSize())){

				// if the price moved over the 200 MA then we'll update the # of recent crosses
				getBarTrader(bar.getSecurity().getKey())
				.addToRecentMACrosses(bar.getBarSize(), bar.getDateTime().getTime());

				// If the number of recent crosses is low & the position is not already open
				// then we'll open a new position
				if(getBarTrader(bar.getSecurity().getKey())
						.getNumOfRecentMACrosses(stratBarSize, bar.getDateTime().getTime()) < 3){

					int quantityToOpen =  baseSharesMultiplier(bar.getSecurity());
					String reason = "OPENING POSITION SHORT - martingale val " + martingaleVals.get(bar.getSecurity().getKey());
					placeMarketOrder(bar, quantityToOpen, OrderAction.SELL, reason);

					stopLosses.put(bar.getSecurity().getKey(), prevBar.getOpen());
					ma9ma20diff.put(bar.getSecurity().getKey(), false);
					ma9Pricediff.put(bar.getSecurity().getKey(), false);
					ma50ma200diff.put(bar.getSecurity().getKey(), false);
				}
			}

			// If a position is open, we'll look to close it
			else if (positionOpen(bar)){
				// Check to see if the bar closed on the opposite side of the 200 MA 
				if((prevBar.getClose() < prevMa200 && bar.getClose() > ma200)
						|| (prevBar.getClose() > prevMa200 && bar.getClose() < ma200)){

					// if the price moved over the 200 MA then we'll update the # of recent crosses
					getBarTrader(bar.getSecurity().getKey())
					.addToRecentMACrosses(bar.getBarSize(), bar.getDateTime().getTime());
				}

				// Check to see if we can close a long position with the following criteria:
				if(longPositionExists(bar)){

					// First check is if we're still within the initial trade window (i.e. 10 bars
					// since the trade was opened. If we we'll check for two possible ways to close:
					if (getBarTrader(bar.getSecurity().getKey())
							.tradePlacedInRecentBars(bar.getDateTime().getTime(), 
									numBarsForLastTradeToClosePosition, 
									stratBarSize)){

						// 1) If the trade is profitable AND if the bar closes on the opposite side of the
						// 20 period Moving Average
						if(getUnrealizedPercent(bar) > 0 && ma20 > bar.getClose()){
							Ma20ShortProfit ++;
							String reason = "CLOSING POSITION for PROFIT but WRONG SIDE OF 20MA"; 
							updateMartingale(bar.getSecurity().getKey());
							closePosition(bar, reason);

						}
						// 2) If the bar closes on the opposite side of the 50 period Moving Average
						else if(ma50 > bar.getClose()){
							Ma50ShortLoss ++;
							String reason = "CLOSING POSITION for BAR on WRONG SIDE OF 50MA"; 
							updateMartingale(bar.getSecurity().getKey());
							closePosition(bar, reason);
						}
					}
				}

				else if	(shortPositionExists(prevBar)){

					// First check is if we're still within the initial trade window (i.e. 10 bars
					// since the trade was opened. If we we'll check for two possible ways to close:					
					if (getBarTrader(bar.getSecurity().getKey())
							.tradePlacedInRecentBars(bar.getDateTime().getTime(), 
									numBarsForLastTradeToClosePosition, 
									stratBarSize)){

						// 1) If the trade is profitable AND if the bar closes on the opposite side of the
						// 20 period Moving Average
						if(getUnrealizedPercent(bar) > 0 && ma20 < bar.getClose()){
							Ma20ShortProfit ++;
							String reason = "CLOSING POSITION for PROFIT but WRONG SIDE OF 20MA"; 
							updateMartingale(bar.getSecurity().getKey());
							closePosition(bar, reason);
						}
						// 2) If the bar closes on the opposite side of the 50 period Moving Average
						else if(ma50 < bar.getClose()){
							Ma50ShortLoss ++;
							String reason = "CLOSING POSITION for BAR on WRONG SIDE OF 50MA";
							updateMartingale(bar.getSecurity().getKey());
							closePosition(bar, reason);
						}
					}
				}
			}
		}
		getBarTrader(bar.getSecurity().getKey()).saveAsPriorBar(bar.getBarSize(), bar);
	}

	protected void backtestComplete() {

		AggregateAnalytics aa = getAnalytics().getAggregateAnalytics();
		StringBuilder sb = new StringBuilder();
		sb.append("BACKTEST COMPLETED");
		sb.append(System.lineSeparator());
		sb.append(aa.getIndividualTradePrintOut());
		sb.append(System.lineSeparator());
		sb.append("*********************");
		sb.append(System.lineSeparator());
		sb.append(aa.getExtremeTradePrintOut());
		sb.append(System.lineSeparator());
		sb.append("*********************");
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Total Trades: " + aa.getTotalTrades() / 2);
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Average Trade Time: " + getTimeDifferenceString(aa.getAverageHoldTime()));
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Win Loss Count: " + aa.getWinLossCount());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Average WIN: " + aa.getAvgWin());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Average LOSS: " + aa.getAvgLoss());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - WIN : LOSS Profit Ratio: " + aa.getAvgWinToLossProfitRatio());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - WIN : LOSS Count Ratio: " + aa.getWinToLossCountRatio());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - WIN Percentage: " + aa.getWinPercentage());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Dollar of Profit Per Dollar of Loss: " + aa.getTotalDollarsOfProfitPerDollarOfLoss());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Total Profit (from Analytics): " + aa.getProfit());
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Total Profit (from Account):   " + (getAccount().getAvailableFunds() - startingBalance));
		sb.append(System.lineSeparator());
		sb.append("ANALYTICS - Total Comissions (from Analytics): " + aa.getTotalComissions());
		sb.append(System.lineSeparator());
		sb.append("ACCOUNT - Available Funds: " + getAccount().getAvailableFunds());
		sb.append(System.lineSeparator());
		sb.append("Tick Count: " + tickCnt);
		sb.append(System.lineSeparator());
		sb.append("*********************");
		sb.append(System.lineSeparator());
		sb.append("QUANTITY OPEN PER POSITION --- " );
		sb.append(System.lineSeparator());
		for(SecurityKey k : getAccount().getPositions().keySet())
			sb.append(getAccount().getPositions().get(k).getQuantity() + " - " + k + System.lineSeparator());
		sb.append(System.lineSeparator());
		sb.append("*********************");
		sb.append(System.lineSeparator());
		if(aa.getTotalTrades() > 1){
			sb.append("------CLOSE REASON % -------");
			sb.append(System.lineSeparator());
			sb.append("priceOppositeMa200: " + (Math.round(100*(priceOppositeMa200 /  (aa.getTotalTrades()/2))))+ "%");
			sb.append(System.lineSeparator());
			sb.append("ma9gtMa50: " + (Math.round(100*(ma9gtMa50 /  (aa.getTotalTrades()/2))))+ "%");
			sb.append(System.lineSeparator());
			sb.append("priceMa9Diff: " + (Math.round(100*(priceMa9Diff /  (aa.getTotalTrades()/2))))+ "%");
			sb.append(System.lineSeparator());
			sb.append("priceLessThanMa9: " + (Math.round(100*(priceLessThanMa9 /  (aa.getTotalTrades()/2))))+ "%");
			sb.append(System.lineSeparator());
			sb.append("priceLessThanOpen: " + (Math.round(100*(priceLessThanOpen /  (aa.getTotalTrades()/2))))+ "%");
			sb.append(System.lineSeparator());
			sb.append("targetReached: " + (Math.round(100*(targetReached /  (aa.getTotalTrades()/2)))) + "%");
			sb.append(System.lineSeparator());
			sb.append("Ma50ShortLoss: " + (Math.round(100*(Ma50ShortLoss /  (aa.getTotalTrades()/2))))+ "%");
			sb.append(System.lineSeparator());
			sb.append("Ma20ShortProfit: " + (Math.round(100*(Ma20ShortProfit /  (aa.getTotalTrades()/2)))) + "%");
			sb.append(System.lineSeparator());
			sb.append("*********************");
		}
		sb.append(System.lineSeparator());
		sb.append("-- PRINTING PROPERTIES --");
		sb.append(System.lineSeparator());
		Properties props = getCurrentProperties();
		for (Object obj : props.keySet()){
			sb.append(obj);
			sb.append("=");
			sb.append(props.getProperty((String) obj));
			sb.append(System.lineSeparator());
		}
		sb.append(System.lineSeparator());
		aa.createEquityGraph(this.getClass().getSimpleName());
		System.out.println(sb.toString());
		aa.writeAnalysisToFile(sb.toString());

	}

	private void updateMartingale(SecurityKey key) {
		if(tradeWasAWin(key)){
			martingaleVals.put(key, 0);
		} else{
			martingaleVals.put(key, martingaleVals.get(key) + 1);
		}
	}

	private int baseSharesMultiplier(Security security){
	
		SecurityKey key = security.getKey();
		int newShares = baseShares;  
		if(martingaleVals.get(key) < 2)
			newShares = baseShares;
		else if(martingaleVals.get(key) == 2)
			newShares = (int) Math.round(1.25 * baseShares); // 1.3 // 1.15
		else if(martingaleVals.get(key) == 3)
			newShares = (int) Math.round(1.5 * baseShares); // 1.75  // 1.3
		else if(martingaleVals.get(key) == 4)
			newShares = (int) Math.round(1.75 * baseShares); // 2.0 // 1.5
		else if(martingaleVals.get(key) == 5)
			newShares = (int) Math.round(2.0 * baseShares); // 3.0 //1.75
		else if (martingaleVals.get(key) > 5)
			newShares = (int) Math.round(2.5 * baseShares); // 4.0 //2
		return ForexConverter.convertPosition(security, newShares, getTickCache().getLastTickPrice(key));
	}


	public int getTickCnt() {
		return tickCnt;
	}

	public void setTickCnt(int tickCnt) {
		this.tickCnt = tickCnt;
	}

	public List<BarSize> getBarSizeList() {
		return barSizeList;
	}

	public void setBarSizeList(List<BarSize> barSizeList) {
		this.barSizeList = barSizeList;
	}

	public List<String> getFileNames() {
		return fileNames;
	}

	public void setFileNames(List<String> fileNames) {
		this.fileNames = fileNames;
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public SimpleDateFormat getSdf() {
		return sdf;
	}

	public void setSdf(SimpleDateFormat sdf) {
		this.sdf = sdf;
	}

	public HashMap<SecurityKey, List<MovingAverage>> getMa30Sec9() {
		return ma30Sec9;
	}

	public void setMa30Sec9(HashMap<SecurityKey, List<MovingAverage>> ma30Sec9) {
		this.ma30Sec9 = ma30Sec9;
	}

	public HashMap<SecurityKey, List<MovingAverage>> getMa30Sec20() {
		return ma30Sec20;
	}

	public void setMa30Sec20(HashMap<SecurityKey, List<MovingAverage>> ma30Sec20) {
		this.ma30Sec20 = ma30Sec20;
	}

	public HashMap<SecurityKey, List<MovingAverage>> getMa30Sec50() {
		return ma30Sec50;
	}

	public void setMa30Sec50(HashMap<SecurityKey, List<MovingAverage>> ma30Sec50) {
		this.ma30Sec50 = ma30Sec50;
	}

	public HashMap<SecurityKey, List<MovingAverage>> getMa30Sec200() {
		return ma30Sec200;
	}

	public void setMa30Sec200(HashMap<SecurityKey, List<MovingAverage>> ma30Sec200) {
		this.ma30Sec200 = ma30Sec200;
	}

	public int getNumVars() {
		return numVars;
	}

	public void setNumVars(int numVars) {
		this.numVars = numVars;
	}

	public HashMap<SecurityKey, Integer> getMartingaleVals() {
		return martingaleVals;
	}

	public void setMartingaleVals(HashMap<SecurityKey, Integer> martingaleVals) {
		this.martingaleVals = martingaleVals;
	}

	public HashMap<SecurityKey, Double> getStopLosses() {
		return stopLosses;
	}

	public void setStopLosses(HashMap<SecurityKey, Double> stopLosses) {
		this.stopLosses = stopLosses;
	}

	public HashMap<SecurityKey, Boolean> getMa9ma20diff() {
		return ma9ma20diff;
	}

	public void setMa9ma20diff(HashMap<SecurityKey, Boolean> ma9ma20diff) {
		this.ma9ma20diff = ma9ma20diff;
	}

	public boolean isAllowTrades() {
		return allowTrades;
	}

	public void setAllowTrades(boolean allowTrades) {
		this.allowTrades = allowTrades;
	}

	public int getBaseShares() {
		return baseShares;
	}

	public void setBaseShares(int baseShares) {
		this.baseShares = baseShares;
	}

	public double getCloseThreshold() {
		return closeThreshold;
	}

	public void setCloseThreshold(double closeThreshold) {
		this.closeThreshold = closeThreshold;
	}

	public double getMa9ma20Thresh() {
		return ma9ma20Thresh;
	}

	public void setMa9ma20Thresh(double ma9ma20Thresh) {
		this.ma9ma20Thresh = ma9ma20Thresh;
	}

	public int getNumBarsForLastTradeToOpenPosition() {
		return numBarsForLastTradeToOpenPosition;
	}

	public void setNumBarsForLastTradeToOpenPosition(
			int numBarsForLastTradeToOpenPosition) {
		this.numBarsForLastTradeToOpenPosition = numBarsForLastTradeToOpenPosition;
	}

	public int getNumBarsForLastTradeToClosePosition() {
		return numBarsForLastTradeToClosePosition;
	}

	public void setNumBarsForLastTradeToClosePosition(
			int numBarsForLastTradeToClosePosition) {
		this.numBarsForLastTradeToClosePosition = numBarsForLastTradeToClosePosition;
	}

	public double getStartingBalance() {
		return startingBalance;
	}

	public void setStartingBalance(double startingBalance) {
		this.startingBalance = startingBalance;
	}

	public long getLastAnalyticsTime() {
		return lastAnalyticsTime;
	}

	public void setLastAnalyticsTime(long lastAnalyticsTime) {
		this.lastAnalyticsTime = lastAnalyticsTime;
	}

	public static BarSize getAnalyticsBar() {
		return analyticsBar;
	}

	public static void setAnalyticsBar(BarSize analyticsBar) {
		MovingAverage_Example.analyticsBar = analyticsBar;
	}

	public static boolean isAutomatedTuning() {
		return automatedTuning;
	}

	public static void setAutomatedTuning(boolean automatedTuningEnabled) {
		automatedTuning = automatedTuningEnabled;
	}

	public int getWindowForMACrosses() {
		return windowForMACrosses;
	}

	public void setWindowForMACrosses(int windowForMACrosses) {
		this.windowForMACrosses = windowForMACrosses;
	}

	public static double getMa9PriceThresh() {
		return ma9PriceThresh;
	}

	public static void setMa9PriceThresh(double ma9PriceThresh) {
		MovingAverage_Example.ma9PriceThresh = ma9PriceThresh;
	}


	public static double getMa50ma200thresh() {
		return ma50ma200thresh;
	}


	public static void setMa50ma200thresh(double ma50ma200thresh) {
		MovingAverage_Example.ma50ma200thresh = ma50ma200thresh;
	}

}
