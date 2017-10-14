package com.g2m.services.variables.delegates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.variables.annotations.VariableDelegates;
import com.g2m.services.variables.entities.DayValues;
import com.g2m.services.variables.entities.DayValues.DayValuesBuilder;
import com.g2m.services.variables.entities.Rsi;
import com.g2m.services.variables.entities.Rsi.RsiBuilder;
import com.g2m.services.variables.entities.Variable;
import com.mongodb.DBObject;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
@Component
@VariableDelegates(DayValues.class)
public class DayValuesDelegate extends VariableDelegate {

	@Override
	protected List<? extends Variable> calculateVariable(List<Bar> bars, Variable variableParameters)
			throws Exception {
		// Not currently used, only a place holder since the 
		// Variables that don't expire will use this function
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<? extends Variable> convertExistingVariablesFromDBObject(List<DBObject> existingValues, 
			Variable parameters){
		// Not currently used, only a place holder since the 
		// Variables that don't expire will use this function
		return null;
	}


	@Override
	protected List<? extends Variable> calculateVariable(List<Bar> bars, Variable variableParameters, 
			int count)
			throws Exception {
		if (!(variableParameters instanceof DayValues)) {
			throw new IllegalArgumentException("variableParameters must be an instance of Day Values");
		}

		List<DayValues> outputValues = new ArrayList<DayValues>();
		DayValuesBuilder dayValuesBuilder = new DayValuesBuilder();
		dayValuesBuilder.setBarSize(variableParameters.getBarSize());
		dayValuesBuilder.setCalculatedOnDateTime(new Date());
		dayValuesBuilder.setCalculated(true);
		dayValuesBuilder.setPeriod(((DayValues) variableParameters).getPeriod());
		dayValuesBuilder.setSecurityKey(variableParameters.getSecurityKey());
		//dayValuesBuilder.setVolumeBarSize(vol);
		for (int i = 0; i < count; i++) {
			dayValuesBuilder.setDateTime(bars.get(bars.size() - count + i).getDateTime());
			dayValuesBuilder.setVolatility(calcVolatility(bars, i, count));
			dayValuesBuilder.setAvgVol(calcAvgVol(bars, i, count));
			dayValuesBuilder.setMinVol(calcMinVol(bars, i, count));
			dayValuesBuilder.setMaxVol(calcMaxVol(bars, i, count));
			dayValuesBuilder.setRateOfChange(calcRateOfChange(bars, i, count));
			outputValues.add(dayValuesBuilder.build());
		}
		return outputValues;
	}

	private long calcMaxVol(List<Bar> bars, int i, int count) {
		long maxVol = 0;
		for (int m = 0 + (count - i); m < bars.size() - (count - i); m++){
			if(bars.get(m +(count - i)).getVolume() > maxVol)
				maxVol = bars.get(m +(count - i)).getVolume();
		}
		return maxVol;
	}

	private long calcMinVol(List<Bar> bars, int i, int count) {
		long minVol = Integer.MAX_VALUE;
		
		for (int m = 0 + (count - i); m < bars.size() - (count - i); m++){
			if(bars.get(m +(count - i)).getVolume() < minVol)
				minVol = bars.get(m +(count - i)).getVolume();
		}
		return minVol;
	}

	private long calcAvgVol(List<Bar> bars, int i, int count) {
		double avgVol = 0;
		int totalDays = 0;
		
		for (int m = 0 + (count - i); m < bars.size() - (count - i); m++){
			avgVol += bars.get(m +(count - i)).getVolume();
			totalDays ++;
		}
		return (long) avgVol / totalDays;
	}

	private double calcVolatility(List<Bar> bars, int i, int count) {
		
		//Collections.reverse(bars);
		double highLowDiffTot = 0;
		int totalBars = 0;
		
		for (int m = 0 + (count - i); m < bars.size() - (count - i); m++){
			highLowDiffTot += (bars.get(m +(count - i)).getHigh() -  bars.get(m +(count - i)).getLow()) /
					bars.get(m +(count - i)).getLow();
			totalBars ++;
		}
		return (highLowDiffTot / totalBars) * 100;
	}
	
	private double calcRateOfChange(List<Bar> bars, int i, int count) {
		
		double firstPrice = bars.get(0 + i).getClose();
		long firstTime = bars.get(0 + i).getDateTime().getTime();
		double lastPrice = bars.get(bars.size()-1).getClose();
		long lastTime = bars.get(bars.size()-1).getDateTime().getTime();
		double roc = (double)(lastPrice - firstPrice)/(lastTime - firstTime); 
		
		return roc * 100000;
	}
	
	@Override
	protected int getLookBackPeriod(Variable variableParameters) {
		if (!(variableParameters instanceof DayValues)) {
			throw new IllegalArgumentException("variableParameters must be an instance of Day Values");
		}
		return ((DayValues) variableParameters).getPeriod();
	}
}
