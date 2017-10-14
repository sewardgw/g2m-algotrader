package com.g2m.services.historicaldataloader;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.ib.controller.Types.BarSize;
import com.ib.controller.Types.Right;
import com.ib.controller.Types.SecType;
import com.ib.controller.Types.WhatToShow;

/**
 * @author Michael Borromeo
 */
public class SimpleDataLoaderParameters {
	private String symbol;
	private String exchange;
	private SecType securityType;
	private Date expiration;
	private Date endDate;
	private boolean regularTradingHours;
	private DurationWithUnit durationWithUnit;
	private BarSize barSize;

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

	public SecType getSecurityType() {
		return securityType;
	}

	public void setSecurityType(SecType securityType) {
		this.securityType = securityType;
	}

	public Date getExpiration() {
		return expiration;
	}

	public void setExpiration(Date expiration) {
		this.expiration = expiration;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public boolean isRegularTradingHours() {
		return regularTradingHours;
	}

	public void setRegularTradingHours(boolean regularTradingHours) {
		this.regularTradingHours = regularTradingHours;
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

	public HistoricalDataLoader createDataLoader() {
		if (null == this.getExpiration() && SecType.FUT == this.getSecurityType()) {
			throw new RuntimeException("Futures must specify an expiration date");
		}

		HistoricalDataLoader loader = new HistoricalDataLoader();
		loader.setIbHost(Constants.DATA_LOADER_SERVER);
		loader.setIbPort(Constants.DATA_LOADER_PORT);
		loader.setIbClientId(Constants.DATA_LOADER_CLIENT_ID);

		loader.setSymbol(this.getSymbol());
		loader.setExchange(this.getExchange());
		loader.setCurrency("USD");
		loader.setSecType(this.getSecurityType());
		loader.setRight(Right.None);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
		loader.setExpiry(SecType.STK == this.getSecurityType() ? "" : sdf.format(this.getExpiration()));
		loader.setLocalSymbol("");
		loader.setMultiplier("");
		loader.setTradingClass("");
		loader.setEndDate(this.getEndDate());
		loader.setRegularTradingHours(this.isRegularTradingHours());
		loader.setDurationWithUnit(this.getDurationWithUnit());
		loader.setBarSize(this.getBarSize());
		loader.setWhatToShow(WhatToShow.TRADES);
		loader.setStartWithLastSuccessfulRequest(false);
		loader.setSaveOutputToFile(false);
		loader.setAsync(false);

		return loader;
	}
}
