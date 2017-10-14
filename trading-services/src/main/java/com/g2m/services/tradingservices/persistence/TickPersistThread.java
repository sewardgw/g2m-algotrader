package com.g2m.services.tradingservices.persistence;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.shared.persistthreads.EntityPersistThread;
import com.g2m.services.tradingservices.entities.Tick;

/**
 * Added 4/25/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class TickPersistThread extends EntityPersistThread<Tick> {
	@Autowired
	private TickRepository tickRepository;

	@Override
	protected void saveItems(List<Tick> items) {
		tickRepository.save(items);
	}
}
