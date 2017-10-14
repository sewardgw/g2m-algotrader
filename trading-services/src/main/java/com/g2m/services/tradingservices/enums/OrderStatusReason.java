package com.g2m.services.tradingservices.enums;

/**
 * Added 5/17/2015.
 * 
 * @author michaelborromeo
 */
public enum OrderStatusReason {
	ORDER_STATUS_PENDING, OK_TO_FILL, INSUFFICIENT_FUNDS, INSUFFICIENT_POSITION, MANUALLY_CANCELLED,
	ORDER_ACTION_NOT_SUPPORTED, UNKNOWN;

	public boolean canFillOrder() {
		return this == OK_TO_FILL;
	}

	public boolean cannotFillOrder() {
		return this == INSUFFICIENT_FUNDS || this == INSUFFICIENT_POSITION || this == ORDER_ACTION_NOT_SUPPORTED
				|| this == MANUALLY_CANCELLED || this == UNKNOWN;
	}
}
