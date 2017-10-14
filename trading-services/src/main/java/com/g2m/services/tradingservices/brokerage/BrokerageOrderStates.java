package com.g2m.services.tradingservices.brokerage;

import java.util.Date;

import com.g2m.services.tradingservices.brokerage.mappers.BrokerageEnumMapper;
import com.g2m.services.tradingservices.entities.orders.OrderState.OrderStateBuilder;
import com.g2m.services.tradingservices.entities.orders.OrderStates;
import com.ib.controller.ApiController.IOrderHandler;
import com.ib.controller.NewOrderState;
import com.ib.controller.OrderStatus;

/**
 * Added 5/6/2015.
 * 
 * @author michaelborromeo
 */
public class BrokerageOrderStates extends OrderStates {
	/**
	 * Reuse the orderState builder since the IB orderState() and orderStatus() calls contain
	 * information that all funnels into the g2m OrderState. The effect will be that each time
	 * orderState() or orderStatus() is called the deltas (changed info) will be recorded and
	 * anything not changed will remain the same as the previous state.
	 */
	OrderStateBuilder builder = new OrderStateBuilder();
	private OrderHandler orderHandler;

	public BrokerageOrderStates() {
		super();
		orderHandler = new OrderHandler();
		builder = new OrderStateBuilder();
		addPreSubmittedState();
	}

	private void addPreSubmittedState() {
		OrderStateBuilder builder = new OrderStateBuilder();
		builder.setOrderStatus(com.g2m.services.tradingservices.enums.OrderStatus.PRE_SUBMITTED);
		addOrderState(builder.build());
	}

	public IOrderHandler getOrderHandler() {
		return orderHandler;
	}

	private class OrderHandler implements IOrderHandler {
		@Override
		public void orderState(NewOrderState orderState) {
			builder.setOrderStatus(BrokerageEnumMapper.getOrderStatus(orderState.status()));
			builder.setInitialMargin(orderState.initMargin());
			builder.setMaintenanceMargin(orderState.maintMargin());
			builder.setEquityWithLoan(orderState.equityWithLoan());
			builder.setMinimumCommission(orderState.minCommission());
			builder.setMaximumCommission(orderState.maxCommission());
			builder.setCommission(orderState.commission());
			builder.setCommissionCurrency(orderState.commissionCurrency());
			builder.setWarningText(orderState.warningText());
			builder.setTimestamp(new Date());
			addOrderState(builder.build());
		}

		@Override
		public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId,
				int parentId, double lastFillPrice, int clientId, String whyHeld) {
			builder.setId(permId);
			builder.setOrderStatus(BrokerageEnumMapper.getOrderStatus(status));
			builder.setQuantityFilled(filled);
			builder.setQuantityRemaining(remaining);
			builder.setAverageFillPrice(avgFillPrice);
			builder.setLastFillPrice(lastFillPrice);
			builder.setWhyHeld(whyHeld);
			addOrderState(builder.build());
		}

		@Override
		public void handle(int errorCode, String errorMsg) {
			builder.setErrorCode(errorCode);
			builder.setErrorMessage(errorMsg);
			addOrderState(builder.build());
		}
	}
}
