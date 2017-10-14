package com.g2m.services.historicaldataloader;

import com.ib.controller.Bar;

/**
 * @author Michael Borromeo
 */
public interface RequestRunnerEventHandler {
	public void requestMessage(String message);

	public void requestStarted(HistoricalRequest historicalRequest);

	public void barReceived(Bar bar, boolean hasGaps);

	public void requestCompleted(HistoricalRequest historicalRequest, int requestsRemaining);

	public void allRequestsCompleted();
}
