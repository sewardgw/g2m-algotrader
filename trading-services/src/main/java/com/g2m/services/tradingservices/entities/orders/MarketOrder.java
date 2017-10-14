package com.g2m.services.tradingservices.entities.orders;

import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.enums.OrderAction;
import com.g2m.services.tradingservices.enums.OrderType;

/**
 * Added 5/7/2015.
 * 
 * @author Michael Borromeo
 */
public class MarketOrder extends Order {
	public MarketOrder(OrderAction action, int quantity, Security security) {
		orderKey = new OrderKey();
		orderType = OrderType.MARKET;
		this.orderAction = action;
		this.quantity = quantity;
		this.security = security;
	}

	@Override
	public Order copyOrder() {
		MarketOrder newMarketOrder = new MarketOrder(orderAction, quantity, security);
		newMarketOrder.orderKey = orderKey;
		return newMarketOrder;
	}

	@Override
	public boolean isReadyToFill(double price) {
		return true;
	}
}
