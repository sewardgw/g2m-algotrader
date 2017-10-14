package com.g2m.services.tradingservices.brokerage;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.ib.controller.ApiController.ITopMktDataHandler;
import com.ib.controller.NewTickType;
import com.ib.controller.Types.MktDataType;

/**
 * Added 3/26/2015.
 * 
 * @author Michael Borromeo
 */
public class BrokerageMarketDataSubscriber {
	boolean updated;
	Map<NewTickType, Integer> sizes;
	Map<NewTickType, Double> prices;
	Date lastUpdated;
	private ITopMktDataHandler marketDataHandler;

	public BrokerageMarketDataSubscriber() {
		this.marketDataHandler = new MarketDataHandler();
		this.sizes = new HashMap<NewTickType, Integer>();
		this.prices = new HashMap<NewTickType, Double>();
		this.lastUpdated = new Date();
	}

	public ITopMktDataHandler getMarketDataHandler() {
		return marketDataHandler;
	}

	public double getLastPrice() {
		return this.prices.containsKey(NewTickType.LAST) ? this.prices.get(NewTickType.LAST) : 0;
	}

	public double getBidPrice() {
		return this.prices.containsKey(NewTickType.BID) ? this.prices.get(NewTickType.BID) : 0;
	}

	public double getAskPrice() {
		return this.prices.containsKey(NewTickType.ASK) ? this.prices.get(NewTickType.ASK) : 0;
	}

	public int getBidSize() {
		return this.sizes.containsKey(NewTickType.BID_SIZE) ? this.sizes.get(NewTickType.BID_SIZE) : 0;
	}

	public int getAskSize() {
		return this.sizes.containsKey(NewTickType.ASK_SIZE) ? this.sizes.get(NewTickType.ASK_SIZE) : 0;
	}

	public int getLastSize() {
		return this.sizes.containsKey(NewTickType.LAST_SIZE) ? this.sizes.get(NewTickType.LAST_SIZE) : 0;
	}

	public int getVolume() {
		return this.sizes.containsKey(NewTickType.VOLUME) ? this.sizes.get(NewTickType.VOLUME) : 0;
	}

	public boolean isUpdated() {
		return updated;
	}

	public void clearUpdatedFlag() {
		this.updated = false;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	private class MarketDataHandler implements ITopMktDataHandler {
		@Override
		public void tickPrice(NewTickType tickType, double price, int canAutoExecute) {
			BrokerageMarketDataSubscriber.this.prices.put(tickType, price);
			BrokerageMarketDataSubscriber.this.updated = true;
		}

		@Override
		public void tickSize(NewTickType tickType, int size) {
			BrokerageMarketDataSubscriber.this.sizes.put(tickType, size);
			BrokerageMarketDataSubscriber.this.updated = true;
		}

		@Override
		public void tickString(NewTickType tickType, String value) {
			if (NewTickType.LAST_TIMESTAMP.equals(tickType)) {
				BrokerageMarketDataSubscriber.this.lastUpdated.setTime(1000 * Long.parseLong(value));
				BrokerageMarketDataSubscriber.this.updated = true;
			}
		}

		@Override
		public void tickSnapshotEnd() {
		}

		@Override
		public void marketDataType(MktDataType marketDataType) {
		}
	}
}
