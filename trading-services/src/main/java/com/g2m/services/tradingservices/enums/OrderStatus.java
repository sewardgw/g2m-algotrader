package com.g2m.services.tradingservices.enums;

/**
 * Added on 5/6/2015.
 * 
 * @author michaelborromeo
 */
public enum OrderStatus {
	API_PENDING, API_CANCELLED, PRE_SUBMITTED, PENDING_CANCEL, CANCELLED, SUBMITTED, FILLED, INACTIVE, PENDING_SUBMIT, UNKNOWN;

	public boolean isActive() {
		return this == PRE_SUBMITTED || this == PENDING_CANCEL || this == SUBMITTED || this == PENDING_SUBMIT;
	}
}
