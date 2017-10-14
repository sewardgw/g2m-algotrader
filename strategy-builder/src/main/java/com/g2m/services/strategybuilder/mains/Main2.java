package com.g2m.services.strategybuilder.mains;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.g2m.services.strategybuilder.HistoricalBarCacheLoader;
import com.g2m.services.tradingservices.caches.BarCache;
import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityBuilder;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.tradingservices.enums.SecurityType;

/**
 * @author Michael Borromeo
 */
@EnableAutoConfiguration
@ComponentScan("com.g2m.services")
@EnableMongoRepositories({ "com.g2m.services.tradingservices.persistence", "com.g2m.services.variables.persistence" })
public class Main2 {
	@Autowired
	private HistoricalBarCacheLoader cacheLoader;
	@Autowired
	private BarCache barCache;

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(Main2.class);
		ApplicationContext context = application.run();
		Main2 main = context.getBean(Main2.class);
		main.run();
	}

	private void run() {
		SecurityBuilder securityBuilder = new SecurityBuilder();
		securityBuilder.setSymbol("ES");
		securityBuilder.setExchange("GLOBEX");
		securityBuilder.setExpiry(2015, Calendar.SEPTEMBER);
		securityBuilder.setSecurityType(SecurityType.FUTURE);
		Security security = securityBuilder.build();
		Date now = new Date();
		try {
			cacheLoader.loadBarsFromIB(security, now, BarSize._5_MINS, 10);
		} catch (Exception e) {
			e.printStackTrace();
		}
		List<Bar> bars = barCache.getBars(security.getKey(), BarSize._5_MINS, now, 10);
		for (Bar bar : bars) {
			System.out.println(bar.toString());
		}
	}
}
