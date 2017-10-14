package com.g2m.strategies.examples;

import com.g2m.services.strategybuilder.Strategy;
import com.g2m.services.strategybuilder.StrategyComponent;
import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Tick;

/**
 * Added 6/28/2015.
 * 
 * @author Michael Borromeo
 */

@StrategyComponent
public class EmptyStrategy extends Strategy {
	public static void main(String... args) {
		Strategy.initialize(EmptyStrategy.class);
	}

	@Override
	protected void run() {
	}

	@Override
	protected void tickReceived(Tick tick) {
	}

	@Override
	protected void barCreated(Bar bar) {
	}
}
