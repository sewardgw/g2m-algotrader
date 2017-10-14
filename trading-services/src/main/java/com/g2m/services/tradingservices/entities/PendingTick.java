package com.g2m.services.tradingservices.entities;

import java.util.Date;

import com.g2m.services.tradingservices.brokerage.BrokerageMarketDataSubscriber;
import com.g2m.services.tradingservices.entities.Tick.TickBuilder;

/**
 * Added 3/26/2015.
 * 
 * @author Michael Borromeo
 */
public class PendingTick {
	private Security security;
	private BrokerageMarketDataSubscriber marketDataSubscriber;

	public PendingTick(Security security, BrokerageMarketDataSubscriber marketDataSubscriber) {
		this.security = security;
		this.marketDataSubscriber = marketDataSubscriber;
	}

	public Tick createTick() {
		TickBuilder tickBuilder = new TickBuilder();
		tickBuilder.setSecurity(security);
		tickBuilder.setAskPrice(marketDataSubscriber.getAskPrice());
		tickBuilder.setBidPrice(marketDataSubscriber.getBidPrice());
		tickBuilder.setDateTime(new Date());
		tickBuilder.setLastPrice(marketDataSubscriber.getLastPrice());
		tickBuilder.setVolume(marketDataSubscriber.getVolume());
		tickBuilder.setVolumeAsk(marketDataSubscriber.getAskSize());
		tickBuilder.setVolumeBid(marketDataSubscriber.getBidSize());

		// TODO not sure where these come from
		tickBuilder.setChange(0);
		tickBuilder.setOpenInterest(0);
		tickBuilder.setSettlement(0);

		return tickBuilder.build();
	}
}
