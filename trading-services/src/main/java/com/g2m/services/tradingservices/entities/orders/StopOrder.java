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
public class StopOrder extends Order {
	private double stopPrice;
	private boolean readyToFill;

	public StopOrder(OrderAction action, int quantity, Security security, double stopPrice) {
		orderKey = new OrderKey();
		readyToFill = false;
		orderType = OrderType.STOP;
		this.orderAction = action;
		this.quantity = quantity;
		this.security = security;
		this.stopPrice = stopPrice;
	}

	public double getStopPrice() {
		return stopPrice;
	}

	@Override
	public Order copyOrder() {
		StopOrder newStopOrder = new StopOrder(orderAction, quantity, security, stopPrice);
		newStopOrder.orderKey = orderKey;
		return newStopOrder;
	}

	@Override
	public boolean isReadyToFill(double price) {
		// once it is marked as ready to fill then it stays that way regardless of the price
		if (readyToFill) {
			return true;
		}
		if (OrderAction.BUY.equals(orderAction)) {
			if (price >= stopPrice) {
				readyToFill = true;
				return true;
			}
		} else if (OrderAction.SELL.equals(orderAction)) {
			if (price <= stopPrice) {
				readyToFill = true;
				return true;
			}
		}
		return false;
	}
}
