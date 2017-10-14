package com.g2m.services.tradingservices;

import java.util.HashMap;

import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.Tick;
import com.g2m.services.tradingservices.enums.SecurityType;

@Component
public class ForexConverter {

	private static HashMap<String, Double> usdConversion;
	private static final String localCurrency = "USD";

	// 20000 is the actual minimum but in order to maximize the 
	// return from commission costs we need to use 100K
	private static int minForexAmount = 100000; 

	public ForexConverter() {
		usdConversion = new HashMap<String, Double>();
	}

	public static void addUSDVal(Security security, double usdVal){
		if(security.getCurrency().equals(localCurrency)){
			usdConversion.put(security.getSymbol(), usdVal);
		}
		else if (security.getSymbol().equals(localCurrency)){
			usdConversion.put(security.getCurrency(), 1/usdVal);
		}
	}

	public static  void addUSDVal(Bar bar){
		addUSDVal(bar.getSecurity(), bar.getClose());
	}


	public static void addUSDVal(Tick tick){
		if (tick.getSecurity().getSecurityType().equals(SecurityType.CASH))
			addUSDVal(tick.getSecurity(), tick.getLastPrice());
	}

	public static boolean conversionExists(Security security){
		if(usdConversion.containsKey(security.getSymbol()) || usdConversion.containsKey(security.getCurrency())
				|| security.getSymbol().equals(localCurrency) || security.getCurrency().equals(localCurrency))
			return true;
		return false;

	}

	public static boolean conversionExists(Tick tick){
		return conversionExists(tick.getSecurity());
	}

	public static boolean conversionExists(Bar bar){
		return conversionExists(bar.getSecurity());
	}

	public static int convertPosition(Security security, double positionSize, double val){

		if (conversionExists(security)){
			int newPositionSize = (int) Math.round((1 / getConversionRatio(security, val) ) * positionSize);	
			return (int) Math.round(Math.max(minForexAmount, newPositionSize));
		}
		return (int) Math.round(Math.max(minForexAmount, positionSize));
	}

	public static double getConversionRatio(Security security, double val) {
		double convertRatio = 1.0; 
		if (security.getCurrency().equals(localCurrency)  && usdConversion.containsKey(security.getSymbol())){
			convertRatio = usdConversion.get(security.getSymbol());
		}
		
		
		else if (security.getSymbol().equals(localCurrency)){ 
			convertRatio = 1.0;
		}
		
//		Found out that when the USD is the symbol, then we just buy that many shares. We can convert the profit 
//		after the trade has been closed
//		else if (security.getSymbol().equals(localCurrency)  && usdConversion.containsKey(security.getCurrency())){
//			convertRatio = usdConversion.get(security.getCurrency());
//		}
		
		else if (usdConversion.containsKey(security.getCurrency())){
			convertRatio = (val / usdConversion.get(security.getCurrency()));
			
		}
		
//		System.out.println("---CONVERSION RATIO FOR " + security.getSymbol() + "." 
//				+ security.getCurrency() + " & val " 
//				+ val +  " -- IS --  " + convertRatio);
//		System.out.println("---CONVERSION RATIO FOR " + security.getSymbol() 
//				+ " is -- " + usdConversion.get(security.getSymbol()));
//		System.out.println("---CONVERSION RATIO FOR " + security.getCurrency() 
//				+ " is -- " + usdConversion.get(security.getCurrency()));
		
		return convertRatio;
	}

	public static int convertPosition(Bar bar, int positionSize){
		return convertPosition(bar.getSecurity(), positionSize, bar.getClose());
	}

	public static int convertPosition(Tick tick, int positionSize, double val){
		return convertPosition(tick.getSecurity(), positionSize, val);
	}

	public static void setMinForexAmount(int minPositionSize){
		minForexAmount = minPositionSize;
	}
}
