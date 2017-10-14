package com.g2m.services.variables.persistence;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.shared.persistthreads.EntityPersistThread;
import com.g2m.services.variables.entities.Variable;

/**
 * Added 4/25/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class VariablePersistThread extends EntityPersistThread<Variable> {
	@Autowired
	private VariableRepository variableRepository;

	@Override
	protected void saveItems(List<Variable> items) {
		variableRepository.save(items);
	}
}
