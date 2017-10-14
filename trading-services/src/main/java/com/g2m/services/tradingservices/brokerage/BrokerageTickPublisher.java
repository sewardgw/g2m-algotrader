package com.g2m.services.tradingservices.brokerage;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.SecurityRegistry;
import com.g2m.services.tradingservices.TickDispatcher;
import com.g2m.services.tradingservices.TickPublisher;
import com.g2m.services.tradingservices.TickSubscriber;
import com.g2m.services.tradingservices.entities.PendingTick;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;

/**
 * Added 3/29/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class BrokerageTickPublisher implements TickPublisher {
	@Autowired
	private BrokerageMarketData marketData;
	@Autowired
	private TickDispatcher tickDispatcher;
	BrokerageTickPublisherRunner runner;
	private static final long PUBLISH_LOOP_SLEEP_MILLISECONDS = 125;
	private boolean publishing;

	@Override
	public void addTickSubscriber(SecurityKey securityKey, TickSubscriber tickSubscriber) {
		tickDispatcher.addTickSubscriber(securityKey, tickSubscriber);
	}

	public void startPublishing() {
		requestMarketData();
		publishing = true;
		runner = new BrokerageTickPublisherRunner();
		runner.start();
	}

	public void stopPublishing() {
		publishing = false;
		runner.interrupt();
	}

	private void requestMarketData() {
		for (SecurityKey securityKey : tickDispatcher.getSecurityKeys()) {
			Security security = SecurityRegistry.get(securityKey);
			marketData.requestMarketData(security);
		}
	}

	private class BrokerageTickPublisherRunner extends Thread {
		@Override
		public void run() {
			long lastDispatchTimestamp = System.currentTimeMillis();
			while (publishing) {
				trySleeping(lastDispatchTimestamp);
				lastDispatchTimestamp = System.currentTimeMillis();
				dispatchUpdatedTicks();
			}
		}

		private void dispatchUpdatedTicks() {
			List<PendingTick> ticks = marketData.getPendingTickUpdates();
			if (0 < ticks.size()) {
				tickDispatcher.dispatchPendingTicks(ticks);
			}
		}

		/**
		 * Publish ticks at a regular interval rather since tick data comes in piecemeal.
		 */
		private void trySleeping(long lastDispatchTimestamp) {
			long sleepTime = PUBLISH_LOOP_SLEEP_MILLISECONDS - (System.currentTimeMillis() - lastDispatchTimestamp);
			if (0 < sleepTime) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					if (!publishing) {
						return;
					}
				}
			}
		}
	}
}
