package com.g2m.services.tradingservices.persistence;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.g2m.services.tradingservices.entities.Tick;

/**
 * Added 4/23/2015.
 * 
 * @author Michael Borromeo
 */
public interface TickRepository extends MongoRepository<Tick, String> {
}
