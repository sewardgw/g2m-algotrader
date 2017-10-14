package com.g2m.services.tradingservices.comissions;

import com.g2m.services.tradingservices.ForexConverter;
import com.g2m.services.tradingservices.entities.Position;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.enums.SecurityType;

public class ComissionHandler {

	private static final double DEFAULT_FUTURES_COMMISSION = .85;
	private static final double DEFAULT_FOREX_COMMISSION = .00002;
	private static final double DEFAULT_STOCK_COMMISSION = .01;
	private static final double DEFAULT_FUTURES_MIN_COMMISSION = 0d;
	private static final double DEFAULT_FOREX_MIN_COMMISSION = 2d;
	private static final double DEFAULT_STOCK_MIN_COMMISSION = 5d;

	public ComissionHandler() {
		// TODO Auto-generated constructor stub
	}

	public static double getCommissionValue(Order order) {
		if(order.getSecurity().getSecurityType().equals(SecurityType.CASH)){
			return Math.max(order.getQuantity() * DEFAULT_FOREX_COMMISSION, DEFAULT_FOREX_MIN_COMMISSION);
		} else if(order.getSecurity().getSecurityType().equals(SecurityType.FUTURE)){
			return Math.max(order.getQuantity() * DEFAULT_FUTURES_COMMISSION, DEFAULT_FUTURES_MIN_COMMISSION);
		} else if(order.getSecurity().getSecurityType().equals(SecurityType.STOCK)){
			return Math.max(order.getQuantity() * DEFAULT_STOCK_COMMISSION, DEFAULT_STOCK_MIN_COMMISSION);
		}
		return 0;
	}

	public static double getCommissionValue(Position position) {
		if(position.getQuantity() != 0){
			double positionVal = Math.abs(position.getQuantity());
			if(position.getSecurity().getSecurityType().equals(SecurityType.CASH)){
				return Math.max(positionVal * DEFAULT_FOREX_COMMISSION, DEFAULT_FOREX_MIN_COMMISSION);
			} else if(position.getSecurity().getSecurityType().equals(SecurityType.FUTURE)){
				return Math.max(positionVal * DEFAULT_FUTURES_COMMISSION, DEFAULT_FUTURES_MIN_COMMISSION);
			} else if(position.getSecurity().getSecurityType().equals(SecurityType.STOCK)){
				return Math.max(positionVal * DEFAULT_STOCK_COMMISSION, DEFAULT_STOCK_MIN_COMMISSION);
			}
		}
		return 0;
	}

}
