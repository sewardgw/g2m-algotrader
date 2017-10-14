package com.g2m.services.tradingservices.backtest.slippage;

import java.util.HashMap;
import java.util.Random;

import com.g2m.services.tradingservices.ForexConverter;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.Tick;
import com.g2m.services.tradingservices.entities.orders.LimitOrder;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.enums.OrderAction;
import com.g2m.services.tradingservices.enums.OrderType;

public class SlippageEstimator {
	private static Random rand = new Random();
	private final static int randomMultiplier = 3;
	private static HashMap<SecurityKey, Double> securitySlippage = new HashMap<SecurityKey, Double>();
	private static HashMap<SecurityKey, Double> securityValueSlippage = new HashMap<SecurityKey, Double>();
	private static boolean estimateSlippage = true;

	public SlippageEstimator() {
		rand.setSeed(10000L);
	}

	public static void setSeed(long seed){
		rand.setSeed(seed);
	}

	public static void estimateSlippage(boolean estimateSlippage) { 
		SlippageEstimator.estimateSlippage = estimateSlippage;
	}

	/**
	 * This will provide the estimated price inclusive of slippage for an order when the last tick is known.
	 * The last tick is used to look at the spread (difference between the bid and the ask) 
	 * and a randomness is injected as well to be more conservative in the slippage
	 * estimate value.
	 * @param lastTick The last tick for a given security.
	 * @param order The order that is being placed.
	 * @return returns the estimated price of a trade when the last tick is known.
	 */
	public static double estimateSlippage(Tick lastTick, Order order) {
		if(estimateSlippage){
			if(order.getOrderType().equals(OrderType.LIMIT))
				return limitOrderSlippageEstimate((LimitOrder) order);

			// Stops and market orders act the same
			else
				return marketOrderSlippageEstimate(lastTick, order);
		}
		else {
			return lastTick.getLastPrice();
		}
	}

	private static double limitOrderSlippageEstimate(LimitOrder order) {
		return order.getLimitPrice();
	}

	private static double marketOrderSlippageEstimate(Tick lastTick, Order order) {
		double newRandDoub = rand.nextDouble();
		double spread = lastTick.getAskPrice() - lastTick.getBidPrice();
		double estimatedPrice = 0.0;

		if(order.getOrderAction().equals(OrderAction.BUY)){
			estimatedPrice = lastTick.getAskPrice() + (spread * (randomMultiplier * newRandDoub));
		} 
		else if (order.getOrderAction().equals(OrderAction.SELL)){
			estimatedPrice = lastTick.getBidPrice() - (spread * (randomMultiplier * newRandDoub));
		}

		// Add the slippage for later analytical analysis. This is mostly random but
		// the amount of slippage should be understood
		addAggregateSlippage(lastTick, order, estimatedPrice); 	
		return estimatedPrice;
	}

	private static void addAggregateSlippage(Tick tick, Order order, double estimatedPrice) {
		double slippage = Math.abs(tick.getLastPrice() - estimatedPrice);
		double slippageValue = (slippage * order.getQuantity()) / ForexConverter.getConversionRatio(tick.getSecurity(), estimatedPrice);
		if(!securitySlippage.containsKey(tick.getSecurity().getKey())){
			securitySlippage.put(tick.getSecurity().getKey(), slippage);
			securityValueSlippage.put(tick.getSecurity().getKey(), slippageValue);
		}
		else{
			slippage += securitySlippage.get(tick.getSecurity().getKey());
			securitySlippage.put(tick.getSecurity().getKey(), slippage);

			slippageValue += securityValueSlippage.get(tick.getSecurity().getKey());
			securityValueSlippage.put(tick.getSecurity().getKey(), slippageValue);
		}
	}

	/**
	 * @return Returns the total points that were estimated to be lost in the 
	 * back test due to slippage
	 */
	public static double getTotalEstimatedSlippagePoints(){
		double totalSlippage = 0.0;
		if(!securitySlippage.isEmpty()){
			for(SecurityKey key : securitySlippage.keySet())
				totalSlippage += securitySlippage.get(key);
			return totalSlippage;
		}
		return totalSlippage;
	}

	/**
	 * @return Returns the total value that was estimated to be lost in the 
	 * back test due to slippage
	 */
	public static double getTotalEstimatedSlippageValue(){
		double totalSlippageValue = 0.0;
		if(!securityValueSlippage.isEmpty()){
			for(SecurityKey key : securityValueSlippage.keySet())
				totalSlippageValue += securityValueSlippage.get(key);
			return totalSlippageValue;
		}
		return totalSlippageValue;
	}


}
