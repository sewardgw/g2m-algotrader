package com.g2m.services.tradingservices.backtest;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.BarPublisher;
import com.g2m.services.tradingservices.TradingService;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;

/**
 * Added 4/1/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class BacktestTradingService extends TradingService {
	@Autowired
	public BacktestTradingService(BacktestAccount testAccount, BacktestTrader testTrader,
			BacktestTickPublisher testTickPublisher, BarPublisher barPublisher) {
		account = testAccount;
		trader = testTrader;
		tickPublisher = testTickPublisher;
		this.barPublisher = barPublisher;
	}

	public void start(Map<SecurityKey, String> testDataFiles) {
		addTestFilesToTickPublisher(testDataFiles);
		subscribeBarsPublishersToTickPublishers();
		((BacktestTickPublisher) tickPublisher).startPublishing();
	}

	private void addTestFilesToTickPublisher(Map<SecurityKey, String> testDataFiles) {
		for (SecurityKey securityKey : testDataFiles.keySet()) {
			String filePath = testDataFiles.get(securityKey);
			((BacktestTickPublisher) tickPublisher).addTestFile(securityKey, filePath);
		}
	}
}
