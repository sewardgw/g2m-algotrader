package com.g2m.services.tradingservices.brokerage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.SecurityRegistry;
import com.g2m.services.tradingservices.Trader;
import com.g2m.services.tradingservices.brokerage.mappers.BrokerageEnumMapper;
import com.g2m.services.tradingservices.brokerage.mappers.BrokerageSecurityMapper;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.orders.LimitOrder;
import com.g2m.services.tradingservices.entities.orders.MarketOrder;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.entities.orders.OrderKey;
import com.g2m.services.tradingservices.entities.orders.StopOrder;
import com.ib.controller.ApiController.ILiveOrderHandler;
import com.ib.controller.ApiController.IOrderHandler;
import com.ib.controller.NewContract;
import com.ib.controller.NewOrder;
import com.ib.controller.NewOrderState;
import com.ib.controller.OrderStatus;

/**
 * Added 3/15/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class BrokerageTrader implements Trader {
	@Autowired
	private BrokerageConnection connection;
	private Map<SecurityKey, List<Order>> ordersBySecurityMap;
	private Map<OrderKey, Order> ordersByKeyMap;
	private Map<Long, Integer> orderIdsByPermanentIdMap;
	private boolean subscribedToAllOrderEvents;
	private AllOrdersHandler allOrdersHandler;

	public BrokerageTrader() {
		subscribedToAllOrderEvents = false;
		initializeMaps();
	}

	private void initializeMaps() {
		ordersBySecurityMap = new HashMap<SecurityKey, List<Order>>();
		ordersByKeyMap = new HashMap<OrderKey, Order>();
		orderIdsByPermanentIdMap = new HashMap<Long, Integer>();
	}

	@Override
	public void submitOrder(Order order) {
		Order newOrder = order.copyOrder();
		checkIfSubscribedToAllOrderEvents();
		newOrder.setOrderStates(new BrokerageOrderStates());
		addOrderToMap(newOrder);
		submitOrderToBrokerage(newOrder);
	}

	private void checkIfSubscribedToAllOrderEvents() {
		if (!subscribedToAllOrderEvents) {
			allOrdersHandler = new AllOrdersHandler();
			connection.getApiController().reqLiveOrders(allOrdersHandler);
			subscribedToAllOrderEvents = true;
		}
	}

	private void submitOrderToBrokerage(Order order) {
		connection.getApiController().placeOrModifyOrder(createContract(order), createOrder(order),
				((BrokerageOrderStates) order.getOrderStates()).getOrderHandler());
	}

	private NewOrder createOrder(Order order) {
		NewOrder ibOrder = new NewOrder();
		ibOrder.account(connection.getAccountCodes().get(0));
		ibOrder.action(BrokerageEnumMapper.getOrderAction(order.getOrderAction()));
		ibOrder.totalQuantity(order.getQuantity());
		ibOrder.orderType(BrokerageEnumMapper.getOrderType(order.getOrderType()));
		ibOrder.transmit(true);
		setDataSpecificToOrderType(order, ibOrder);
		return ibOrder;
	}

	private void setDataSpecificToOrderType(Order order, NewOrder ibOrder) {
		if (order instanceof MarketOrder) {
			// nothing more to set
		} else if (order instanceof LimitOrder) {
			ibOrder.lmtPrice(((LimitOrder) order).getLimitPrice());
		} else if (order instanceof StopOrder) {
			ibOrder.auxPrice(((StopOrder) order).getStopPrice());
		}
	}

	private NewContract createContract(Order order) {
		return BrokerageSecurityMapper.createContract(SecurityRegistry.get(order.getSecurityKey()));
	}

	private void addOrderToMap(Order order) {
		if (!ordersBySecurityMap.containsKey(order.getSecurityKey())) {
			ordersBySecurityMap.put(order.getSecurityKey(), new ArrayList<Order>());
		}
		ordersBySecurityMap.get(order.getSecurityKey()).add(order);
		ordersByKeyMap.put(order.getKey(), order);
	}

	@Override
	public void cancelOrder(OrderKey orderKey) {
		if (ordersByKeyMap.containsKey(orderKey)) {
			connection.getApiController().cancelOrder(orderIdsByPermanentIdMap
					.get(ordersByKeyMap.get(orderKey).getOrderStates().getLatestOrderState().getId()));
		}
	}

	@Override
	public Order getOrder(OrderKey orderKey) {
		return ordersByKeyMap.get(orderKey);
	}

	@Override
	public List<Order> getOrders(SecurityKey securityKey) {
		return ordersBySecurityMap.get(securityKey);
	}

	/*
	 * Since the IB API doesn't provide the orderId which is needed to cancel orders to the
	 * IOrderHandler, only to the ILiveOrderHandler, we need to save the orderId here by mapping the
	 * permId to the orderId. (the permId is available to orders). The order events are fully
	 * handled in the BrokerageOrderState object.
	 */
	public class AllOrdersHandler implements ILiveOrderHandler {
		@Override
		public void openOrder(NewContract contract, NewOrder order, NewOrderState orderState) {
			// intentionally blank
		}

		@Override
		public void openOrderEnd() {
			// intentionally blank
		}

		@Override
		public void orderStatus(int orderId, OrderStatus status, int filled, int remaining, double avgFillPrice, long permId,
				int parentId, double lastFillPrice, int clientId, String whyHeld) {
			Long permanentId = Long.valueOf(permId);
			if (!orderIdsByPermanentIdMap.containsKey(permanentId)) {
				Integer orderIdObj = Integer.valueOf(orderId);
				orderIdsByPermanentIdMap.put(permanentId, orderIdObj);
			}
			if (!status.isActive()) {
				connection.getApiController().removeOrderHandler(findOrderHandlerByPermanentId(permId));
			}
		}

		private IOrderHandler findOrderHandlerByPermanentId(long permanentId) {
			for (SecurityKey securityKey : ordersBySecurityMap.keySet()) {
				for (Order order : ordersBySecurityMap.get(securityKey)) {
					if (order.getLatestOrderState().getId() == permanentId) {
						return ((BrokerageOrderStates) order.getOrderStates()).getOrderHandler();
					}
				}
			}
			return null;
		}

		@Override
		public void handle(int orderId, int errorCode, String errorMsg) {
			// intentionally blank
		}
	}
}
