package com.g2m.services.tradingservices.entities.orders;

import java.util.UUID;

/**
 * Added 5/31/2015.
 * 
 * @author michaelborromeo
 */
public class OrderKey {
	String key;

	public OrderKey() {
		key = UUID.randomUUID().toString();
	}

	@Override
	public String toString() {
		return key;
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public boolean equals(Object otherKey) {
		if (null == otherKey) {
			return false;
		} else if (otherKey instanceof OrderKey) {
			return ((OrderKey) otherKey).key.equals(key);
		} else if (otherKey instanceof String) {
			return otherKey.equals(key);
		}
		return false;
	}
}
