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
public class Macd extends Variable {
	@VariableParameter
	private int slowPeriod;
	@VariableParameter
	private int fastPeriod;
	@VariableParameter
	private int signalPeriod;
	@VariableValue
	private double macd;
	@VariableValue
	private double signal;
	@VariableValue
	private double histogram;

	public Macd() {
		variableName = "MACD";
	}

	public int getSlowPeriod() {
		return slowPeriod;
	}

	public int getFastPeriod() {
		return fastPeriod;
	}

	public int getSignalPeriod() {
		return signalPeriod;
	}

	public double getMacd() {
		return macd;
	}

	public double getSignal() {
		return signal;
	}

	public double getHistogram() {
		return histogram;
	}

	public static Macd createParameters(SecurityKey securityKey, BarSize barSize, int slowPeriod, int fastPeriod,
			int signalPeriod) {
		Macd parameters = new Macd();
		parameters.securityKey = securityKey;
		parameters.barSize = barSize;
		parameters.slowPeriod = slowPeriod;
		parameters.fastPeriod = fastPeriod;
		parameters.signalPeriod = signalPeriod;
		return parameters;
	}
	
	public static Macd createParameters(SecurityKey securityKey, BarSize barSize) {
		return createParameters(securityKey, barSize, 26, 12, 9);
	}

	public static class MacdBuilder {
		private Macd macd;

		public MacdBuilder() {
			macd = new Macd();
		}

		public void setSecurityKey(SecurityKey securityKey) {
			macd.securityKey = securityKey;
		}

		public void setBarSize(BarSize barSize) {
			macd.barSize = barSize;
		}

		public void setCalculated(boolean isCalculated) {
			macd.isCalculated = isCalculated;
		}

		public void setDateTime(Date dateTime) {
			macd.dateTime = dateTime;
		}

		public void setCalculatedOnDateTime(Date calculatedOnDateTime) {
			macd.calculatedOnDateTime = calculatedOnDateTime;
		}

		public void setSlowPeriod(int slowPeriod) {
			macd.slowPeriod = slowPeriod;
		}

		public void setFastPeriod(int fastPeriod) {
			macd.fastPeriod = fastPeriod;
		}

		public void setSignalPeriod(int signalPeriod) {
			macd.signalPeriod = signalPeriod;
		}

		public void setMacd(double macd) {
			this.macd.macd = macd;
		}

		public void setSignal(double signal) {
			macd.signal = signal;
		}

		public void setHistogram(double histogram) {
			macd.histogram = histogram;
		}

		public Macd build() {
			Macd macd = new Macd();
			macd.barSize = this.macd.barSize;
			macd.calculatedOnDateTime = this.macd.calculatedOnDateTime;
			macd.dateTime = this.macd.dateTime;
			macd.isCalculated = this.macd.isCalculated;
			macd.securityKey = this.macd.securityKey;
			macd.slowPeriod = this.macd.slowPeriod;
			macd.fastPeriod = this.macd.fastPeriod;
			macd.signalPeriod = this.macd.signalPeriod;
			macd.macd = this.macd.macd;
			macd.signal = this.macd.signal;
			macd.histogram = this.macd.histogram;

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(macd.variableName).append(": ");
			stringBuilder.append("dateTime=").append(macd.dateTime.toString()).append(",");
			stringBuilder.append("barSize=").append(macd.barSize.toString()).append(",");
			stringBuilder.append("slowPeriod=").append(macd.slowPeriod).append(",");
			stringBuilder.append("fastPeriod=").append(macd.fastPeriod).append(",");
			stringBuilder.append("signalPeriod=").append(macd.signalPeriod).append(",");
			stringBuilder.append("macd=").append(macd.macd).append(",");
			stringBuilder.append("signal").append(macd.signal).append(",");
			stringBuilder.append("histogram=").append(macd.histogram).append(",");
			stringBuilder.append(macd.securityKey.toString());
			macd.toString = stringBuilder.toString();

			return macd;
		}
	}
}
