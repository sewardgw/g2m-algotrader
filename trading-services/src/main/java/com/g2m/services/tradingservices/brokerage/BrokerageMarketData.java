package com.g2m.services.tradingservices.brokerage;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.brokerage.mappers.BrokerageSecurityMapper;
import com.g2m.services.tradingservices.entities.PendingTick;
import com.g2m.services.tradingservices.entities.Security;

/**
 * Added 4/4/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class BrokerageMarketData {
	@Autowired
	private BrokerageConnection connection;
	@Autowired
	private BrokerageMarketDataSubscriptions marketDataSubscriptions;

	public void requestMarketData(Security security) {
		BrokerageMarketDataSubscriber marketDataSubscriber = new BrokerageMarketDataSubscriber();
		marketDataSubscriptions.addMarketDataSubscription(marketDataSubscriber, security);
		submitMarketDataRequestToBroker(marketDataSubscriber, security);
	}

	private void submitMarketDataRequestToBroker(BrokerageMarketDataSubscriber marketDataSubscriber, Security security) {
		connection.getApiController().reqTopMktData(BrokerageSecurityMapper.createContract(security), "", false,
				marketDataSubscriber.getMarketDataHandler());
	}

	public List<PendingTick> getPendingTickUpdates() {
		return marketDataSubscriptions.getPendingTickUpdates();
	}
}
