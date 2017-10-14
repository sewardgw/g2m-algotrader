package com.g2m.services.variables.entities;

import java.util.Date;
import java.util.List;

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
public class MovingAverage extends Variable {
	@VariableParameter
	private int period;
	@VariableValue
	private double movingAverage;

	public MovingAverage() {
		variableName = "MOVING_AVERAGE";
	}

	public int getPeriod() {
		return period;
	}

	public double getMovingAverage() {
		return movingAverage;
	}

	public boolean isTrendConstant(List<MovingAverage> values, int numBars, boolean movingUpwards){
		
		// Loop through the list of moving averages and compare item
		// n to n-1 for however many values were requested. 
		int i = 0;
		if(movingUpwards){
			while (i < numBars-2){
				if(values.get(0+i).getMovingAverage() > values.get(0+(i+1)).getMovingAverage())
					return false;
				i++;
			}
		} else {
			while (i < numBars-2){
				if(values.get(0+i).getMovingAverage() < values.get(0+(i+1)).getMovingAverage())
					return false;
				i++;
			}
		}
		
		return true;
	}
	
	public static MovingAverage createParameters(SecurityKey securityKey, BarSize barSize, int period) {
		MovingAverage parameters = new MovingAverage();
		parameters.securityKey = securityKey;
		parameters.barSize = barSize;
		parameters.period = period;
		return parameters;
	}

	public static class MovingAverageBuilder {
		private MovingAverage movingAverage;

		public MovingAverageBuilder() {
			movingAverage = new MovingAverage();
		}

		public void setSecurityKey(SecurityKey securityKey) {
			movingAverage.securityKey = securityKey;
		}

		public void setBarSize(BarSize barSize) {
			movingAverage.barSize = barSize;
		}

		public void setCalculated(boolean isCalculated) {
			movingAverage.isCalculated = isCalculated;
		}

		public void setDateTime(Date dateTime) {
			movingAverage.dateTime = dateTime;
		}

		public void setCalculatedOnDateTime(Date calculatedOnDateTime) {
			movingAverage.calculatedOnDateTime = calculatedOnDateTime;
		}

		public void setPeriod(int period) {
			movingAverage.period = period;
		}

		public void setMovingAverage(double movingAverage) {
			this.movingAverage.movingAverage = movingAverage;
		}

		public MovingAverage build() {
			MovingAverage movingAverage = new MovingAverage();
			movingAverage.barSize = this.movingAverage.barSize;
			movingAverage.calculatedOnDateTime = this.movingAverage.calculatedOnDateTime;
			movingAverage.dateTime = this.movingAverage.dateTime;
			movingAverage.isCalculated = this.movingAverage.isCalculated;
			movingAverage.securityKey = this.movingAverage.securityKey;
			movingAverage.period = this.movingAverage.period;
			movingAverage.movingAverage = this.movingAverage.movingAverage;

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(movingAverage.variableName).append(": ");
			stringBuilder.append("dateTime=").append(movingAverage.dateTime.toString()).append(",");
			stringBuilder.append("barSize=").append(movingAverage.barSize.toString()).append(",");
			stringBuilder.append("period=").append(movingAverage.period).append(",");
			stringBuilder.append("movingAverage=").append(movingAverage.movingAverage).append(",");
			stringBuilder.append(movingAverage.securityKey.toString());
			movingAverage.toString = stringBuilder.toString();

			return movingAverage;
		}
	}
}
