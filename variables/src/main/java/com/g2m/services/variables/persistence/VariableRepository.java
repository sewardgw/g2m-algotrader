package com.g2m.services.variables.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.g2m.services.variables.entities.Variable;

/**
 * Added 4/23/2015.
 * 
 * @author Michael Borromeo
 */
public interface VariableRepository extends MongoRepository<Variable, String> {
}
