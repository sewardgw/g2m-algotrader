package com.g2m.services.tradingservices;

import com.g2m.services.tradingservices.entities.Tick;

/**
 * Added 3/29/2015.
 * 
 * @author Michael Borromeo
 */
public interface TickSubscriber {
	public void tickReceived(Tick tick);
	public void tickReceivedFillCacheOnly(Tick tick);
}
