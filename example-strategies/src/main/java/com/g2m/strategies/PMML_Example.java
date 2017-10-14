package com.g2m.strategies.examples;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;

import javax.sound.midi.SysexMessage;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;

import org.dmg.pmml.AssociationRule;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Item;
import org.dmg.pmml.Itemset;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.OutputField.Algorithm;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.AssociationModelEvaluator;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.HasRuleValues;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.PMMLManager;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.g2m.services.historicaldataloader.HistoricalDataLoader;
import com.g2m.services.strategybuilder.HistoricalBarCacheLoader;
import com.g2m.services.strategybuilder.Strategy;
import com.g2m.services.strategybuilder.StrategyComponent;
import com.g2m.services.strategybuilder.enums.EntityPersistMode;
import com.g2m.services.tradingservices.analytics.Analytics;
import com.g2m.services.tradingservices.backtest.BacktestAccount;
import com.g2m.services.tradingservices.backtest.BacktestTradingService;
import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Position;
import com.g2m.services.tradingservices.entities.Position.PositionBuilder;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityBuilder;
import com.g2m.services.tradingservices.entities.orders.MarketOrder;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.entities.Tick;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.tradingservices.enums.OrderAction;
import com.g2m.services.tradingservices.enums.SecurityType;
import com.g2m.services.variables.entities.DayValues;
import com.g2m.services.variables.entities.Macd;
import com.g2m.services.variables.entities.MovingAverage;
import com.g2m.services.variables.entities.PivotPoints;
import com.g2m.services.variables.entities.Rsi;
import com.g2m.services.variables.entities.TrendLine;
import com.g2m.services.variables.pmml.PmmlEvaluator;
import com.g2m.services.variables.pmml.PmmlResults;

/**
 * Added 6/28/2015.
 * 
 * @author Grant Seward
 */
@StrategyComponent
public class PMML_Example extends Strategy {

	@Autowired
	private HistoricalBarCacheLoader cacheLoader;
	private int existingPositionQuantity;

	private int baseShares = 1000;
	private double winThresh = 1.035;
	private double lossThresh = .995;
	int tickCnt = 0;
	DecimalFormat df = new DecimalFormat("###.####");

	BarSize bar1Min = BarSize._1_MIN;
	BarSize bar10Min = BarSize._10_MINS;
	BarSize bar15Min = BarSize._15_MINS;
	BarSize bar5Min = BarSize._5_MINS;
	BarSize bar30Min = BarSize._30_MINS;
	BarSize bar1Hr = BarSize._1_HOUR;
	BarSize bar4Hr = BarSize._4_HOURS;
	BarSize bar1Day = BarSize._1_DAY;

	String fileName;

	String currency = "USD";
	String exchange = "NYSE";
	String symbol = "BAC";

	File rFile;
	FileWriter writer; 

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	ArrayList<String> targetValues = new ArrayList<String>();

	String headerRcd;

	static PmmlEvaluator pmmlEval;
	AssociationModelEvaluator associativeEvaluator;
	int lastTarget = 0;
	int lastTargetCnt = 0;
	double predValThresh = .7;
	int predValThreshcnt = 0;

	int lastTargetRuleCnt = 30;
	double predValThreshRuleCnt = 4;

	String lastTradeCutoffString = "15:15";
	String firstTradeCutoffString = "14:30";
	long lastTradeCutoffTime;
	long firstTradeCutoffTime;

	double target = 0.005;
	double thresh = 0.0025;

	List<MovingAverage> ma5min9;
	List<MovingAverage> ma5min50;
	
	private List<String> fileNames = new ArrayList<String>();
	

	public static void main(String... args) {
		Strategy.initialize(PMML_Example.class);
	}

	@Override
	protected void run() {

		/*
		 * SEE ROW 956 FOR HOW TO USE THE PMML EVALUATOR:
		 * 
		 * - A PMML EVALUATOR IS ABLE TO READ ANY TYPE OF MODEL
		 * 		THAT IS EXPORTED AS A PMML FILE
		 * - LOAD THE PMML FILE TO CREATE A NEW EVALUATOR AS SEEN BELOW
		 * - THEN SIMPLY PASS THE SAME DATA ATTRIBUTES USED TO CREATE THE
		 *		MODEL INTO THE EVALUATOR CLASS
		 */
		
		URL pmmlUrl = ClassLoader.getSystemResource("randomForest.pmml");
		pmmlEval = new PmmlEvaluator(pmmlUrl);
		
		
		// Currently Hard Coded, needs to be refactored so that 
		// all file writing is moved to Strategy class for reusability
		
		// This creates an exportable file that can be used to analyze the data in 
		// an analytical environment. This should not be used for live scoring
		String usrHome = System.getProperty("user.home");
		String tickLocs =  usrHome + "/Github/g2m-algotrader/resources/ticks/";
		fileNames.add(tickLocs);
		
		String outputLoc = usrHome + "/Github/g2m-algotrader/resources/output/"; 
		
		try {
			writer = new FileWriter(createFileForModeling(outputLoc + symbol));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		registerSecuritiesFromFileBackTest(fileNames);

		// Set the cutoff trade time if desired (i.e. dont trade after 3pm or dont trade before 10pm)
		lastTradeCutoffTime = setTradeCutoffTime(lastTradeCutoffString);
		firstTradeCutoffTime = setTradeCutoffTime(firstTradeCutoffString);

		// This should ONLY be used when writing the file for analysis
		// DOES NOT NEED to be used this when running live 
		createHeader();
		
		setEntityPersistMode(EntityPersistMode.NONE);
		startBacktest();	
	
	}


	@Override
	protected void tickReceived(Tick tick) {

		// For each security
		// Calculate the rolling derivative (Rate of Change) across a X minute window 
		// 


		tickCnt ++;
		//		List<TrendLine> trendLines = getVariablesThatDontExpire(
		//				TrendLine.createParameters(tick.getSecurity().getKey(), bar5Min),tick.getDateTime());

		List<MovingAverage> ma1min20 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar1Min, 20), tick.getDateTime(), 1);
		List<MovingAverage> ma1min50 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar1Min, 50), tick.getDateTime(), 1);
		List<MovingAverage> ma1min200 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar1Min, 200), tick.getDateTime(), 1);
		ma5min9 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar5Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma5min20 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar5Min, 20), tick.getDateTime(), 1);
		ma5min50 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar5Min, 50), tick.getDateTime(), 1);
		List<MovingAverage> ma5min200 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar5Min, 50), tick.getDateTime(), 1);
		List<MovingAverage> ma10min9 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma10min20 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma10min50 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma10min200 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma15min9 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma15min20 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma15min50 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma15min200 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma30min9 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma30min20 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma30min50 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma30min200 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma1hr9 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma1hr20 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma1hr50 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma1hr200 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		
		List<MovingAverage> ma4hr9 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma4hr20 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma4hr50 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma4hr200 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		
		List<MovingAverage> ma1day9 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma1day20 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma1day50 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		List<MovingAverage> ma1day200 = getVariables(
				MovingAverage.createParameters(tick.getSecurity().getKey(), bar10Min, 9), tick.getDateTime(), 1);
		

		List<Macd> macd10min = getVariables(
				Macd.createParameters(tick.getSecurity().getKey(), bar10Min, 26, 12, 9), tick.getDateTime(),1);
		List<Macd> macd15min = getVariables(
				Macd.createParameters(tick.getSecurity().getKey(), bar10Min, 26, 12, 9), tick.getDateTime(),1);
		List<Macd> macd30min = getVariables(
				Macd.createParameters(tick.getSecurity().getKey(), bar10Min, 26, 12, 9), tick.getDateTime(),1);
		List<Macd> macd1hr = getVariables(
				Macd.createParameters(tick.getSecurity().getKey(), bar10Min, 26, 12, 9), tick.getDateTime(),1);
		List<Macd> macd4hr = getVariables(
				Macd.createParameters(tick.getSecurity().getKey(), bar10Min, 26, 12, 9), tick.getDateTime(),1);
		List<Macd> macd1day = getVariables(
				Macd.createParameters(tick.getSecurity().getKey(), bar10Min, 26, 12, 9), tick.getDateTime(),1);

		PivotPoints pp = getVariables(
				PivotPoints.createParameters(tick.getSecurity().getKey()), tick.getDateTime());

		List<Rsi> rsi5min = getVariables(
				Rsi.createParameters(tick.getSecurity().getKey(), bar5Min, 14),tick.getDateTime(),1);
		List<Rsi> rsi10min = getVariables(
				Rsi.createParameters(tick.getSecurity().getKey(), bar10Min, 14),tick.getDateTime(),1);
		List<Rsi> rsi15min = getVariables(
				Rsi.createParameters(tick.getSecurity().getKey(), bar10Min, 14),tick.getDateTime(),1);
		List<Rsi> rsi30min = getVariables(
				Rsi.createParameters(tick.getSecurity().getKey(), bar5Min, 14),tick.getDateTime(),1);
		List<Rsi> rsi1hr = getVariables(
				Rsi.createParameters(tick.getSecurity().getKey(), bar5Min, 14),tick.getDateTime(),1);
		List<Rsi> rsi4hr = getVariables(
				Rsi.createParameters(tick.getSecurity().getKey(), bar5Min, 14),tick.getDateTime(),1);
		List<Rsi> rsi1day = getVariables(
				Rsi.createParameters(tick.getSecurity().getKey(), bar5Min, 14),tick.getDateTime(),1);

		//		List<TrendLine> tl5min = getVariablesThatDontExpire(
		//				TrendLine.createParameters(tick.getSecurity().getKey(), bar5Min),  tick.getDateTime());
		//		List<TrendLine> tl15min = getVariablesThatDontExpire(
		//				TrendLine.createParameters(tick.getSecurity().getKey(), bar15Min),  tick.getDateTime());
		//		List<TrendLine> tl30min = getVariablesThatDontExpire(
		//				TrendLine.createParameters(tick.getSecurity().getKey(), bar30Min),  tick.getDateTime());
		//		List<TrendLine> tl1hr = getVariablesThatDontExpire(
		//				TrendLine.createParameters(tick.getSecurity().getKey(), bar1Hr),  tick.getDateTime());
		//		List<TrendLine> tl4hr = getVariablesThatDontExpire(
		//				TrendLine.createParameters(tick.getSecurity().getKey(), bar4Hr),  tick.getDateTime());
		//		List<TrendLine> tl1day = getVariablesThatDontExpire(
		//				TrendLine.createParameters(tick.getSecurity().getKey(), bar1Day),  tick.getDateTime());

		List<DayValues> dv1min = getVariables(
				DayValues.createParameters(tick.getSecurity().getKey(), bar1Min, 20),tick.getDateTime(),10);
		List<DayValues> dv5min = getVariables(
				DayValues.createParameters(tick.getSecurity().getKey(), bar1Min, 20),tick.getDateTime(),10);
		List<DayValues> dv10min = getVariables(
				DayValues.createParameters(tick.getSecurity().getKey(), bar1Min, 20),tick.getDateTime(),10);
		List<DayValues> dv15min = getVariables(
				DayValues.createParameters(tick.getSecurity().getKey(), bar1Min, 20),tick.getDateTime(),10);

		if (!pp.isEmpty()){
			StringBuilder output = new StringBuilder();
			List<Map<FieldName, FieldValue>> fieldValueRecords = new ArrayList<Map<FieldName, FieldValue>>();
			Map<FieldName, FieldValue> fieldValueRecord = new HashMap<FieldName, FieldValue>();

			// Price
			output.append(tick.getLastPrice());
			output.append(",");
			// Time
			output.append(tick.getDateTime().getTime());
			output.append(",");
			// UniqueID (Hashcode)
			output.append(tick.hashCode());
			output.append(",");
			// Symbol
			output.append(tick.getSecurity().getSymbol());
			output.append(",");
			// ma1min20 - ma1min50 percent diff
			output.append(df.format(100*(ma1min20.get(0).getMovingAverage() - ma1min50.get(0).getMovingAverage()) / ma1min50.get(0).getMovingAverage()));
			output.append(",");
			// ma1min50 - ma1min200 percent diff
			output.append(df.format(100*(ma1min50.get(0).getMovingAverage() - ma1min200.get(0).getMovingAverage()) / ma1min200.get(0).getMovingAverage()));
			output.append(",");
			// ma5min9 - ma5min20 percent diff
			output.append(df.format(100*(ma5min9.get(0).getMovingAverage() - ma5min20.get(0).getMovingAverage()) / ma5min20.get(0).getMovingAverage()));
			output.append(",");
			// ma5min20 - ma5min50 percent diff
			output.append(df.format(100*(ma5min20.get(0).getMovingAverage() - ma5min50.get(0).getMovingAverage()) / ma5min50.get(0).getMovingAverage()));
			output.append(",");
			// ma5min50 - ma5min200 percent diff
			output.append(df.format(100*(ma5min50.get(0).getMovingAverage() - ma5min200.get(0).getMovingAverage()) / ma5min200.get(0).getMovingAverage()));
			output.append(",");
			// ma5min20 - price percent diff
			output.append(df.format(100*(ma5min20.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma5min50 - price percent diff
			output.append(df.format(100*(ma5min50.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma5min200 - price percent diff
			output.append(df.format(100*(ma5min200.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma10min9 - ma10min20 percent diff
			output.append(df.format(100*(ma10min9.get(0).getMovingAverage() - ma10min20.get(0).getMovingAverage()) / ma10min20.get(0).getMovingAverage()));
			output.append(",");
			// ma10min20 - ma10min50 percent diff
			output.append(df.format(100*(ma10min20.get(0).getMovingAverage() - ma10min50.get(0).getMovingAverage()) / ma10min50.get(0).getMovingAverage()));
			output.append(",");
			// ma10min50 - ma10min200 percent diff
			output.append(df.format(100*(ma10min50.get(0).getMovingAverage() - ma10min200.get(0).getMovingAverage()) / ma10min200.get(0).getMovingAverage()));
			output.append(",");
			// ma10min20 - price percent diff
			output.append(df.format(100*(ma10min20.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma10min50 - price percent diff
			output.append(df.format(100*(ma10min50.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma10min50 - price percent diff
			output.append(df.format(100*(ma10min200.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma15min9 - ma15min20 percent diff
			output.append(df.format(100*(ma15min9.get(0).getMovingAverage() - ma15min20.get(0).getMovingAverage()) / ma15min20.get(0).getMovingAverage()));
			output.append(",");
			// ma15min20 - ma15min50 percent diff
			output.append(df.format(100*(ma15min20.get(0).getMovingAverage() - ma15min50.get(0).getMovingAverage()) / ma15min50.get(0).getMovingAverage()));
			output.append(",");
			// ma15min50 - ma15min200 percent diff
			output.append(df.format(100*(ma15min50.get(0).getMovingAverage() - ma15min200.get(0).getMovingAverage()) / ma15min200.get(0).getMovingAverage()));
			output.append(",");
			// ma15min20 - price percent diff
			output.append(df.format(100*(ma15min20.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma15min50 - price percent diff
			output.append(df.format(100*(ma15min50.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma15min200 - price percent diff
			output.append(df.format(100*(ma15min200.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma30min9 - ma30min20 percent diff
			output.append(df.format(100*(ma30min9.get(0).getMovingAverage() - ma30min20.get(0).getMovingAverage()) / ma30min20.get(0).getMovingAverage()));
			output.append(",");
			// ma30min20 - ma30min50 percent diff
			output.append(df.format(100*(ma30min20.get(0).getMovingAverage() - ma30min50.get(0).getMovingAverage()) / ma30min50.get(0).getMovingAverage()));
			output.append(",");
			// ma30min50 - ma30min200 percent diff
			output.append(df.format(100*(ma30min50.get(0).getMovingAverage() - ma30min200.get(0).getMovingAverage()) / ma30min200.get(0).getMovingAverage()));
			output.append(",");
			// ma30min200 - price percent diff
			output.append(df.format(100*(ma30min20.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma30min50 - price percent diff
			output.append(df.format(100*(ma30min50.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma30min50 - price percent diff
			output.append(df.format(100*(ma30min200.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma1hr9 - ma1hr20 percent diff
			output.append(df.format(100*(ma1hr9.get(0).getMovingAverage() - ma1hr20.get(0).getMovingAverage()) / ma1hr20.get(0).getMovingAverage()));
			output.append(",");
			// ma1hr20 - ma1hr50 percent diff
			output.append(df.format(100*(ma1hr20.get(0).getMovingAverage() - ma1hr50.get(0).getMovingAverage()) / ma1hr50.get(0).getMovingAverage()));
			output.append(",");
			// ma1hr50 - ma1hr200 percent diff
			output.append(df.format(100*(ma1hr50.get(0).getMovingAverage() - ma1hr200.get(0).getMovingAverage()) / ma1hr200.get(0).getMovingAverage()));
			output.append(",");
			// ma1hr20 - price percent diff
			output.append(df.format(100*(ma1hr20.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma1hr50 - price percent diff
			output.append(df.format(100*(ma1hr50.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma1hr200 - price percent diff
			output.append(df.format(100*(ma1hr200.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma4hr9 - ma4hr20 percent diff
			output.append(df.format(100*(ma4hr9.get(0).getMovingAverage() - ma4hr20.get(0).getMovingAverage()) / ma4hr20.get(0).getMovingAverage()));
			output.append(",");
			// ma4hr20 - ma4hr50 percent diff
			output.append(df.format(100*(ma4hr20.get(0).getMovingAverage() - ma4hr50.get(0).getMovingAverage()) / ma4hr50.get(0).getMovingAverage()));
			output.append(",");
			// ma4hr50 - ma4hr200 percent diff
			output.append(df.format(100*(ma4hr50.get(0).getMovingAverage() - ma4hr200.get(0).getMovingAverage()) / ma4hr200.get(0).getMovingAverage()));
			output.append(",");
			// ma4hr20 - price percent diff
			output.append(df.format(100*(ma4hr20.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma4hr50 - price percent diff
			output.append(df.format(100*(ma4hr50.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma4hr200 - price percent diff
			output.append(df.format(100*(ma4hr200.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma1day9 - ma1day20 percent diff
			output.append(df.format(100*(ma1day9.get(0).getMovingAverage() - ma1day20.get(0).getMovingAverage()) / ma1day20.get(0).getMovingAverage()));
			output.append(",");
			// ma1day20 - ma1day50 percent diff
			output.append(df.format(100*(ma1day20.get(0).getMovingAverage() - ma1day50.get(0).getMovingAverage()) / ma1day50.get(0).getMovingAverage()));
			output.append(",");
			// ma1day50 - ma1day200 percent diff
			output.append(df.format(100*(ma1day50.get(0).getMovingAverage() - ma1day200.get(0).getMovingAverage()) / ma1day200.get(0).getMovingAverage()));
			output.append(",");
			// ma1day20 - price percent diff
			output.append(df.format(100*(ma1day20.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma1day50 - price percent diff
			output.append(df.format(100*(ma1day50.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// ma1day200 - price percent diff
			output.append(df.format(100*(ma1day200.get(0).getMovingAverage() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// macd10min macd
			output.append(df.format((macd10min.get(0).getMacd())));
			output.append(",");
			// macd10min histogram
			output.append(df.format((macd10min.get(0).getHistogram())));
			output.append(",");
			// macd15min macd
			output.append(df.format((macd15min.get(0).getMacd())));
			output.append(",");
			// macd15min histogram
			output.append(df.format((macd15min.get(0).getHistogram())));
			output.append(",");
			// macd30min macd
			output.append(df.format((macd30min.get(0).getMacd())));
			output.append(",");
			// macd30min histogram
			output.append(df.format((macd30min.get(0).getHistogram())));
			output.append(",");
			// macd1hr macd
			output.append(df.format((macd1hr.get(0).getMacd())));
			output.append(",");
			// macd1hr histogram
			output.append(df.format((macd1hr.get(0).getHistogram())));
			output.append(",");
			// macd4hr macd
			output.append(df.format((macd4hr.get(0).getMacd())));
			output.append(",");
			// macd4hr histogram
			output.append(df.format((macd4hr.get(0).getHistogram())));
			output.append(",");
			// macd1day macd
			output.append(df.format((macd1day.get(0).getMacd())));
			output.append(",");
			// macd1day histogram
			output.append(df.format((macd1day.get(0).getHistogram())));
			output.append(",");
			// pp - price percent diff
			output.append(df.format(100*(pp.getPivotPoint() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// pp - ma1min20 percent diff
			output.append(df.format(100*(pp.getPivotPoint() - ma1min20.get(0).getMovingAverage()) / ma1min20.get(0).getMovingAverage()));
			output.append(",");
			// r1 - price percent diff
			output.append(df.format(100*(pp.getResistance1() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// r1 - ma1min20 percent diff
			output.append(df.format(100*(pp.getResistance1()  - ma1min20.get(0).getMovingAverage()) / ma1min20.get(0).getMovingAverage()));
			output.append(",");
			// r2 - price percent diff
			output.append(df.format(100*(pp.getResistance2() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// r2 - ma1min20 percent diff
			output.append(df.format(100*(pp.getResistance2()  - ma1min20.get(0).getMovingAverage()) / ma1min20.get(0).getMovingAverage()));
			output.append(",");
			// r3 - price percent diff
			output.append(df.format(100*(pp.getResistance3() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// r3 - ma1min20 percent diff
			output.append(df.format(100*(pp.getResistance3()  - ma1min20.get(0).getMovingAverage()) / ma1min20.get(0).getMovingAverage()));
			output.append(",");
			// s1 - price percent diff
			output.append(df.format(100*(pp.getSupport1() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// s1 - ma1min20 percent diff
			output.append(df.format(100*(pp.getSupport1()  - ma1min20.get(0).getMovingAverage()) / ma1min20.get(0).getMovingAverage()));
			output.append(",");
			// s2 - price percent diff
			output.append(df.format(100*(pp.getSupport2() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// s2 - ma1min20 percent diff
			output.append(df.format(100*(pp.getSupport2()  - ma1min20.get(0).getMovingAverage()) / ma1min20.get(0).getMovingAverage()));
			output.append(",");
			// s3 - price percent diff
			output.append(df.format(100*(pp.getSupport3() - tick.getLastPrice()) / tick.getLastPrice()));
			output.append(",");
			// s3 - ma1min20 percent diff
			output.append(df.format(100*(pp.getSupport3()  - ma1min20.get(0).getMovingAverage()) / ma1min20.get(0).getMovingAverage()));
			output.append(",");
			// rsi5min
			output.append(df.format(rsi5min.get(0).getRsi()));
			output.append(",");
			// rsi10min
			output.append(df.format(rsi10min.get(0).getRsi()));
			output.append(",");
			// rsi15min
			output.append(df.format(rsi15min.get(0).getRsi()));
			output.append(",");
			// rsi30min
			output.append(df.format(rsi30min.get(0).getRsi()));
			output.append(",");
			// rsi1hr
			output.append(df.format(rsi1hr.get(0).getRsi()));
			output.append(",");
			// rsi4hr
			output.append(df.format(rsi4hr.get(0).getRsi()));
			output.append(",");
			// rsi1day
			output.append(df.format(rsi1day.get(0).getRsi()));
			output.append(",");
			// dv1volatility
			output.append(0.0);
			//output.append(df.format(dv1min.get(dv1min.size()-1).getVolatility()));
			output.append(",");
			// dv1volatility5diff
			output.append(11.11);
			//output.append(df.format(dv1min.get(dv1min.size()-1).calcVolatilityAvgDiff(dv1min, 5)));
			output.append(",");
			// dv5volatility
			output.append(df.format(dv5min.get(dv5min.size()-1).getVolatility()));
			output.append(",");
			// dv5volatility5diff
			output.append(df.format(dv5min.get(dv5min.size()-1).calcVolatilityAvgDiff(dv5min, 5)));
			output.append(",");
			// dv10volatility
			output.append(df.format(dv10min.get(dv10min.size()-1).getVolatility()));
			output.append(",");
			// dv10volatility5diff
			output.append(df.format(dv10min.get(dv10min.size()-1).calcVolatilityAvgDiff(dv10min, 5)));
			output.append(",");
			// dv15volatility
			output.append(df.format(dv15min.get(dv15min.size()-1).getVolatility()));
			output.append(",");
			// dv15volatility5diff
			output.append(df.format(dv15min.get(dv15min.size()-1).calcVolatilityAvgDiff(dv15min, 5))) ;
			output.append(",");
			
			/*
			 * 
			 * ------------------------------------------------
			 * 		THIS IS HOW WE GET THE PMML RESULTS
			 * ------------------------------------------------
			 * 
			 */
			
			// pmmlOutput - predicted target
			PmmlResults res = new PmmlResults(pmmlEval, output.toString());
			output.append(res.getClassificationTarget());
			
			System.out.println("MODEL SCORE FROM PMML: " + res.getClassificationTarget());
			/*
			 * 
			 * ------------------------------------------------
			 * 				END PMML RESULTS
			 * 	This results field can be used to make decisions
			 * off of, such as if variable = 'X' then make trade
			 * ------------------------------------------------
			 * 
			 */
			
			output.append(",");
			// pmmlOutput - predicted target probability
			output.append(res.getPredictedProbability(res.getClassificationTarget()));
			output.append(",");

			consecutiveTargetCnt(res.getClassificationTarget());
			consecutiveThreshTargetCnt(res.getPredictedProbability(res.getClassificationTarget()), 
					res.getClassificationTarget());

			// pmmlOutput - predicted target conescutive thresh cnt
			output.append(predValThreshcnt);
			output.append(",");

			output.append(lastTargetCnt);
			output.append(",");
			// target
			output.append("T");

			checkToPlaceOrder(res.getClassificationTarget(), 
					res.getPredictedProbability(res.getClassificationTarget()), tick);
			//checkTargetAndWriteToFile(output.toString(), tick);
		}

	}



	private void checkToPlaceOrder(int classificationTarget,
			double predictedProbability, Tick tick) {

		if(!positionOpen(tick) && lastTargetCnt > lastTargetRuleCnt && predValThreshcnt > predValThreshRuleCnt
				&& tick.getDateTime().getTime() % milliOneDay > firstTradeCutoffTime
				&& tick.getDateTime().getTime() % milliOneDay < lastTradeCutoffTime){ // 
			//			System.out.println(positionOpen(tick));
			Random rand = new Random();
			if (classificationTarget == 2){
				String reason = "MODEL PREDICTION SAYS OPEN POSITION";
				placeMarketOrder(tick, baseShares, OrderAction.BUY, reason);

			} else if (classificationTarget == 5){
				String reason = "MODEL PREDICTION SAYS OPEN POSITION";
				placeMarketOrder(tick, baseShares, OrderAction.BUY, reason);
			}
		} else if (longPositionExists(tick)){
			if(target < getUnrealizedPercent(tick)){
				String reason = "CLOSING POSIION FOR TARGET REACHED";
				placeMarketOrder(tick, baseShares, OrderAction.SELL, reason);

			}else if (-thresh > getUnrealizedPercent(tick)){
				String reason = "CLOSING POSIION FOR STOP LOSS REACHED";
				placeMarketOrder(tick, baseShares, OrderAction.SELL, reason);

			} else if (isEndOfDay(tick)){
				String reason = "CLOSING POSIION FOR END OF DAY";
				placeMarketOrder(tick, baseShares, OrderAction.BUY, reason);

			}
		}  else if (shortPositionExists(tick)){
			if(target < -getUnrealizedPercent(tick)){
				String reason = "CLOSING POSIION FOR TARGET REACHED";
				placeMarketOrder(tick, baseShares, OrderAction.BUY, reason);

			} else if (thresh < getUnrealizedPercent(tick)){
				String reason = "CLOSING POSIION FOR STOP LOSS REACHED";
				placeMarketOrder(tick, baseShares, OrderAction.BUY, reason);

			} else if (isEndOfDay(tick)){
				String reason = "CLOSING POSIION FOR END OF DAY";
				placeMarketOrder(tick, baseShares, OrderAction.SELL, reason);
			}
		}
	}	




	private void consecutiveThreshTargetCnt(double currThresh, int currTarget) {
		if(lastTarget == currTarget && 
				(predValThresh < currThresh || 1-predValThresh > currThresh)){
			predValThreshcnt ++;
		}
		else
			predValThreshcnt = 0;
	}

	private void consecutiveTargetCnt(int currTarget) {
		if (lastTarget != currTarget){
			lastTarget = currTarget;
			lastTargetCnt = 0;
		}
		else
			lastTargetCnt ++;
	}

	private String checkForNonNumericValues(String s){

		for (int i = 0; i < s.length(); i++) {
			if ((!Character.isDigit(s.charAt(i)) && !s.contains(".")) || s.charAt(i)=='∞' || s.charAt(i)=='�') {
				StringBuilder sb = new StringBuilder(s);
				sb.setCharAt(i, '0');
				return checkForNonNumericValues(sb.toString());
			}
		}
		return s;
	}

	private void checkTargetAndWriteToFile(String s, Tick tick) {

		targetValues.add(checkForNonNumericValues(s));

		// 1 is positive threshold met and positive target not met
		// 2 is positive threshold met and positive target met
		// 3 is positive threshold met and negative target met
		// 4 is negative threshold met and negative target not met
		// 5 is negative threshold met and negative target met
		// 6 is negative threshold met and positive target met
		// 7 is time ran out for day but positive trade
		// 8 is time ran out for day but negative trade

		ArrayList<String> tempArray = new ArrayList<String>(); 

		for (String key : targetValues){
			double origPrice =  Double.valueOf(key.substring(0, key.indexOf(",")));
			if(key.substring(key.length()-1).equals("1")){
				if (origPrice > (1+target)*tick.getLastPrice()){
					String newString = key.substring(0, key.length()-1) + "2";
					//targetValues.remove(key);
					writeToFileForR(newString);
					//System.out.println("PRINTING 2 -" + targetValues.size() + " - " + newString);
				} else if (origPrice < (1-target)*tick.getLastPrice()){
					String newString = key.substring(0, key.length()-1) + "3";
					//targetValues.remove(key);
					writeToFileForR(newString);
					//System.out.println("PRINTING 3 -" + targetValues.size() + " - " + newString);
				} else if (tick.getDateTime().getTime() % milliOneDay >= 71940000){
					String newString; 
					if(origPrice > tick.getLastPrice()){
						newString = key.substring(0, key.length()-1) + "7";
					} else {
						newString = key.substring(0, key.length()-1) + "8";
					}
					//targetValues.remove(key);
					writeToFileForR(newString);
					//System.out.println("PRINTING 7/8 -" + targetValues.size() + " - " + newString);
				} else {
					tempArray.add(key);
				}
			} else if(key.substring(key.length()-1).equals("4")){
				if (origPrice > (1+target)*tick.getLastPrice()){
					String newString = key.substring(0, key.length()-1) + "6";
					//targetValues.remove(key);
					writeToFileForR(newString);
					//System.out.println("PRINTING 6 -" + targetValues.size() + " - " + newString);
				} else if (origPrice < (1-target)*tick.getLastPrice()){
					String newString = key.substring(0, key.length()-1) + "5";
					//targetValues.remove(key);
					writeToFileForR(newString);
					//System.out.println("PRINTING 5 -" + targetValues.size() + " - " + newString);
				} else if (tick.getDateTime().getTime() % milliOneDay >= 71940000){
					String newString; 
					if(origPrice > tick.getLastPrice()){
						newString = key.substring(0, key.length()-1) + "7";
					} else {
						newString = key.substring(0, key.length()-1) + "8";
					}
					//targetValues.remove(key);
					writeToFileForR(newString);
					//System.out.println("PRINTING 7/8 -" + targetValues.size() + " - " + newString);
				} else {
					tempArray.add(key);
				}
			}
			else if(key.substring(key.length()-1).equals("T")){
				if (origPrice > (1+thresh)*tick.getLastPrice()){
					String newString = key.substring(0, key.length()-1) + "1";
					//targetValues.remove(key);
					tempArray.add(newString);
					//targetValues.add(newString);
					//writeToFileForR(newString);
					//System.out.println("PRINTING 1 -" + targetValues.size() + " - " + newString);
				} else if (origPrice < (1-thresh)*tick.getLastPrice()){
					String newString = key.substring(0, key.length()-1) + "4";
					//targetValues.remove(key);
					//targetValues.add(newString);
					tempArray.add(newString);
					//writeToFileForR(newString);
					//System.out.println("PRINTING 4 -" + targetValues.size() + " - " + newString);
				} else if (tick.getDateTime().getTime() % milliOneDay >= 71940000){
					String newString; 
					if(origPrice > tick.getLastPrice()){
						newString = key.substring(0, key.length()-1) + "7";
					} else {
						newString = key.substring(0, key.length()-1) + "8";
					}
					//targetValues.remove(key);
					writeToFileForR(newString);
					//System.out.println("PRINTING 7/8 -" + targetValues.size() + " - " + newString);
				} else {
					//System.out.println("ADDING Back to Temp Array");
					tempArray.add(key);
				}
			}

		}
		//System.out.println("RESETTING TARGET ARRAY");
		targetValues = tempArray;
		//System.out.println("TARGET ARRAY RESET");
	}


	@Override
	protected void barCreated(Bar bar) {
		//System.out.println("Bar created at " + sdf.format(bar.getDateTime().getTime()));

	}

	protected void backtestComplete() {
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("BACKTEST COMPLETED");
		System.out.println("ANALYTICS - Total Trades: " + getAnalytics().getAggregateAnalytics().getTotalTrades()/2);
		System.out.println("ANALYTICS - Total Profit: " + getAnalytics().getAggregateAnalytics().getProfit());
		System.out.println("ANALYTICS - Total Profit: " + getAnalytics().getAggregateAnalytics().getWinLossCount().toString());
		System.out.println("ANALYTICS - Profit per Trade: " + (getAnalytics().getAggregateAnalytics().getProfit() / 
				getAnalytics().getAggregateAnalytics().getTotalTrades()/2) / baseShares);
		System.out.println("ACCOUNT - Available Funds: " + getAccount().getAvailableFunds());

	}

	private File createFileForModeling(String s){
		File file = new File(s);

		try {
			file.createNewFile();
			return file;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void createHeader() {
		StringBuilder header = new StringBuilder();
		header.append("price,");
		header.append("dateTime,");
		header.append("ID,");
		header.append("symbol,");
		header.append("ma1min20ma1min50pd,");
		header.append("ma1min50ma1min200pd,");
		header.append("ma5min9ma5min20pd,");
		header.append("ma5min20ma5min50pd,");
		header.append("ma5min50ma5min200pd,");
		header.append("ma5min20pricepd,");
		header.append("ma5min50pricepd,");
		header.append("ma5min200pricepd,");
		header.append("ma10min9ma10min20pd,");
		header.append("ma10min20ma10min50pd,");
		header.append("ma10min50ma10min200pd,");
		header.append("ma10min20pricepd,");
		header.append("ma10min50pricepd,");
		header.append("ma10min200pricepd,");
		header.append("ma15min9ma15min20pd,");
		header.append("ma15min20ma15min50pd,");
		header.append("ma15min50ma15min200pd,");
		header.append("ma15min20pricepd,");
		header.append("ma15min50pricepd,");
		header.append("ma15min200pricepd,");
		header.append("ma30min9ma30min20pd,");
		header.append("ma30min20ma30min50pd,");
		header.append("ma30min50ma30min200pd,");
		header.append("ma30min20pricepd,");
		header.append("ma30min50pricepd,");
		header.append("ma30min200pricepd,");
		header.append("ma1hr9ma1hr20pd,");
		header.append("ma1hr20ma1hr50pd,");
		header.append("ma1hr50ma1hr200pd,");
		header.append("ma1hr20pricepd,");
		header.append("ma1hr50pricepd,");
		header.append("ma1hr200pricepd,");
		header.append("ma4hr9ma4hr20pd,");
		header.append("ma4hr20ma4hr50pd,");
		header.append("ma4hr50ma4hr200pd,");
		header.append("ma4hr20pricepd,");
		header.append("ma4hr50pricepd,");
		header.append("ma4hr200pricepd,");
		header.append("ma1day9ma1day20pd,");
		header.append("ma1day20ma1day50pd,");
		header.append("ma1day50ma1day200pd,");
		header.append("ma1day20pricepd,");
		header.append("ma1day50pricepd,");
		header.append("ma1day200pricepd,");
		header.append("macd10minMacd,");
		header.append("macd10minHist,");
		header.append("macd15minMacd,");
		header.append("macd15minHist,");
		header.append("macd30minMacd,");
		header.append("macd30minHist,");
		header.append("macd1hrMacd,");
		header.append("macd1hrHist,");
		header.append("macd4hrMacd,");
		header.append("macd4hrHist,");
		header.append("macd1dayMacd,");
		header.append("macd1dayHist,");
		header.append("ppPricepd,");
		header.append("ppma1min20pd,");
		header.append("r1Pricepd,");
		header.append("r1ma1min20pd,");
		header.append("r2Pricepd,");
		header.append("r2ma1min20pd,");
		header.append("r3Pricepd,");
		header.append("r3ma1min20pd,");
		header.append("s1Pricepd,");
		header.append("s1ma1min20pd,");
		header.append("s2Pricepd,");
		header.append("s2ma1min20pd,");
		header.append("s3Pricepd,");
		header.append("s3ma1min20pd,");
		header.append("rsi5min,");
		header.append("rsi10min,");
		header.append("rsi15min,");
		header.append("rsi30min,");
		header.append("rsi1hr,");
		header.append("rsi4hr,");
		header.append("rsi1day,");
		header.append("dv1volatility,");
		header.append("dv1volatility5diff,");
		header.append("dv5volatility,");
		header.append("dv5volatility5diff,");
		header.append("dv10volatility,");
		header.append("dv10volatility5diff,");
		header.append("dv15volatility,");
		header.append("dv15volatility5diff,");
		header.append("pmmlPredTarg,");
		header.append("pmmlPredTargProb,");
		header.append("pmmlPredTargProbConsecCnt,");
		header.append("pmmlPredTargConsecCnt,");
		header.append("target");
		headerRcd = header.toString();
		writeToFileForR(headerRcd);

	}

	private void writeToFileForR(String s){
		try {
			s += System.lineSeparator();
			writer.write(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	
}
