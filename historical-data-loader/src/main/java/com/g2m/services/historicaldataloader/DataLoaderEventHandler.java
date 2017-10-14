package com.g2m.services.historicaldataloader;

import java.util.List;

import com.ib.controller.Bar;

/**
 * @author Michael Borromeo
 */
public interface DataLoaderEventHandler {
	public void barsReceived(List<Bar> bars);

	public void allRequestsComplete(List<HistoricalRequest> requests);

	public void message(String message);

	public void debug(String message);
}
