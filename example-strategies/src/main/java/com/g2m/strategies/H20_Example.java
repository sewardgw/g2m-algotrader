package com.g2m.strategies.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
import com.g2m.services.variables.models.h20.RF_Forex;


/**
 * Added 6/28/2015.
 * 
 * @author Grant Seward
 */
@StrategyComponent
public class H20_Example extends Strategy {

	//  --------- BACK TEST ONLY VARIABLES --------- 
	// The location of the properties file to use for this strategy
	static String propsFile;

	// This flag is used to determine whether or not the test is being done
	// by an automated tuner
	static boolean automatedTuning;

	// --------- END BACK TEST ONLY VARIABLES ---------
	// --------- BEGIN SHARED VARIABLES ---------

	// The ForexConverter is used to convert the value of forex
	// positions from the base currency into USD so that all
	// positions can be managed with a similar level of
	// risk from the perspective of USD at risk
	@Autowired
	ForexConverter fx;

	static int tickCnt;
	private List<BarSize> barSizeList = new ArrayList<BarSize>();  
	private List<String> fileNames = new ArrayList<String>();
	static Long startTime;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	// The number of variables to return as part of the moving average lists
	static int numVars = 10;

	static boolean allowTrades;
	static int baseShares;

	// The starting balance of the account
	double startingBalance;

	// The last time that the analytics output was created and the
	// bar size, which helps calculate the duration between creating
	// the aggregate output.
	static BarSize analyticsBar = BarSize._1_HOUR;
	long lastAnalyticsTime;


	/*
	 * ---------Strategy specific variables---------------------
	 */

	HashMap <SecurityKey, ArrayList<Double>> shortWindow;
	HashMap <SecurityKey, ArrayList<Double>> mediumWindow;
	HashMap <SecurityKey, ArrayList<Double>> longWindow;
	HashMap <SecurityKey, HashMap <BarSize, List<MovingAverage>>> mas;

	BarSize barSize1Min = BarSize._1_MIN;
	BarSize barSize30Sec = BarSize._30_SECS;
	BarSize barSize5Sec = BarSize._5_SECS;

	ArrayList<Integer> windows = new ArrayList<Integer>();
	static int w3 = 3;
	static int w5 = 5;
	static int w10 = 10;
	static int w20 = 20;
	static int w50 = 50;
	static int w100 = 100;
	static int largestWindow;

	// The records that are created but not ready to be written to a file because they do
	// not yet have the target variable assigned  
	HashMap <SecurityKey, StringBuilder> records;
	
	// This captures the records that are ready to be written to the file after the 
	// target variable has been assigned
	HashMap <SecurityKey, ArrayList<String>> outputRecords;

	// The scores that will be used to export for analysis. Not used in live scoring.
	ArrayList<Double> scores = new ArrayList<Double>();

	// Used to determine if the header has been written to the output file yet
	boolean headerExists = false;
	
	static String outputLocation; 

	public static void main(String... args) {
		Strategy.initialize(H20_Example.class);
	}


	public void automateTuning(String propsFileLocation){
		automatedTuning = true;
		propsFile = propsFileLocation;
		Strategy.initialize(H20_Example.class);
	}

	@Override
	protected void run() {
        String usrHome = System.getProperty("user.home");
        String propsLoc = usrHome + "/GitHub/g2m-algotrader/resources/properties/H20_Example.props";
        setStrategyProperties(this, propsLoc);

		// Create the records HashMap so that we can start to add values
		records = new HashMap<SecurityKey, StringBuilder>();
		outputRecords = new HashMap <SecurityKey,ArrayList<String>>();

		// Add the bar sizes for the strategy.
		List<BarSize> allBarSizes = new ArrayList<BarSize>();
		allBarSizes.add(BarSize._5_SECS); allBarSizes.add(BarSize._10_SECS); allBarSizes.add(BarSize._15_SECS);
		allBarSizes.add(BarSize._30_SECS);allBarSizes.add(barSize1Min);


		// Add the window sizes that will be used for the Strategy
		windows.add(w3);windows.add(w5);windows.add(w10);
		windows.add(w20);windows.add(w50);windows.add(w100);
		setLargestWindow(windows);
		
		// The location of all of the tick files that will be used in the back test.
		// TODO make this relative
		String tickLocs =  usrHome + "/Github/g2m-algotrader/resources/ticks/";
		fileNames.add(tickLocs);
		String outputLoc = usrHome + "/Github/g2m-algotrader/resources/output"; 
		outputLocation = outputLoc;
		
		registerSecuritiesFromFileBackTest(allBarSizes, fileNames);
		startTime = System.currentTimeMillis();

		setEntityPersistMode(EntityPersistMode.NONE);

		addMomentumTraders(windows);
		startBacktest();	
	}


	@Override
	protected void tickReceived(Tick tick) {

		// Increase the tick for each tick that comes through. Used for monitoring and reporting.
		tickCnt ++;

		// Each tick that comes through updates the ForexConverter to make sure that 
		// when we place trades that we change the position size where necessary
		// to keep all strategies equal in terms of total wins / losses matching
		// total % change in price
		fx.addUSDVal(tick);


		//System.out.println(tick.getVolume());

		// Set the starting balance relatively early on. This is only used for 
		// analysis and is not currently leveraged in the strategy..
		if (tickCnt == 1){
			startingBalance = getAccount().getAvailableFunds();
		}

		
		if(positionOpen(tick) && getUnrealizedPercent(tick) < -.000075)
			closePosition(tick, "closing position for stop loss");



		if(isEndOfWeek(tick)){
			if(allowTrades)
				closeAllPositions("END OF WEEK");
			allowTrades = false;
		}
	}


	@Override
	protected void barCreated(Bar bar) {

		getMomentumTrader(bar).updatePricePoints(bar);

		//System.out.println(bar.getBarSize());
		//System.out.println(numVars);
		
		if(movingAverageListFull(bar) && bar.getBarSize() == barSize1Min){

			double[] predictionRecord = new double[93];
			// Bar Size 1 min
			double barDirectionCount1min = getBarDirectionCount(bar, barSize1Min, 10);
			double lastPriceMove1min =  getPriceActionAsPercentOfVolatility(bar, barSize1Min, 10);
			double ma1min3bar5roc = getRateOfChange(bar, barSize1Min, 3, 5);
			double ma1min5bar5roc = getRateOfChange(bar, barSize1Min, 5, 5);
			double ma1min9bar5roc = getRateOfChange(bar, barSize1Min, 9, 5);
			double ma1min20bar5roc = getRateOfChange(bar, barSize1Min, 20, 5);
			double ma1min50bar5roc = getRateOfChange(bar, barSize1Min, 50, 5);
			double ma1min100bar5roc = getRateOfChange(bar, barSize1Min, 100, 5);
			double ma1min3ma5diff = getMAPercentDiff(bar, barSize1Min, 3, 5);
			double ma1min3ma5roc = getMAPercentDiffRoc(bar, barSize1Min, 3, 5, 2);
			double ma1min5ma9diff = getMAPercentDiff(bar, barSize1Min, 5, 9);
			double ma1min5ma9roc = getMAPercentDiffRoc(bar, barSize1Min, 5, 9, 2);
			double ma1min9ma20diff = getMAPercentDiff(bar, barSize1Min, 9, 20);
			double ma1min9ma20roc = getMAPercentDiffRoc(bar, barSize1Min, 9, 20, 2);
			double ma1min20ma50diff = getMAPercentDiff(bar, barSize1Min, 20, 50);
			double ma1min20ma50roc = getMAPercentDiffRoc(bar, barSize1Min, 20, 50, 2);
			double ma1minVolTo3BarAvgVol = getRatioToAverageVolume(bar, barSize1Min, 3);
			double ma1minVolTo5BarAvgVol = getRatioToAverageVolume(bar, barSize1Min, 5);
			double ma1minVolTo9BarAvgVol = getRatioToAverageVolume(bar, barSize1Min, 9);
			double ma1minVolTo20BarAvgVol = getRatioToAverageVolume(bar, barSize1Min, 20);
			double ma1minMacd = getMacdHistogram(bar, barSize1Min);
			double ma1minMacdTrendInd = getMacdHistogramTrendIndactor(bar, barSize1Min);
			double ma1minRsi9 = getRsi(bar, barSize1Min, 9);
			double ma1minRsi20 = getRsi(bar, barSize1Min, 20);
			double ma1minRsi50 = getRsi(bar, barSize1Min, 50);
			double ma1minRsi9TrendInd = getRsiTrendIndicator(bar, barSize1Min, 9);
			double ma1minRsi20TrendInd = getRsiTrendIndicator(bar, barSize1Min, 20);
			double ma1minRsi50TrendInd = getRsiTrendIndicator(bar, barSize1Min, 50);
			double ma1min5BarTrendInd = getMaTrendIndicator(bar, barSize1Min, 5);
			double ma1min9BarTrendInd = getMaTrendIndicator(bar, barSize1Min, 9);
			double ma1min20BarTrendInd = getMaTrendIndicator(bar, barSize1Min, 9);

			predictionRecord[0] = barDirectionCount1min;;
			predictionRecord[1] = lastPriceMove1min;; 
			predictionRecord[2] = ma1min3bar5roc;
			predictionRecord[3] = ma1min5bar5roc;
			predictionRecord[4] = ma1min9bar5roc;
			predictionRecord[5] = ma1min20bar5roc;
			predictionRecord[6] = ma1min50bar5roc;
			predictionRecord[7] = ma1min100bar5roc;
			predictionRecord[8] = ma1min3ma5diff;
			predictionRecord[9] = ma1min3ma5roc;
			predictionRecord[10] = ma1min5ma9diff;
			predictionRecord[11] = ma1min5ma9roc;
			predictionRecord[12] = ma1min9ma20diff;
			predictionRecord[13] = ma1min9ma20roc;
			predictionRecord[14] = ma1min20ma50diff;
			predictionRecord[15] = ma1min20ma50roc;
			predictionRecord[16] = ma1minVolTo3BarAvgVol;
			predictionRecord[17] = ma1minVolTo5BarAvgVol;
			predictionRecord[18] = ma1minVolTo9BarAvgVol;
			predictionRecord[19] = ma1minVolTo20BarAvgVol;
			predictionRecord[20] = ma1minMacd;
			predictionRecord[21] = ma1minMacdTrendInd;
			predictionRecord[22] = ma1minRsi9;
			predictionRecord[23] = ma1minRsi20;
			predictionRecord[24] = ma1minRsi50;
			predictionRecord[25] = ma1minRsi9TrendInd;
			predictionRecord[26] = ma1minRsi20TrendInd;
			predictionRecord[27] = ma1minRsi50TrendInd;
			predictionRecord[28] = ma1min5BarTrendInd;
			predictionRecord[29] = ma1min9BarTrendInd;
			predictionRecord[30] = ma1min20BarTrendInd;


			// Bar Size 30 seconds
			double barDirectionCount30sec = getBarDirectionCount(bar, barSize30Sec, 10);
			double lastPriceMove30sec =  getPriceActionAsPercentOfVolatility(bar, barSize30Sec, 10);
			double ma30sec3bar5roc = getRateOfChange(bar, barSize30Sec, 3, 5);
			double ma30sec5bar5roc = getRateOfChange(bar, barSize30Sec, 5, 5);
			double ma30sec9bar5roc = getRateOfChange(bar, barSize30Sec, 9, 5);
			double ma30sec20bar5roc = getRateOfChange(bar, barSize30Sec, 20, 5);
			double ma30sec50bar5roc = getRateOfChange(bar, barSize30Sec, 50, 5);
			double ma30sec100bar5roc = getRateOfChange(bar, barSize30Sec, 100, 5);
			double ma30sec3ma5diff = getMAPercentDiff(bar, barSize30Sec, 3, 5);
			double ma30sec3ma5roc = getMAPercentDiffRoc(bar, barSize30Sec, 3, 5, 2);
			double ma30sec5ma9diff = getMAPercentDiff(bar, barSize30Sec, 5, 9);
			double ma30sec5ma9roc = getMAPercentDiffRoc(bar, barSize30Sec, 5, 9, 2);
			double ma30sec9ma20diff = getMAPercentDiff(bar, barSize30Sec, 9, 20);
			double ma30sec9ma20roc = getMAPercentDiffRoc(bar, barSize30Sec, 9, 20, 2);
			double ma30sec20ma50diff = getMAPercentDiff(bar, barSize30Sec, 20, 50);
			double ma30sec20ma50roc = getMAPercentDiffRoc(bar, barSize30Sec, 20, 50, 2);
			double ma30secVolTo3BarAvgVol = getRatioToAverageVolume(bar, barSize30Sec, 3);
			double ma30secVolTo5BarAvgVol = getRatioToAverageVolume(bar, barSize30Sec, 5);
			double ma30secVolTo9BarAvgVol = getRatioToAverageVolume(bar, barSize30Sec, 9);
			double ma30secVolTo20BarAvgVol = getRatioToAverageVolume(bar, barSize30Sec, 20);
			double ma30secMacd = getMacdHistogram(bar, barSize30Sec);
			double ma30secMacdTrendInd = getMacdHistogramTrendIndactor(bar, barSize30Sec);
			double ma30secRsi9 = getRsi(bar, barSize30Sec, 9);
			double ma30secRsi20 = getRsi(bar, barSize30Sec, 20);
			double ma30secRsi50 = getRsi(bar, barSize30Sec, 50);
			double ma30secRsi9TrendInd = getRsiTrendIndicator(bar, barSize30Sec, 9);
			double ma30secRsi20TrendInd = getRsiTrendIndicator(bar, barSize30Sec, 20);
			double ma30secRsi50TrendInd = getRsiTrendIndicator(bar, barSize30Sec, 50);
			double ma30sec5BarTrendInd = getMaTrendIndicator(bar, barSize30Sec, 5);
			double ma30sec9BarTrendInd = getMaTrendIndicator(bar, barSize30Sec, 9);
			double ma30sec20BarTrendInd = getMaTrendIndicator(bar, barSize30Sec, 9);

			predictionRecord[31] = barDirectionCount30sec;;
			predictionRecord[32] = lastPriceMove30sec;; 
			predictionRecord[33] = ma30sec3bar5roc;
			predictionRecord[34] = ma30sec5bar5roc;
			predictionRecord[35] = ma30sec9bar5roc;
			predictionRecord[36] = ma30sec20bar5roc;
			predictionRecord[37] = ma30sec50bar5roc;
			predictionRecord[38] = ma30sec100bar5roc;
			predictionRecord[39] = ma30sec3ma5diff;
			predictionRecord[40] = ma30sec3ma5roc;
			predictionRecord[41] = ma30sec5ma9diff;
			predictionRecord[42] = ma30sec5ma9roc;
			predictionRecord[43] = ma30sec9ma20diff;
			predictionRecord[44] = ma30sec9ma20roc;
			predictionRecord[45] = ma30sec20ma50diff;
			predictionRecord[46] = ma30sec20ma50roc;
			predictionRecord[47] = ma30secVolTo3BarAvgVol;
			predictionRecord[48] = ma30secVolTo5BarAvgVol;
			predictionRecord[49] = ma30secVolTo9BarAvgVol;
			predictionRecord[50] = ma30secVolTo20BarAvgVol;
			predictionRecord[51] = ma30secMacd;
			predictionRecord[52] = ma30secMacdTrendInd;
			predictionRecord[53] = ma30secRsi9;
			predictionRecord[54] = ma30secRsi20;
			predictionRecord[55] = ma30secRsi50;
			predictionRecord[56] = ma30secRsi9TrendInd;
			predictionRecord[57] = ma30secRsi20TrendInd;
			predictionRecord[58] = ma30secRsi50TrendInd;
			predictionRecord[59] = ma30sec5BarTrendInd;
			predictionRecord[60] = ma30sec9BarTrendInd;
			predictionRecord[61] = ma30sec20BarTrendInd;


			// Bar Size 5 seconds
			double barDirectionCount5sec = getBarDirectionCount(bar, barSize5Sec, 10);
			double lastPriceMove5sec =  getPriceActionAsPercentOfVolatility(bar, barSize5Sec, 10);
			double ma5sec3bar5roc = getRateOfChange(bar, barSize5Sec, 3, 5);
			double ma5sec5bar5roc = getRateOfChange(bar, barSize5Sec, 5, 5);
			double ma5sec9bar5roc = getRateOfChange(bar, barSize5Sec, 9, 5);
			double ma5sec20bar5roc = getRateOfChange(bar, barSize5Sec, 20, 5);
			double ma5sec50bar5roc = getRateOfChange(bar, barSize5Sec, 50, 5);
			double ma5sec100bar5roc = getRateOfChange(bar, barSize5Sec, 100, 5);
			double ma5sec3ma5diff = getMAPercentDiff(bar, barSize5Sec, 3, 5);
			double ma5sec3ma5roc = getMAPercentDiffRoc(bar, barSize5Sec, 3, 5, 2);
			double ma5sec5ma9diff = getMAPercentDiff(bar, barSize5Sec, 5, 9);
			double ma5sec5ma9roc = getMAPercentDiffRoc(bar, barSize5Sec, 5, 9, 2);
			double ma5sec9ma20diff = getMAPercentDiff(bar, barSize5Sec, 9, 20);
			double ma5sec9ma20roc = getMAPercentDiffRoc(bar, barSize5Sec, 9, 20, 2);
			double ma5sec20ma50diff = getMAPercentDiff(bar, barSize5Sec, 20, 50);
			double ma5sec20ma50roc = getMAPercentDiffRoc(bar, barSize5Sec, 20, 50, 2);
			double ma5secVolTo3BarAvgVol = getRatioToAverageVolume(bar, barSize5Sec, 3);
			double ma5secVolTo5BarAvgVol = getRatioToAverageVolume(bar, barSize5Sec, 5);
			double ma5secVolTo9BarAvgVol = getRatioToAverageVolume(bar, barSize5Sec, 9);
			double ma5secVolTo20BarAvgVol = getRatioToAverageVolume(bar, barSize5Sec, 20);
			double ma5secMacd = getMacdHistogram(bar, barSize5Sec);
			double ma5secMacdTrendInd = getMacdHistogramTrendIndactor(bar, barSize5Sec);
			double ma5secRsi9 = getRsi(bar, barSize5Sec, 9);
			double ma5secRsi20 = getRsi(bar, barSize5Sec, 20);
			double ma5secRsi50 = getRsi(bar, barSize5Sec, 50);
			double ma5secRsi9TrendInd = getRsiTrendIndicator(bar, barSize5Sec, 9);
			double ma5secRsi20TrendInd = getRsiTrendIndicator(bar, barSize5Sec, 20);
			double ma5secRsi50TrendInd = getRsiTrendIndicator(bar, barSize5Sec, 50);
			double ma5sec5BarTrendInd = getMaTrendIndicator(bar, barSize5Sec, 5);
			double ma5sec9BarTrendInd = getMaTrendIndicator(bar, barSize5Sec, 9);
			double ma5sec20BarTrendInd = getMaTrendIndicator(bar, barSize5Sec, 9);

			predictionRecord[62] = barDirectionCount5sec;;
			predictionRecord[63] = lastPriceMove5sec;; 
			predictionRecord[64] = ma5sec3bar5roc;
			predictionRecord[65] = ma5sec5bar5roc;
			predictionRecord[66] = ma5sec9bar5roc;
			predictionRecord[67] = ma5sec20bar5roc;
			predictionRecord[68] = ma5sec50bar5roc;
			predictionRecord[69] = ma5sec100bar5roc;
			predictionRecord[70] = ma5sec3ma5diff;
			predictionRecord[71] = ma5sec3ma5roc;
			predictionRecord[72] = ma5sec5ma9diff;
			predictionRecord[73] = ma5sec5ma9roc;
			predictionRecord[74] = ma5sec9ma20diff;
			predictionRecord[75] = ma5sec9ma20roc;
			predictionRecord[76] = ma5sec20ma50diff;
			predictionRecord[77] = ma5sec20ma50roc;
			predictionRecord[78] = ma5secVolTo3BarAvgVol;
			predictionRecord[79] = ma5secVolTo5BarAvgVol;
			predictionRecord[80] = ma5secVolTo9BarAvgVol;
			predictionRecord[81] = ma5secVolTo20BarAvgVol;
			predictionRecord[82] = ma5secMacd;
			predictionRecord[83] = ma5secMacdTrendInd;
			predictionRecord[84] = ma5secRsi9;
			predictionRecord[85] = ma5secRsi20;
			predictionRecord[86] = ma5secRsi50;
			predictionRecord[87] = ma5secRsi9TrendInd;
			predictionRecord[88] = ma5secRsi20TrendInd;
			predictionRecord[89] = ma5secRsi50TrendInd;
			predictionRecord[90] = ma5sec5BarTrendInd;
			predictionRecord[91] = ma5sec9BarTrendInd;
			predictionRecord[92] = ma5sec20BarTrendInd;

			/*
			 * ------------------------------------
			 * IF TRYING TO CREATE A TEST FILE FOR ANALYSIS, DO THESE ACTIONS
			 * ------------------------------------
			 */
			
			//StringBuilder sb = getStringFromArray(predictionRecord);
			//updateRecordsWithTargetVariable(sb, bar);
			
			/*
			 * ------------------------------------
			 * IF TRYING TO COMPLETE A BACK TEST, DO THESE ACTIONS
			 * ------------------------------------
			 */
			double score = getModelScore(predictionRecord);
			System.out.println("Model output score: " + String.valueOf(score));
			openPosition(score, bar);
		}		
	}

	private void openPosition(double score, Bar bar) {
		int numBarsForLastTradeToOpenPosition = 10;
		int quantityToOpen = 100000;
		
		// Open positions if these thresholds are crossed
		double longScore = .000015;
		double shortScore = -.00003;

		if(!positionOpen(bar) && score > longScore
				&& !getBarTrader(bar.getSecurity().getKey())
					.tradePlacedInRecentBars(bar.getDateTime().getTime(), 
											 numBarsForLastTradeToOpenPosition, 
											 bar.getBarSize())){
			placeMarketOrder(bar, quantityToOpen, OrderAction.BUY, "OPENING LONG POSITION ");
		}
		else if (!positionOpen(bar) && score < -.0000275
				&& !getBarTrader(bar.getSecurity().getKey())
					.tradePlacedInRecentBars(bar.getDateTime().getTime(), 
											 numBarsForLastTradeToOpenPosition, 
											 bar.getBarSize())){
			placeMarketOrder(bar, quantityToOpen, OrderAction.SELL, "OPENING SHORT POSITION ");
		}
		else if (positionOpen(bar)){

				quantityToOpen = getAccount().getPositions().get(bar.getSecurity().getKey()).getQuantity();
				closePosition(bar, "closing position @ end of minute");
		}

	}


	private double getModelScore(double[] data) {
		RF_Forex model = new RF_Forex();
		double[] preds = new double[model.getPredsSize()];

		preds = model.score0(data, preds);

		return preds[0];
	}


	private StringBuilder getStringFromArray(double[] array) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; i++){
			sb.append(array[i]);
			sb.append("|");
		}
		return sb;
	}


	private double[] getData(StringBuilder sb) {
		String[] parsed = sb.toString().split("|");

		double data[] = new double[parsed.length-1];

		for(int i = 0; i < parsed.length - 2; i++){
			try{
				data[i] = Double.parseDouble(parsed[i]);
			} catch (NumberFormatException e) {
				if (parsed[i].equals("A"))
					data[i] = 0.0;
				else if (parsed[i].equals("B"))
					data[i] = 1.0;
				else if (parsed[i].equals("C"))
					data[i] = 2.0;
				else if (parsed[i].equals("D"))
					data[i] = 3.0;
				else if (parsed[i].equals("E"))
					data[i] = 4.0;
			}	
		}
		return data;
	}


	private void updateRecordsWithTargetVariable(StringBuilder sb, Bar bar) {
		double target;

		if(records.containsKey(bar.getSecurity().getKey())){
			List<Bar> bars = getBarCache().getMostRecentBars(bar.getSecurity().getKey(), bar.getBarSize(), 2);
			target = (bar.getClose() - bars.get(0).getClose()) / bars.get(0).getClose(); 

			// Get the previous record for the security key, we'll add the target value, 
			// which is the % change in price of the current bar, to the existing record
			StringBuilder finalRecord = records.get(bar.getSecurity().getKey());
			finalRecord.append(target);
			finalRecord.append(System.lineSeparator());
			//System.out.println(records.get(bar.getSecurity().getKey()).toString());
			addToQueueForFile(finalRecord.toString(), bar.getSecurity().getKey(), target);
		}
		records.put(bar.getSecurity().getKey(), sb);
	}

	private void addToQueueForFile(String string, SecurityKey key, double target) {

		if(!outputRecords.containsKey(key)){
			outputRecords.put(key, new ArrayList<String>());
			outputRecords.get(key).add(string);
		} else
			outputRecords.get(key).add(string);


		if(outputRecords.get(key).size() > 2000){
			System.out.println("TRYING TO WRITE TO FILE");
			writeToFile(outputRecords.get(key));
			outputRecords.get(key).clear();
		}

	}


	private void clearRecordQueue() {
		for (SecurityKey key : outputRecords.keySet()){
			writeToFile(outputRecords.get(key));

		}
	}

	private void writeToFile(ArrayList<String> records) {

		System.out.println("Adding to file");
		String newFileName = outputLocation + startTime;
		File f = new File(newFileName);
		if(!f.exists()){
			try {
				f.createNewFile();

				Writer writer = new BufferedWriter(
						new OutputStreamWriter(
								new FileOutputStream(newFileName, true),"utf-8"));

				if(!headerExists){
					writer.append(getHeader());
					headerExists = true;
				}

				for(String s : records)
					writer.append(s);
				writer.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}


	private double getTrendIndicatorValue(double val1, double val2, double val3){

		// The type indicator indicates what type of movement is occurring:
		//	1/A = Trend is increasing at an increasing rate (or trend switched from decreasing to increasing)
		// 	2/B = Trend is increasing at a decreasing rate
		// 	3/C = Trend is decreasing at an increasing rate (or trend switched from increasing to decreasing)
		//	4/D = Trend is decreasing at a decreasing rate 

		// This takes the difference b/w the 2nd and 1st values, an increase from val1
		// to val2 will cause this to be positive, same applies for the value below it. 
		double val2Val1Diff = val2 - val1;
		double val3Val2Diff = val3 - val2;
		if(val2Val1Diff > 0){

			if(val3Val2Diff > 0 && val3Val2Diff > val2Val1Diff)
				return 0.0; // A
			else if(val3Val2Diff > 0 && val3Val2Diff < val2Val1Diff)
				return 1.0; // B
			else if (val3Val2Diff <= 0)
				return 2.0; // C

		} else if (val2Val1Diff < 0){

			if(val3Val2Diff < 0 && val3Val2Diff < val2Val1Diff)
				return 2.0; // C
			else if(val3Val2Diff < 0 && val3Val2Diff > val2Val1Diff)
				return 3.0; // D
			else if (val3Val2Diff >= 0)
				return 0.0; //A

		} else if (val2Val1Diff == 0){

			if(val3Val2Diff > 0)
				return 0.0; // A
			else if(val3Val2Diff < 0)
				return 2.0; // C
		}

		return 4.0; // E
	}

	private double getMaTrendIndicator(Bar bar, BarSize barSize, int windowSize) {
		// TODO Auto-generated method stub

		// The list of moving averages used to compute the trend indicator. 
		// Always pull exactly the 3 most recent MAs to compute this indicator
		List<MovingAverage> mas = getVariables(
				MovingAverage.createParameters(bar.getSecurity().getKey(), 
						barSize, windowSize),
						bar.getDateTime(),
						3);
		return getTrendIndicatorValue(mas.get(0).getMovingAverage(), 
				mas.get(1).getMovingAverage(), mas.get(2).getMovingAverage());
	}


	private double getRsi(Bar bar, BarSize barSize, int period) {
		Rsi r = getVariables(Rsi.createParameters(bar.getSecurity().getKey(), barSize, period), bar.getDateTime());
		return r.getRsi();
	}

	private double getRsiTrendIndicator(Bar bar, BarSize barSize, int period) {
		List<Rsi> r = getVariables(Rsi.createParameters(bar.getSecurity().getKey(), barSize, period), bar.getDateTime(), 3);
		return getTrendIndicatorValue(r.get(0).getRsi(), r.get(1).getRsi(), r.get(2).getRsi());
	}


	private double getMacdHistogram(Bar bar, BarSize barSize) {
		Macd m = getVariables(Macd.createParameters(bar.getSecurity().getKey(), barSize), bar.getDateTime());
		return m.getHistogram();
	}


	private double getMacdHistogramTrendIndactor(Bar bar, BarSize barSize) {
		List<Macd> m = getVariables(Macd.createParameters(bar.getSecurity().getKey(), barSize), bar.getDateTime(), 3);
		return getTrendIndicatorValue(m.get(0).getHistogram(), m.get(1).getHistogram(), m.get(2).getHistogram());
	}


	// TODO create a version of this function that returns an array of values so that 
	// TODO we only have to call the Bar Cache once
	// This function provides the ratio of the current bar's volume to the average volume
	// of the last N bars
	private double getRatioToAverageVolume(Bar bar, BarSize barSize, int numBars) {
		int totalVolume = 0;
		for(Bar b : getBarCache().getMostRecentBars(bar.getSecurity().getKey(), barSize, numBars)){
			totalVolume += b.getVolume();
		}

		if(totalVolume <= 1)
			return 0.0;
		return bar.getVolume() / (totalVolume / numBars);
	}


	private double getBarDirectionCount(Bar bar, BarSize barSize, int numBars) {
		List<Bar> bars = getBarCache().getMostRecentBars(bar.getSecurity().getKey(), barSize, numBars);
		double consecCnt = 0.0;
		for (int i = bars.size()-1; i >= 0; i--){
			if(bars.get(i).getOpen() - bars.get(i).getClose() < 0)
				if(consecCnt < 0)
					return consecCnt;
				else
					consecCnt ++;
			else if (bars.get(i).getOpen() - bars.get(i).getClose() > 0)
				if(consecCnt > 0)
					return consecCnt;
				else
					consecCnt --;
		}
		return consecCnt;
	}


	private double getPriceActionAsPercentOfVolatility(Bar bar, BarSize barSize, int numBars) {
		return Math.abs((bar.getHigh() - bar.getLow())) / 
				getMomentumTrader(bar).getPricePoints(barSize, numBars).getAbsoluteTotalMovement();
	}


	// This function provides the rate of change in the percent difference between two points in time
	private double getMAPercentDiffRoc(Bar bar, BarSize barSize, int shortMa, int longMa, int numBars) {
		List<MovingAverage> shortMas = getVariables(
				MovingAverage.createParameters(bar.getSecurity().getKey(), 
						barSize1Min, shortMa),
						bar.getDateTime(),
						numBars);
		List<MovingAverage> longMas = getVariables(
				MovingAverage.createParameters(bar.getSecurity().getKey(), 
						barSize1Min, longMa),
						bar.getDateTime(),
						numBars);


		long startTime = shortMas.get(0).getDateTime().getTime(); 
		double startVal = getPercentDifference(shortMas.get(0).getMovingAverage(), longMas.get(0).getMovingAverage());
		long endTime = shortMas.get(numBars-1).getDateTime().getTime();
		double endVal = getPercentDifference(shortMas.get(numBars-1).getMovingAverage(), longMas.get(numBars-1).getMovingAverage());

		return getRateOfChange(startVal, startTime, endVal, endTime);
	}


	// Given a BarSize, the short MovingAverage length and the long MovingAverage length,
	// this will return the percentage difference between the two MAs
	private double getMAPercentDiff(Bar bar, BarSize barSize,int shortMa, int longMa) {
		double maShort = getVariables(
				MovingAverage.createParameters(bar.getSecurity().getKey(), 
						barSize, shortMa),
						bar.getDateTime()).getMovingAverage();
		double maLong = getVariables(
				MovingAverage.createParameters(bar.getSecurity().getKey(), 
						barSize, longMa),
						bar.getDateTime()).getMovingAverage();

		return getPercentDifference(maShort, maLong);
	}

	private double getPercentDifference(double n1, double n2){
		return (n1 - n2) / n2;
	}


	// Given a certain window length, this function will effeciently provide an array of Rate Of Changes
	// for a given security across multiple points in time for a single bar width
	private double[] getRateOfChangeForAllByWindowLength(Bar bar, BarSize barSize, int windowLength){

		// Get the maximum number of moving averages required to compute the multiple rate of changes
		List<MovingAverage> ma = getVariables(
				MovingAverage.createParameters(bar.getSecurity().getKey(), 
						barSize1Min, windowLength),
						bar.getDateTime(),
						getLargestWindow());

		// Create an array to hold the exact number of ROCs that will be created
		double[] roc = new double[windows.size()];

		// Iterate over all of the window durations (i.e. 9 bars, 20 bars, 50 bars, etc.) 
		// and store each of their values in the array.
		for(int i : windows){
			double startVal = ma.get(0).getMovingAverage();
			long startTime = ma.get(0).getDateTime().getTime();
			double endVal = ma.get(i - 1).getMovingAverage();
			long endTime = ma.get(i - 1).getDateTime().getTime();
			roc[windows.indexOf(i)] = getRateOfChange(startVal, startTime, endVal, endTime);
		}
		return roc;
	}

	private double getRateOfChange(Bar bar, BarSize barSize1Min2, int maWindowLength, int numBarsInROC) {
		List<MovingAverage> ma = getVariables(
				MovingAverage.createParameters(bar.getSecurity().getKey(), 
						barSize1Min, maWindowLength),
						bar.getDateTime(),
						numBarsInROC);

		double startVal = ma.get(0).getMovingAverage();
		long startTime = ma.get(0).getDateTime().getTime();
		double endVal = ma.get(ma.size()-1).getMovingAverage();
		long endTime = ma.get(ma.size()-1).getDateTime().getTime();

		return getRateOfChange(startVal, startTime, endVal, endTime);
	}

	private double getRateOfChange(double startVal, long startTime, double endVal, long endTime){
		return (endVal - startVal) / (endTime - startTime);
	}


	private boolean movingAverageListFull(Bar bar) {
		if(!getVariables(
				MovingAverage.createParameters(bar.getSecurity().getKey(), 
						barSize1Min, largestWindow),
						bar.getDateTime(),
						numVars)
						.isEmpty()){
			return true;
		}
		return false;
	}


	protected void backtestComplete() {
		clearRecordQueue();

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

		// This is only done if the scores array list is not null. Used to write
		// the output scores to a file for analysis
		ArrayList<String> stringRecords = new ArrayList<String>();
		StringBuilder s = new StringBuilder();
		s.append("scores");
		s.append(System.lineSeparator());
		
		stringRecords.add(s.toString());
		
		for(double d : scores){
			StringBuilder s2 = new StringBuilder();
			s2.append(d);
			s2.append(System.lineSeparator());
			stringRecords.add(s2.toString());
		}
		
		writeToFile(stringRecords);
	}

	private void setLargestWindow(ArrayList<Integer> windows) {
		for(int i : windows)
			if(i > largestWindow)
				largestWindow = i;
	}

	private String getHeader(){
		StringBuilder sb = new StringBuilder();

		sb.append("barDirectionCount1min");sb.append("|");
		sb.append("lastPriceMove1min");sb.append("|");
		sb.append("ma1min3bar5roc");sb.append("|");
		sb.append("ma1min5bar5roc");sb.append("|");
		sb.append("ma1min9bar5roc");sb.append("|");
		sb.append("ma1min20bar5roc");sb.append("|");
		sb.append("ma1min50bar5roc");sb.append("|");
		sb.append("ma1min100bar5roc");sb.append("|");
		sb.append("ma1min3ma5diff");sb.append("|");
		sb.append("ma1min3ma5roc");sb.append("|");
		sb.append("ma1min5ma9diff");sb.append("|");
		sb.append("ma1min5ma9roc");sb.append("|");
		sb.append("ma1min9ma20diff");sb.append("|");
		sb.append("ma1min9ma20roc");sb.append("|");
		sb.append("ma1min20ma50diff");sb.append("|");
		sb.append("ma1min20ma50roc");sb.append("|");
		sb.append("ma1minVolTo3BarAvgVol");sb.append("|");
		sb.append("ma1minVolTo5BarAvgVol");sb.append("|");
		sb.append("ma1minVolTo9BarAvgVol");sb.append("|");
		sb.append("ma1minVolTo20BarAvgVol");sb.append("|");
		sb.append("ma1minMacd");sb.append("|");
		sb.append("ma1minMacdTrendInd");sb.append("|");
		sb.append("ma1minRsi9");sb.append("|");
		sb.append("ma1minRsi20");sb.append("|");
		sb.append("ma1minRsi50");sb.append("|");
		sb.append("ma1minRsi9TrendInd");sb.append("|");
		sb.append("ma1minRsi20TrendInd");sb.append("|");
		sb.append("ma1minRsi50TrendInd");sb.append("|");
		sb.append("ma1min5BarTrendInd");sb.append("|");
		sb.append("ma1min9BarTrendInd");sb.append("|");
		sb.append("ma1min20BarTrendInd");sb.append("|");
		sb.append("barDirectionCount30sec");sb.append("|");
		sb.append("lastPriceMove30sec");sb.append("|");
		sb.append("ma30sec3bar5roc");sb.append("|");
		sb.append("ma30sec5bar5roc");sb.append("|");
		sb.append("ma30sec9bar5roc");sb.append("|");
		sb.append("ma30sec20bar5roc");sb.append("|");
		sb.append("ma30sec50bar5roc");sb.append("|");
		sb.append("ma30sec100bar5roc");sb.append("|");
		sb.append("ma30sec3ma5diff");sb.append("|");
		sb.append("ma30sec3ma5roc");sb.append("|");
		sb.append("ma30sec5ma9diff");sb.append("|");
		sb.append("ma30sec5ma9roc");sb.append("|");
		sb.append("ma30sec9ma20diff");sb.append("|");
		sb.append("ma30sec9ma20roc");sb.append("|");
		sb.append("ma30sec20ma50diff");sb.append("|");
		sb.append("ma30sec20ma50roc");sb.append("|");
		sb.append("ma30secVolTo3BarAvgVol");sb.append("|");
		sb.append("ma30secVolTo5BarAvgVol");sb.append("|");
		sb.append("ma30secVolTo9BarAvgVol");sb.append("|");
		sb.append("ma30secVolTo20BarAvgVol");sb.append("|");
		sb.append("ma30secMacd");sb.append("|");
		sb.append("ma30secMacdTrendInd");sb.append("|");
		sb.append("ma30secRsi9");sb.append("|");
		sb.append("ma30secRsi20");sb.append("|");
		sb.append("ma30secRsi50");sb.append("|");
		sb.append("ma30secRsi9TrendInd");sb.append("|");
		sb.append("ma30secRsi20TrendInd");sb.append("|");
		sb.append("ma30secRsi50TrendInd");sb.append("|");
		sb.append("ma30sec5BarTrendInd");sb.append("|");
		sb.append("ma30sec9BarTrendInd");sb.append("|");
		sb.append("ma30sec20BarTrendInd");sb.append("|");
		sb.append("barDirectionCount5sec");sb.append("|");
		sb.append("lastPriceMove5sec"); sb.append("|");
		sb.append("ma5sec3bar5roc");sb.append("|");
		sb.append("ma5sec5bar5roc");sb.append("|");
		sb.append("ma5sec9bar5roc");sb.append("|");
		sb.append("ma5sec20bar5roc");sb.append("|");
		sb.append("ma5sec50bar5roc");sb.append("|");
		sb.append("ma5sec100bar5roc");sb.append("|");
		sb.append("ma5sec3ma5diff");sb.append("|");
		sb.append("ma5sec3ma5roc");sb.append("|");
		sb.append("ma5sec5ma9diff");sb.append("|");
		sb.append("ma5sec5ma9roc");sb.append("|");
		sb.append("ma5sec9ma20diff");sb.append("|");
		sb.append("ma5sec9ma20roc");sb.append("|");
		sb.append("ma5sec20ma50diff");sb.append("|");
		sb.append("ma5sec20ma50roc");sb.append("|");
		sb.append("ma5secVolTo3BarAvgVol");sb.append("|");
		sb.append("ma5secVolTo5BarAvgVol");sb.append("|");
		sb.append("ma5secVolTo9BarAvgVol");sb.append("|");
		sb.append("ma5secVolTo20BarAvgVol");sb.append("|");
		sb.append("ma5secMacd");sb.append("|");
		sb.append("ma5secMacdTrendInd");sb.append("|");
		sb.append("ma5secRsi9");sb.append("|");
		sb.append("ma5secRsi20");sb.append("|");
		sb.append("ma5secRsi50");sb.append("|");
		sb.append("ma5secRsi9TrendInd");sb.append("|");
		sb.append("ma5secRsi20TrendInd");sb.append("|");
		sb.append("ma5secRsi50TrendInd");sb.append("|");
		sb.append("ma5sec5BarTrendInd");sb.append("|");
		sb.append("ma5sec9BarTrendInd");sb.append("|");
		sb.append("ma5sec20BarTrendInd");sb.append("|");
		sb.append("target");
		sb.append(System.lineSeparator());

		return sb.toString();
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
		H20_Example.analyticsBar = analyticsBar;
	}

	public static boolean isAutomatedTuning() {
		return automatedTuning;
	}

	public static void setAutomatedTuning(boolean automatedTuningEnabled) {
		automatedTuning = automatedTuningEnabled;
	}

	public int getNumVars() {
		return numVars;
	}

	public void setNumVars(int numVars) {
		this.numVars = numVars;
	}

	private void setLargestWindow(int largeWindow){
		largestWindow = largeWindow;
	}

	private int getLargestWindow(){
		return largestWindow;
	}
}
