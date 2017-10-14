package com.g2m.services.tradingservices;

import java.util.List;

import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.entities.orders.OrderKey;

/**
 * Added 3/15/2015.
 * 
 * @author Michael Borromeo
 */
public interface Trader {
	
	public void submitOrder(Order order);

	public void cancelOrder(OrderKey orderKey);

	public Order getOrder(OrderKey orderKey);

	public List<Order> getOrders(SecurityKey securityKey);
}
