package com.g2m.services.tradingservices.brokerage.mappers;

import com.g2m.services.tradingservices.SecurityRegistry;
import com.g2m.services.tradingservices.entities.Position.PositionBuilder;
import com.g2m.services.tradingservices.entities.Security;

/**
 * Added 3/29/2015.
 * 
 * @author Michael Borromeo
 */
public class BrokeragePositionMapper {
	public static com.g2m.services.tradingservices.entities.Position createPosition(com.ib.controller.Position position,
			Security security) {
		PositionBuilder builder = new PositionBuilder();
		builder.setSecurity(security);
		builder.setAccount(position.account());
		builder.setIBAverageCost(position.averageCost());
		builder.setQuantity(position.position());
		builder.setRealizedPnl(position.realPnl());
		builder.setUnrealizedPnl(position.unrealPnl());
		builder.setOpenPrice(position.averageCost());
		builder.setAverageCost(position.averageCost(), position.position());
		builder.setLastPrice(position.marketPrice());

		return builder.build();
	}
}
