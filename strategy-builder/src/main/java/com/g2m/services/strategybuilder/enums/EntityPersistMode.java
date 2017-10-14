package com.g2m.services.strategybuilder.enums;

/**
 * Added 5/25/2015.
 * 
 * @author michaelborromeo
 */
public enum EntityPersistMode {
	NONE, LIVE_ONLY, ALL;

	public boolean persistForLive() {
		return this == LIVE_ONLY || this == ALL;
	}

	public boolean persistForBacktest() {
		return this == ALL;
	}
}
