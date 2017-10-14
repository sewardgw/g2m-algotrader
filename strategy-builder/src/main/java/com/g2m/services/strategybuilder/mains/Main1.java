package com.g2m.services.strategybuilder.mains;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.g2m.services.tradingservices.entities.Bar.BarBuilder;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityBuilder;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.tradingservices.enums.SecurityType;
import com.g2m.services.tradingservices.persistence.BarPersistThread;

/**
 * @author Michael Borromeo
 */
@EnableAutoConfiguration
@ComponentScan("com.g2m.services.strategybuilder")
@EnableMongoRepositories("com.g2m.services.strategybuilder.repositories")
public class Main1 {
	@Autowired
	private BarPersistThread persistThread;

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(Main1.class);
		ApplicationContext context = application.run();
		Main1 main = context.getBean(Main1.class);
		
		System.out.println("Let's inspect the beans provided by Spring Boot:");
		String[] beanNames = context.getBeanDefinitionNames();
		Arrays.sort(beanNames);
		for (String beanName : beanNames) {
			System.out.println(beanName);
		}

		main.run();
	}

	private void run() {
		persistThread.start();

		SecurityBuilder securityBuilder = new SecurityBuilder();
		securityBuilder.setSymbol("ES");
		securityBuilder.setExchange("GLOBEX");
		securityBuilder.setExpiry(2015, Calendar.JUNE);
		securityBuilder.setSecurityType(SecurityType.FUTURE);
		Security security = securityBuilder.build();

		for (int i = 0; i < 5; i++) {
			System.out.println("Offering " + i);
			BarBuilder barBuilder = new BarBuilder();
			barBuilder.setBarSize(BarSize._10_MINS);
			barBuilder.setOpen(Math.random());
			barBuilder.setClose(Math.random());
			barBuilder.setHigh(Math.random());
			barBuilder.setLow(Math.random());
			barBuilder.setDateTime(new Date());
			barBuilder.setVolume(1);
			barBuilder.setSecurity(security);
			persistThread.persist(barBuilder.build());
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		persistThread.stopRunning();
	}
}
