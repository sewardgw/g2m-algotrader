package com.g2m.services.tradingservices.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.shared.persistthreads.EntityPersistThread;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.entities.orders.OrderStates;

/**
 * Added 5/31/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class OrderPersistThread extends EntityPersistThread<Order> {
	@Autowired
	private OrderRepository orderRepository;
	/**
	 * This is a hack until generic events are implemented (currently only tick events are
	 * available). When an order comes in its OrderStates object is saved here along with the
	 * counter of OrderState objects. When every new tick comes through a call to
	 * persistOrderStates() is made which will check if there are additional OrderState objects
	 * saved (i.e. the order state has changed). If so then it will call persist().
	 */
	private Map<Order, Integer> orderStatesMap;

	public OrderPersistThread() {
		super();
		orderStatesMap = new HashMap<Order, Integer>();
	}

	public void addOrderToWatchList(Order order) {
		if (!orderStatesMap.containsKey(order)) {
			orderStatesMap.put(order, 0);
		}
	}

	public void persistOrdersIfChanged() {
		for (Order order : orderStatesMap.keySet()) {
			OrderStates orderStates = order.getOrderStates();
			int orderStatesCount = orderStatesMap.get(order);
			if (orderStates.getOrderStates().size() > orderStatesCount) {
				persist(order);
			}
		}
	}

	@Override
	protected void saveItems(List<Order> items) {
		orderRepository.save(items);
	}
}
