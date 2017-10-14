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
public class Rsi extends Variable {
	@VariableParameter
	private int period;
	@VariableValue
	private double rsi;

	public Rsi() {
		variableName = "RSI";
	}

	public double getRsi() {
		return rsi;
	}

	public int getPeriod() {
		return period;
	}

	public static Rsi createParameters(SecurityKey securityKey, BarSize barSize, int period) {
		Rsi parameters = new Rsi();
		parameters.securityKey = securityKey;
		parameters.barSize = barSize;
		parameters.period = period;
		return parameters;
	}

	public static class RsiBuilder {
		private Rsi rsi;

		public RsiBuilder() {
			rsi = new Rsi();
		}

		public void setSecurityKey(SecurityKey securityKey) {
			rsi.securityKey = securityKey;
		}

		public void setBarSize(BarSize barSize) {
			rsi.barSize = barSize;
		}

		public void setCalculated(boolean isCalculated) {
			rsi.isCalculated = isCalculated;
		}

		public void setDateTime(Date dateTime) {
			rsi.dateTime = dateTime;
		}

		public void setCalculatedOnDateTime(Date calculatedOnDateTime) {
			rsi.calculatedOnDateTime = calculatedOnDateTime;
		}

		public void setPeriod(int period) {
			rsi.period = period;
		}

		public void setRsi(double rsi) {
			this.rsi.rsi = rsi;
		}

		public Rsi build() {
			Rsi rsi = new Rsi();
			rsi.barSize = this.rsi.barSize;
			rsi.calculatedOnDateTime = this.rsi.calculatedOnDateTime;
			rsi.dateTime = this.rsi.dateTime;
			rsi.isCalculated = this.rsi.isCalculated;
			rsi.securityKey = this.rsi.securityKey;
			rsi.period = this.rsi.period;
			rsi.rsi = this.rsi.rsi;

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(rsi.variableName).append(": ");
			stringBuilder.append("dateTime=").append(rsi.dateTime.toString()).append(",");
			stringBuilder.append("barSize=").append(rsi.barSize.toString()).append(",");
			stringBuilder.append("period=").append(rsi.period).append(",");
			stringBuilder.append("rsi=").append(rsi.rsi).append(",");
			stringBuilder.append(rsi.securityKey.toString());
			rsi.toString = stringBuilder.toString();

			return rsi;
		}
	}
}
