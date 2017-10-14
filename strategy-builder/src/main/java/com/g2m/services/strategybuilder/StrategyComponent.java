package com.g2m.services.strategybuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Added 6/29/2015.
 * 
 * @author michaelborromeo
 */
@EnableAutoConfiguration
@ComponentScan("com.g2m")
@EnableMongoRepositories({ "com.g2m.services.tradingservices.persistence", "com.g2m.services.variables.persistence" })
@Retention(RetentionPolicy.RUNTIME)
public @interface StrategyComponent {
}
