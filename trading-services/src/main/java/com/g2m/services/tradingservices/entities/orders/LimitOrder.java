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
public class LimitOrder extends Order {
	private double limitPrice;
	private boolean readyToFill;

	public LimitOrder(OrderAction action, int quantity, Security security, double limitPrice) {
		orderKey = new OrderKey();
		readyToFill = false;
		orderType = OrderType.LIMIT;
		this.orderAction = action;
		this.quantity = quantity;
		this.security = security;
		this.limitPrice = limitPrice;
	}

	public double getLimitPrice() {
		return limitPrice;
	}

	@Override
	public Order copyOrder() {
		LimitOrder newLimitOrder = new LimitOrder(orderAction, quantity, security, limitPrice);
		newLimitOrder.orderKey = orderKey;
		return newLimitOrder;
	}

	@Override
	public boolean isReadyToFill(double price) {
		// once it is marked as ready to fill then it stays that way regardless of the price
		if (readyToFill) {
			return true;
		}
		if (OrderAction.BUY.equals(orderAction)) {
			if (price <= limitPrice) {
				readyToFill = true;
				return true;
			}
		} else if (OrderAction.SELL.equals(orderAction)) {
			if (price >= limitPrice) {
				readyToFill = true;
				return true;
			}
		}
		return false;
	}
}
