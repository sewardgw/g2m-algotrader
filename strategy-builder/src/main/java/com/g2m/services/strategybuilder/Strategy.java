package com.g2m.services.strategybuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.g2m.services.tradingservices.persistence.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import com.g2m.services.strategybuilder.Strategy.MomentumTrader.PricePoints;
import com.g2m.services.strategybuilder.enums.EntityPersistMode;
import com.g2m.services.strategybuilder.enums.StrategyStatus;
import com.g2m.services.strategybuilder.utilities.PropertyHandler;
import com.g2m.services.strategybuilder.utilities.SecurityFilePathCreator;
import com.g2m.services.strategybuilder.utilities.SecurityFileReader;
import com.g2m.services.tradingservices.Account;
import com.g2m.services.tradingservices.BarPublisher;
import com.g2m.services.tradingservices.BarSubscriber;
import com.g2m.services.tradingservices.SecurityRegistry;
import com.g2m.services.tradingservices.TickDispatcher;
import com.g2m.services.tradingservices.TickPublisher;
import com.g2m.services.tradingservices.TickSubscriber;
import com.g2m.services.tradingservices.TradingService;
import com.g2m.services.tradingservices.analytics.Analytics;
import com.g2m.services.tradingservices.backtest.BacktestAccount;
import com.g2m.services.tradingservices.backtest.BacktestTradingService;
import com.g2m.services.tradingservices.brokerage.BrokerageAccount;
import com.g2m.services.tradingservices.brokerage.BrokerageTradingService;
import com.g2m.services.tradingservices.caches.BarCache;
import com.g2m.services.tradingservices.caches.TickCache;
import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Bar.BarBuilder;
import com.g2m.services.tradingservices.entities.Position;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.Tick;
import com.g2m.services.tradingservices.entities.Tick.TickBuilder;
import com.g2m.services.tradingservices.entities.orders.MarketOrder;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.entities.orders.OrderKey;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.tradingservices.enums.OrderAction;
import com.g2m.services.tradingservices.utilities.EmailUtility;
import com.g2m.services.tradingservices.utilities.TimeFormatter;
import com.g2m.services.variables.VariableService;
import com.g2m.services.variables.entities.Variable;
import com.g2m.services.variables.persistence.VariablePersistThread;

/**
 * Added 5/20/2015.
 *
 * @author Michael Borromeo
 */
public abstract class Strategy {
	@Autowired
	private BrokerageTradingService brokerageTradingService;
	@Autowired
	private BacktestTradingService backtestTradingService;
	@Autowired
	private VariableService variableService;
	@Autowired
	private TickPersistThread tickPersistThread;
	@Autowired
	private BarPersistThread barPersistThread;
	@Autowired
	private VariablePersistThread variablePersistThread;
	@Autowired
	private OrderPersistThread orderPersistThread;
	@Autowired
	private SlackMessageThread slackMessageThread;
	@Autowired
	private TickCache tickCache;
	@Autowired
	private BarCache barCache;
	@Autowired
	private HistoricalBarCacheLoader cacheLoader;
	@Autowired
	private Analytics analytics;
	@Autowired
	private EmailUtility emailUtil;
	@Autowired
	private PropertyHandler propertyHandler;
	protected Set<SecurityKey> securityKeys;
	private Map<SecurityKey, Set<BarSize>> securityBarSizes;
	private Map<SecurityKey, String> securityTestDataFiles;
	private TickAndBarSubscriber tickAndBarSubscriber;
	private StrategyStatus status;
	private EntityPersistMode persistMode;
	protected long milliOneDay = 86400000;
	private String defaultWorkingDirectory;
	private String baseCurrentStrategyDirectory;
	protected String orgName = "G2M";
	private static String strategyName;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd'-'HH.mm.ss");
	// The starting balance of the account
	protected double startingBalance;
	long lastSlackUpdateTime = 0;
	protected static Long startTime;
	

	// A bar trader is a tool that simplifies the trading strategies that rely
	// heavily on using bars. This HashMap is used to keep on bar trader
	// for each security key
	private HashMap<SecurityKey, BarTrader> barTraders;

	private HashMap<SecurityKey, MomentumTrader> momentumTraders;

	public Set<SecurityKey> getSecurityKeys() {
		return securityKeys;
	}

	/**
	 * This will need to be called by any Strategy implementation. This sets up all
	 * of the objects that will be used to hold data for the strategy.
	 */
	public Strategy() {
		status = StrategyStatus.NOT_STARTED;
		persistMode = EntityPersistMode.NONE;
		securityKeys = new HashSet<SecurityKey>();
		securityBarSizes = new HashMap<SecurityKey, Set<BarSize>>();
		tickAndBarSubscriber = new TickAndBarSubscriber();
		securityTestDataFiles = new HashMap<SecurityKey, String>();
		barTraders = new HashMap<SecurityKey, BarTrader>();
		momentumTraders = new HashMap<SecurityKey, MomentumTrader>();
	}

	/**
	 * This method initializes the strategy as a {@link org.springframework.boot.SpringApplication}
	 * and creates the {@link org.springframework.context.ApplicationContext} by running
	 * the SpringApplication. The the implemented method <b>run()</b> is then called.<br><br>
	 * This should be called by the implemented strategy such as initialize(YourStrategyName.class) 
	 * @param clazz An implemented Strategy class. 
	 */
	public static void initialize(Class<? extends Strategy> clazz) {
		strategyName = clazz.getSimpleName();
		SpringApplication application = new SpringApplication(clazz);
		ApplicationContext context = application.run();
		Strategy strategy = context.getBean(clazz);
		strategy.run();
	}

	/**
	 * This will take ticks from a file and fill the tick cache with those ticks. 
	 * @deprecated This needs to be updated to run the ticks through the bar publisher so that
	 * bars are created at the same time that the ticks are flowing through. The current setup has
	 * would simply create all prior bars with a high, low, open, close value equal to the last
	 * tick price.<br><br> 
	 * File format: dateTime, last, lastDateTime, volBid, volAsk, bid, ask, vol, openIntrest,
	 * settlement
	 * @param securityKey the unique security key related to the ticks in the associated file
	 * @param filePath the absolute path to the file where the ticks are located
	 */
	@Deprecated
	public void fillTickCacheFromFile(SecurityKey securityKey, String filePath) throws Exception {
		BufferedReader reader = null;
		try {
			String line = null;
			reader = new BufferedReader(new FileReader(filePath));
			while (null != (line = reader.readLine())) {
				String[] lineValues = line.split(",");
				if (10 != lineValues.length) {
					TickBuilder builder = new TickBuilder();
					builder.setSecurity(SecurityRegistry.get(securityKey));
					builder.setDateTime(new Date(Long.parseLong(lineValues[0].trim())));
					builder.setLastPrice(Double.parseDouble(lineValues[1].trim()));
					builder.setVolumeBid(Integer.parseInt(lineValues[3].trim()));
					builder.setVolumeAsk(Integer.parseInt(lineValues[4].trim()));
					builder.setBidPrice(Double.parseDouble(lineValues[6].trim()));
					builder.setAskPrice(Double.parseDouble(lineValues[6].trim()));
					builder.setVolume(Integer.parseInt(lineValues[7].trim()));
					builder.setOpenInterest(Integer.parseInt(lineValues[8].trim()));
					builder.setSettlement(Double.parseDouble(lineValues[9].trim()));
					tickCache.save(builder.build());
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (null != reader) {
				reader.close();
			}
		}
	}

	/**
	 * This will take a file containing bars for a given security and will populate
	 * the bar cache with those bars.<br><br>
	 * File format: dateTime, barSize, high, low, open, close, volume
	 * @deprecated We are moving away from managing bars in files and in the database
	 * in lieu of having the more granular tick level data. Instead of loading bars 
	 * into the system via a file, ticks should be sent in in order to capture as much
	 * information as possible from the data. see {@link #fillTickCacheFromFile(SecurityKey, String)}
	 * as an alternative.
	 * @param securityKey the unique security key related to the bars in the associated file
	 * @param filePath the absolute path to the file where the bars are located
	 */
	@Deprecated
	public void fillBarCacheFromFile(SecurityKey securityKey, String filePath) throws Exception {
		BufferedReader reader = null;
		try {
			String line = null;
			reader = new BufferedReader(new FileReader(filePath));
			while (null != (line = reader.readLine())) {
				String[] lineValues = line.split(",");
				if (7 != lineValues.length) {
					BarBuilder builder = new BarBuilder();
					builder.setSecurity(SecurityRegistry.get(securityKey));
					builder.setDateTime(new Date(Long.parseLong(lineValues[0].trim())));
					builder.setBarSize(BarSize.valueOf(lineValues[1].trim()));
					builder.setHigh(Double.parseDouble(lineValues[2].trim()));
					builder.setLow(Double.parseDouble(lineValues[3].trim()));
					builder.setOpen(Double.parseDouble(lineValues[4].trim()));
					builder.setClose(Double.parseDouble(lineValues[5].trim()));
					builder.setVolume(Integer.parseInt(lineValues[6].trim()));
					barCache.save(builder.build());
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (null != reader) {
				reader.close();
			}
		}
	}

	/**
	 * The tick cache contains the most recent ticks that have come into the application. There are
	 * several use cases where the last N ticks are required in a strategy such as when you
	 * want to check to see if the price has increased for three straight ticks, what the rate 
	 * of change is over the past 15 ticks, etc.<br> <br>
	 * The tick cache allows easy access to retrieving the recent ticks but only a small amount
	 * of ticks is kept in memory to prevent out of memory errors. 
	 * @return returns the tick cache object that is being used by the strategy.
	 */
	protected TickCache getTickCache() {
		return tickCache;
	}

	/**
	 * The bar cache contains the most recent bar that have been created by the application. There are
	 * several use cases where the last N bars are required in a strategy such as when you
	 * want to check to see if the most recent bars have increased for three straight bars, what the rate 
	 * of change is over the past 15 bars, etc.<br> <br>
	 * The bar cache allows easy access to retrieving the recent bars but only a several hundred
	 * bars are kept in memory to prevent out of memory errors. 
	 * @return returns the tick cache object that is being used by the strategy.
	 */
	protected BarCache getBarCache() {
		return barCache;
	}

	/** 
	 * This registers the security so that it will receive ticks once the back test or live 
	 * connection to the broker has been established.<br><br>
	 * NOTE - securities 
	 * registered here for live trading MUST match exactly as the broker is expecting
	 * them. If a security is registered for live trading but the broker is not able
	 * to find the corresponding security, no ticks will be sent to the application.
	 * @param security The unique security to be registered.
	 */
	public void subscribeToTicks(Security security) {
		SecurityRegistry.addIfNotPresent(security);
		securityKeys.add(security.getKey());
	}
	/** 
	 * This registers the security so that it will receive the associated bar size 
	 * once the back test or live connection to the broker has been established.<br><br>
	 * NOTE - securities 
	 * registered here for live trading MUST match exactly as the broker is expecting
	 * them. If a security is registered for live trading but the broker is not able
	 * to find the corresponding security, no ticks will be sent to the application.
	 * @param security The unique security to be registered.
	 * @param the bar size that will be registered for the security.
	 */
	public void subscribeToBars(Security security, BarSize barSize) {
		SecurityRegistry.addIfNotPresent(security);
		securityKeys.add(security.getKey());
		if (!securityBarSizes.containsKey(security.getKey())) {
			securityBarSizes.put(security.getKey(), new HashSet<BarSize>());
		}
		securityBarSizes.get(security.getKey()).add(barSize);

		// Create a new bar trader for each security to keep track of
		// when the bars cross over, when a trade is placed, etc.
		addBarTrader(security.getKey(), barSize);
	}

	/**
	 * This adds a test file for back testing and associates the file to the given security. All
	 * ticks within the test file will have the security and security key for the security listed.
	 * @param security The security that will be associated to the file.
	 * @param testFilePath The absolute path to the test file.
	 */
	public void addTestFile(Security security, String testFilePath) {
		SecurityRegistry.addIfNotPresent(security);
		securityKeys.add(security.getKey());
		securityTestDataFiles.put(security.getKey(), testFilePath);
	}

	/**
	 * Registers each security within the Security Registry to have the ticks for 
	 * that particular security sent into the implementation strategy. This method
	 * should only be used after ticks have been added to the 
	 * {@link com.g2m.services.tradingservices.SecurityRegistry} <br> <br>
	 * This method handles both back testing and live trading. NOTE - securities 
	 * registered here for live trading MUST match exactly as the broker is expecting
	 * them. If a security is registered for live trading but the broker is not able
	 * to find the corresponding security, no ticks will be sent to the application.
	 */
	private void setupTickSubscribers() {
		Set<Security> securities = new HashSet<Security>();
		for (SecurityKey securityKey : securityKeys) {
			securities.add(SecurityRegistry.get(securityKey));
		}
		getTradingService().addTickSubscriptions(securities, tickAndBarSubscriber);
	}

	/**
	 * Registers each security within the Security Registry to have the bars for 
	 * that particular security sent into the implementation strategy. This method
	 * should only be used after the bar sizes for each security have been added to the 
	 * {@link com.g2m.services.tradingservices.SecurityRegistry}. <br> <br>
	 * This method handles both back testing and live trading. NOTE - securities 
	 * registered here for live trading MUST match exactly as the broker is expecting
	 * them. If a security is registered for live trading but the broker is not able
	 * to find the corresponding security, no bars will be sent to the application.
	 */
	private void setupBarSubscribers() {
		Map<Security, Set<BarSize>> securities = new HashMap<Security, Set<BarSize>>();

		for (SecurityKey securityKey : securityBarSizes.keySet()) {
			Security security = SecurityRegistry.get(securityKey);
			securities.put(security, new HashSet<BarSize>());
			for (BarSize barSize : securityBarSizes.get(securityKey)) {
				securities.get(security).add(barSize);
			}
		}
		getTradingService().addBarSubscriptions(securities, tickAndBarSubscriber);
	}

	/**
	 * Throws a {@link java.lang.RuntimeException} if no file is available for a given security
	 * that the system is trying to use for back testing.
	 */
	private void throwExceptionIfTestDataFilesAreAbsent() {
		for (SecurityKey securityKey : securityKeys) {
			if (!securityTestDataFiles.containsKey(securityKey)) {
				throw new RuntimeException("Security doesn't have associated test data file: " + securityKey.toString());
			}
		}
	}

	/**
	 * This makes several checks to ensure that the strategy is in the correct state to
	 * begin the back test and then starts to ingest the ticks from the test files into
	 * the strategy. Once the ticks have completed, this method will call 
	 * {@link #backtestComplete()} to run any post-strategy cleanup.
	 */
	public void startBacktest() {
		throwExceptionIfNotReadyToStart();
		throwExceptionIfTestDataFilesAreAbsent();
		status = StrategyStatus.STARTED_BACKTEST;
		setWorkingDirectory();
		setupTickSubscribers();
		setupBarSubscribers();
		startPersistThreads();
		backtestTradingService.start(securityTestDataFiles);
		// TODO Add test completion cleanup/calls
		backtestComplete();
		stopPersistThreads();
		status = StrategyStatus.STOPPED;
	}

	/**
	 * 
	 * @param brokerageServer The IP address of the brokerage server that the strategy should
	 * connect to.
	 * @param brokeragePort The port used to connect to the brokerage server. For IB, this
	 * needs to be set through the desktop GUI under Settings > API
	 * @param brokerageClientId A unique ID used for the broker to track this particular session
	 * that is making the connection. IB allows multiple connections so distinct client IDs should
	 * be used for each connection.
	 */
	public void start(String brokerageServer, int brokeragePort, int brokerageClientId) {
		throwExceptionIfNotReadyToStart();
		status = StrategyStatus.STARTED_LIVE;
		setWorkingDirectory();
		setupTickSubscribers();
		setupBarSubscribers();
		startPersistThreads();
		brokerageTradingService.start(brokerageServer, brokeragePort, brokerageClientId);
	}

	/**
	 * 
	 * @param brokerageServer The IP address of the brokerage server that the strategy should
	 * connect to.
	 * @param brokeragePort The port used to connect to the brokerage server. For IB, this
	 * needs to be set through the desktop GUI under Settings > API
	 * @param brokerageClientId A unique ID used for the broker to track this particular session
	 * that is making the connection. IB allows multiple connections so distinct client IDs should
	 * be used for each connection.
	 */
	public void start(String brokerageServer, int brokeragePort, int brokerageClientId, boolean loadToCache) {
		if(!loadToCache)
			start(brokerageServer, brokeragePort, brokerageClientId);
		else{
			throwExceptionIfNotReadyToStart();
			status = StrategyStatus.STARTED_LIVE;
			setWorkingDirectory();
			setupTickSubscribers();
			setupBarSubscribers();
			loadTickFilesToCache();
			startPersistThreads();
			brokerageTradingService.start(brokerageServer, brokeragePort, brokerageClientId);
		}

	}
	
	private void loadTickFilesToCache(){
		long preLoadCacheStartTime = System.currentTimeMillis();
		System.out.println("STARTING TO SAVE BARS & TICKS TO CACHE");
		BarPublisher.setFillBarCacheOnlyNotPublish(true);
		TickDispatcher.setFillTickCacheAndDontDispatch(true);
		backtestTradingService.start(securityTestDataFiles);
		BarPublisher.setFillBarCacheOnlyNotPublish(false);
		TickDispatcher.setFillTickCacheAndDontDispatch(false);
		System.out.println("FINISHED PRELOADING BARS & TICKS TO CACHE, took: " 
				+ TimeFormatter.getTimeDifferenceString(System.currentTimeMillis() - preLoadCacheStartTime));
	}

	
	/**
	 * Ensures that the threads persisting data to the DB, as well as the thread sending
	 * external messages to Slack, are running. This should be called prior to starting 
	 * either the back testing or starting the connection to the broker.
	 */
	private void startPersistThreads() {
		tickPersistThread.start();
		barPersistThread.start();
		variablePersistThread.start();
		orderPersistThread.start();
		if (status.isLive()) {
			BrokerageAccount.startPositionPersistThread();
			slackMessageThread.start();
		} else if (status.isBacktest() && persistMode.persistForBacktest()) {
			BacktestAccount.startPositionPersistThread();
		}
	}

	/**
	 * Stops all of the threads that are running to persist data to the DB or to
	 * send messages to Slack.
	 */
	private void stopPersistThreads() {
		tickPersistThread.stopRunning();
		barPersistThread.stopRunning();
		variablePersistThread.stopRunning();
		orderPersistThread.stopRunning();
		if (status.isLive() && persistMode.persistForLive()) {
			BrokerageAccount.stopPersistThread();
			slackMessageThread.stopRunning();
		} else if (status.isBacktest() && persistMode.persistForBacktest()) {
			BacktestAccount.stopPersistThread();
		}
	}

	/**
	 * 
	 */
	public void stop() {
		throwExceptionIfNotLive();
		brokerageTradingService.stop();
		status = StrategyStatus.STOPPED;
		stopPersistThreads();
	}

	private void throwExceptionIfNotLive() {
		if (!status.isLive()) {
			throw new RuntimeException("Non-live strategies can't be stopped.");
		}
	}

	private void throwExceptionIfNotReadyToStart() {
		if (null == status) {
			throw new RuntimeException("Strategy must be initialized with super() in constructor.");
		} else if (status.isStarted()) {
			throw new RuntimeException("Strategy has already been started and/or completed.");
		}
	}

	private void throwExceptionIfNotRunning() {
		if (null == status) {
			throw new RuntimeException("Strategy must be initialized with super() in constructor.");
		} else if (!status.isRunning()) {
			throw new RuntimeException("Strategy must be started.");
		}
	}

	private void throwExceptionIfSecurityNotRegistered(SecurityKey securityKey) {
		if (null == securityKey) {
			throw new RuntimeException("Security is null.");
		}
		if (null == SecurityRegistry.get(securityKey)) {
			throw new RuntimeException("Security is null.");
		}
	}

	private void setWorkingDirectory() {
		if (defaultWorkingDirectory == null)
			setWorkingDirectory(getUserHomeLocation());
		setAnalyticalOutputUrlAndFile();
		setPropertiesFileOutputLocation();
		System.out.println("BASE DIRECTORY SET TO: " + defaultWorkingDirectory);
	}

	private String getUserHomeLocation() {
		String directory = System.getProperty("user.home")
				+ System.getProperty("file.separator")
				+ orgName
				+ System.getProperty("file.separator");
		return directory;
	}

	protected void setWorkingDirectory(String directoryLocation) {
		File f = new File(directoryLocation);
		if (!f.exists())
			f.mkdirs();
		defaultWorkingDirectory = directoryLocation;
	}

	public void submitOrder(Order order) {
		throwExceptionIfNotRunning();
		throwExceptionIfSecurityNotRegistered(order.getSecurityKey());
		getTradingService().getTrader().submitOrder(order);
		saveOrder(getTradingService().getTrader().getOrder(order.getKey()));
	}

	// TODO add a reason to all orders and simply have the method above
	// use the reason from the order object instead of having two seperate methods
	public void submitOrder(Order order, String orderReason) {
		throwExceptionIfNotRunning();
		throwExceptionIfSecurityNotRegistered(order.getSecurityKey());
		getTradingService().getTrader().submitOrder(order);
		saveOrder(getTradingService().getTrader().getOrder(order.getKey()));
		sendMessageToSlack(orderReason);
	}

	public void cancelOrder(OrderKey orderKey) {
		throwExceptionIfNotRunning();
		getTradingService().getTrader().cancelOrder(orderKey);
	}

	public List<Order> getOrders(SecurityKey securityKey) {
		return getTradingService().getTrader().getOrders(securityKey);
	}

	public Order getLastOrder(SecurityKey securityKey) {
		if (null != getTradingService().getTrader().getOrders(securityKey)) {
			int size = getTradingService().getTrader().getOrders(securityKey).size();
			return getTradingService().getTrader().getOrders(securityKey).get(size - 1);
		} else {
			return null;
		}
	}

	public <T extends Variable> T getVariables(T parameters, Date currentDateTime) {
		T variable = variableService.get(parameters, currentDateTime);
		saveVariable(variable);
		return variable;
	}

	@SuppressWarnings("unchecked")
	public <T extends Variable> List<T> getVariables(T parameters, Date currentDateTime, int count) {
		List<T> variables = variableService.get(parameters, currentDateTime, count);
		saveVariables((List<Variable>) variables);
		return variables;
	}

	// Added to be able to return lists where the number of items changes and is never known at any
	// given point in time such as TrendLines - there can be 0-n trend lines at one time
	@SuppressWarnings("unchecked")
	public <T extends Variable> List<T> getVariablesThatDontExpire(T parameters, Date currentDateTime) {
		List<T> variables = variableService.getValuesUnknownLength(parameters, currentDateTime);
		saveVariables((List<Variable>) variables);
		return variables;
	}

	public Account getAccount() {
		return getTradingService().getAccount();
	}

	/**
	 * 
	 * @return returns the analytics object that for the strategy. This analytics object can be used
	 * to retrieve the aggregate analytics, a list of specific position analytics or a single 
	 * position analytics if a security key is known. 
	 */
	public Analytics getAnalytics() {
		return analytics	;
	}

	/**
	 * 
	 * @return returns the trading service for the Strategy. If this is running live, it will 
	 * return the trading service that is connected to the broker. If this is running in back test,
	 * it will return the mocked up trading service that is specific for back testing. Methods and 
	 * attributes for both the live trading service and the back testing trading service are the same.
	 */
	private TradingService getTradingService() {
		if (status.isBacktest()) {
			return backtestTradingService;
		} else if (status.isLive()) {
			return brokerageTradingService;
		} else {
			throw new RuntimeException("StrategyStatus not set to either backtest or live.");
		}
	}

	/**
	 * The strategy status depicts the current state of the strategy (i.e. started, live,
	 * back test, etc.)
	 * Look at {@link com.g2m.services.strategybuilder.enums.StrategyStatus} for more 
	 * details on the Strategy Status
	 * @return returns the current StrategyStatus of the strategy.
	 */
	protected StrategyStatus getStrategyStatus() {
		return status;
	}

	/**
	 * The entity persist mode helps the strategy determine if objects should be persisted
	 * to the DB.
	 * Look at {@link com.g2m.services.strategybuilder.enums.EntityPersistMode} for more 
	 * details on the Entity Persist Modes
	 * @return returns the current EntityPersisMode of the strategy.
	 */
	protected EntityPersistMode getEntityPersistMode() {
		return persistMode;
	}

	/**
	 * Sets the entity persist mode of the strategy. This should be called from
	 * the {@link #run()} method that is implemented by the implemented strategy.
	 * If this method is never called, the default is to not persist data. <br> <br>
	 * NOTE - currently MongoDB is the DB that is 
	 * required in order for entities to be persisted.
	 * @param persistMode The EntityPersistMode that the implemented strategy should
	 * use. See {@link com.g2m.services.strategybuilder.enums.EntityPersistMode} for more 
	 * details on the Entity Persist Modes.
	 */
	protected void setEntityPersistMode(EntityPersistMode persistMode) {
		this.persistMode = persistMode;
	}

	/**
	 * This will check to see if the entity persist mode is one that allows objects to
	 * be persisted to the DB. If the persistence mode is one that does allow the object
	 * to be persisted, this tick will be stored in the DB. If it is not one that allows
	 * persistence, this method will not do anything.
	 * @param tick the tick to the persisted in the DB.
	 */
	private void saveTick(Tick tick) {
		if (areEntitiesPersisted()) {
			tickPersistThread.persist(tick);
		}
	}

	/**
	 * This will check to see if the entity persist mode is one that allows objects to
	 * be persisted to the DB. If the persistence mode is one that does allow the object
	 * to be persisted, this bar will be stored in the DB. If it is not one that allows
	 * persistence, this method will not do anything.
	 * @param bar the bar to the persisted in the DB.
	 */
	private void saveBar(Bar bar) {
		if (areEntitiesPersisted()) {
			barPersistThread.persist(bar);
		}
	}

	/**
	 * This will check to see if the entity persist mode is one that allows objects to
	 * be persisted to the DB. If the persistence mode is one that does allow the object
	 * to be persisted, this variable object will be stored in the DB. If it is not one 
	 * that allows persistence, this method will not do anything.
	 * @param variable the variable to the persisted in the DB.
	 */
	private void saveVariable(Variable variable) {
		if (areEntitiesPersisted()) {
			variablePersistThread.persist(variable);
		}
	}

	/**
	 * This will check to see if the entity persist mode is one that allows objects to
	 * be persisted to the DB. If the persistence mode is one that does allow the object
	 * to be persisted, this list of variables will be stored in the DB. If it is not one 
	 * that allows persistence, this method will not do anything.
	 * @param variables A list of variables to the persisted in the DB.
	 */
	private void saveVariables(List<Variable> variables) {
		if (areEntitiesPersisted()) {
			variablePersistThread.persist(variables);
		}
	}

	/**
	 * This function will check to see if the strategy is eligible to persist data
	 * and if so, it will add the order to the watch list wherein once it's state
	 * changes the order will be persisted in the DB.
	 * @param order The order to be saved to the DB.
	 */
	private void saveOrder(Order order) {
		if (areEntitiesPersisted()) {
			orderPersistThread.addOrderToWatchList(order);
			orderPersistThread.persistOrdersIfChanged();
		}
	}

	/**
	 * This method will check to see if the state of any orders have changed state since
	 * the prior time that this method ran and will persist any orders which have 
	 * changed state to the DB.
	 * <br>
	 * <br>
	 * TODO is this wokring??
	 */
	private void saveChangedOrders() {
		if (areEntitiesPersisted()) {
			orderPersistThread.persistOrdersIfChanged();
		}
	}

	/**
	 * This method provides the current state of whether entities are being persisted or not.
	 * @return returns true if the entity persist mode is registered to send events to the
	 * DB.
	 */
	private boolean areEntitiesPersisted() {
		if (status.isLive() && persistMode.persistForLive()) {
			return true;
		} else if (status.isBacktest() && persistMode.persistForBacktest()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * This method is called after the strategy has been initialized. It should be leveraged
	 * to setup any pre-strategy configurations or to create any objects / values required
	 * for the strategy to run. 
	 * <br><br>
	 * It should be implemented with @Override and the last method
	 * function called within this implementation in the stratgey should be either 
	 * {@link #startBacktest()} for back testing 
	 * or {@link #start(String, int, int)} for live trading.
	 */
	protected abstract void run();

	/**
	 * This method is called for each tick that is received for both live trading
	 * and in back testing. 
	 * <br><br>
	 * This is how ticks are sent to the strategy and this should be implemented
	 * by each strategy with @Override
	 */
	protected abstract void tickReceived(Tick tick);

	/**
	 * This method is called for each bar that is created for both live trading
	 * and in back testing.
	 * <br><br> 
	 * This is how bars are sent to the strategy and this should be implemented
	 * by each strategy with @Override
	 */
	protected abstract void barCreated(Bar bar);

	/**
	 * This method is called after the final tick from the final back testing file
	 * has been processed. 
	 * <br><br>
	 * It should be implemented by each strategy with @Override
	 */
	protected void backtestComplete() {
	}

	/**
	 * The TickAndBarSubscriber is responsible for publishing ticks and bars 
	 * to the Strategy that is registered, saving ticks to the tick cache,
	 * saving the bars to the bar cache, and sending ticks to the analytics
	 * package to update the drawdown / drawup for any open position.
	 *
	 */
	public class TickAndBarSubscriber implements TickSubscriber, BarSubscriber {
		@Override
		public void tickReceived(Tick tick) {
			//System.out.println("DISPATCHING LIVE TICK");
			saveTick(tick);
			analytics.updateFromTick(tick);
			Strategy.this.tickReceived(tick);
			// TODO Remove this hack (saving changed orders on every tick) when generic events are
			// implemented
			saveChangedOrders();
		}
		
		@Override
		public void tickReceivedFillCacheOnly(Tick tick){
			//System.out.println("RECEIVED TICK BUT NOT DISPATCHING");
			saveTick(tick);
		}
		
		@Override
		public void barCreated(Bar bar) {
			saveBar(bar);
			Strategy.this.barCreated(bar);
		}
		
		@Override
		public void barCreatedFillCacheOnly(Bar bar) {
			saveBar(bar);
			//System.out.println("FILLING CACHE ONLY " + bar);
		}
	}

	/**
	 * This method loads the historical values for non-expirable variables, such as 
	 * trend lines, into memory where it can then be processed and updated by
	 * the strategies.
	 * @deprecated nearly all forms of loading historical data from the DB are
	 * deprecated while a cleaner solution is designed. 
	 * @param parameters the variable parameters which will be used to query the database.
	 * These parameters should match the existing parameters in the DB.
	 */
	@Deprecated
	protected void getExistingNonExpirableVariables(Variable parameters) {
		variableService.getExistingVariablesFromDB(parameters);

	}

	/**
	 * This method converts the file name into a security and registers each security with the 
	 * associated file for back testing. Ticks from that file will then be sent into the strategy
	 * with the security that was created for that file name. <br>
	 * This method also registers ALL bar sizes for each security as a default. If the bar sizes 
	 * required for the strategy are known, it is highly suggested that 
	 * {@link #registerSecuritiesFromFile(List, String) is used 
	 * @param fileNames A file name to be converted into securities and to have historical
	 * tick data registered with the strategy.
	 */
	protected void registerSecuritiesFromFile(String fileName) {

		List<Security> securities = SecurityFileReader.getSecuritiesFromFile(fileName);
		for (Security security : securities) {
			SecurityRegistry.add(security);
			// Create a new bar trader for each security
			barTraders.put(security.getKey(), new BarTrader());

			// We use the register all Securities method to return the list of all securities.
			// Since we build larger bars from smaller bars there is minimal additional ovehead
			// to capture all of the bars versus a subset of bars. We can then refer to any
			// bar size we want in the strategies.
			for (BarSize barSize : BarSize.registerAllSecurities(true)) {
				subscribeToBars(security, barSize);
			}
			subscribeToTicks(security);
		}
	}


	/**
	 * This method converts the file name into a security and registers the security with the 
	 * associated file for back testing. Ticks from that file will then be sent into the strategy
	 * with the security that was created for that file name. <br>
	 * This method also registers a list of bar sizes for each security. For instance, if the 
	 * list of bar sizes contains the bar sizes of 10, 15 and 30 seconds, each security will
	 * have those three bar sizes registered.
	 * @param barSizes A list of Bar Sizes that should be registered for each security. <br>
	 * Note - The less bar sizes that are required, the faster the strategy will be and the faster
	 * the back test will be. 
	 * @param fileName A single file name to be converted into a security and to have historical
	 * tick data registered with the strategy.
	 */
	protected void registerSecuritiesFromFile(List<BarSize> allBarSizes, String fileName){
		List<Security> securities = SecurityFileReader.getSecuritiesFromFile(fileName);
		registerSecuritiesAndBarSizes(allBarSizes, securities);
	}
	
	/**
	 * This method registers each security with the associated bars and for ticks. This is used for
	 * both back testing when creating securities from files, as well as production when creating
	 * securities from a CSV or from creating securities from tick files. <br>
	 * @param barSizes A list of Bar Sizes that should be registered for each security. <br>
	 * Note - The less bar sizes that are required, the faster the strategy will be and the faster
	 * the back test will be. 
	 * @param fileName A single file name to be converted into a security and to have historical
	 * tick data registered with the strategy.
	 */
	private void registerSecuritiesAndBarSizes(List<BarSize> allBarSizes, List<Security> securities) {

		// Register the bar sizes that are set with the strategy so that we
		// don't create unused bar sizes and take up too much memory
		List<BarSize> updatedBarSizes = BarSize.registerSpecificBarSize(allBarSizes);
		for (Security security : securities) {
			System.out.println("SUBSCRIBING TO: "
					+ security.getSymbol()
					+ "."
					+ security.getCurrency()
					+ " FOR TICKS AND BARS: "
					+ updatedBarSizes.toString());

			SecurityRegistry.add(security);
			barTraders.put(security.getKey(), new BarTrader());

			// We use the register all Securities method to return the list of all securities.
			// Since we build larger bars from smaller bars there is minimal additional ovehead
			// to capture all of the bars versus a subset of bars. We can then refer to any
			// bar size we want in the strategies.
			for (BarSize barSize : updatedBarSizes) {
				subscribeToBars(security, barSize);
			}
			subscribeToTicks(security);
		}
	}
	
	/**
	 * This method converts the file name into a security and registers each security with the 
	 * associated file for back testing. Ticks from that file will then be sent into the strategy
	 * with the security that was created for that file name. <br>
	 * This method also registers ALL bar sizes for each security as a default. If the bar sizes 
	 * required for the strategy are known, it is highly suggested that 
	 * {@link #registerSecuritiesFromFileBackTest(List, List)} is used 
	 * @param fileNames A list of file names to be converted into securities and to have historical
	 * tick data registered with the strategy.
	 */
	protected void registerSecuritiesFromFileBackTest(List<String> fileNames) {

		List<File> files = getFilesForTesting(fileNames);
		List<Security> securities = SecurityFileReader.createSecuritiesFromFileName(files);

		for (int i = 0; i < securities.size(); i++) {
			addTestFile(securities.get(i), files.get(i).getAbsolutePath());

			barTraders.put(securities.get(i).getKey(), new BarTrader());
			// We use the register all Securities method to return the list of all securities.
			// Since we build larger bars from smaller bars there is minimal additional ovehead
			// to capture all of the bars versus a subset of bars. We can then refer to any
			// bar size we want in the strategies.

			// If we're using real tick data for historical testing we will use all
			// bar sizes and not a minimum bar size of 1Min. We can tell because the historical
			// data that we've created from the tick data ends in "-TICK"
			if (files.get(i).getAbsolutePath().contains("-TICK")) {
				for (BarSize barSize : BarSize.registerAllSecurities(true)) {
					subscribeToBars(securities.get(i), barSize);
				}
			} else {
				for (BarSize barSize : BarSize.registerAllSecurities(false)) {
					subscribeToBars(securities.get(i), barSize);
				}
			}
			subscribeToTicks(securities.get(i));
		}
	}
	

	/**
	 * This method converts the file names into securities and registers each security with the 
	 * associated file for back testing. Ticks from that file will then be sent into the strategy
	 * with the security that was created for that file name. <br>
	 * This method also registers a list of bar sizes for each security. For instance, if the 
	 * list of bar sizes contains the bar sizes of 10, 15 and 30 seconds, each security will
	 * have those three bar sizes registered.
	 * @param barSizes A list of Bar Sizes that should be registered for each security. <br>
	 * Note - The less bar sizes that are required, the faster the strategy will be and the faster
	 * the back test will be. 
	 * @param fileNames A list of file names to be converted into securities and to have historical
	 * tick data registered with the strategy.
	 */
	protected void registerSecuritiesFromFileBackTest(List<BarSize> barSizes, List<String> fileNames) {

		List<File> files = getFilesForTesting(fileNames);
		List<Security> securities = SecurityFileReader.createSecuritiesFromFileName(files);
		
		for (int i = 0; i < securities.size(); i++) 
			addTestFile(securities.get(i), files.get(i).getAbsolutePath());
			
		registerSecuritiesAndBarSizes(barSizes, securities);
	}

	/**
	 * This method converts the file names into securities and registers each security with the 
	 * associated file for back testing. Ticks from that file will then be sent into the strategy
	 * with the security that was created for that file name. <br>
	 * This method also registers a list of bar sizes for each security. For instance, if the 
	 * list of bar sizes contains the bar sizes of 10, 15 and 30 seconds, each security will
	 * have those three bar sizes registered.
	 * @param barSizes A list of Bar Sizes that should be registered for each security. <br>
	 * Note - The less bar sizes that are required, the faster the strategy will be and the faster
	 * the back test will be. 
	 * @param fileNames A list of file names to be converted into securities and to have historical
	 * tick data registered with the strategy.
	 */
	protected void registerSecuritiesAndPreLoadCache(List<BarSize> barSizes, List<String> fileNames) {

		List<File> files = getFilesForTesting(fileNames);
		List<Security> securities = SecurityFileReader.createSecuritiesFromFileName(files);
		registerSecuritiesAndBarSizes(barSizes, securities);
		
		// Register the bar sizes that are set with the strategy so that we
		// don't create unused bar sizes and take up too much memory
		for (int i = 0; i < securities.size(); i++) {
			addTestFile(securities.get(i), files.get(i).getAbsolutePath());
		}
	}
	
	/**
	 * This will retrieve all of the files a list that can be used
	 * for historical testing. This does NOT check the data inside the files
	 * and therefore all files in the specified list must be
	 * fit for back testing.
	 * @param fileNames A list of potential file names or directories in which
	 * to bring into testing. If a file name within the list is a directory, 
	 * then all files within that directory are retrieved as well (this method
	 * does not search further than the one level of the directory provided).
	 * @return Returns a list of files.
	 */
	private List<File> getFilesForTesting(List<String> fileNames) {
		// If the first entry in the list is a directory, we assume that a folder has
		// been provided and all of the files needed for the back test are in the folder
		// If it is a directory, we go and create the list of file names, otherwise,
		// we just pass along the list of file names that we were provided

		List<File> files;
		File folder = new File(fileNames.get(0));
		if (!folder.isDirectory()) {
			files = SecurityFilePathCreator.getFilesFromPaths(fileNames);
		} else {
			ArrayList<String> newFileNames = new ArrayList<String>();
			for (String s : folder.list()) {
				// TODO Need to change this so its not hardcoded
				if (!s.equals(".DS_Store"))
					newFileNames.add(fileNames.get(0) + "/" + s);
			}
			files = SecurityFilePathCreator.getFilesFromPaths(newFileNames);
		}
		return files;
	}

	/**
	 * This will retrieve bars from Interactive Brokers leveraging the historical data
	 * loader and will put the bars into the cache.
	 * @deprecated While this method has shown to work, there are other reasons why this 
	 * is being deprecated such as inaccurate data from IB, it is nearly impossible to 
	 * query IB and to then to put the bars into the cache just before new bars are created
	 * to prevent a gap in the bars, and IB limits the amount of bars that can be requested 
	 * at a time so scaling this is nearly impossible.
	 * @param security The security that the request is for
	 * @param barSize The bar size that is being retrieved
	 */
	@Deprecated
	protected void preloadBarsIntoCacheFromIB(Security security, BarSize barSize) {
		int barCount = 40;
		try {
			// loads historical bars into the bar cache
			cacheLoader.setHost("localhost");
			cacheLoader.setPort(4001);
			cacheLoader.setClientId(13);
			cacheLoader.loadBarsFromIB(security, new Date(), barSize, barCount);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Not currently used. 
	 * TODO need to see if this still works based off of changes since this was implemented
	 * @deprecated This likely does not work, needs to be re-evaluated and likely removed.
	 * Bars should be put into the Cache by rerunning ticks through the system.
	 * @param security Security for which the bars will be retrieved
	 * @param barSize the bar size that should be returned for the request. (i.e. 30 seconds,
	 * 1 hour, 1 day, etc.) See {@link com.g2m.services.tradingservices.enums.BarSize}
	 */
	@Deprecated
	protected void preloadBarsIntoCacheFromDB(Security security, BarSize barSize) {

		// TODO add ability to put the correct date in, then make sure we only select greater than the date.
		// TODO retest this functionality
		try {
			cacheLoader.loadBarsFromDB(security, barSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * This method returns true if the tick has crossed the time that is set
	 * as the end of the day. <br> <br>
	 * Currently, for this implementation, the end of the week is 3:59pm and 52 seconds
	 * each afternoons, 8 seconds before stocks close and 1 hour and 8 seconds before 
	 * Forex markets close (Forex markets only close between 5-5:15pm each day. <br><br>
	 * TODO this needs to be updated so that the time can be changed dynamically.<br><br>
	 * TODO need to update this or to create another method to check for the beginning of the
	 * day.
	 * @param tick the tick used to retrive the current date / time that is in turn
	 * used to compare if it has crossed the 3:59:52pm threshold on a give day.
	 * @return returns true if the tick is after 3:59:52pm threshold for a day  
	 * (and also returns true if the tick is earlier than 6pm Sunday afternoons). 
	 */
	protected boolean isEndOfDay(Tick tick) {

		if (status == StrategyStatus.STARTED_LIVE) {
			// tick time % milliOneDay = 72000000 at 4pm EST,
			// we will close the position ~8 seconds before close
			if (tick.getDateTime().getTime() % milliOneDay >= 71992000)
				return true;
		} else if (status == StrategyStatus.NOT_STARTED ||
				status == StrategyStatus.STARTED_BACKTEST ||
				status == StrategyStatus.STOPPED) {
			// in historical mode the last time stamp is at 3:59pm each day
			if (tick.getDateTime().getTime() % milliOneDay >= 71940000)
				return true;
		} else
			return false;

		return false;
	}

	/**
	 * This method returns true if the tick has crossed the time that is set
	 * as the end of the week. <br> <br>
	 * Currently, for this implementation, the end of the week is 3:59pm and 52 seconds
	 * Friday afternoons, 8 seconds before stocks close and 1 hour and 8 seconds before 
	 * Forex markets close.<br><br>
	 * TODO this needs to be updated so that the time can be changed dynamically.
	 * @param tick the tick used to retrive the current date / time that is in turn
	 * used to compare if it has crossed the 3:59:52pm threshold on a Friday afternoon.
	 * @return returns true if the tick is after 3:59:52pm threshold on a Friday afternoon 
	 * (and also returns true if the tick is earlier than 6pm Sunday afternoons). 
	 */
	protected boolean isEndOfWeek(Tick tick) {

		if (status == StrategyStatus.STARTED_LIVE) {
			// we will close the position at 3:59pm and 52 seconds, ~8 seconds before 4pm
			// and we will allow trading to resume at 6pm Sunday
			// 158400000 is 4pm during non-daylight savings time
			// 162000000 is 4pm during daylight savings time
			if (tick.getDateTime().getTime() % (milliOneDay * 7) >= 162000000 // 158400000 is 4pm
					&& tick.getDateTime().getTime() % (milliOneDay * 7) <= 338400000)
				return true;
		} else if (status == StrategyStatus.NOT_STARTED ||
				status == StrategyStatus.STARTED_BACKTEST ||
				status == StrategyStatus.STOPPED) {
			// in historical mode the last time stamp is at 3:59pm each day
			if (tick.getDateTime().getTime() % (milliOneDay * 7) >= 158340000 // 158400000 is 4pm
					&& tick.getDateTime().getTime() % (milliOneDay * 7) <= 338400000)
				return true;
		} else
			return false;

		return false;
	}

	/**
	 * This method returns true if a position exists, regardless of whether the position
	 * is short or long. If the quantity for the given security key <> 0, 
	 * this returns true for.
	 * @param tick the tick used to retrieve the security key 
	 * @return returns true if the quantity for a given security key <> 0
	 */
	protected boolean positionOpen(Tick tick) {
		if (getAccount().getPositions().containsKey(tick.getSecurity().getKey())) {
			return getAccount().getPositions().get(tick.getSecurity().getKey()).isOpen();
		} else
			return false;
	}

	/**
	 * This method returns true if a position exists, regardless of whether the position
	 * is short or long. If the quantity for the given security key <> 0, 
	 * this returns true for.
	 * @param bar the bar used to retrieve the security key 
	 * @return returns true if the quantity for a given security key <> 0
	 */
	protected boolean positionOpen(Bar bar) {
		if (getAccount().getPositions().containsKey(bar.getSecurity().getKey())) {
			return getAccount().getPositions().get(bar.getSecurity().getKey()).isOpen();
		} else
			return false;
	}

	/**
	 * This method returns whether whether or not a long position exists for a given security key. 
	 * @param key the security key used to identify if a unique security currently has a long
	 * position open
	 * @return true if a long position exists, otherwise false
	 */
	protected boolean longPositionExists(SecurityKey key) {
		if (getAccount().getPositions().containsKey(key)) {
			if (getAccount().getPositions().get(key).getQuantity() > 0)
				return true;
			else
				return false;
		} else
			return false;
	}

	/**
	 * This method returns whether whether or not a long position exists for a given tick. 
	 * Internally this calls {@link #longPositionExists(SecurityKey)} with the 
	 * tick.getSecurity().getSecurityKey()
	 * @param tick the tick used to get the security key
	 * @return true if a long position exists, otherwise false
	 */
	protected boolean longPositionExists(Tick tick) {
		return longPositionExists(tick.getSecurity().getKey());
	}

	/**
	 * This method returns whether whether or not a long position exists for a given bar. 
	 * Internally this calls {@link #longPositionExists(SecurityKey)} with the 
	 * bar.getSecurity().getSecurityKey()
	 * @param bar the bar used to get the security key
	 * @return true if a long position exists, otherwise false
	 */
	protected boolean longPositionExists(Bar bar) {
		return longPositionExists(bar.getSecurity().getKey());
	}

	/**
	 * This method returns whether whether or not a short position exists for a given security key. 
	 * @param key the security key used to identify if a unique security currently has a short
	 * position open
	 * @return true if a short position exists, otherwise false
	 */
	protected boolean shortPositionExists(SecurityKey key) {
		if (getAccount().getPositions().containsKey(key)) {
			if (getAccount().getPositions().get(key).getQuantity() < 0)
				return true;
			else
				return false;
		} else
			return false;
	}

	/**
	 * This method returns whether whether or not a short position exists for a given tick. 
	 * Internally this calls {@link #shortPositionExists(SecurityKey)} with the 
	 * tick.getSecurity().getSecurityKey()
	 * @param tick the tick used to get the security key
	 * @return true if a short position exists, otherwise false
	 */
	protected boolean shortPositionExists(Tick tick) {
		return shortPositionExists(tick.getSecurity().getKey());
	}

	/**
	 * This method returns whether whether or not a short position exists for a given bar. 
	 * Internally this calls {@link #shortPositionExists(SecurityKey)} with the 
	 * bar.getSecurity().getSecurityKey()
	 * @param bar the bar used to get the security key
	 * @return true if a short position exists, otherwise false
	 */
	protected boolean shortPositionExists(Bar bar) {
		return shortPositionExists(bar.getSecurity().getKey());
	}

	/**
	 * This returns the unrealized percent of profit for a given security associated to a tick. 
	 * The security must have and open position for this to return a value other than 0.0; <br> <br>
	 * Since the tick always has the most recent price (unlike bars), this will return the 
	 * unrealized percent based off of the current tick price, not what the last tick in the tick
	 * cache has.
	 * @param tick the tick that contains security key that is used to identify the unique security
	 * as well as the current price. 
	 * for the open position
	 * @return this returns the unaltered % profit, it has not been multiplied by 100. 
	 * Example response for 1.5% would be .0015
	 */
	protected double getUnrealizedPercent(Tick tick) {
		SecurityKey key = tick.getSecurity().getKey();
		if (shortPositionExists(key))
			return (getAccount().getPositions().get(key).getOpenPrice() - tick.getLastPrice())
					/ getAccount().getPositions().get(key).getOpenPrice();
		else
			return (tick.getLastPrice() - getAccount().getPositions().get(key).getOpenPrice())
					/ getAccount().getPositions().get(key).getOpenPrice();

	}

	/**
	 * This returns the unrealized percent of profit for a given security associated to a bar. 
	 * The security must have and open position for this to return a value other than 0.0; <br> <br>
	 * Internally, this calls the {@link # getUnrealizedPercent(SecurityKey)} method
	 * @param bar the bar that contains security key that is used to identify the unique security 
	 * for the open position
	 * @return this returns the unaltered % profit, it has not been multiplied by 100. 
	 * Example response for 1.5% would be .0015
	 */
	protected double getUnrealizedPercent(Bar bar) {
		return getUnrealizedPercent(bar.getSecurity().getKey());
	}

	/**
	 * This returns the unrealized percent of profit for a given security. The security must have
	 * and open position for this to return a value other than 0.0;
	 * @param key the security key that is used to identify the unique security 
	 * for the open position
	 * @return this returns the unaltered % profit, it has not been multiplied by 100. 
	 * Example response for 1.5% would be .0015
	 */
	private double getUnrealizedPercent(SecurityKey key) {
		if (shortPositionExists(key))
			return (getAccount().getPositions().get(key).getOpenPrice() - getTickCache().getLastTickPrice(key))
					/ getAccount().getPositions().get(key).getOpenPrice();
		else if (longPositionExists(key))
			return (getTickCache().getLastTickPrice(key) - getAccount().getPositions().get(key).getOpenPrice())
					/ getAccount().getPositions().get(key).getOpenPrice();
		else 
			return 0.0;
	}

	/**
	 *  This function will place an order at the current market price. It does not guarantee
	 * that the order will be filled at any specified price.<br> <br>
	 * Internally it calls {@link #placeMarketOrder(Tick, int, OrderAction, String)} 
	 * with a default value for the reason that is passed
	 * @param tick the tick that came through which triggered the order to be placed. This is 
	 * used to get the security for the order
	 * @param quantity the quantity for the position
	 * @param orderAction the order action to be used, should be OrderAction.BUY or 
	 * OrderAction.SELL
	 */
	protected void placeMarketOrder(Tick tick, int quantity, OrderAction orderAction) {
		String reason;
		if (OrderAction.BUY.equals(orderAction)) {
			reason = "PLACING MARKET ORDER - LONG - for NO REASON";
		} else {
			reason = "PLACING MARKET ORDER - SELL - for NO REASON";
		}
		placeMarketOrder(tick, quantity, orderAction, reason);
	}
	/**
	 * This function will place an order at the current market price. It does not guarantee
	 * that the order will be filled at any specified price.<br> <br>
	 * Internally, this method will update the last trade time for the associated bar trader 
	 * {@link BarTrader#setLastTradeTime(long)} and it will also submit the order
	 * via {@link #submitOrder(Order, String)} 
	 * @param tick the tick that came through which triggered the order to be placed. This is 
	 * used to get the security for the order
	 * @param quantity the quantity for the position
	 * @param orderAction the order action to be used, should be OrderAction.BUY or 
	 * OrderAction.SELL
	 * @param reason a string for the reason why the trade is being placed.
	 */
	protected void placeMarketOrder(Tick tick, int quantity, OrderAction orderAction, String reason) {
		Order order;
		if (OrderAction.BUY.equals(orderAction)) {
			reason = "LONG - " + reason;
			order = new MarketOrder(OrderAction.BUY, quantity, tick.getSecurity());
		} else {
			reason = "SHORT - " + reason;
			order = new MarketOrder(OrderAction.SELL, quantity, tick.getSecurity());
		}

		reason += (" - WITH QUANTITY: " + quantity);

		StringBuilder sb = new StringBuilder();
		sb.append(tick.getDateTime());
		sb.append(" -- " );
		sb.append(reason);
		sb.append(" @ ");
		sb.append(tick.getLastPrice());
		sb.append(" -- ");
		sb.append(tick.getSecurity().getSymbol()); 
		sb.append(".");
		sb.append(tick.getSecurity().getCurrency());

		System.out.println(sb.toString());

		getBarTrader(tick.getSecurity().getKey()).setLastTradeTime(tick.getDateTime().getTime());
		submitOrder(order, sb.toString());
	}

	/**
	 *  This function will place an order at the current market price. It does not guarantee
	 * that the order will be filled at any specified price.<br> <br>
	 * Internally it calls {@link #placeMarketOrder(Bar, int, OrderAction, String)} 
	 * with a default value for the reason that is passed
	 * @param bar the bar that came through which triggered the order to be placed. This is 
	 * used to get the security for the order
	 * @param quantity the quantity for the position
	 * @param orderAction the order action to be used, should be OrderAction.BUY or 
	 * OrderAction.SELL
	 */
	protected void placeMarketOrder(Bar bar, int quantity, OrderAction orderAction) {
		String reason;
		if (OrderAction.BUY.equals(orderAction)) {
			reason = "PLACING MARKET ORDER - LONG - for NO REASON";
		} else {
			reason = "PLACING MARKET ORDER - SELL - for NO REASON";
		}
		placeMarketOrder(bar, quantity, orderAction, reason);
	}

	/**
	 * This function will place an order at the current market price. It does not guarantee
	 * that the order will be filled at any specified price.<br> <br>
	 * Internally, this method will update the last trade time for the associated bar trader 
	 * {@link BarTrader#setLastTradeTime(long)} and it will also submit the order
	 * via {@link #submitOrder(Order, String)} 
	 * @param bar the bar that came through which triggered the order to be placed. This is 
	 * used to get the security for the order
	 * @param quantity the quantity for the position
	 * @param orderAction the order action to be used, should be OrderAction.BUY or 
	 * OrderAction.SELL
	 * @param reason a string for the reason why the trade is being placed.
	 */
	protected void placeMarketOrder(Bar bar, int quantity, OrderAction orderAction, String reason) {
		Order order;
		if (OrderAction.BUY.equals(orderAction)) {
			reason = "LONG - " + reason;
			order = new MarketOrder(OrderAction.BUY, quantity, bar.getSecurity());
		} else {
			reason = "SHORT - " + reason;
			order = new MarketOrder(OrderAction.SELL, quantity, bar.getSecurity());
		}
		reason += (" - WITH QUANTITY: " + quantity);

		StringBuilder sb = new StringBuilder();
		sb.append(bar.getDateTime());
		sb.append(" -- " );
		sb.append(reason);
		sb.append(" @ ");
		sb.append(bar.getClose());
		sb.append(" -- ");
		sb.append(bar.getSecurity().getSymbol()); 
		sb.append(".");
		sb.append(bar.getSecurity().getCurrency());

		System.out.println(sb.toString());

		getBarTrader(bar.getSecurity().getKey()).setLastTradeTime(bar.getDateTime().getTime());
		submitOrder(order, sb.toString());
	}

	/**
	 * This method creates a long value that is equivelant to the amount of milliseconds
	 * that have passed in a day at the specified input time.<br><br>
	 * Example: an input of "02:00" (2am) would be processed as:<br>
	 * (2 hours * 60 minutes * 60 seconds * 1000 milliseconds) = 7200000
	 * @param militaryTime this is a string representation of time that 
	 * must be provided in "HH:MM" format
	 * @return the total number of milliseconds between midnight and the specified input
	 * time for a single day.
	 */
	protected long setTradeCutoffTime(String militaryTime) {
		long tradeCutoffTime;

		String hours = militaryTime.substring(0, militaryTime.indexOf(":"));
		String minutes = militaryTime.substring(militaryTime.indexOf(":") + 1);

		long hourVal = Long.valueOf(hours) * 60 * 60 * 1000;
		long minuteVal = Long.valueOf(minutes) * 60 * 1000;

		tradeCutoffTime = minuteVal + hourVal;

		return tradeCutoffTime;
	}

	/**
	 * Retrieves a bar trader for a given security.
	 * @param key the security key for a unique security.
	 * @return returns the bar trader for the given security.
	 */
	protected BarTrader getBarTrader(SecurityKey key) {
		return barTraders.get(key);
	}


	/**
	 * This method adds a new bar size that should be tracked to a specific bar trader.<br><br>
	 * This can be configured directly from the strategy but the current implementation has
	 * this automatically register all bar sizes that a strategy uses.<br> <br>
	 * The bar trader must already exist for a security.
	 * @param key the security key that is used to retrieve the bar trader for the unique security.
	 * @param barSize the bar size that is to be added for the bar trader to track.
	 */
	protected void addBarTrader(SecurityKey key, BarSize barSize) {
		getBarTrader(key).addBarSize(barSize);
	}

	/**
	 * This method closes all positions without requiring a reason to be provided.<br> <br>
	 * Internally it calls {@link #closeAllPositions(String)} with a default value for the string.
	 */
	protected void closeAllPositions() {
		closeAllPositions("CLOSING ALL POSITIONS");
	}

	/**
	 * This method closes all positions. <br> <br>
	 * Internally, it calls {@link #placeMarketOrder(Tick, int, OrderAction, String)}
	 * for each security key that has a quantity <> 0 
	 * @param reason the reason associated to each order when it is placed.
	 */
	protected void closeAllPositions(String reason) {
		Set<SecurityKey> keys = getAccount().getPositions().keySet();
		for (SecurityKey key : keys) {
			int quantity = 0;
			if (getAccount().getPositions().containsKey(key))
				quantity = getAccount().getPositions().get(key).getQuantity();
			if (quantity > 0) {
				placeMarketOrder(getTickCache().getLastTick(key), quantity, OrderAction.SELL, reason);
			} else if (quantity < 0) {
				placeMarketOrder(getTickCache().getLastTick(key), -quantity, OrderAction.BUY, reason);
			}
		}
	}

	/**
	 * Closes the position related to the security key for the given bar. This will
	 * close both long and short positions. <br> <br>
	 * Internally this calls {@link #closePosition(Bar, String)} to close the position 
	 * and provides the as reason of "NO REAOSON" as the string.<br>
	 * @param bar the bar used to get the security key required to retrieve the 
	 * quantity for the open position.
	 */
	protected void closePosition(Bar bar){
		closePosition(bar.getSecurity().getKey(), "CLOSING POSITION WITH NO REASON");
	}

	/**
	 * Closes the position related to the security key for the given tick. This will
	 * close both long and short positions. <br> <br>
	 * Internally this calls {@link #closePosition(Tick, String)} to close the position 
	 * and provides the as reason of "NO REAOSON" as the string.<br>
	 * @param tick the bar used to get the security key required to retrieve the 
	 * quantity for the open position.
	 */
	protected void closePosition(Tick tick){
		closePosition(tick.getSecurity().getKey(), "CLOSING POSITION WITH NO REASON");
	}

	/**
	 * This method closes the entire position for a given security key. When called, it
	 * will retrieve the quantity open for the position (can be positive or negative) and 
	 * will place a market order for the opposite value. <br> <br>
	 * Internally this calls the {@link #placeMarketOrder(Tick, int, OrderAction, String)} 
	 * method.
	 * Example: if open position is -400, this will place an order for 400 
	 * for the same security.
	 * @param key the security key for the unique security for which the position will be closed.
	 * @param reason the reason the position is being closed.
	 */
	private void closePosition(SecurityKey key, String reason) {
		int quantity = getAccount().getPositions().get(key).getQuantity();
		if (quantity > 0) {
			placeMarketOrder(getTickCache().getLastTick(key), quantity, OrderAction.SELL, reason);
		} else if (quantity < 0) {
			placeMarketOrder(getTickCache().getLastTick(key), -quantity, OrderAction.BUY, reason);
		}
	}

	/**
	 * Closes the position related to the security key for the given bar. This will
	 * close both long and short positions. <br> <br>
	 * Internally this calls {@link #closePosition(SecurityKey, String)} to close the position.<br>
	 * @param bar the bar used to get the security key required to retrieve the 
	 * quantity for the open position.
	 * @param reason the reason why the trade is being closed.
	 */
	protected void closePosition(Bar bar, String reason) {
		closePosition(bar.getSecurity().getKey(), reason);
	}

	/**
	 * Closes the position related to the security key for the given tick. This will
	 * close both long and short positions. <br> <br>
	 * Internally this calls {@link #closePosition(SecurityKey, String)} to close the position.<br>
	 * @param tick the tick used to get the security key required to retrieve the 
	 * quantity for the open position.
	 * @param reason the reason why the trade is being closed.
	 */
	protected void closePosition(Tick tick, String reason) {
		closePosition(tick.getSecurity().getKey(), reason);
	}

	/**
	 * This method determines if the most recent trade placed was a win or a loss by comparing
	 * the open price of the position to the most recent tick price from the tick cache. <br> <br>
	 * This MUST be called while the position is still open and should be called immediately 
	 * before the position is closed. <br> <br>
	 * @param key the security key used to get the last tick price from the tick cache.
	 * @return returns true if the trade was a win, false if the trade was a loss or
	 * no profit
	 */
	protected boolean tradeWasAWin(SecurityKey key) {
		if (longPositionExists(key)) {
			if (tickCache.getLastTickPrice(key) > getAccount().getPositions().get(key).getOpenPrice())
				return true;
		} else if (shortPositionExists(key)) {
			if (tickCache.getLastTickPrice(key) <= getAccount().getPositions().get(key).getOpenPrice())
				return true;
		}
		return false;
	}

	/**
	 * This method determines if the most recent trade placed was a win or a loss by comparing
	 * the open price of the position to the most recent tick price from the tick cache. <br> <br>
	 * Internally this calls {@link #tradeWasAWin(SecurityKey)} with the bar security key.<br> <br>
	 * This MUST be called while the position is still open and should be called immediately 
	 * before the position is closed. <br>
	 * @param bar the tick used to determine when to close the position.
	 * @return returns true if the trade was a win, false if the trade was a loss or
	 * no profit
	 */
	protected boolean tradeWasAWin(Bar bar) {
		return tradeWasAWin(bar.getSecurity().getKey());
	}

	/**
	 * This method determines if the most recent trade placed was a win or a loss by comparing
	 * the open price of the position to the most recent tick price from the tick cache. <br> <br>
	 * Internally this calls {@link #tradeWasAWin(SecurityKey)} with the tick security key.<br> <br>
	 * This MUST be called while the position is still open and should be called immediately 
	 * before the position is closed. <br>
	 * @param tick the tick used to determine when to close the position.
	 * @return returns true if the trade was a win, false if the trade was a loss or
	 * no profit
	 */
	protected boolean tradeWasAWin(Tick tick) {
		return tradeWasAWin(tick.getSecurity().getKey());
	}

	private void setAnalyticalOutputUrlAndFile() {
		baseCurrentStrategyDirectory = defaultWorkingDirectory
				+ "Files"
				+ System.getProperty("file.separator")
				+ status.toString().substring(status.toString().lastIndexOf("_") + 1)
				+ System.getProperty("file.separator")
				+ strategyName
				+ System.getProperty("file.separator")
				+ sdf.format(System.currentTimeMillis())
				+ System.getProperty("file.separator");
		
		String analyticsDirectory = baseCurrentStrategyDirectory
				+ "AnalyticalOutput"
				+ System.getProperty("file.separator");

		File f = new File(analyticsDirectory);
		if (!f.exists())
			f.mkdirs();

		getAnalytics().getAggregateAnalytics().setOutputUrlAndFile(analyticsDirectory);
	}

	/**
	 * Sets the absolute path for the properties file to be written to. This 
	 * leverages the working directory that has been set by the strategy or 
	 * the default working directory as the root and creates the appropriate
	 * structure for the strategy that is running based off of whether it is
	 * running LIVE or BACKTEST. 
	 */
	private void setPropertiesFileOutputLocation() {

		propertyHandler.setStrategyName(strategyName);

		String propsFileDirectory = defaultWorkingDirectory
				+ "Files"
				+ System.getProperty("file.separator")
				+ status.toString().substring(status.toString().lastIndexOf("_") + 1)
				+ System.getProperty("file.separator")
				+ strategyName
				+ System.getProperty("file.separator")
				+ sdf.format(System.currentTimeMillis())
				+ System.getProperty("file.separator")
				+ "PropertyFiles"
				+ System.getProperty("file.separator");

		File f = new File(propsFileDirectory);
		if (!f.exists())
			f.mkdirs();

		propertyHandler.setPropertyFileOutputLocation((propsFileDirectory));
	}

	/**
	 * Writes the current properties being used in a strategy to a file. All properties
	 * are pulled from the variables in the strategy provided and written to the given location.
	 * @param strat the strategy that will have all of it's global variables written to
	 * a properties file. The currently the strategy MUST have getter methods for 
	 * all global variables. 
	 * @param propsFileLocation location where the file will be stored. Should be 
	 * provided as a directory.
	 */
	protected void setPropertiesInFile(Strategy strat, String propsFileLocation) {
		propertyHandler.writeStrategyPropertiesToFile(strat, propsFileLocation);
	}

	/**
	 * Sets the properties for the strategies. Currently, in order for a property
	 * in the property file to be set within the strategy, the strategy must 1) 
	 * contain a variable with the same name as the property and 2) contain getter
	 * and setter methods for the variable. <br> <br>
	 * This method uses the property file location provided in order to 
	 * retrieve the properties from the file as to set the properties for the stratgey.  
	 * 
	 * @param strat A strategy object where the properties are being set. Typically, the 
	 * strategy calls this method and passes 'this'
	 * @param propsFileLocation the location of the property file to be used. 
	 * Must be in the form of a directory or in the resources folder.
	 */
	protected void setStrategyProperties(Strategy strat, String propsFileLocation) {
		propertyHandler.setStrategyPropertiesFromFile(strat, propsFileLocation);
	}

	/**
	 * Sets the properties for the strategies. Currently, in order for a property
	 * in the property file to be set within the strategy, the strategy must 1) 
	 * contain a variable with the same name as the property and 2) contain getter
	 * and setter methods for the variable. <br> <br>
	 * This method uses the initial properties file as the default for setting the properties.  
	 * 
	 * @param strat A strategy object where the properties are being set. Typically, the 
	 * strategy calls this method and passes 'this'
	 */
	protected void setStrategyProperties(Strategy strat) {
		propertyHandler.setStrategyPropertiesFromFile(strat);
	}

	/**
	 * This method returns a set of properties from a file when the file location 
	 * is specified.
	 * @param propsFileLocation the location of the properties file
	 * @return returns a properties object with the properties of the file.
	 */
	protected Properties getProperties(String propsFileLocation) {
		return propertyHandler.getProperties(propsFileLocation);
	}

	/**
	 * @return returns the initial properties that were used when the strategy started.
	 */
	protected Properties getInitialProperties() {
		return propertyHandler.getInitialProperties();
	}

	/**
	 * @return returns the current properties that are being used in the strategy. 
	 */
	protected Properties getCurrentProperties() {
		return propertyHandler.getCurrentProperties();
	}

	//TODO	protected void sendEmail (String subject, String message){
	//		emailUtil.send(subject, message);
	//	}
	/**
	 * Format a long value that is the difference between two times
	 * into a DD:HH:MM readable format
	 *
	 * @deprecated use the TimeFormatter class instead.  
	 */
	@Deprecated
	protected String getTimeDifferenceString(Long timeDiff) {
		return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(timeDiff),
				TimeUnit.MILLISECONDS.toMinutes(timeDiff) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeDiff)),
				TimeUnit.MILLISECONDS.toSeconds(timeDiff) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes
						(timeDiff)));
	}

	/**
	 * Sets the slack web hook where the message should be posted to.
	 * Slack web hooks are provided by Slack.
	 * @param url The url that messages should be posted to.
	 */
	protected void setSlackWebHookUrl(String url){
		SlackMessageThread.setSlackWebhookUrl(url);
	}

	/**
	 * Sends a message to slack. The message will only be sent if the
	 * slack web hook has been set via the {@link #setSlackWebHookUrl(String)} method  
	 * @param message The message that is to be sent to Slack.
	 */
	protected void sendMessageToSlack(String message){
		slackMessageThread.sendMessage(message);
	}

	/**
	 * A customer function that handles specific account updates every
	 * two hours between 930am amd and 730pm. {@link #setSlackWebHookUrl(String)}
	 */
	protected void sendAccountUpdateToSlack(long updateTime){
		// Only send updates a max of every 2 hours between 930am and 730pm
		if(System.currentTimeMillis() - lastSlackUpdateTime >= BarSize._1_HOUR.getSecondsInBar() * 2 * 1000
				&& (System.currentTimeMillis() % milliOneDay >= 52200000 
				|| System.currentTimeMillis() % milliOneDay <= 1800000)){
			StringBuilder sb = new StringBuilder();
			if(startTime!=null){
				sb.append("STRATEGY STARTED AT: " + new Date(startTime)); 
				sb.append(System.lineSeparator());
			}
			sb.append("CURRENT BALANCE: " + getAccount().getEquityWithLoanValue()); 
			sb.append(System.lineSeparator());
			sb.append("PROFIT SINCE STARTING STRATEGY: " + (startingBalance + (getAccount().getEquityWithLoanValue() - startingBalance)));
			if(!getAccount().getPositions().isEmpty()){
				Map<SecurityKey, Position> positions = getAccount().getPositions();
				StringBuilder sb2 = new StringBuilder();
				boolean positionsExist = false;
				for(SecurityKey k : positions.keySet()){
					if(positions.get(k).getQuantity() != 0 && securityKeys.contains(k)){
						positionsExist = true;
						sb2.append("----------------------------");
						sb2.append(System.lineSeparator());
						if(positions.get(k).getQuantity() > 0)
							sb2.append("Position is: LONG");
						else
							sb2.append("Position is: SHORT");
						sb2.append(System.lineSeparator());
						sb2.append("Symbol: " + positions.get(k).getSecurity().getSymbol() + "." + positions.get(k).getSecurity().getCurrency());
						sb2.append(System.lineSeparator());
						sb2.append("Quantity: " + positions.get(k).getQuantity());
						sb2.append(System.lineSeparator());
						sb2.append("Open Price: " + positions.get(k).getAverageCost());
						sb2.append(System.lineSeparator());
						sb2.append("Last Market Price: " + getTickCache().getLastTickPrice(k));
						sb2.append(System.lineSeparator());
						sb2.append("Unrealized Profit: " + ((positions.get(k).getAverageCost()*getUnrealizedPercent(k))*Math.abs(positions.get(k).getQuantity())));
						sb2.append(System.lineSeparator());
					}
				}
				if(positionsExist){
					sb.append(System.lineSeparator());
					sb.append("OPEN POSITIONS: ");
					sb.append(System.lineSeparator());
					sb.append(sb2.toString());
				}
			}
			sendMessageToSlack(sb.toString());
			lastSlackUpdateTime = updateTime;
		}

	}

	/**
	 * @return returns all of the account attributes to a string with each attribute
	 * on a separate line. When the Strategy is running live, this returns significantly
	 * more data since the broker provides more attributes that can be passed on. 
	 */
	protected String getAccountAttributesToString() {
		StringBuilder sb = new StringBuilder();
		if (status.equals(StrategyStatus.STARTED_LIVE)) {
			sb.append("AVAILABLE FUNDS: " + getAccount().getAvailableFunds());
			sb.append(System.lineSeparator());
			sb.append("FULL AVAILABLE FUNDS: " + getAccount().getFullAvailableFunds());
			sb.append(System.lineSeparator());
			sb.append("BUYING POWER: " + getAccount().getBuyingPower());
			sb.append(System.lineSeparator());
			sb.append("CASH BALANCE: " + getAccount().getCashBalance());
			sb.append(System.lineSeparator());
			sb.append("INITIAL MARGIN REQUIREMENT: " + getAccount().getInitialMarginRequirement());
			sb.append(System.lineSeparator());
			sb.append("FULL INITIAL MARGIN REQUIREMENT: " + getAccount().getFullInitialMarginRequirement());
			sb.append(System.lineSeparator());
			sb.append("MAINTENANCE MARGIN REQUIREMENT: " + getAccount().getFullMaintenanceMarginRequirement());
			sb.append(System.lineSeparator());
			sb.append("REG T MARGIN REQUIREMENT: " + getAccount().getRegulationTMargin());
			sb.append(System.lineSeparator());
			sb.append("SMA: " + getAccount().getSma());
			sb.append(System.lineSeparator());
			sb.append("ACCRUED CASH: " + getAccount().getAccruedCash());
			sb.append(System.lineSeparator());
			sb.append("EQUITY WITH LOAN VALUE: " + getAccount().getEquityWithLoanValue());
			sb.append(System.lineSeparator());
			sb.append("EXCESS LIQUIDITY: " + getAccount().getExcessLiquidity());
			sb.append(System.lineSeparator());
			sb.append("DAY TRADES REMAINING: " + getAccount().getDayTradesRemaining());
			sb.append(System.lineSeparator());
		} else {
			sb.append("AVAILABLE FUNDS: " + getAccount().getAvailableFunds());
		}
		return sb.toString();

	}

	/**
	 * This method captures the first tick time and writes the name of the tick tick to a file.
	 * This file allows easy access to view the date range for a given back test.
	 * This method also writes the names of all of the securities within the 
	 * test to the file contents.
	 * @param tick
	 */
	protected void captureTickTime(Tick tick){
		
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd");
		String tickDate = sdf.format(tick.getDateTime());
		
		String datesPrefix = "Dates_";
		String loc = baseCurrentStrategyDirectory;
		
		File directory = new File(loc);
		File[] files = directory.listFiles();
		
		boolean dateFileExists = false;
		
		for(File f : files){
			if(f.getName().startsWith(datesPrefix)){
				dateFileExists = true;
				String newName = loc 
						+ f.getName()
						+ "_"
						+ tickDate;
				f.renameTo(new File(newName));
			}
		}
		
		if(!dateFileExists){
			String fileName = loc
					+ datesPrefix
					+ tickDate;
			File f = new File(fileName);
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			addSecuritiesToFile(f);
		}
		
	}
	
	private void addSecuritiesToFile(File f) {
		StringBuilder sb = new StringBuilder();
		for(SecurityKey key : SecurityRegistry.getSecurities()){
			Security s = SecurityRegistry.get(key);
			sb.append(s.getSymbol());
			sb.append(".");
			sb.append(s.getCurrency());
			sb.append(System.lineSeparator());
		}
			
			
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(f, true),"utf-8"))) {
			writer.append(sb.toString());
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}	
		
	}

	/**
	 * @return returns a String that is in the exact same format as the data
	 * on the properties file.
	 */
	protected String getPropertiesPrintout(){
		StringBuilder sb = new StringBuilder();
		Properties props = getCurrentProperties();
		for (Object obj : props.keySet()){
			sb.append(obj);
			sb.append("=");
			sb.append(props.getProperty((String) obj));
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}

	/**
	 * This returns the current equity of a Strategy as well as the profit for a Strategy
	 * when the prior balance is known. 
	 * @param startingBalance The starting balance of the strategy 
	 * @return returns a String of the total profit from the strategy and the available funds for the account
	 * on separate lines.
	 */
	protected String getEquityAndCurrentProfit(double startingBalance){
		StringBuilder sb = new StringBuilder();
		if(status.equals(StrategyStatus.STARTED_LIVE)){
			sb.append("ANALYTICS - Total Profit (from Account):   " + (getAccount().getEquityWithLoanValue() - startingBalance));
			sb.append(System.lineSeparator());
			sb.append("ACCOUNT - Available Funds: " + getAccount().getEquityWithLoanValue());
			sb.append(System.lineSeparator());
		}
		else {
			sb.append("ANALYTICS - Total Profit (from Account):   " + (getAccount().getAvailableFunds() - startingBalance));
			sb.append(System.lineSeparator());
			sb.append("ACCOUNT - Available Funds: " + getAccount().getAvailableFunds());
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}

	/**
	 * Creates a {@link MomentumTrader} for each security that has been registered for the Strategy
	 * And will automatically register a list of window sizes for ALL bar sizes that
	 * have been registered for the security. 
	 * @param numWindows A list of window sizes that should be added to each momentum trader. <br>
	 * For example, if you want to track the min and max values for the last 5 bars and the last 13 bars
	 * for each bar size or if you want to see if the price is trending up or trending down, etc., 
	 * you could add a list containing [5,13] and pass it to this function and
	 * a momentum trader would be created to capture price points for each of those window sizes for
	 * every bar size that is registered.
	 */
	protected void addMomentumTraders(ArrayList<Integer> numWindows) {
		for (SecurityKey key : SecurityRegistry.getSecurities()) {
			System.out.println("REGISTERING MOMENTUM TRADER FOR - " + key);
			momentumTraders.put(key, new MomentumTrader(key));
			for (BarSize bs : securityBarSizes.get(key)) {
				for (int window : numWindows) {
					System.out.println("REGISTERING BAR SIZE AND WINDOWS: " + bs + " " + window);
					momentumTraders.get(key).addPricePoints(bs, window);
				}
			}
		}
	}

	/**
	 * This updates the values of the momentum trader for this particular bar size.
	 * See {@link MomentumTrader#updatePricePoints(Bar)} for more details
	 * @param bar the bar to be passed to the momentum trader to be updated.
	 */
	protected void updateMomentumTraders(Bar bar) {
		momentumTraders.get(bar.getSecurity().getKey()).updatePricePoints(bar);
	}

	/**
	 * @param key the unique security key required to retrieve a momentum trader that
	 * is associated to a specific security.
	 * @return returns the momentum trader for the given security key. 
	 */
	protected MomentumTrader getMomentumTrader(SecurityKey key) {
		return momentumTraders.get(key);
	}

	/**
	 * 
	 * @param bar The bar containing the unique security key required to retrieve a momentum trader that
	 * is associated to a specific security.
	 * @return returns the momentum trader for the given security key within the bar.
	 */
	protected MomentumTrader getMomentumTrader(Bar bar) {
		return momentumTraders.get(bar.getSecurity().getKey());
	}


	/**
	 * 
	 * @param tick The tick containing the unique security key required to retrieve a momentum trader that
	 * is associated to a specific security.
	 * @return returns the momentum trader for the given security key within the tick.
	 */
	protected MomentumTrader getMomentumTrader(Tick tick) {
		return momentumTraders.get(tick.getSecurity().getKey());
	}


	/**
	 * MomentumTraders are useful for when trading off of momentum. This class
	 * should be able to handle short-term and long-term momentum trading based
	 * off of speed, price and direction.<br> <br>
	 * MomentumTraders internally create {@link PricePoints} which are useful
	 * for keeping track of specific attributes relating to price action 
	 * over a certain period of time which can be used to allow the MomentumTrader
	 * to determine if the price is trending up or down, how long it has been since
	 * the price changed, the rate of change of the price, the trading range
	 * of the price, etc. <br> <br>
	 * TODO: This class should be moved outside of the Strategy class and should
	 * be expanded tremendously
	 * Examples for 
	 * @author Grant Seward
	 *
	 */
	public class MomentumTrader {

		SecurityKey key;

		// This tells us what direction the price is moving in.
		// true = price is moving up; false = price is moving down
		boolean trendingUp;

		// This tells us how long it has been since the last price change
		long timeSincePriceChange;

		// This lets us set multiple windows where we can see what the
		// average price change is, max price change, etc. The bar size
		// tells us how wide the a window unit is and the integer
		// tells us how many units to look back
		HashMap<BarSize, HashMap<Integer, PricePoints>> pricePoints;

		public MomentumTrader(SecurityKey key) {
			this.key = key;
			pricePoints = new HashMap<BarSize, HashMap<Integer, PricePoints>>();
		}

		public void addPricePoints(BarSize barSize, int numBars) {

			if (pricePoints.get(barSize) == null) {
				HashMap<Integer, PricePoints> newPP = new HashMap<Integer, PricePoints>();
				newPP.put(numBars, new PricePoints());
				System.out.println("ADDING NEW PRICE POINT FOR " + barSize + " " + numBars);
				pricePoints.put(barSize, newPP);
			} else {
				System.out.println("ADDING NEW PRICE POINT FOR " + barSize + " " + numBars);
				pricePoints.get(barSize).put(numBars, new PricePoints());
			}
		}

		public PricePoints getPricePoints(BarSize barSize, int numBars) {
			if (!pricePoints.get(barSize).containsKey(numBars)) {
				System.out.println("CANNOT FIND PricePoint for BARSIZE: " + barSize + " and numBars: " + numBars);
				return null;
			} else
				return pricePoints.get(barSize).get(numBars);
		}

		public void updatePricePoints(Bar bar) {
			if (pricePoints.containsKey(bar.getBarSize())) {
				for (Integer numBars : pricePoints.get(bar.getBarSize()).keySet()) {
					List<Bar> bars = new ArrayList<Bar>();
					bars = barCache.getMostRecentBars(this.key, bar.getBarSize(), numBars);
					pricePoints.get(bar.getBarSize()).get(numBars).setValues(bars);
				}
			}
		}

		public class PricePoints {
			// Price action is the total highest value - the lowest value, it is the
			// total range that the price fluctuated in
			double priceAction;

			// Absolute total movement is cumulative value of
			// Abs(high - low) of each bar
			double absoluteTotalMovement;
			double fastestMovement;

			public PricePoints() {

			}

			private void setValues(List<Bar> bars) {
				double min = Double.MAX_VALUE;
				double max = Double.MIN_VALUE;
				for (Bar b : bars) {
					if (b.getLow() < min)
						min = b.getLow();
					if (b.getHigh() > max)
						max = b.getHigh();
					if (Math.abs(b.getHigh() - b.getLow()) > fastestMovement)
						fastestMovement = Math.abs(b.getHigh() - b.getLow());
					absoluteTotalMovement += Math.abs(b.getHigh() - b.getLow());
				}
				priceAction = max - min;

			}

			public double getPriceAaction() {
				return priceAction;
			}

			public double getAbsoluteTotalMovement() {
				return absoluteTotalMovement;
			}

			public double getFastestMovement() {
				return fastestMovement;
			}
		}
	}


	// BarTraders are used when a portion of the strategy relies on leveraging
	// the patterns and behaviors of a bar. This is used to abstract away the
	// effort of interacting with the bar cache each time the methods are called
	/**
	 * A bar trader is a simple way
	 * of tracking trading metrics that are typically associated with bars such as when the 
	 * last trade was placed, how many bars have passed since the last trade, 
	 * the times (in milliseconds) that a particular bar size crossed over a moving average, 
	 * the count of these times within a certain window, etc. <br> <br>
	 * One bar trader should be created for each security (this is currently setup to be
	 * done by default) but the individual bar sizes that should be tracked each need to be 
	 * added individually. 
	 * 
	 * @author grantseward
	 *
	 */
	public class BarTrader {
		// Keeps track of the prior bar for each of the bar sizes related to that security Key
		HashMap<BarSize, Bar> priorBar;

		// Keeps track of the time that the bar size crossed over the a MA
		// This looks at the numBarsForMACrosses to determine how far back
		// It should keep the bars that crossed before pruning them
		public HashMap<BarSize, ArrayList<Long>> recentMACrosses;

		// Used with the recentMACrosses. This determines how many bars should
		// be looked at when determining how many times the bars crossed over
		// the MA
		int numBarsForMACrosses;

		// Time of the last trade for a given security key.
		long lastTradeTime = 0;

		// The current stop loss for a security key
		double stopLoss;

		public BarTrader() {
			priorBar = new HashMap<BarSize, Bar>();
			recentMACrosses = new HashMap<BarSize, ArrayList<Long>>();
		}

		public Bar getPriorBar(BarSize barSize) {
			return priorBar.get(barSize);
		}

		public void saveAsPriorBar(BarSize barSize, Bar bar) {
			priorBar.put(barSize, bar);

		}

		public void setLastTradeTime(long tradeTime) {
			lastTradeTime = tradeTime;
		}

		public void setNumBarsForMACrosses(int numBars) {
			numBarsForMACrosses = numBars;
		}

		public void setStopLoss(double stopLossVal) {
			stopLoss = stopLossVal;
		}

		public double getStopLoss() {
			return stopLoss;
		}

		public long getLastTradeTime(){
			return lastTradeTime;
		}

		public void addToRecentMACrosses(BarSize barSize, long currTime) {
			recentMACrosses.get(barSize).add(currTime);
		}

		public void addBarSize(BarSize barSize) {
			priorBar.put(barSize, null);
			recentMACrosses.put(barSize, new ArrayList<Long>());
		}

		public int getNumOfRecentMACrosses(BarSize barSize, long currTime) {
			pruneOldMACrosses(barSize, currTime);
			return recentMACrosses.get(barSize).size();
		}

		public double barsSinceLastTrade(BarSize barSize, long currTime) {
			return (currTime - lastTradeTime) / (barSize.getSecondsInBar() * 1000);
		}

		public boolean tradePlacedInRecentBars(long currTime, int numBars, BarSize barSize) {
			if (lastTradeTime > currTime - ((numBars + 1) * barSize.getSecondsInBar() * 1000))
				return true;
			return false;
		}

		private void pruneOldMACrosses(BarSize barSize, long currTime) {

			if (recentMACrosses.get(barSize).size() > 1) {
				for (int i = 0; i < recentMACrosses.get(barSize).size(); i++) {
					if (recentMACrosses.get(barSize).get(i) < currTime - (barSize.getSecondsInBar() * 1000 * numBarsForMACrosses))
						recentMACrosses.get(barSize).remove(recentMACrosses.get(barSize).get(i));
					else
						break;
				}

			}
		}
	}

	protected void setOrgName(String orgName) {
		this.orgName = orgName;
	}
}
