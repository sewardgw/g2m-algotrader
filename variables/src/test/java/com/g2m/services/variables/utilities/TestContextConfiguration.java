package com.g2m.services.variables.utilities;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Added 5/1/2015.
 * 
 * @author Michael Borromeo
 */
@ComponentScan("com.g2m.services")
@EnableMongoRepositories({ "com.g2m.services.variables.persistence", "com.g2m.services.tradingservices.persistence" })
@EnableAutoConfiguration
public class TestContextConfiguration {
	// TODO
}