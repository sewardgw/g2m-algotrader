package com.g2m.services.strategybuilder.enums;

/**
 * Added 5/25/2015.
 * 
 * @author michaelborromeo
 */
public enum StrategyStatus {
	NOT_STARTED, STARTED_BACKTEST, STARTED_LIVE, STOPPED;

	public boolean isBacktest() {
		return this == STARTED_BACKTEST;
	}

	public boolean isLive() {
		return this == STARTED_LIVE;
	}

	public boolean isStarted() {
		return this != NOT_STARTED;
	}

	public boolean isRunning() {
		return this == STARTED_BACKTEST || this == STARTED_LIVE;
	}
}
