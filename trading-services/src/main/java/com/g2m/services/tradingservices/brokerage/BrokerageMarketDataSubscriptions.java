package com.g2m.services.tradingservices.brokerage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.entities.PendingTick;
import com.g2m.services.tradingservices.entities.Security;

/**
 * Added 3/22/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class BrokerageMarketDataSubscriptions {
	private Map<Security, BrokerageMarketDataSubscriber> marketDataSubscribers;
	private Map<Security, PendingTick> pendingTicks;
	private List<PendingTick> updatedPendingTicks;
	
	public BrokerageMarketDataSubscriptions() {
		this.marketDataSubscribers = new HashMap<Security, BrokerageMarketDataSubscriber>();
		this.pendingTicks = new HashMap<Security, PendingTick>();
		this.updatedPendingTicks = new ArrayList<PendingTick>();
	}
	
	public void addMarketDataSubscription(BrokerageMarketDataSubscriber marketDataSubscriber, Security security) {
		marketDataSubscribers.put(security, marketDataSubscriber);
		marketDataSubscribers.put(security, marketDataSubscriber);
		pendingTicks.put(security, new PendingTick(security, marketDataSubscriber));
	}

	public List<PendingTick> getPendingTickUpdates() {
		this.updatedPendingTicks.clear();
		for (Entry<Security, BrokerageMarketDataSubscriber> subscriberSet : this.marketDataSubscribers.entrySet()) {
			Security security = subscriberSet.getKey();
			BrokerageMarketDataSubscriber subscriber = subscriberSet.getValue();
			if (subscriber.isUpdated()) {
				this.updatedPendingTicks.add(this.pendingTicks.get(security));
				subscriber.clearUpdatedFlag();
			}
		}

		return this.updatedPendingTicks;
	}
}
