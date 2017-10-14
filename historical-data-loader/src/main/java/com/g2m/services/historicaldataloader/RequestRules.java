package com.g2m.services.historicaldataloader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.ib.controller.Types.BarSize;

/**
 * @author Michael Borromeo
 */
public class RequestRules {
	final static Logger LOGGER = Logger.getLogger(RequestRules.class);
	public static final int MAX_REQUESTS_PER_10_MINUTES = 60;
	public static final int MAX_REQUESTS_PER_2_SECONDS = 5;
	public static final long REQUEST_WAIT_BUFFER_10_MINUTE_RULE = 30;
	public static final long REQUEST_WAIT_BUFFER_2_SECOND_RULE = 1;
	private static List<Date> requestsFromPreviousSession = new ArrayList<Date>();
	private static final String REQUEST_TIMESTAMPS_FILENAME = "RequestTimestamps.txt";
	private static String REQUEST_TIMESTAMPS_FILE_PATH = "";

	public static int getNumberOfRequests(BarSize barSize, DurationWithUnit duration) {
		DurationWithUnit largestDurationFromBarSize = ValidBarSizesForDurations.getLargestDurationFromBarSize(barSize);
		return (int) Math.ceil((double) duration.getDurationInSeconds()
				/ (double) largestDurationFromBarSize.getDurationInSeconds());
	}

	/**
	 * @return Returns the estimated time (in seconds) to complete a group of requests based on the
	 *         number of requests. There are two rules that govern timing: (1) no more than 60
	 *         requests can be submitted over a 10 minute window; and (2) no more than 5 requests
	 *         can be submitted over a 2 second window.
	 */
	public static long getEstimatedSecondsToComplete(int numberOfRequests) {
		int requestGroupCount10MinuteRule = (int) Math.ceil((double) numberOfRequests
				/ (double) RequestRules.MAX_REQUESTS_PER_10_MINUTES);
		long timeFrom10MinuteRule = Math.max(0, requestGroupCount10MinuteRule - 1) * 60 * 10;

		int remainingRequestsAfter10MinuteRule = numberOfRequests % RequestRules.MAX_REQUESTS_PER_10_MINUTES;
		if (0 < numberOfRequests && 0 == remainingRequestsAfter10MinuteRule)
			remainingRequestsAfter10MinuteRule = RequestRules.MAX_REQUESTS_PER_10_MINUTES;

		int requestGroupCount2SecondRule = (int) Math.ceil((double) remainingRequestsAfter10MinuteRule
				/ (double) RequestRules.MAX_REQUESTS_PER_2_SECONDS);
		long timeFrom2SecondRule = Math.max(0, requestGroupCount2SecondRule - 1) * 2;

		return timeFrom10MinuteRule + timeFrom2SecondRule;
	}

	/**
	 * @return Time is returned in milliseconds.
	 */
	public static long getTimeToWaitForNextRequest(List<HistoricalRequest> historicalRequests) {
		List<Date> combinedRequests = RequestRules.createCombinedSessionRequestList(historicalRequests);

		long millisecondsToWait2SecondRule = RequestRules.getTimeToWaitForNextRequest(combinedRequests,
				Calendar.SECOND, 2, RequestRules.MAX_REQUESTS_PER_2_SECONDS, RequestRules.REQUEST_WAIT_BUFFER_2_SECOND_RULE);

		long millisecondsToWait10MinuteRule = RequestRules.getTimeToWaitForNextRequest(combinedRequests,
				Calendar.MINUTE, 10, RequestRules.MAX_REQUESTS_PER_10_MINUTES,
				RequestRules.REQUEST_WAIT_BUFFER_10_MINUTE_RULE);

		return Math.max(millisecondsToWait2SecondRule, millisecondsToWait10MinuteRule);
	}

	private static List<Date> createCombinedSessionRequestList(List<HistoricalRequest> historicalRequests) {
		List<Date> combinedRequests = new ArrayList<Date>();

		for (Date date : RequestRules.requestsFromPreviousSession)
			combinedRequests.add(date);

		for (HistoricalRequest request : historicalRequests) {
			for (Date date : request.getAttempts()) {
				combinedRequests.add(date);
			}
		}

		return combinedRequests;
	}

	private static long getTimeToWaitForNextRequest(List<Date> historicalRequests, int ruleTimeUnit,
			int ruleWindowDuration, int maxRequestsInWindow, long requestWaitBuffer) {
		Date windowStart = RequestRules.getStartOfWindow(ruleTimeUnit, ruleWindowDuration);
		List<Date> requestsInWindow = RequestRules.getRequestsInWindow(historicalRequests, windowStart);

		long millisecondsToWait = 0;
		if (maxRequestsInWindow <= requestsInWindow.size()) {
			millisecondsToWait = requestsInWindow.get(0).getTime() - windowStart.getTime();
			millisecondsToWait += (requestWaitBuffer * 1000);
		}

		return millisecondsToWait;
	}

	private static List<Date> getRequestsInWindow(List<Date> historicalRequests, Date windowStart) {
		List<Date> requestsInWindow = new ArrayList<Date>();

		for (Date date : historicalRequests) {
			if (date.compareTo(windowStart) > 0)
				requestsInWindow.add(date);
		}

		return requestsInWindow;
	}

	private static Date getStartOfWindow(int timeUnit, int duration) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(timeUnit, 0 - duration);
		return calendar.getTime();
	}

	@SuppressWarnings("deprecation")
	private static File findRequestTimestampFile() {
		if (0 == RequestRules.REQUEST_TIMESTAMPS_FILE_PATH.length()) {
			RequestRules.REQUEST_TIMESTAMPS_FILE_PATH = URLDecoder.decode(RequestRules.class.getProtectionDomain()
					.getCodeSource().getLocation().getPath()
					+ RequestRules.REQUEST_TIMESTAMPS_FILENAME);
		}

		File file = new File(RequestRules.REQUEST_TIMESTAMPS_FILE_PATH);

		try {
			file.createNewFile();
		} catch (IOException e) {
			LOGGER.debug(e.getMessage(), e);
		}

		return file;
	}

	/**
	 * Load the requests from the request file. This should be called once at the start of a request
	 * batch.
	 */
	public static void loadRequestsFromPreviousSession() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(RequestRules.findRequestTimestampFile()));
			RequestRules.requestsFromPreviousSession = new ArrayList<Date>();
			String line;

			while ((line = reader.readLine()) != null) {
				SimpleDateFormat dateParser = new SimpleDateFormat(Constants.DATE_SAVE_FORMAT);
				RequestRules.requestsFromPreviousSession.add(dateParser.parse(line));
			}

			reader.close();
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
		}
	}

	/**
	 * Save the currently completed requests to the request file after each request. This is
	 * necessary incase the request batch gets stopped midway. If that happens then we'll need to
	 * load the requests made to properly pace any future requests since request pacing is monitored
	 * at the IB server level.
	 */
	public static void saveRequestsToFile(List<HistoricalRequest> historicalRequests) {
		Date windowStart = RequestRules.getStartOfWindow(Calendar.MINUTE, 10);
		List<Date> requestsInWindow = RequestRules.getRequestsInWindow(
				RequestRules.createCombinedSessionRequestList(historicalRequests), windowStart);

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(RequestRules.findRequestTimestampFile()));

			for (Date date : requestsInWindow) {
				SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_SAVE_FORMAT);
				writer.write(dateFormat.format(date));
				writer.newLine();
			}

			writer.close();
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
		}
	}
}
