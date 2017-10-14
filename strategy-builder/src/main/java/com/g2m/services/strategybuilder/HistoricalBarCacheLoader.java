package com.g2m.services.strategybuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.historicaldataloader.DataLoaderEventHandler;
import com.g2m.services.historicaldataloader.DurationWithUnit;
import com.g2m.services.historicaldataloader.HistoricalDataLoader;
import com.g2m.services.historicaldataloader.HistoricalRequest;
import com.g2m.services.tradingservices.brokerage.mappers.BrokerageEnumMapper;
import com.g2m.services.tradingservices.caches.BarCache;
import com.g2m.services.tradingservices.entities.Bar.BarBuilder;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.variables.persistence.DBRetrieval;
import com.g2m.services.variables.utilities.DBObjectConverter;
import com.ib.controller.Bar;
import com.ib.controller.Types.DurationUnit;
import com.ib.controller.Types.WhatToShow;
import com.mongodb.DBObject;

/**
 * Added 6/30/2015.
 * 
 * @author michaelborromeo
 */
@Component
public class HistoricalBarCacheLoader {
	final static Logger LOGGER = Logger.getLogger(HistoricalBarCacheLoader.class);
	@Autowired
	private BarCache barCache;
	private HistoricalDataLoader dataLoader;
	private DataLoaderEventHandler dataLoaderEventHandler;
	private Security security;
	private BarSize barSize;

	public HistoricalBarCacheLoader() {
		dataLoaderEventHandler = new CacheLoaderEventHandler();
		dataLoader = new HistoricalDataLoader();
	}

	public void loadBarsFromIB(Security security, Date dateTime, BarSize barSize, int barCount) throws Exception {
		this.security = security;
		this.barSize = barSize;

		// set from security
		dataLoader.setSymbol(security.getSymbol());
		dataLoader.setExchange(security.getExchange());
		dataLoader.setCurrency(security.getCurrency());
		dataLoader.setSecType(BrokerageEnumMapper.getSecurityType(security.getSecurityType()));
		dataLoader.setRight(BrokerageEnumMapper.getRight(security.getRight()));
		dataLoader.setExpiry(security.getExpiry());
		dataLoader.setLocalSymbol(security.getLocalSymbol());
		dataLoader.setMultiplier(security.getMultiplier());
		dataLoader.setTradingClass(security.getTradingClass());

		dataLoader.setEndDate(dateTime);
		dataLoader.setIncludeExpired(false);
		dataLoader.setRegularTradingHours(false);
		dataLoader.setDurationWithUnit(new DurationWithUnit(DurationUnit.SECOND, barCount * barSize.getSecondsInBar()));
		dataLoader.setBarSize(BrokerageEnumMapper.getBarSize(barSize));
		dataLoader.setWhatToShow(WhatToShow.TRADES);
		dataLoader.setStartWithLastSuccessfulRequest(false);
		dataLoader.setAsync(false);
		dataLoader.setSaveOutputToFile(false);

		dataLoader.startRequest(dataLoaderEventHandler);
	}

	public String getHost() {
		return dataLoader.getIbHost();
	}

	public void setHost(String host) {
		dataLoader.setIbHost(host);
	}

	public int getPort() {
		return dataLoader.getIbPort();
	}

	public void setPort(int port) {
		dataLoader.setIbPort(port);
	}

	public int getClientId() {
		return dataLoader.getIbClientId();
	}

	public void setClientId(int clientId) {
		dataLoader.setIbClientId(clientId);
	}

	public Security getSecurity() {
		return security;
	}

	public void setSecurity(Security security) {
		this.security = security;
	}

	public BarSize getBarSize() {
		return barSize;
	}

	public void setBarSize(BarSize barSize) {
		this.barSize = barSize;
	}

	private class CacheLoaderEventHandler implements DataLoaderEventHandler {
		@Override
		public void barsReceived(List<Bar> bars) {
			BarBuilder builder = new BarBuilder();
			builder.setSecurity(security);
			builder.setBarSize(barSize);
			for (Bar bar : bars) {
				builder.setDateTime(new Date(1000 * bar.time()));
				builder.setHigh(bar.high());
				builder.setLow(bar.low());
				builder.setOpen(bar.open());
				builder.setClose(bar.close());
				builder.setVolume(bar.volume());
				barCache.save(builder.build());
			}
		}

		@Override
		public void allRequestsComplete(List<HistoricalRequest> requests) {
			LOGGER.debug("All requests complete.");
		}

		@Override
		public void message(String message) {
			LOGGER.debug(message);
		}

		@Override
		public void debug(String message) {
			LOGGER.debug(message);
		}
	}

	public void loadBarsFromDB(Security security, BarSize barSize) throws Exception {

		DBRetrieval barRetriever = new DBRetrieval();
		
		List<DBObject> oldBars = barRetriever.getBarsFromDB(security, barSize);
		// We get the 10K most recent bars, we reverse them so that we can put them into  
		// memory in chronological order
		Collections.reverse(oldBars);
		for(DBObject object : oldBars){
			barCache.save(DBObjectConverter.convertFromObjectToBar(object));
		}
		System.out.println(this.getClass() + " NUM BARS IN CACHE FOR " 
				+ security.getSymbol() + " & BAR SIZE "
				+ barSize + " IS  -  "
				+ barCache.getBarGroupCount(security.getKey(), barSize));
	}
}
