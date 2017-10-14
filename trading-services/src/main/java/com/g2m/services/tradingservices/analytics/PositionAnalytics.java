package com.g2m.services.tradingservices.analytics;

import java.util.Date;
import java.util.List;

import com.g2m.services.tradingservices.Account;
import com.g2m.services.tradingservices.caches.TickCache;
import com.g2m.services.tradingservices.comissions.ComissionHandler;
import com.g2m.services.tradingservices.entities.Position;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.Tick;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.enums.WinLoss;

/**
 * Added 6/18/2015.
 * 
 * @author michaelborromeo
 */
public class PositionAnalytics {
	private Security security;
	private double profit;
	private WinLoss winLoss;
	private double comissions;
	private int tradeCount;
	private Date openedDateTime;
	private Date closedDateTime;
	private double maxDrawUp;
	private double maxDrawDown;
	private int quantity;
	private int tradeOpenedQuantity;
	private double highestPrice;
	private double lowestPrice;
	private boolean opened;
	private double totalCost;
	private Date currentDateTime;
	private String tradeType;
	private double tradeOpenPrice;
	private double tradeClosePrice;

	public PositionAnalytics() {
		opened = false;
		winLoss = WinLoss.UNKNOWN;
		lowestPrice = Double.MAX_VALUE;
	}

	public boolean isClosed() {
		return 0 == quantity;
	}

	public Security getSecurity() {
		return security;
	}

	public SecurityKey getSecurityKey() {
		return security.getKey();
	}

	public double getProfit() {
		return profit;
	}

	public WinLoss getWinLoss() {
		return winLoss;
	}

	public double getComissions() {
		return comissions;
	}

	public int getTradeCount() {
		return tradeCount;
	}

	public Date getOpenedDateTime() {
		return openedDateTime;
	}

	public Date getClosedDateTime() {
		return closedDateTime;
	}

	public double getMaxDrawUp() {
		return maxDrawUp;
	}

	public double getMaxDrawDown() {
		return maxDrawDown;
	}

	public int getQuantity() {
		return quantity;
	}
	
	public String getTradeType() {
		return tradeType;
	}
	
	public double getTradeOpenPrice() {
		return tradeOpenPrice;
	}
	
	public double getTradeClosePrice() {
		return tradeClosePrice;
	}
	
	public double getClosedTradeDifference() {
		return (getTradeClosePrice() - getTradeOpenPrice());
	}
	
	public double getRealizedProfit() {
		return (getTradeOpenedQuantity() * getClosedTradeDifference());
	}
		
	
	public int getTradeOpenedQuantity() {
		return tradeOpenedQuantity;
	}
	public String getTradeOutput() {
		StringBuilder sb = new StringBuilder();
		sb.append("Symbol: " + getSecurity().getSymbol() + "." + getSecurity().getCurrency());
		sb.append(System.lineSeparator());
		sb.append("Trade type: " + getTradeType());
		sb.append(System.lineSeparator());
		sb.append("Win/Loss: " + getWinLoss());
		sb.append(System.lineSeparator());
		sb.append("Price difference: " + getClosedTradeDifference());
		sb.append(System.lineSeparator());
		sb.append("Quantity: " + getTradeOpenedQuantity());
		sb.append(System.lineSeparator());
		sb.append("Realized profit: " + getRealizedProfit());
		sb.append(System.lineSeparator());
		sb.append("Trade opened: " + getOpenedDateTime());
		sb.append(System.lineSeparator());
		sb.append("Trade closed: " + getClosedDateTime());
		sb.append(System.lineSeparator());
		sb.append("Open price:  " + getTradeOpenPrice());
		sb.append(System.lineSeparator());
		sb.append("Close price: " + getTradeClosePrice());
		sb.append(System.lineSeparator());
		sb.append("Max Drawdown(broke?): " + getMaxDrawDown());
		sb.append(System.lineSeparator());
		sb.append("Max Drawup(broke?): " + getMaxDrawUp());
		return sb.toString();
	}


	public static class PositionAnalyticsBuilder {
		private PositionAnalytics positionAnalytics;

		public PositionAnalyticsBuilder(Position position) {
			positionAnalytics = new PositionAnalytics();
			positionAnalytics.security = position.getSecurity();
			positionAnalytics.tradeOpenedQuantity = position.getQuantity();
			
			if(position.getQuantity() > 0)
				positionAnalytics.tradeType = "LONG";
			else
				positionAnalytics.tradeType = "SHORT";
			
		}

		public boolean isClosed() {
			return 0 == positionAnalytics.quantity && positionAnalytics.opened;
		}

		public void updateFromTick(Tick tick) {
			if (isClosed()) {
				return;
			}
			positionAnalytics.currentDateTime = tick.getDateTime();
			if(positionAnalytics.getOpenedDateTime()==null)
				positionAnalytics.openedDateTime = tick.getDateTime();
			updateFromTickPrice(tick.getLastPrice());
		}

		private void updateFromTickPrice(double price) {
			if (price > positionAnalytics.highestPrice) {
				positionAnalytics.highestPrice = price;
			}
			if (price < positionAnalytics.lowestPrice) {
				positionAnalytics.lowestPrice = price;
			}
			if (price - positionAnalytics.lowestPrice > positionAnalytics.maxDrawUp) {
				positionAnalytics.maxDrawUp = price - positionAnalytics.lowestPrice;
			}
			if (positionAnalytics.highestPrice - price > positionAnalytics.maxDrawDown) {
				positionAnalytics.maxDrawDown = positionAnalytics.highestPrice - price;
			}
		}

		/**
		 * TODO refactor/clean up
		 */
		public void updateFromPosition(Position position) {
			if (isClosed()) {
				return;
			}
			if (!positionAnalytics.opened) {
				positionAnalytics.opened = true;
			}
			
			if (positionAnalytics.quantity != position.getQuantity()) {
				if (0 == position.getQuantity()) {
					// closed position
					//updateFromTickPrice(position.getMarketPrice());
					int quantityChange = position.getQuantity() - positionAnalytics.quantity;
					double cost = quantityChange * position.getLastPrice();
					//positionAnalytics.comissions += ComissionHandler.getCommissionValue(position);

					positionAnalytics.totalCost += cost; // quantity * price
					positionAnalytics.profit = -positionAnalytics.totalCost; // profit = p
					positionAnalytics.tradeClosePrice = position.getLastPrice();

					if (0 < positionAnalytics.profit) {
						positionAnalytics.winLoss = WinLoss.WIN;
					} else if (0 > positionAnalytics.profit) {
						positionAnalytics.winLoss = WinLoss.LOSS;
					} else if (0 == positionAnalytics.profit){
						positionAnalytics.winLoss = WinLoss.NOPROFIT;
					} else{
						positionAnalytics.winLoss = WinLoss.UNKNOWN;
					}
						
					if (null != positionAnalytics.currentDateTime) {
						positionAnalytics.closedDateTime = positionAnalytics.currentDateTime;
					} else {
						// TODO find a way to get the date/time of the current ticks
						// positionAnalytics.closedDateTime = 
					}
		
				} else {
					// increase/reduce position
					updateFromTickPrice(position.getMarketPrice());
					int quantityChange = position.getQuantity() - positionAnalytics.quantity;
					double cost = quantityChange * position.getMarketPrice(); // quantity * price
					positionAnalytics.totalCost += cost; // total cost (total profit) = Prev Total Cost + (quantity * price)
					if(position.getQuantity() != 0)
						positionAnalytics.comissions += 2*(ComissionHandler.getCommissionValue(position));
					
					double positionValue = position.getQuantity() * position.getMarketPrice(); // Position quantity * position price

					// TODO This profit is not calculated correctly. We need to capture the 
					// prior value that the position was opened at. This should be calculated
					// as ((position current price - position open) * position quantity)
					// for each incremental buy / sell of a position
					// We don't currently capture the position open price each time the position
					// is increased / decreased
					positionAnalytics.profit += positionValue - positionAnalytics.totalCost;    
					
					if (0 == positionAnalytics.quantity) {
						// openening a new position
						positionAnalytics.tradeOpenPrice = position.getLastPrice();
						if (null != positionAnalytics.currentDateTime) {
							positionAnalytics.openedDateTime = positionAnalytics.currentDateTime;
						} else {
							// TODO find a way to get the date/time of the current ticks
							//		positionAnalytics.closedDateTime = 
							//		positionAnalytics.tickCache.getLastTick(positionAnalytics.getSecurityKey()).getDateTime();
						}
					}
					
					if (position.getQuantity() > positionAnalytics.getTradeOpenedQuantity())
						positionAnalytics.tradeOpenedQuantity = position.getQuantity();
				}

				// TODO find the commission for whatever trade made this order
				positionAnalytics.comissions += 0;
				positionAnalytics.quantity = position.getQuantity();
				positionAnalytics.tradeCount++;
			}
		}

		public PositionAnalytics build() {
			return positionAnalytics;
		}

	}
}