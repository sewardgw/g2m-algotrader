package com.g2m.services.tradingservices;

import java.util.HashMap;

import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.enums.OrderAction;
import com.g2m.services.tradingservices.enums.SecurityType;

public class MarginImpact {

	private final static double forexMultiplier = .04;

	private static HashMap<SecurityKey, Double> securityLeverage = new HashMap<SecurityKey, Double>();

	//TODO need to load security keys and their associated margin on startup
	public MarginImpact() {

	}

	/**
	 * Converts the order cost and quantity to the impact that it will have on the margin
	 * requirements for an account when taking into account the margin requirements
	 * for each security as well as the multiplier value for each security. <br> <br>
	 * NOTE - CURRENTLY HARD CODED FOR FOREX AND NEEDS TO BE REVISITED
	 * @param security The security that is associated to the order
	 * @param quantity The quantity for the order
	 * @param avgCost The average cost of the order
	 * @return Returns the impact that the order will have on the account when taking into account
	 * margin and multipliers.
	 */
	public static double getMarginImpact(Security security, int quantity, double avgCost){
		if(security.getSecurityType().equals(SecurityType.CASH)){
			return ForexConverter.getConversionRatio(security, avgCost) * quantity * forexMultiplier;
		}

		return quantity * avgCost;
	}

	public static double getMarginImpact(Order order, double avgCost){
		return getMarginImpact(order.getSecurity(), order.getQuantity(), avgCost);

	}

}
