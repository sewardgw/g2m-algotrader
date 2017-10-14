package com.g2m.services.tradingservices.backtest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.ForexConverter;
import com.g2m.services.tradingservices.TickSubscriber;
import com.g2m.services.tradingservices.Trader;
import com.g2m.services.tradingservices.backtest.slippage.SlippageEstimator;
import com.g2m.services.tradingservices.caches.TickCache;
import com.g2m.services.tradingservices.comissions.ComissionHandler;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.Tick;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.entities.orders.OrderKey;
import com.g2m.services.tradingservices.entities.orders.OrderState;
import com.g2m.services.tradingservices.entities.orders.OrderState.OrderStateBuilder;
import com.g2m.services.tradingservices.entities.orders.OrderStates;
import com.g2m.services.tradingservices.enums.OrderAction;
import com.g2m.services.tradingservices.enums.OrderStatus;
import com.g2m.services.tradingservices.enums.OrderStatusReason;
import com.g2m.services.tradingservices.enums.SecurityType;

/**
 * Added 3/15/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class BacktestTrader implements Trader {
	@Autowired
	private BacktestAccount account;
	@Autowired
	BacktestTickPublisher tickPublisher;
	@Autowired
	private TickCache tickCache;
	private Map<SecurityKey, List<Order>> ordersBySecurityMap;
	private Map<OrderKey, Order> ordersByKeyMap;
	private long lastPermanentId;
	private double commission;
	private TickSubscriber catchAllTickSubscriber;
	private static final double DEFAULT_COMMISSION = 5d;
	

	public BacktestTrader() {
		ordersBySecurityMap = new HashMap<SecurityKey, List<Order>>();
		ordersByKeyMap = new HashMap<OrderKey, Order>();
		lastPermanentId = 0;
		commission = DEFAULT_COMMISSION;
	}

	@PostConstruct
	private void subscribeToTickPublisher() {
		catchAllTickSubscriber = new CatchAllTickSubscriber();
		tickPublisher.addCatchAllTickSubscriber(catchAllTickSubscriber);
	}

	@Override
	public void submitOrder(Order order) {
		Order newOrder = order.copyOrder();
		newOrder.setOrderStates(new OrderStates());
		newOrder.addOrderState(createOrderState(order));
		addOrderToMap(newOrder);
		fillOrderIfPossible(newOrder);
	}

	private void fillOrderIfPossible(Order order) {
		if (order.isReadyToFill(order.getOrderPrice())) {
			OrderStatusReason reason = account.canFillOrder(order);
			if (reason.canFillOrder()) {
				fillOrder(order, reason);
			} else if (reason.cannotFillOrder()) {
				cancelOrder(order, reason);
			} else {
				// order can't be filled right now, but maybe in the future, e.g. stop/limit order
			}
		}
	}

	private OrderState createOrderState(Order order) {
		OrderStateBuilder builder = new OrderStateBuilder();
		lastPermanentId++;
		builder.setId(lastPermanentId);
		builder.setAverageFillPrice(0);
		builder.setCommission(ComissionHandler.getCommissionValue(order));
		builder.setCommissionCurrency("USD");
		builder.setEquityWithLoan("");
		builder.setErrorCode(0);
		builder.setErrorMessage("");
		builder.setInitialMargin("");
		builder.setLastFillPrice(0);
		builder.setMaintenanceMargin("");
		builder.setMaximumCommission(commission);
		builder.setMinimumCommission(commission);
		builder.setOrderStatus(OrderStatus.SUBMITTED);
		builder.setQuantityFilled(0);
		builder.setQuantityRemaining(order.getQuantity());
		builder.setWarningText("");
		builder.setWhyHeld("");
		return builder.build();
	}

	private void fillOrder(Order order, OrderStatusReason reason) {
		Tick lastTick = tickCache.getLastTick(order.getSecurityKey());
		double priceWithEstimatedSlippage = SlippageEstimator.estimateSlippage(lastTick, order);
		OrderStateBuilder builder = new OrderStateBuilder(order.getLatestOrderState());
		builder.setOrderStatus(OrderStatus.FILLED);
		builder.setOrderStatusReason(reason);
		builder.setAverageFillPrice(priceWithEstimatedSlippage);
		builder.setLastFillPrice(priceWithEstimatedSlippage);
		builder.setQuantityFilled(order.getQuantity());
		builder.setQuantityRemaining(0);
		order.addOrderState(builder.build());
		order.setOrderPrice(priceWithEstimatedSlippage);
		account.updateAccountFromOrder(order);
	}

	@Override
	public void cancelOrder(OrderKey orderKey) {
		if (ordersByKeyMap.containsKey(orderKey)) {
			cancelOrder(ordersByKeyMap.get(orderKey), OrderStatusReason.MANUALLY_CANCELLED);
		}
	}

	private void cancelOrder(Order order, OrderStatusReason reason) {
		OrderStateBuilder builder = new OrderStateBuilder(order.getLatestOrderState());
		builder.setOrderStatus(OrderStatus.CANCELLED);
		builder.setOrderStatusReason(reason);
		builder.setAverageFillPrice(0);
		builder.setLastFillPrice(0);
		builder.setQuantityFilled(0);
		builder.setQuantityRemaining(order.getQuantity());
		order.addOrderState(builder.build());
	}

	private void addOrderToMap(Order order) {
		if (!ordersBySecurityMap.containsKey(order.getSecurityKey())) {
			ordersBySecurityMap.put(order.getSecurityKey(), new ArrayList<Order>());
		}
		ordersBySecurityMap.get(order.getSecurityKey()).add(order);
		ordersByKeyMap.put(order.getKey(), order);
	}

	@Override
	public Order getOrder(OrderKey orderKey) {
		return ordersByKeyMap.get(orderKey);
	}

	@Override
	public List<Order> getOrders(SecurityKey securityKey) {
		return ordersBySecurityMap.get(securityKey);
	}

	public double getCommission() {
		return commission;
	}

	public void setCommission(double commission) {
		this.commission = commission;
	}

	private List<Order> getActiveOrders(SecurityKey securityKey) {
		List<Order> orders = ordersBySecurityMap.get(securityKey);
		if (null == orders || orders.isEmpty()) {
			return Collections.emptyList();
		}
		List<Order> activeOrders = new ArrayList<Order>();
		for (Order order : orders) {
			if (order.getLatestOrderState().getOrderStatus().isActive()) {
				activeOrders.add(order);
			}
		}
		return activeOrders;
	}

	/**
	 * This class exists so that delayed orders (stop and limit orders) can be reviewed after each
	 * tick to determine if they should be executed.
	 */
	private class CatchAllTickSubscriber implements TickSubscriber {
		@Override
		public void tickReceived(Tick tick) {
			List<Order> activeOrders = getActiveOrders(tick.getSecurity().getKey());
			for (Order order : activeOrders) {
				fillOrderIfPossible(order);
			}
		}

		@Override
		public void tickReceivedFillCacheOnly(Tick tick) {
			// Do nothing here, should never receive a tick
			
		}
	}
}
