package com.g2m.services.tradingservices.entities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.caches.TickCache;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class Position {

	private TickCache tickCache = null;

	private Security security;
	private String account;
	private int quantity = 0;
	private double openPrice;
	private double lastPrice;
	private double averageCost;
	private double unrealizedPnl;
	private double realizedPnl;

	public Position() {

	}

	public Security getSecurity() {
		return security;
	}

	public String getAccount() {
		return account;
	}

	public int getQuantity() {
		return quantity;
	}

	public double getOpenPrice() {
		return openPrice;
	}

	public double getMarketPrice() {
		//TODO determine how to get ticks into the position
		return lastPrice;// TickCache.getLastTick(security.getKey()).getLastPrice();
	}

	public double getMarketValue() {
		return getMarketPrice() * quantity;
	}

	public double getAverageCost() {
		return averageCost;
	}

	public double getUnrealizedPnl() {
		return unrealizedPnl;
	}

	public double getRealizedPnl() {
		return realizedPnl;
	}
	
	public double getLastPrice() {
		return lastPrice;
	}

	public boolean isOpen() {
		return 0 != quantity;
	}

	public static class PositionBuilder {
		private Position position;

		public PositionBuilder() {
			position = new Position();
		}

		public PositionBuilder(Position position) {
			this.position = new Position();
			this.position.security = position.security;
			this.position.account = position.account;
			this.position.quantity = position.quantity;
			this.position.openPrice = position.openPrice;
			this.position.lastPrice = position.lastPrice;
			this.position.averageCost = position.averageCost; // position.quantity;
			this.position.unrealizedPnl = position.unrealizedPnl;
			this.position.realizedPnl = position.realizedPnl;
		}

		public void setSecurity(Security security) {
			position.security = security;
		}

		public void setAccount(String account) {
			position.account = account;
		}

		public void setQuantity(int quantity) {
			position.quantity = quantity;
		}

		public void setOpenPrice(double openPrice) {
			position.openPrice = openPrice;
		}

		public void setAverageCost(double averageCost, int newQuantity) {
			// (new cost * new price + previous average cost * quantity) / total quantity 
			position.averageCost = ((newQuantity * averageCost) 
					+ (position.averageCost * (position.quantity - newQuantity)))
					/ position.quantity; 
		}
		
		public void setIBAverageCost(double averageCost) {
			// (new cost * new price + previous average cost * quantity) / total quantity 
			position.averageCost = averageCost;
		}


		public void setUnrealizedPnl(double unrealizedPnl) {
			position.unrealizedPnl = unrealizedPnl;
		}

		public void setRealizedPnl(double realizedPnl) {
			position.realizedPnl = realizedPnl;
		}

		public void setLastPrice(double lastPrice) {
			position.lastPrice = lastPrice;
		}
		public Position build() {
			Position position = new Position();
			position.security = this.position.security;
			position.account = this.position.account;
			position.quantity = this.position.quantity;
			position.openPrice = this.position.openPrice;
			position.lastPrice = this.position.lastPrice;
			position.averageCost = this.position.averageCost;
			position.unrealizedPnl = this.position.unrealizedPnl;
			position.realizedPnl = this.position.realizedPnl;
			return position;
		}
	}
}
