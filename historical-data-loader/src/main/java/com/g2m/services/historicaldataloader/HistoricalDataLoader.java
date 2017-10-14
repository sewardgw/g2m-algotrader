package com.g2m.services.historicaldataloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.ib.controller.ApiConnection.ILogger;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IConnectionHandler;
import com.ib.controller.Bar;
import com.ib.controller.NewContract;
import com.ib.controller.Types.BarSize;
import com.ib.controller.Types.Right;
import com.ib.controller.Types.SecType;
import com.ib.controller.Types.WhatToShow;

/**
 * @author Michael Borromeo
 */
public class HistoricalDataLoader implements RequestRunnerEventHandler, ILogger, IConnectionHandler {
	final static Logger LOGGER = Logger.getLogger(HistoricalDataLoader.class);
	private ApiController ibApiController;
	private String ibHost;
	private int ibPort;
	private int ibClientId;

	private int requestsSucceeded = 0;
	private int requestsFailed = 0;
	private String outputFolder;
	private String outputFile;
	private List<Bar> barBuffer;
	private FileWriter fileWriter;
	private DataLoaderEventHandler eventHandler;
	private List<HistoricalRequest> requests;
	private boolean startWithLastSuccessfulRequest = false;
	private NewContract contract;
	private boolean saveOutputToFile;
	private boolean isAsync = false;
	private HistoricalEntityTypes saveToFileMode = HistoricalEntityTypes.TICKS;

	private static final String REQUEST_PARAMETERS_FILENAME = "RequestParameters.txt";
	private static final String SUCCESSFUL_REQUESTS_FILENAME = "SuccessfulRequests.txt";
	private static String REQUESTS_FOLDER_PATH = "";

	private String symbol;
	private String exchange;
	private String currency;
	private SecType secType;
	private Right right = Right.None;
	private String expiry;
	private boolean includeExpired;
	private double strike;
	private double multiplier;
	private String localSymbol;
	private String tradingClass;
	private Date endDate;
	private boolean isRegularTradingHours;
	private DurationWithUnit durationWithUnit;
	private BarSize barSize;
	private WhatToShow whatToShow = WhatToShow.TRADES;

	public HistoricalDataLoader() {
		this.ibHost = "localhost";
		this.ibPort = 4001;
		this.ibClientId = 1;
		this.outputFolder = System.getProperty("user.home") + System.getProperty("file.separator") + "HistoricalDataLoader";
	}

	public void setIbHost(String ibHost) {
		this.ibHost = ibHost;
	}

	public void setIbPort(int ibPort) {
		this.ibPort = ibPort;
	}

	public void setIbClientId(int ibClientId) {
		this.ibClientId = ibClientId;
	}

	public void startRequest(DataLoaderEventHandler handler) throws IOException {
		if (null == handler) {
			throw new RuntimeException("Must provide a DataLoaderEventHandler");
		}

		this.eventHandler = handler;
		this.contract = this.createContract();

		/*
		 * this will begin the connection process, which if successful will be the request
		 */
		this.connectToIb();

		if (!this.isAsync()) {
			synchronized (this) {
				try {
					this.wait();
				} catch (Exception e) {
					LOGGER.debug(e.getMessage(), e);
				}
			}
		}
	}

	private void saveRequestParametersToFile() throws IOException {
		FileWriter writer = new FileWriter(this.findRequestParametersFile(), false);
		writer.write(this.getRequestString());
		writer.close();
	}

	private void prepareOutputFolder(boolean appendToFile) throws IOException {
		File folder = new File(this.outputFolder);

		if (folder.exists() && folder.isDirectory()) {
			String outputFilePath = this.outputFolder + System.getProperty("file.separator") + this.getOutputFile();
			this.fileWriter = new FileWriter(outputFilePath, appendToFile);

			if (!appendToFile) {
				if (HistoricalEntityTypes.BARS.equals(saveToFileMode)) {
					this.fileWriter.write("dateTime, barSize, high, low, open, close, volume\n");
				} else if (HistoricalEntityTypes.TICKS.equals(saveToFileMode)) {
					this.fileWriter
							.write("dateTime, last, lastDateTime, volBid, volAsk, bid, ask, vol, openIntrest, settlement\n");
				}
			}
		} else {
			if (!folder.mkdir()) {
				throw new RuntimeException("Unable to create output folder: " + this.outputFolder);
			} else {
				String outputFilePath = this.outputFolder + System.getProperty("file.separator") + this.getOutputFile();
				this.fileWriter = new FileWriter(outputFilePath, appendToFile);

				if (!appendToFile) {
					if (HistoricalEntityTypes.BARS.equals(saveToFileMode)) {
						this.fileWriter.write("dateTime, barSize, high, low, open, close, volume\n");
					} else if (HistoricalEntityTypes.TICKS.equals(saveToFileMode)) {
						this.fileWriter.write(
								"dateTime, last, lastDateTime, volBid, volAsk, bid, ask, vol, openIntrest, settlement\n");
					}
				}
			}
		}
	}

	/**
	 * This will start the connect thread in the ApiController which when connected will call
	 * connected() below which will in turn call submitRequestToRunner().
	 */
	private void connectToIb() {
		this.ibApiController = new ApiController(this, this, this);
		this.ibApiController.connect(this.ibHost, this.ibPort, this.ibClientId);
	}

	private void submitRequestToRunner() throws IOException {
		List<HistoricalRequest> subRequestList = this.createSubRequestList();

		this.barBuffer = new LinkedList<Bar>();

		if (this.isSaveOutputToFile()) {
			if (this.isRequestSameAsLast() && this.startWithLastSuccessfulRequest()) {
				this.prepareOutputFolder(true);
				RequestRunner.startRequest(this.ibApiController, this.removeSuccessfulRequests(subRequestList), this);
			} else {
				this.prepareOutputFolder(false);
				RequestRunner.startRequest(this.ibApiController, subRequestList, this);
			}
		} else {
			RequestRunner.startRequest(this.ibApiController, subRequestList, this);
		}

		this.saveRequestParametersToFile();
	}

	private String getRequestString() {
		StringBuilder builder = new StringBuilder();

		builder.append(this.contract.toString());
		builder.append(this.getEndDate());
		builder.append("\n").append(this.isRegularTradingHours());
		builder.append("\n").append(this.getDurationWithUnit().toString());
		builder.append("\n").append(this.getBarSize());
		builder.append("\n").append(this.getWhatToShow());
		builder.append("\n").append(this.getOutputFile());

		return builder.toString();
	}

	private boolean isRequestSameAsLast() {
		try {
			FileReader reader = new FileReader(this.findRequestParametersFile());
			StringBuilder builder = new StringBuilder();
			char[] buffer = new char[1000];
			int bufferLength;

			while (0 < (bufferLength = reader.read(buffer))) {
				builder.append(buffer, 0, bufferLength);
			}

			reader.close();

			if (builder.toString().equals(this.getRequestString())) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
			return false;
		}
	}

	@SuppressWarnings("deprecation")
	private File findRequestParametersFile() {
		if (0 == HistoricalDataLoader.REQUESTS_FOLDER_PATH.length()) {
			HistoricalDataLoader.REQUESTS_FOLDER_PATH = URLDecoder
					.decode(HistoricalDataLoader.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		}

		File file = new File(HistoricalDataLoader.REQUESTS_FOLDER_PATH + HistoricalDataLoader.REQUEST_PARAMETERS_FILENAME);

		try {
			file.createNewFile();
		} catch (IOException e) {
			LOGGER.debug(e.getMessage(), e);
		}

		return file;
	}

	@SuppressWarnings("deprecation")
	private File findSuccessfulRequestsFile() {
		if (0 == HistoricalDataLoader.REQUESTS_FOLDER_PATH.length()) {
			HistoricalDataLoader.REQUESTS_FOLDER_PATH = URLDecoder
					.decode(HistoricalDataLoader.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		}

		File file = new File(HistoricalDataLoader.REQUESTS_FOLDER_PATH + HistoricalDataLoader.SUCCESSFUL_REQUESTS_FILENAME);

		try {
			file.createNewFile();
		} catch (IOException e) {
			LOGGER.debug(e.getMessage(), e);
		}

		return file;
	}

	/**
	 * This should only get called if the last request is the exact same as this current request,
	 * thereby creating the exact same sub-request list. In this case there exists a subset of
	 * requests which occur after the last successful request. The requests still need to be run.
	 */
	private List<HistoricalRequest> removeSuccessfulRequests(List<HistoricalRequest> requestList) {
		Date dateOfLastSuccessfulRequest = null;

		try {
			BufferedReader reader = new BufferedReader(new FileReader(this.findSuccessfulRequestsFile()));
			String line;

			while ((line = reader.readLine()) != null) {
				SimpleDateFormat dateParser = new SimpleDateFormat(Constants.DATE_SAVE_FORMAT);
				dateOfLastSuccessfulRequest = dateParser.parse(line);
			}

			reader.close();
		} catch (Exception e) {
			dateOfLastSuccessfulRequest = null;
			LOGGER.debug(e.getMessage(), e);
		}

		List<HistoricalRequest> trimmedRequestList = new LinkedList<HistoricalRequest>();
		for (HistoricalRequest request : requestList) {
			if (null == dateOfLastSuccessfulRequest || 0 < request.getEndDate().compareTo(dateOfLastSuccessfulRequest)) {
				trimmedRequestList.add(request);
			}
		}

		return trimmedRequestList;
	}

	private NewContract createContract() {
		NewContract contract = new NewContract();
		contract.symbol(this.getSymbol());
		contract.exchange(this.getExchange());
		contract.currency(this.getCurrency());
		contract.secType(this.getSecType());
		contract.right(this.getRight());
		contract.expiry(this.getExpiry());
		contract.strike(this.getStrike());
		contract.multiplier(String.valueOf(this.getMultiplier()));
		contract.localSymbol(this.getLocalSymbol());
		contract.tradingClass(this.getTradingClass());

		return contract;
	}

	@Override
	public void requestStarted(HistoricalRequest historicalRequest) {
		this.eventHandler.debug("Request started: " + historicalRequest.getContract().symbol());
	}

	@Override
	public void barReceived(Bar bar, boolean hasGaps) {
		this.barBuffer.add(bar);
	}

	@Override
	public void requestCompleted(HistoricalRequest historicalRequest, int requestsRemaining) {
		if (historicalRequest.wasRequestSuccessful()) {
			this.eventHandler
					.debug("Request completed (" + requestsRemaining + " remaining): " + historicalRequest.toString());
			this.requestsSucceeded++;

			this.updateSuccessfulRequestsFile(historicalRequest);

			if (this.isSaveOutputToFile()) {
				if (saveToFileMode.equals(HistoricalEntityTypes.BARS)) {
					this.writeBufferToFileAsBars();
				} else if (saveToFileMode.equals(HistoricalEntityTypes.TICKS)) {
					this.writeBufferToFileAsTicks();
				}
			}

			this.eventHandler.barsReceived(barBuffer);
		} else {
			this.eventHandler.debug("Request failed (" + requestsRemaining + " remaining): " + historicalRequest.toString());
			this.requestsFailed++;
		}

		this.barBuffer.clear();
	}

	/**
	 * Takes in the latest successful sub-request object and writes it to file. If subsequent
	 * sub-requests error out or if the program is stopped and if the user happens to run the
	 * program again with the same exact request then this file will be scanned and the sub-requests
	 * will start right after this point.
	 */
	private void updateSuccessfulRequestsFile(HistoricalRequest historicalRequest) {
		try {
			FileWriter writer = new FileWriter(this.findSuccessfulRequestsFile(), false);
			SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_SAVE_FORMAT);
			writer.write(dateFormat.format(historicalRequest.getEndDate()));
			writer.close();
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
		}
	}

	private void writeBufferToFileAsTicks() {
		try {
			StringBuilder outputBuffer = new StringBuilder();
			/*
			 * dateTime, last, lastDateTime, volBid, volAsk, bid, ask, vol, openIntrest, settlement
			 */
			for (Bar bar : this.barBuffer) {
				outputBuffer.append(bar.time()).append("000,");
				outputBuffer.append(bar.close()).append(",");
				outputBuffer.append(bar.time()).append("000,");
				outputBuffer.append("100").append(",");
				outputBuffer.append("100").append(",");
				outputBuffer.append(bar.close()).append(",");
				outputBuffer.append(bar.close()).append(",");
				outputBuffer.append(bar.volume()).append(",");
				outputBuffer.append("0").append(",");
				outputBuffer.append("0").append("\n");
			}

			this.fileWriter.write(outputBuffer.toString());
			this.fileWriter.flush();
		} catch (IOException e) {
			this.eventHandler.message(e.getMessage());
		}
	}

	private void writeBufferToFileAsBars() {
		try {
			StringBuilder outputBuffer = new StringBuilder();
			/*
			 * dateTime, last, lastDateTime, volBid, volAsk, bid, ask, vol, openIntrest, settlement
			 * dateTime, high, low, open, close, volume
			 */
			for (Bar bar : this.barBuffer) {
				outputBuffer.append(bar.time()).append("000,");
				outputBuffer.append(barSize.name().toUpperCase()).append(",");
				outputBuffer.append(bar.high()).append(",");
				outputBuffer.append(bar.low()).append(",");
				outputBuffer.append(bar.open()).append(",");
				outputBuffer.append(bar.close()).append(",");
				outputBuffer.append(bar.volume()).append("\n");
			}

			this.fileWriter.write(outputBuffer.toString());
			this.fileWriter.flush();
		} catch (IOException e) {
			this.eventHandler.message(e.getMessage());
		}
	}

	private List<HistoricalRequest> createSubRequestList() {
		int numberOfRequests = RequestRules.getNumberOfRequests(this.getBarSize(), this.getDurationWithUnit());
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(this.getEndDate());

		DurationWithUnit incrementalDuration = ValidBarSizesForDurations.getLargestDurationFromBarSize(this.getBarSize());

		if (incrementalDuration.getDurationInSeconds() > this.getDurationWithUnit().getDurationInSeconds()) {
			incrementalDuration = this.getDurationWithUnit();
		}

		List<HistoricalRequest> requestList = new ArrayList<HistoricalRequest>();
		for (int i = 0; i < numberOfRequests; i++) {
			HistoricalRequest historicalRequest = new HistoricalRequest();
			historicalRequest.setBarSize(this.getBarSize());
			historicalRequest.setContract(this.contract);
			historicalRequest.setDuration(incrementalDuration);
			historicalRequest.setEndDate(calendar.getTime());
			//System.out.println(this.getClass().getSimpleName() + " Calendar start time: " + calendar.getTime());
			//System.out.println(this.getClass().getSimpleName() + " incremental duration : " + (double)(incrementalDuration.getDurationInSeconds() / 86400) );
			historicalRequest.setRegularTradingHours(this.isRegularTradingHours());
			historicalRequest.setWhatToShow(this.getWhatToShow());
			requestList.add(historicalRequest);

			calendar.add(Calendar.SECOND, (int) -(incrementalDuration.getDurationInSeconds()));
		}

		/*
		 * Reverse the list so the resulting bars will show up in ascending order. The bars that are
		 * returned from IB are in ascending order, but the requests are formed in descending order,
		 * working backwards from the endDate.
		 */
		Collections.reverse(requestList);

		return requestList;
	}

	@Override
	public void connected() {
		this.eventHandler.message("Connected");

		try {
			this.submitRequestToRunner();
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
			this.eventHandler.message("Error while submitting request to runner: " + e.getMessage());
			System.exit(1);
		}
	}

	@Override
	public void disconnected() {
		this.eventHandler.message("Disconnected");
	}

	@Override
	public void accountList(ArrayList<String> list) {
	}

	@Override
	public void error(Exception e) {
		this.eventHandler.message("Error: " + e.toString());

	}

	@Override
	public void message(int id, int errorCode, String errorMsg) {
		this.eventHandler.message("Message (id= " + id + "): " + errorCode + " - " + errorMsg);

		/*
		 * TODO Handle error messages and potentially stop the request early
		 * https://www.interactivebrokers.com/en/software /api/apiguide/tables/api_message_codes.htm
		 */
		if (200 == errorCode || 321 == errorCode || 502 == errorCode) {
			RequestRunner.getRequestRunner().skipAndErrorOutCurrentRequest();
		}
	}

	@Override
	public void show(String string) {
		// this.eventHandler.debug(string);
	}

	@Override
	public void log(String valueOf) {
		// if (0 < valueOf.trim().length())
		// this.eventHandler.debug(valueOf);
	}

	@Override
	public void allRequestsCompleted() {
		if (this.isSaveOutputToFile()) {
			try {
				this.fileWriter.close();
			} catch (IOException e) {
				this.eventHandler.debug(e.getMessage());
			}
		}

		this.ibApiController.disconnect();
		this.eventHandler.allRequestsComplete(this.requests);

		if (!this.isAsync()) {
			synchronized (this) {
				this.notify();
			}
		}
	}

	@Override
	public void requestMessage(String message) {
		this.eventHandler.message(message);
	}

	public int getRequestsTotal() {
		return this.requestsFailed + this.requestsSucceeded;
	}

	public int getRequestsSucceeded() {
		return requestsSucceeded;
	}

	public int getRequestsFailed() {
		return requestsFailed;
	}

	public String getOutputFolder() {
		return outputFolder;
	}

	public void setOutputFolder(String outputFolder) {
		// The name of the output file is set for us
		// so that we can leverage the same naming 
		// convention across the application and 
		// automate some tasks in back testing by
		// pulling in the data based off of the file names
		setOutputFile();
		this.outputFolder = outputFolder;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getExchange() {
		return exchange;
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public SecType getSecType() {
		return secType;
	}

	public void setSecType(SecType secType) {
		this.secType = secType;
	}

	public Right getRight() {
		return right;
	}

	public void setRight(Right right) {
		this.right = right;
	}

	public String getExpiry() {
		return expiry;
	}

	public void setExpiry(String expiry) {
		this.expiry = expiry;
	}

	public double getStrike() {
		return strike;
	}

	public void setStrike(double strike) {
		this.strike = strike;
	}

	public double getMultiplier() {
		return multiplier;
	}

	public void setMultiplier(double multiplier) {
		this.multiplier = multiplier;
	}
	
	public void setMultiplier(String multiplier) {
		if(!multiplier.equals(""))
			setMultiplier(Double.valueOf(multiplier));
	}

	public String getLocalSymbol() {
		return localSymbol;
	}

	public void setLocalSymbol(String localSymbol) {
		this.localSymbol = localSymbol;
	}

	public String getTradingClass() {
		return tradingClass;
	}

	public void setTradingClass(String tradingClass) {
		this.tradingClass = tradingClass;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public boolean isRegularTradingHours() {
		return isRegularTradingHours;
	}

	public void setRegularTradingHours(boolean isRegularTradingHours) {
		this.isRegularTradingHours = isRegularTradingHours;
	}

	public DurationWithUnit getDurationWithUnit() {
		return durationWithUnit;
	}

	public void setDurationWithUnit(DurationWithUnit durationWithUnit) {
		this.durationWithUnit = durationWithUnit;
	}

	public BarSize getBarSize() {
		return barSize;
	}

	public void setBarSize(BarSize barSize) {
		this.barSize = barSize;
	}

	public WhatToShow getWhatToShow() {
		return whatToShow;
	}

	public void setWhatToShow(WhatToShow whatToShow) {
		this.whatToShow = whatToShow;
	}

	public String getIbHost() {
		return ibHost;
	}

	public int getIbPort() {
		return ibPort;
	}

	public int getIbClientId() {
		return ibClientId;
	}

	public boolean isSaveOutputToFile() {
		return saveOutputToFile;
	}

	public void setSaveOutputToFile(boolean saveOutputToFile) {
		this.saveOutputToFile = saveOutputToFile;
	}

	public boolean isAsync() {
		return isAsync;
	}

	public void setAsync(boolean isAsync) {
		this.isAsync = isAsync;
	}

	public HistoricalEntityTypes getSaveToFileMode() {
		return saveToFileMode;
	}

	public void setSaveToFileMode(HistoricalEntityTypes saveToFileMode) {
		this.saveToFileMode = saveToFileMode;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public void setOutputFile() {
		StringBuilder sb = new StringBuilder();
		sb.append(symbol);
		sb.append("-");
		sb.append(expiry);
		sb.append("-");
		sb.append(secType);
		sb.append("-");
		sb.append(currency);
		sb.append("-");
		sb.append(exchange);
		sb.append("-");
		sb.append(System.currentTimeMillis());
		this.outputFile = sb.toString();
	}

	public boolean startWithLastSuccessfulRequest() {
		return this.startWithLastSuccessfulRequest;
	}

	public void setStartWithLastSuccessfulRequest(boolean startWithLastSuccessfulRequest) {
		this.startWithLastSuccessfulRequest = startWithLastSuccessfulRequest;
	}

	public boolean isIncludeExpired() {
		return includeExpired;
	}

	public void setIncludeExpired(boolean includeExpired) {
		this.includeExpired = includeExpired;
	}
}
