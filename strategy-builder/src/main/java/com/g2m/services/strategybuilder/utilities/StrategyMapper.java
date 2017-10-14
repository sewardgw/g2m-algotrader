package com.g2m.services.strategybuilder.utilities;

import com.g2m.services.tradingservices.enums.BarSize;

public class StrategyMapper {

	public StrategyMapper() {
		
	}
	
	public BarSize getMinBarSizeForHistoricalPull(BarSize currBarSize){
		
		if(currBarSize == BarSize._1_SEC || currBarSize == BarSize._5_SECS ||
			currBarSize == BarSize._10_SECS || currBarSize == BarSize._15_SECS || 
			currBarSize == BarSize._30_SECS || currBarSize == BarSize._1_MIN ||    
			currBarSize == BarSize._2_MINS || currBarSize == BarSize._3_MINS ||
			currBarSize == BarSize._5_MINS || currBarSize == BarSize._10_MINS || 
			currBarSize == BarSize._15_MINS)
			return BarSize._1_MIN;
		
		else if (currBarSize == BarSize._30_MINS || currBarSize == BarSize._1_HOUR ||
				currBarSize == BarSize._4_HOURS )
			return BarSize._5_MINS;
		
		else if (currBarSize == BarSize._1_DAY)
			return BarSize._15_MINS;
			
		return BarSize._1_MIN;	
	}

}
