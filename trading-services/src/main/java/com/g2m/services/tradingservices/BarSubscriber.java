package com.g2m.services.tradingservices;

import com.g2m.services.tradingservices.entities.Bar;

/**
 * Added 4/1/2015.
 * 
 * @author Michael Borromeo
 */
public interface BarSubscriber {
	public void barCreated(Bar bar);
	public void barCreatedFillCacheOnly(Bar bar);
}
