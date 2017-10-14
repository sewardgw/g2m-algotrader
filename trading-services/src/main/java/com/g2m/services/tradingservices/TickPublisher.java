package com.g2m.services.tradingservices;

import com.g2m.services.tradingservices.entities.Security.SecurityKey;

/**
 * Added 4/16/2015.
 * 
 * @author Michael Borromeo
 */
public interface TickPublisher {
	public void addTickSubscriber(SecurityKey securityKey, TickSubscriber tickSubscriber);
}
