package com.g2m.services.historicaldataloader.mains;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.g2m.services.historicaldataloader.DataLoaderEventHandler;
import com.g2m.services.historicaldataloader.DurationWithUnit;
import com.g2m.services.historicaldataloader.HistoricalDataLoader;
import com.g2m.services.historicaldataloader.HistoricalEntityTypes;
import com.g2m.services.historicaldataloader.HistoricalRequest;
import com.ib.controller.Bar;
import com.ib.controller.Types.BarSize;
import com.ib.controller.Types.DurationUnit;
import com.ib.controller.Types.Right;
import com.ib.controller.Types.SecType;
import com.ib.controller.Types.WhatToShow;

/**
 * @author Michael Borromeo
 */
public class Main1 implements DataLoaderEventHandler {
	public static void main(String[] args) throws IOException {
		System.out.println("starting");
		HistoricalDataLoader loader = new HistoricalDataLoader();

		loader.setSymbol("ES");
		loader.setExchange("GLOBEX");
		loader.setCurrency("USD");
		loader.setSecType(SecType.FUT);
		loader.setRight(Right.None);
		loader.setExpiry("201509");

		loader.setLocalSymbol("");
		loader.setMultiplier("");
		loader.setTradingClass("");

		Calendar endDate = Calendar.getInstance();
		endDate.setTime(new Date());
		endDate.add(Calendar.DATE, -1);
		loader.setEndDate(endDate.getTime());

		loader.setIncludeExpired(false);

		loader.setRegularTradingHours(false);
		loader.setDurationWithUnit(new DurationWithUnit(DurationUnit.MONTH, 1));
		loader.setBarSize(BarSize._5_mins);
		loader.setWhatToShow(WhatToShow.TRADES);

		loader.setStartWithLastSuccessfulRequest(false);
		loader.setAsync(false);

		loader.setSaveOutputToFile(false);
		loader.setSaveToFileMode(HistoricalEntityTypes.BARS);
		loader.setOutputFolder("/Users/grantseward/HistoricalDataLoader");

		System.out.println("Starting requests");
		loader.startRequest(new Main1());
		System.out.println("Requests complete");
	}

	@Override
	public void allRequestsComplete(List<HistoricalRequest> requests) {
		System.out.println("All requests complete");
	}

	@Override
	public void message(String message) {
		System.out.println(message);
	}

	@Override
	public void debug(String message) {
		System.out.println(message);
	}

	@Override
	public void barsReceived(List<Bar> bars) {
		for (Bar bar : bars) {
			System.out.println(bar.toString());
		}
	}
}
