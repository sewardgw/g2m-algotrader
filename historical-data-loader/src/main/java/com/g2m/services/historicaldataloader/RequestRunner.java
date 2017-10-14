package com.g2m.services.historicaldataloader;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IHistoricalDataHandler;
import com.ib.controller.Bar;

/**
 * @author Michael Borromeo
 */
public class RequestRunner implements Runnable, IHistoricalDataHandler {
	final static Logger LOGGER = Logger.getLogger(RequestRunner.class);
	private static RequestRunner requestRunner;
	private List<HistoricalRequest> historicalRequests;
	private RequestRunnerEventHandler requestEventHandler;
	private CountDownLatch countDownLatch;
	private ApiController apiController;
	private final int REQUEST_TIMEOUT_SECONDS = 30;
	private final int MAX_REQUEST_ATTEMPTS = 3;
	private Date currentRequestStart;
	private Date currentRequestEnd;
	private boolean requestError;

	private RequestRunner(ApiController apiController, List<HistoricalRequest> historicalRequests,
			RequestRunnerEventHandler requestEventHandler) {
		if (null == apiController) {
			throw new IllegalArgumentException("apiController cannot be null");
		}

		if (null == historicalRequests || 0 == historicalRequests.size()) {
			throw new IllegalArgumentException("Must provide a populated list of HistoricalRequests");
		}

		if (null == requestEventHandler) {
			throw new IllegalArgumentException("requestEventHandler cannot be null");
		}

		this.apiController = apiController;
		this.historicalRequests = historicalRequests;
		this.requestEventHandler = requestEventHandler;
	}

	public static void startRequest(ApiController apiController, List<HistoricalRequest> historicalRequests,
			RequestRunnerEventHandler requestEventHandler) {
		RequestRunner.requestRunner = new RequestRunner(apiController, historicalRequests, requestEventHandler);
		new Thread(RequestRunner.requestRunner).start();
	}

	@Override
	public void run() {
		this.requestError = false;
		boolean requestTimeout = false;
		RequestRules.loadRequestsFromPreviousSession();

		int counter = 0;

		for (HistoricalRequest request : this.historicalRequests) {
			updateRequestWindow(request);
			counter++;

			if (request.isRequestComplete())
				continue;

			while (this.MAX_REQUEST_ATTEMPTS > request.getAttempts().size()) {
				this.requestError = false;

				try {
					this.countDownLatch = new CountDownLatch(1);

					long sleepTime = RequestRules.getTimeToWaitForNextRequest(this.historicalRequests);

					if (0 < sleepTime) {
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(new Date());
						calendar.add(Calendar.MILLISECOND, (int) sleepTime);

						this.requestEventHandler.requestMessage("Sleeping for " + sleepTime
								+ " milliseconds - will wake at " + calendar.getTime().toString());
						Thread.sleep(sleepTime);
					}

					this.apiController.reqHistoricalData(request.getContract(), request.getFormattedEndDate(),
							(int) request.getDurationValue(), request.getDurationUnit(), request.getBarSize(),
							request.getWhatToShow(), request.isRegularTradingHours(), this);
					request.attemptMade();
					this.requestEventHandler.requestStarted(request);

					requestTimeout = !this.countDownLatch.await(this.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
				} catch (Exception e) {
					this.requestError = true;
					LOGGER.debug(e.getMessage(), e);
				}

				request.setRequestSuccessful(!this.requestError && !requestTimeout);
				RequestRules.saveRequestsToFile(this.historicalRequests);

				if (request.wasRequestSuccessful())
					break;
			}

			request.setRequestComplete(true);
			this.requestEventHandler.requestCompleted(request, this.historicalRequests.size() - counter);
		}

		this.requestEventHandler.allRequestsCompleted();
	}

	public void skipAndErrorOutCurrentRequest() {
		this.requestError = true;
		this.countDownLatch.countDown();
	}

	private void updateRequestWindow(HistoricalRequest request) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(request.getEndDate());
		calendar.add(Calendar.SECOND, (int) -request.getDuration().getDurationInSeconds());
		this.currentRequestStart = calendar.getTime();
		this.currentRequestEnd = request.getEndDate();
	}

	private boolean isBarInRequestWindow(Bar bar) {
		Date dateTime = new Date(bar.time() * 1000);
		return (0 >= this.currentRequestStart.compareTo(dateTime) && 0 <= this.currentRequestEnd.compareTo(dateTime));
	}

	@Override
	public void historicalData(Bar bar, boolean hasGaps) {
		if (this.isBarInRequestWindow(bar)) {
			this.requestEventHandler.barReceived(bar, hasGaps);
		}
	}

	@Override
	public void historicalDataEnd() {
		this.countDownLatch.countDown();
	}

	public static RequestRunner getRequestRunner() {
		return requestRunner;
	}
}
