package com.g2m.services.tradingservices.persistence;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.g2m.services.tradingservices.entities.Position;

/**
 * Added 5/29/2015.
 * 
 * @author Michael Borromeo
 */
public interface PositionRepository extends MongoRepository<Position, String> {
}
