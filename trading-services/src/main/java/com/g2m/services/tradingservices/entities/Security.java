package com.g2m.services.tradingservices.entities;

import org.springframework.data.annotation.Transient;

import com.g2m.services.tradingservices.enums.Right;
import com.g2m.services.tradingservices.enums.SecurityType;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
public class Security {
	private String symbol;
	private String listingExchange;
	private String exchange;
	private String currency = "USD";
	private SecurityType securityType = SecurityType.NONE;
	private Right right = Right.NONE;
	private String expiry;
	private double strike;
	private double multiplier; 
	private String localSymbol;
	private String tradingClass;
	@Transient
	private SecurityKey key;

	public String getSymbol() {
		return symbol;
	}

	public String getListingExchange() {
		return listingExchange;
	}

	public String getExchange() {
		return exchange;
	}

	public String getCurrency() {
		return currency;
	}

	public SecurityType getSecurityType() {
		return securityType;
	}
	
	public String getSecurityTypeString() {
		return securityType.toString();
	}

	public Right getRight() {
		return right;
	}

	public String getExpiry() {
		return expiry;
	}

	public double getStrike() {
		return strike;
	}

	public double getMultiplier() {
		return multiplier;
	}

	public String getLocalSymbol() {
		return localSymbol;
	}

	public String getTradingClass() {
		return tradingClass;
	}

	public SecurityKey getKey() {
		return key;
	}

	public boolean equals(Security security) {
		if (null != symbol && null != security.getSymbol()) {
			if (!symbol.equals(security.getSymbol())) {
				return false;
			}
		}
//		if (null != listingExchange && null != security.getListingExchange()) {
//			if (!listingExchange.equals(security.getListingExchange())) {
//				return false;
//			}
//		}

		if (null != exchange && null != security.getExchange()) {
			if (!exchange.equals(security.getExchange())) {
				return false;
			}
		}
		if (null != currency && null != security.getCurrency()) {
			if (!currency.equals(security.getCurrency())) {
				return false;
			}
		}
		if (!securityType.equals(SecurityType.NONE) && !securityType.equals(security.getSecurityType())) {
			return false;
		}
		if (!right.equals(security.getRight())) {
			return false;
		}
		if (null != expiry && null != security.getExpiry()) {
			if (!expiry.equals(security.getExpiry())) {
				return false;
			}
		}
		if (strike != security.getStrike()) {
			return false;
		}
		
//		if (null != localSymbol && null != security.localSymbol) {
//			if (!localSymbol.equals(security.localSymbol)) {
//				return false;
//			}
//		}
		if (null != tradingClass && null != security.tradingClass) {
			if (!tradingClass.equals(security.tradingClass)) {
				return false;
			}
		}
		
		return true;
	}

	public static class SecurityBuilder {
		private Security security;

		public SecurityBuilder() {
			security = new Security();
		}

		public SecurityBuilder setSymbol(String symbol) {
			security.symbol = symbol;
			return this;
		}

		public SecurityBuilder setListingExchange(String listingExchange) {
			security.listingExchange = listingExchange;
			return this;
		}

		public SecurityBuilder setExchange(String exchange) {
			security.exchange = exchange;
			return this;
		}

		public SecurityBuilder setCurrency(String currency) {
			security.currency = currency;
			return this;
		}

		public SecurityBuilder setSecurityType(SecurityType securityType) {
			security.securityType = securityType;
			return this;
		}

		public SecurityBuilder setRight(Right right) {
			security.right = right;
			return this;
		}

		/**
		 * @param year e.g. 2015
		 * @param month e.g. Calendar.JANUARY (zero-based month index)
		 */
		public SecurityBuilder setExpiryFromCalendar(int year, int month) {
			security.expiry = String.format("%04d%02d", year, month + 1);
			return this;
		}
		
		public SecurityBuilder setExpiry(int year, int month) {
			security.expiry = String.format("%04d%02d", year, month);
			return this;
		}
		
		public SecurityBuilder setExpiry(String string) {
			return setExpiry(getYearFromString(string), getMonthFromString(string));
		}

		private int getMonthFromString(String string) {
			if (string.substring(4,4).equals("0")){
				return Integer.parseInt(string.substring(4, 5));
			
			}
			else{
				return Integer.parseInt(string.substring(4));
			}
				
		}

		private int getYearFromString(String string) {
			return (Integer.parseInt(string.substring(0, 4)));
		}

		public SecurityBuilder setStrike(double strike) {
			security.strike = strike;
			return this;
		}

		public SecurityBuilder setMultiplier(double multiplier) {
			security.multiplier = multiplier;
			return this;
		}

		public SecurityBuilder setLocalSymbol(String localSymbol) {
			security.localSymbol = localSymbol;
			return this;
		}

		public SecurityBuilder setTradingClass(String tradingClass) {
			security.tradingClass = tradingClass;
			return this;
		}

		public Security build() {
			// creating a new security here in case users want to create multiple Security objects
			// with a single builder
			Security newSecurity = new Security();
			newSecurity.symbol = security.symbol;
			newSecurity.exchange = security.exchange;
			newSecurity.listingExchange = security.listingExchange;
			newSecurity.securityType = security.securityType;
			newSecurity.right = security.right;
			newSecurity.expiry = security.expiry;
			newSecurity.strike = security.strike;
			newSecurity.multiplier = security.multiplier;
			newSecurity.localSymbol = security.localSymbol;
			newSecurity.tradingClass = security.tradingClass;
			
			if(security.currency != null)
				newSecurity.currency = security.currency;
			else
				newSecurity.currency = "USD";
			
			newSecurity.key = new SecurityKey(security);

			return newSecurity;
		}
	}

	/**
	 * Created this class so instead of having maps with Map<String, SomeObject> that use the
	 * security key as they key they will look like Map<SecurityKey, SomeObject>.
	 */
	public static class SecurityKey {
		private final String key;
		// listing exchange needs to = exchange
		// securityType should = none
		// local symbol = symbol + "." + currency
		// trading class = local symbol
		public SecurityKey(Security security) {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("symbol=").append(security.symbol);
			stringBuilder.append(",exchange=").append(security.exchange);
			//stringBuilder.append(",listingExchange=").append(security.listingExchange);
			stringBuilder.append(",currency=").append(security.currency);
			//stringBuilder.append(",securityType=").append(security.securityType.toString());
			stringBuilder.append(",right=").append(security.right.toString());
			stringBuilder.append(",expiry=").append(security.expiry);
			stringBuilder.append(",strike=").append(security.strike);
			stringBuilder.append(",multiplier=").append(security.multiplier);
			//stringBuilder.append(",localSymbol=").append(security.localSymbol);
			//stringBuilder.append(",tradingClass=").append(security.tradingClass);
			key = stringBuilder.toString();
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
			} else if (otherKey instanceof SecurityKey) {
				return ((SecurityKey) otherKey).key.equals(key);
			} else if (otherKey instanceof String) {
				return otherKey.equals(key);
			}

			return false;
		}
	}
}
