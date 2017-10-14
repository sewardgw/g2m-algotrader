package com.g2m.services.tradingservices.backtest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.Account;
import com.g2m.services.tradingservices.SecurityRegistry;
import com.g2m.services.tradingservices.analytics.Analytics;
import com.g2m.services.tradingservices.caches.TickCache;
import com.g2m.services.tradingservices.entities.Position;
import com.g2m.services.tradingservices.entities.Position.PositionBuilder;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.enums.OrderAction;
import com.g2m.services.tradingservices.enums.OrderStatusReason;
import com.g2m.services.tradingservices.persistence.PositionPersistThread;

/**
 * Added 3/18/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class BacktestAccount extends Account {
	@Autowired
	private TickCache tickCache;
	@Autowired
	private Analytics analytics;
	private double startingFunds;
	private static PositionPersistThread positionPersistThread;
	private static boolean persist;

	public BacktestAccount() {
		super();
		setupStartingFunds(1000000);
	}

	public static void startPositionPersistThread(){
		positionPersistThread = new PositionPersistThread();
		positionPersistThread.start();
		setEntityPersistMode(true);
	}

	public static void setEntityPersistMode(boolean persistVal){
		persist = persistVal;
	}
	
	public static void stopPersistThread() {
		positionPersistThread.stopRunning();
	}

	public OrderStatusReason canFillOrder(Order order) {
		switch (order.getOrderAction()) {
		case BUY:
			if (getOrderCost(order) > availableFunds) {
				return OrderStatusReason.INSUFFICIENT_FUNDS;
			} else {
				return OrderStatusReason.OK_TO_FILL;
			}
		case SELL:
			if (getOrderCost(order) > availableFunds) {
				if(positions.get(order.getSecurityKey()).getQuantity() > 0)
					return OrderStatusReason.OK_TO_FILL;
				// if the money gained from the sale is outweighed by the commission
				return OrderStatusReason.INSUFFICIENT_FUNDS;
			} else if (order.getQuantity() > getPositionQuantity(order.getSecurityKey())) {
				// This should be OrderStatusReason.INSUFFICIENT_POSITION but because sell short is not implemented
				// we manually say that it is OK to Fill
				return OrderStatusReason.OK_TO_FILL;
			} else {
				return OrderStatusReason.OK_TO_FILL;
			}
			// TODO add sell short as an option here
		default:
			return OrderStatusReason.ORDER_ACTION_NOT_SUPPORTED;
		}
	}

	private int getPositionQuantity(SecurityKey securityKey) {
		if (positions.containsKey(securityKey)) {
			return positions.get(securityKey).getQuantity();
		} else {
			return 0;
		}
	}
	
	private double getOrderCost(Order order) {
		if (OrderAction.BUY.equals(order.getOrderAction())) {
			return (order.getQuantity() * order.getOrderPrice()) + order.getCommission();
		} else if (OrderAction.SELL.equals(order.getOrderAction())) {
			return (-order.getQuantity() * order.getOrderPrice()) + order.getCommission();
		} else {
			return 0;
		}
	}

	public void updateAccountFromOrder(Order order) {
		switch (order.getOrderAction()) {
		case BUY:
			updateAccountFromBuyOrder(order);
			break;
		case SELL:
			updateAccountFromSellOrder(order);
			break;
		case SELL_SHORT:
			updateAccountFromSellShortOrder(order);
		default:
			/*
			 * Should never get to this point, unsupporoted order actions should be caught by
			 * canFillOrder() and cancelled
			 */
		}

	}


	/**
	 * When this is called we know that a buy order is supported by availableFunds.
	 */
	private void updateAccountFromBuyOrder(Order order) {
		double orderPrice = order.getOrderPrice();
		double orderCost = getOrderCost(order);
		updateAccountFundsDataFromOrder(orderCost);

		Position newPosition = null;
		if (positions.containsKey(order.getSecurityKey())) {
			Position position = positions.get(order.getSecurityKey());
			int quantity = position.getQuantity() + order.getQuantity();
			PositionBuilder builder = new PositionBuilder(position);
			builder.setQuantity(quantity);
			builder.setAverageCost(orderPrice, order.getQuantity());
			builder.setLastPrice(orderPrice);
			if(position.getQuantity() == 0)
				builder.setOpenPrice(orderPrice);
			newPosition = builder.build();
			positions.put(order.getSecurityKey(), newPosition);
		} else {
			PositionBuilder builder = new PositionBuilder();
			builder.setAccount("");
			builder.setAverageCost(orderCost, order.getQuantity());
			builder.setOpenPrice(orderPrice);
			builder.setQuantity(order.getQuantity());
			builder.setRealizedPnl(0);
			builder.setSecurity(SecurityRegistry.get(order.getSecurityKey()));
			builder.setUnrealizedPnl(0);
			builder.setLastPrice(orderPrice);
			newPosition = builder.build();
			positions.put(order.getSecurityKey(), newPosition);
		}
		analytics.updateFromPosition(newPosition);
		persistPosition(newPosition);
	}

	/**
	 * When this is called we know that a sell order is supported by open positions.
	 */
	private void updateAccountFromSellOrder(Order order) {
		double orderPrice = order.getOrderPrice();
		double orderCost = getOrderCost(order);
		updateAccountFundsDataFromOrder(orderCost);

		Position newPosition = null;
		if (positions.containsKey(order.getSecurityKey())) {
			Position position = positions.get(order.getSecurityKey());
			PositionBuilder builder = new PositionBuilder(position);
			int quantity = position.getQuantity() - order.getQuantity();
			builder.setQuantity(quantity);
			builder.setAverageCost(orderPrice, order.getQuantity());
			builder.setLastPrice(orderPrice);
			if(position.getQuantity() == 0)
				builder.setOpenPrice(orderPrice);
			newPosition = builder.build();
			positions.put(order.getSecurityKey(), newPosition);
		} else {
			PositionBuilder builder = new PositionBuilder();
			builder.setAccount("");
			builder.setAverageCost(orderCost, order.getQuantity());
			builder.setOpenPrice(orderPrice);
			builder.setQuantity(-order.getQuantity());
			builder.setRealizedPnl(0);
			builder.setSecurity(SecurityRegistry.get(order.getSecurityKey()));
			builder.setUnrealizedPnl(0);
			builder.setLastPrice(orderPrice);
			newPosition = builder.build();
			positions.put(order.getSecurityKey(), newPosition);
		}
		analytics.updateFromPosition(newPosition);
		persistPosition(newPosition);
	}

	private void persistPosition(Position position){
		if(persist){
			positionPersistThread.persist(position);
		}
	}

	private void updateAccountFromSellShortOrder(Order order) {
		double lastTickPrice = tickCache.getLastTickPrice(order.getSecurityKey());
		double orderCost = getOrderCost(order);
		updateAccountFundsDataFromOrder(orderCost);

		Position newPosition = null;

		analytics.updateFromPosition(newPosition);
	}

	private void updateAccountFundsDataFromOrder(double orderCost) {
		availableFunds -= orderCost;
		buyingPower -= orderCost;
		cashBalance -= orderCost;
		fullAvailableFunds -= orderCost;
	}

	public void setupStartingFunds(double startingFunds) {
		this.startingFunds = startingFunds;
		availableFunds = startingFunds;
		buyingPower = availableFunds;
		cashBalance = availableFunds;
		fullAvailableFunds = availableFunds;
		accountInitialized = true;
	}

	public double getStartingFunds() {
		return startingFunds;
	}
}
