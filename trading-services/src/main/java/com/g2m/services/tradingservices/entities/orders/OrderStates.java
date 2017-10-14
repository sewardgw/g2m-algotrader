package com.g2m.services.tradingservices.entities.orders;

import java.util.ArrayList;
import java.util.List;

/**
 * Added 5/6/2015.
 * 
 * @author michaelborromeo
 */
public class OrderStates {
	protected List<OrderState> orderStates;

	public OrderStates() {
		orderStates = new ArrayList<OrderState>();
	}

	public List<OrderState> getOrderStates() {
		return orderStates;
	}

	public OrderState getLatestOrderState() {
		if (orderStates.isEmpty()) {
			return null;
		} else {
			return orderStates.get(orderStates.size() - 1);
		}
	}

	public void addOrderState(OrderState orderState) {
		orderStates.add(orderState);
	}
}
