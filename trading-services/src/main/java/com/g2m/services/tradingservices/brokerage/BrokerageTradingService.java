package com.g2m.services.tradingservices.brokerage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.BarPublisher;
import com.g2m.services.tradingservices.TradingService;

/**
 * Added 4/1/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class BrokerageTradingService extends TradingService {
	@Autowired
	private BrokerageConnection connection;

	@Autowired
	public BrokerageTradingService(BrokerageAccount brokerageAccount, BrokerageTrader brokerageTrader,
			BrokerageTickPublisher brokerageTickPublisher, BarPublisher barPublisher) {
		account = brokerageAccount;
		trader = brokerageTrader;
		tickPublisher = brokerageTickPublisher;
		this.barPublisher = barPublisher;
		
	}

	public void start(String server, int port, int clientId) {
		subscribeBarsPublishersToTickPublishers();
		connection.connect(server, port, clientId);
		((BrokerageAccount) account).subscribeToUpdates();
		((BrokerageTickPublisher) tickPublisher).startPublishing();
	}

	public void stop() {
		((BrokerageTickPublisher) tickPublisher).stopPublishing();
		connection.disconnect();
	}
}
