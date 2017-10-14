package com.g2m.services.tradingservices.mains;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import com.g2m.services.tradingservices.SecurityRegistry;
import com.g2m.services.tradingservices.TickSubscriber;
import com.g2m.services.tradingservices.backtest.BacktestTickPublisher;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityBuilder;
import com.g2m.services.tradingservices.entities.Tick;
import com.g2m.services.tradingservices.enums.SecurityType;

/**
 * @author Michael Borromeo
 */
@EnableAutoConfiguration
@ComponentScan("com.g2m.services.tradingservices")
public class Main4 implements TickSubscriber {
	@Autowired
	private BacktestTickPublisher tickPublisher;

	public static void main(String[] args) throws URISyntaxException {
		SpringApplication application = new SpringApplication(Main4.class);
		ApplicationContext context = application.run();
		Main4 main = context.getBean(Main4.class);
		main.run();
	}

	private void run() throws URISyntaxException {
		SecurityBuilder securityBuilder = new SecurityBuilder();
		securityBuilder.setSymbol("ES");
		securityBuilder.setExchange("GLOBEX");
		securityBuilder.setExpiry(2015, Calendar.JUNE);
		securityBuilder.setSecurityType(SecurityType.FUTURE);
		Security security1 = securityBuilder.build();

		securityBuilder.setSymbol("SX");
		securityBuilder.setExpiry(2015, Calendar.SEPTEMBER);
		Security security2 = securityBuilder.build();

		SecurityRegistry.add(security1);
		SecurityRegistry.add(security2);

		URL url1 = Main4.class.getClassLoader().getResource("security1.csv");
		URL url2 = Main4.class.getClassLoader().getResource("security2.csv");

		tickPublisher.addTestFile(security1.getKey(), Paths.get(url1.toURI()).toAbsolutePath().toString());
		tickPublisher.addTestFile(security2.getKey(), Paths.get(url2.toURI()).toAbsolutePath().toString());

		tickPublisher.addTickSubscriber(security1.getKey(), this);
		tickPublisher.addTickSubscriber(security2.getKey(), this);

		tickPublisher.startPublishing();
	}

	@Override
	public void tickReceived(Tick tick) {
		System.out.println(tick.toString());
	}
	
	@Override
	public void tickReceivedFillCacheOnly(Tick tick) {
		System.out.println(tick.toString());
	}
}
