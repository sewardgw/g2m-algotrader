package com.g2m.services.tradingservices.entities.orders;

import java.util.Date;

import com.g2m.services.tradingservices.enums.OrderStatus;
import com.g2m.services.tradingservices.enums.OrderStatusReason;

/**
 * Added 6/5/2015.
 * 
 * @author michaelborromeo
 */
public class OrderState {
	protected long id;
	protected OrderStatus orderStatus;
	protected int quantityFilled;
	protected int quantityRemaining;
	protected double averageFillPrice;
	protected double lastFillPrice;
	protected String whyHeld;
	protected String initialMargin;
	protected String maintenanceMargin;
	protected String equityWithLoan;
	protected double commission;
	protected double minimumCommission;
	protected double maximumCommission;
	protected String commissionCurrency;
	protected String warningText;
	protected String errorMessage;
	protected int errorCode;
	protected OrderStatusReason orderStatusReason = OrderStatusReason.ORDER_STATUS_PENDING;
	protected Date timestamp;

	public long getId() {
		return id;
	}

	public OrderStatus getOrderStatus() {
		return orderStatus;
	}

	public int getQuantityFilled() {
		return quantityFilled;
	}

	public int getQuantityRemaining() {
		return quantityRemaining;
	}

	public double getAverageFillPrice() {
		return averageFillPrice;
	}

	public double getLastFillPrice() {
		return lastFillPrice;
	}

	public String getWhyHeld() {
		return whyHeld;
	}

	public String getInitialMargin() {
		return initialMargin;
	}

	public String getMaintenanceMargin() {
		return maintenanceMargin;
	}

	public String getEquityWithLoan() {
		return equityWithLoan;
	}

	public double getCommission() {
		return commission;
	}

	public double getMinimumCommission() {
		return minimumCommission;
	}

	public double getMaximumCommission() {
		return maximumCommission;
	}

	public String getCommissionCurrency() {
		return commissionCurrency;
	}

	public String getWarningText() {
		return warningText;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public OrderStatusReason getOrderStatusReason() {
		return orderStatusReason;
	}

	public static class OrderStateBuilder {
		private OrderState orderState;

		public OrderStateBuilder() {
			orderState = new OrderState();
		}

		public OrderStateBuilder(OrderState orderState) {
			// do this so the original orderState being copied doesn't get corrupted
			this.orderState = orderState;
			this.orderState = build();
		}

		public void setId(long id) {
			orderState.id = id;
		}

		public void setOrderStatus(OrderStatus orderStatus) {
			orderState.orderStatus = orderStatus;
		}

		public void setQuantityFilled(int quantityFilled) {
			orderState.quantityFilled = quantityFilled;
		}

		public void setQuantityRemaining(int quantityRemaining) {
			orderState.quantityRemaining = quantityRemaining;
		}

		public void setAverageFillPrice(double averageFillPrice) {
			orderState.averageFillPrice = averageFillPrice;
		}

		public void setLastFillPrice(double lastFillPrice) {
			orderState.lastFillPrice = lastFillPrice;
		}

		public void setWhyHeld(String whyHeld) {
			orderState.whyHeld = whyHeld;
		}

		public void setInitialMargin(String initialMargin) {
			orderState.initialMargin = initialMargin;
		}

		public void setMaintenanceMargin(String maintenanceMargin) {
			orderState.maintenanceMargin = maintenanceMargin;
		}

		public void setEquityWithLoan(String equityWithLoan) {
			orderState.equityWithLoan = equityWithLoan;
		}

		public void setCommission(double commission) {
			orderState.commission = commission;
		}

		public void setMinimumCommission(double minimumCommission) {
			orderState.minimumCommission = minimumCommission;
		}

		public void setMaximumCommission(double maximumCommission) {
			orderState.maximumCommission = maximumCommission;
		}

		public void setCommissionCurrency(String commissionCurrency) {
			orderState.commissionCurrency = commissionCurrency;
		}

		public void setWarningText(String warningText) {
			orderState.warningText = warningText;
		}

		public void setErrorMessage(String errorMessage) {
			orderState.errorMessage = errorMessage;
		}

		public void setErrorCode(int errorCode) {
			orderState.errorCode = errorCode;
		}

		public void setOrderStatusReason(OrderStatusReason orderStatusReason) {
			orderState.orderStatusReason = orderStatusReason;
		}

		public void setTimestamp(Date timestamp) {
			orderState.timestamp = timestamp;
		}

		public OrderState build() {
			OrderState orderState = new OrderState();
			orderState.averageFillPrice = this.orderState.averageFillPrice;
			orderState.commission = this.orderState.commission;
			orderState.commissionCurrency = this.orderState.commissionCurrency;
			orderState.equityWithLoan = this.orderState.equityWithLoan;
			orderState.errorCode = this.orderState.errorCode;
			orderState.errorMessage = this.orderState.errorMessage;
			orderState.id = this.orderState.id;
			orderState.initialMargin = this.orderState.initialMargin;
			orderState.lastFillPrice = this.orderState.lastFillPrice;
			orderState.maintenanceMargin = this.orderState.maintenanceMargin;
			orderState.maximumCommission = this.orderState.maximumCommission;
			orderState.minimumCommission = this.orderState.minimumCommission;
			orderState.orderStatus = this.orderState.orderStatus;
			orderState.orderStatusReason = this.orderState.orderStatusReason;
			orderState.quantityFilled = this.orderState.quantityFilled;
			orderState.quantityRemaining = this.orderState.quantityRemaining;
			orderState.timestamp = this.orderState.timestamp;
			orderState.warningText = this.orderState.warningText;
			orderState.whyHeld = this.orderState.whyHeld;
			return orderState;
		}
	}
}
