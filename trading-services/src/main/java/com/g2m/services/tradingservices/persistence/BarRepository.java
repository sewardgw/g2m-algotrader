package com.g2m.services.tradingservices.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.g2m.services.tradingservices.entities.Bar;

/**
 * Added 4/23/2015.
 * 
 * @author Michael Borromeo
 */
public interface BarRepository extends MongoRepository<Bar, String> {
}
