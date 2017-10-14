package com.g2m.services.tradingservices.persistence;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.shared.persistthreads.EntityPersistThread;
import com.g2m.services.tradingservices.entities.Bar;

/**
 * Added 4/25/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class BarPersistThread extends EntityPersistThread<Bar> {
	@Autowired
	private BarRepository barRepository;

	@Override
	protected void saveItems(List<Bar> items) {
		barRepository.save(items);
	}
}
