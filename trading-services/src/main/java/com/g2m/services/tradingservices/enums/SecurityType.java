package com.g2m.services.tradingservices.enums;

import com.ib.controller.Types.SecType;

/**
 * Added 3/29/2015.
 * 
 * This enum is here so we don't have to use the IB SecType enum. We're only starting with a subset
 * of security types and will add more as needed.
 * 
 * @author Michael Borromeo
 */
public enum SecurityType {
	// TODO add more security types as needed
	NONE, STOCK, FUTURE, CASH;
	
	public static boolean containsMappedValue(String ibSecType){
		if(ibSecType.equals("none")
				|| ibSecType.equals("STK")
				|| ibSecType.equals("STOCK")
				|| ibSecType.equals("FUT")
				|| ibSecType.equals("FUTURE")
				|| ibSecType.equals("CASH"))
			return true;
				
		return false;
	}
	
	public static SecurityType getMappedValue(String ibSecTypeString){
		if(ibSecTypeString.equals("none"))
			return SecurityType.NONE;
		if(ibSecTypeString.equals("STK") || ibSecTypeString.equals("STOCK"))
			return SecurityType.STOCK;
		if(ibSecTypeString.equals("FUT") || ibSecTypeString.equals("FUTURE"))
			return SecurityType.FUTURE;
		if(ibSecTypeString.equals("CASH"))
			return SecurityType.CASH;
		return null;
	}
	
}
