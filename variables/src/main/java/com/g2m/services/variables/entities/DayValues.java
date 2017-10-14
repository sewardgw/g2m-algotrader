package com.g2m.services.variables.entities;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.variables.annotations.VariableEntity;
import com.g2m.services.variables.annotations.VariableParameter;
import com.g2m.services.variables.annotations.VariableValue;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
@VariableEntity
public class DayValues extends Variable {
	
	@VariableParameter
	private int period;
	@VariableValue
	private double volatility;
	@VariableValue
	private BarSize volumeBarSize;
	@VariableValue
	private long avgVolume;
	@VariableValue
	private long maxVolume;
	@VariableValue
	private long minVolume;
	@VariableValue
	private double rateOfChange;
	@Autowired
	@VariableValue
	private Map <String, Integer> timeVolumeMap;
	
	public DayValues() {
		variableName = "DAY_VALUES";
	}

	public double getVolatility() {
		return volatility;
	}
	
	public int getPeriod() {
		return period;
	}
	
	public long getAvgVolume() {
		return avgVolume;
	}
	
	public long getMaxVolume() {
		return maxVolume;
	}
	
	public long getMinVolume() {
		return minVolume;
	}
	
	public double getRateOfChange() {
		return rateOfChange;
	}
	
	public BarSize getVolumeBarSize() {
		return volumeBarSize;
	}
	
	public int getTimeWindowVolumeAvg(long currTime){
		// Change this so that it identifies the current start time for the volume bar
		return timeVolumeMap.get(Long.toString(currTime));
	}
	
	public double calcVolatilityAvgDiff(List<DayValues> dv, int numBarsHistorical){
		
		double histVol = 0.0;
		
		for (int n = 1; n <= numBarsHistorical; n++){
			histVol += dv.get(dv.size()-(n+1)).getVolatility();
		}
		
		return (dv.get(dv.size()-1).getVolatility()-histVol) / histVol;
	}

	
	public static DayValues createParameters(SecurityKey securityKey, BarSize volumeBarSize, int period) {
		DayValues parameters = new DayValues();
		parameters.securityKey = securityKey;
		parameters.barSize = BarSize._1_DAY;
		parameters.barSize = volumeBarSize;
		parameters.period = period;
		return parameters;
	}

	public static class DayValuesBuilder {
		private DayValues dayValues;

		public DayValuesBuilder() {
			dayValues = new DayValues();
		}

		public void setSecurityKey(SecurityKey securityKey) {
			dayValues.securityKey = securityKey;
		}

		public void setBarSize(BarSize barSize) {
			dayValues.barSize = barSize;
		}

		public void setCalculated(boolean isCalculated) {
			dayValues.isCalculated = isCalculated;
		}

		public void setDateTime(Date dateTime) {
			dayValues.dateTime = dateTime;
		}

		public void setCalculatedOnDateTime(Date calculatedOnDateTime) {
			dayValues.calculatedOnDateTime = calculatedOnDateTime;
		}

		public void setPeriod(int period) {
			dayValues.period = period;
		}
		
		public void setVolatility(double vol){
			dayValues.volatility = vol;
		}
		
		public void setAvgVol(long vol){
			dayValues.avgVolume = vol;
		}
		
		public void setMinVol(long vol){
			dayValues.minVolume = vol;
		}
		
		public void setMaxVol(long vol){
			dayValues.maxVolume = vol;
		}
		
		public void setRateOfChange(double roc){
			dayValues.rateOfChange = roc;
		}
		
		public void setVolumeBarSize(BarSize barSize){
			dayValues.volumeBarSize = barSize;
		}

		public DayValues build() {
			DayValues dayValues = new DayValues();
			dayValues.barSize = this.dayValues.barSize;
			dayValues.calculatedOnDateTime = this.dayValues.calculatedOnDateTime;
			dayValues.dateTime = this.dayValues.dateTime;
			dayValues.isCalculated = this.dayValues.isCalculated;
			dayValues.securityKey = this.dayValues.securityKey;
			dayValues.period = this.dayValues.period;
			dayValues.volumeBarSize = this.dayValues.volumeBarSize;
			dayValues.maxVolume = this.dayValues.maxVolume;
			dayValues.minVolume = this.dayValues.minVolume;
			dayValues.avgVolume = this.dayValues.avgVolume;
			dayValues.volatility = this.dayValues.volatility;
			

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(dayValues.variableName).append(": ");
			stringBuilder.append("dateTime=").append(dayValues.dateTime.toString()).append(",");
			stringBuilder.append("barSize=").append(dayValues.barSize.toString()).append(",");
			stringBuilder.append("volumeBarSize=").append(dayValues.volumeBarSize).append(",");
			stringBuilder.append("period=").append(dayValues.period).append(",");
			stringBuilder.append("volatility=").append(dayValues.volatility).append(",");
			stringBuilder.append("avgVol=").append(dayValues.avgVolume).append(",");
			stringBuilder.append("maxVol=").append(dayValues.maxVolume).append(",");
			stringBuilder.append("minVol=").append(dayValues.minVolume).append(",");
			stringBuilder.append(dayValues.securityKey.toString());
			dayValues.toString = stringBuilder.toString();

			return dayValues;
		}
	}
}
