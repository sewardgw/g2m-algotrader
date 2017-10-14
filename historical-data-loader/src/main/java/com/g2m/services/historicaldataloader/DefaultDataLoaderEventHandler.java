package com.g2m.services.historicaldataloader;

import java.util.List;

import com.ib.controller.Bar;

/**
 * Added 7/5/2015.
 * 
 * @author Michael Borromeo
 */
public abstract class DefaultDataLoaderEventHandler implements DataLoaderEventHandler {
	@Override
	public void barsReceived(List<Bar> bars) {
	}

	@Override
	public void allRequestsComplete(List<HistoricalRequest> requests) {
	}

	@Override
	public void message(String message) {
	}

	@Override
	public void debug(String message) {
	}
}
