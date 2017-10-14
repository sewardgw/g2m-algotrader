package com.g2m.services.variables.entities;

import java.util.Date;

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
public class BollingerBands extends Variable {
	@VariableParameter
	private int period;
	@VariableParameter
	private double deviationsUp;
	@VariableParameter
	private double deviationsDown;
	@VariableValue
	private double upperBand;
	@VariableValue
	private double middleBand;
	@VariableValue
	private double lowerBand;

	public BollingerBands() {
		variableName = "BOLLINGER_BANDS";
	}

	public int getPeriod() {
		return period;
	}

	public double getDeviationsUp() {
		return deviationsUp;
	}

	public double getDeviationsDown() {
		return deviationsDown;
	}

	public double getUpperBand() {
		return upperBand;
	}

	public double getMiddleBand() {
		return middleBand;
	}

	public double getLowerBand() {
		return lowerBand;
	}
	
	public double getWidthAsPerc(){
		return (upperBand - lowerBand) / lowerBand;
	}
	
	public static BollingerBands createParameters(SecurityKey securityKey, BarSize barSize, 
			int period, double deviationsUp, double deviationsDown) {
		BollingerBands parameters = new BollingerBands();
		parameters.securityKey =  securityKey;
		parameters.barSize = barSize;
		parameters.period = period;
		parameters.deviationsUp = deviationsUp;
		parameters.deviationsDown = deviationsDown;
		return parameters;
	}

	public static class BollingerBandsBuilder {
		private BollingerBands bollingerBands;
		
		public BollingerBandsBuilder() {
			bollingerBands = new BollingerBands();
		}

		public void setSecurityKey(SecurityKey securityKey) {
			bollingerBands.securityKey = securityKey;
		}

		public void setBarSize(BarSize barSize) {
			bollingerBands.barSize = barSize;
		}

		public void setCalculated(boolean isCalculated) {
			bollingerBands.isCalculated = isCalculated;
		}

		public void setDateTime(Date dateTime) {
			bollingerBands.dateTime = dateTime;
		}

		public void setCalculatedOnDateTime(Date calculatedOnDateTime) {
			bollingerBands.calculatedOnDateTime = calculatedOnDateTime;
		}

		public void setPeriod(int period) {
			bollingerBands.period = period;
		}

		public void setDeviationsUp(double deviationsUp) {
			bollingerBands.deviationsUp = deviationsUp;
		}

		public void setDeviationsDown(double deviationsDown) {
			bollingerBands.deviationsDown = deviationsDown;
		}

		public void setUpperBand(double upperBand) {
			bollingerBands.upperBand = upperBand;
		}

		public void setMiddleBand(double middleBand) {
			bollingerBands.middleBand = middleBand;
		}

		public void setLowerBand(double lowerBand) {
			bollingerBands.lowerBand = lowerBand;
		}

		public BollingerBands build() {
			BollingerBands bollingerBands = new BollingerBands();
			bollingerBands.barSize = this.bollingerBands.barSize;
			bollingerBands.calculatedOnDateTime = this.bollingerBands.calculatedOnDateTime;
			bollingerBands.dateTime = this.bollingerBands.dateTime;
			bollingerBands.isCalculated = this.bollingerBands.isCalculated;
			bollingerBands.securityKey = this.bollingerBands.securityKey;
			bollingerBands.period = this.bollingerBands.period;
			bollingerBands.deviationsUp = this.bollingerBands.deviationsUp;
			bollingerBands.deviationsDown = this.bollingerBands.deviationsDown;
			bollingerBands.upperBand = this.bollingerBands.upperBand;
			bollingerBands.middleBand = this.bollingerBands.middleBand;
			bollingerBands.lowerBand = this.bollingerBands.lowerBand;

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(bollingerBands.variableName).append(": ");
			stringBuilder.append("dateTime=").append(bollingerBands.dateTime.toString()).append(",");
			stringBuilder.append("barSize=").append(bollingerBands.barSize.toString()).append(",");
			stringBuilder.append("period=").append(bollingerBands.period).append(",");
			stringBuilder.append("deviationsUp=").append(bollingerBands.deviationsUp).append(",");
			stringBuilder.append("deviationsDown=").append(bollingerBands.deviationsDown).append(",");
			stringBuilder.append("upperBand=").append(bollingerBands.upperBand).append(",");
			stringBuilder.append("middleBand=").append(bollingerBands.middleBand).append(",");
			stringBuilder.append("lowerBand=").append(bollingerBands.lowerBand).append(",");
			stringBuilder.append(bollingerBands.securityKey.toString());
			bollingerBands.toString = stringBuilder.toString();

			return bollingerBands;
		}
	}
}
