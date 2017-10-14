package com.g2m.strategies.examples;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.g2m.services.historicaldataloader.DefaultDataLoaderEventHandler;
import com.g2m.services.historicaldataloader.DurationWithUnit;
import com.g2m.services.historicaldataloader.HistoricalDataLoader;
import com.g2m.services.historicaldataloader.HistoricalEntityTypes;
import com.g2m.services.historicaldataloader.HistoricalRequest;
import com.ib.controller.Types.BarSize;
import com.ib.controller.Types.DurationUnit;
import com.ib.controller.Types.SecType;

/**
 * Added 7/5/2015.
 * 
 * @author Michael Borromeo
 */
public class DownloadHistoricalDataToFileForBacktest {

	/*
	 * -------- CHANGE THESE PARAMETERS
	 */

	// This can be set to localhost or to the IP address of where IB is running in the cloud
	static String host = "localhost";

	// Where the output should be sent to
	static String outputDirectory = "~/g2m-services/resources/ticks";

	// Lookup the IB API specs for the time intervals, max number of bars, 
	// etc that can be set here: 
	// https://www.interactivebrokers.com/en/software/api/apiguide/tables/historical_data_limitations.htm
	static String symbol = "ES";
	static String exchange = "GLOBEX";
	static String expiry = "201512";
	static String currency = "USD";
	static SecType securityType = SecType.FUT;
	static BarSize barSize = BarSize._5_mins;

	// Allows you to specify the end date for pulling data and the duration for 
	// how far back the data should be pulled from that end date.
	static Date endDate = new Date();
	static DurationWithUnit historicalDuration = new DurationWithUnit(DurationUnit.WEEK, 4);

	/*
	 * -------- PROBABLY DON'T NEED TO CHANGE THESE PARAMETERS
	 */

	// I believe Regular trading hours are 9:30 - 16:00 for all securities
	// setting to false allows 8:00 - 17:00 for stocks & options
	// & 00:00 - 23:59 for futures and options
	static boolean regularTradingHoursOnly = false;

	// G2M always use this port for back IB connections
	static int ibPort = 4001;
	// IB allow N number of client IDs, as a standard we have set aside 
	// 70 - 79 for back test connections
	static int ibClientId = 70;

	static boolean saveToFile = true;

	// Should probably always set to ticks, I dont believe the BARS function
	// was fully tested
	static HistoricalEntityTypes dataType = HistoricalEntityTypes.TICKS;


	public static void main(String... args) throws IOException {
		HistoricalDataLoader loader = new HistoricalDataLoader();
		loader.setIbHost(host);
		loader.setIbPort(ibPort);
		loader.setIbClientId(ibClientId);
		loader.setSymbol(symbol);
		loader.setExchange(exchange);
		loader.setCurrency(currency);
		loader.setSecType(securityType);
		loader.setExpiry(expiry);
		loader.setRegularTradingHours(regularTradingHoursOnly);
		loader.setBarSize(barSize);
		loader.setEndDate(endDate);
		loader.setDurationWithUnit(historicalDuration);
		loader.setSaveOutputToFile(saveToFile);
		loader.setSaveToFileMode(HistoricalEntityTypes.TICKS);
		// The file name is set in the setOutputFolder method as a concatenation of
		// the security attributes. This concatination is then parsed when running a 
		// back test to simplify the creation of securities and to very easily 
		// allow multiple securities in a single back test
		loader.setOutputFolder(outputDirectory);

		loader.startRequest(new DefaultDataLoaderEventHandler() {
			@Override
			public void allRequestsComplete(List<HistoricalRequest> requests) {
				System.out.println("Requests complete.");
			}
		});
	}
}
