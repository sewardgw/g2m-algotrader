package com.g2m.strategies.examples;

import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import com.g2m.services.strategybuilder.HistoricalBarCacheLoader;
import com.g2m.services.strategybuilder.Strategy;
import com.g2m.services.strategybuilder.StrategyComponent;
import com.g2m.services.tradingservices.SecurityRegistry;
import com.g2m.services.tradingservices.caches.BarCache;
import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityBuilder;
import com.g2m.services.tradingservices.entities.Tick;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.tradingservices.enums.SecurityType;
import com.g2m.services.variables.entities.MovingAverage;

/**
 * Added 6/28/2015.
 * 
 * @author Michael Borromeo
 */
@StrategyComponent
public class PreloadHistoricalDataIntoCacheForLive extends Strategy {
	@Autowired
	private HistoricalBarCacheLoader cacheLoader;
	@Autowired
	private BarCache barCache;
	private Security security;
	private BarSize barSize = BarSize._5_MINS;

	public static void main(String... args) {
		Strategy.initialize(PreloadHistoricalDataIntoCacheForLive.class);
	}

	private void createSecurity() {
		SecurityBuilder builder = new SecurityBuilder();
		builder.setCurrency("USD");
		builder.setExchange("GLOBEX");
		builder.setExpiry(2015, Calendar.SEPTEMBER);
		builder.setSecurityType(SecurityType.FUTURE);
		builder.setSymbol("ES");
		security = builder.build();
		SecurityRegistry.add(security);
	}

	private void preloadBarsIntoCache() {
		int barCount = 30;
		try {
			// loads historical bars into the bar cache
			cacheLoader.setHost("localhost");
			cacheLoader.setPort(4001);
			cacheLoader.setClientId(1);
			cacheLoader.loadBarsFromIB(security, new Date(), barSize, barCount);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// make sure the bar cache now has at least the requested number of bars
		//Assert.assertTrue(barCount <= barCache.getBarGroupCount(security.getKey(), barSize));
	}

	@Override
	protected void run() {
		createSecurity();
		preloadBarsIntoCache();
		subscribeToTicks(security);
		start("localhost", 4001, 2);
	}

	@Override
	protected void tickReceived(Tick tick) {
		MovingAverage value = getVariables(MovingAverage.createParameters(security.getKey(), BarSize._5_MINS, 20),
				tick.getDateTime());
		System.out.println(value.toString());
	}

	@Override
	protected void barCreated(Bar bar) {
	}
}
