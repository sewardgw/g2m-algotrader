package com.g2m.services.tradingservices;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.enums.BarSize;

/**
 * Added 3/15/2015.
 * 
 * @author Michael Borromeo
 */
public abstract class TradingService {
	protected Account account;
	protected Trader trader;
	protected TickPublisher tickPublisher;
	protected BarPublisher barPublisher;
	
	protected void registrySecurities(Collection<Security> securities) {
		for (Security security : securities) {
			SecurityRegistry.add(security);
		}
	}

	public void addTickSubscriptions(Set<Security> securities, TickSubscriber tickSubscriber) {
		registrySecurities(securities);
		for (Security security : securities) {
			tickPublisher.addTickSubscriber(security.getKey(), tickSubscriber);
		}
	}

	public void addBarSubscriptions(Map<Security, Set<BarSize>> securities, BarSubscriber barSubscriber) {
		registrySecurities(securities.keySet());
		for (Security security : securities.keySet()) {
			Set<BarSize> barSizes = securities.get(security);
			for (BarSize barSize : barSizes) {
				barPublisher.addBarSubscriber(security.getKey(), barSize, barSubscriber);
			}
		}
	}

	protected void subscribeBarsPublishersToTickPublishers() {
		for (SecurityKey securityKey : barPublisher.getSecurityKeys()) {
			tickPublisher.addTickSubscriber(securityKey, barPublisher.getTickSubscriber());
		}
	}

	public Account getAccount() {
		return account;
	}

	public Trader getTrader() {
		return trader;
	}
}
