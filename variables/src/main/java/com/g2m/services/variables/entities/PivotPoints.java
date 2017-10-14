package com.g2m.services.variables.entities;

import java.util.Date;

import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.variables.annotations.VariableEntity;
import com.g2m.services.variables.annotations.VariableValue;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
@VariableEntity
public class PivotPoints extends Variable {
	@VariableValue
	private double pivotPoint;
	@VariableValue
	private double resistance1;
	@VariableValue
	private double resistance2;
	@VariableValue
	private double resistance3;
	@VariableValue
	private double support1;
	@VariableValue
	private double support2;
	@VariableValue
	private double support3;

	public PivotPoints() {
		variableName = "PIVOT_POINTS";
		barSize = BarSize._1_DAY;
	}

	public double getPivotPoint() {
		return pivotPoint;
	}

	public double getResistance1() {
		return resistance1;
	}

	public double getResistance2() {
		return resistance2;
	}

	public double getResistance3() {
		return resistance3;
	}

	public double getSupport1() {
		return support1;
	}

	public double getSupport2() {
		return support2;
	}

	public double getSupport3() {
		return support3;
	}

	public static PivotPoints createParameters(SecurityKey securityKey) {
		PivotPoints parameters = new PivotPoints();
		parameters.securityKey = securityKey;
		parameters.barSize = BarSize._1_DAY;
		return parameters;
	}

	public static class PivotPointsBuilder {
		private PivotPoints pivotPoints;

		public PivotPointsBuilder() {
			pivotPoints = new PivotPoints();
		}

		public void setSecurityKey(SecurityKey securityKey) {
			pivotPoints.securityKey = securityKey;
		}

		public void setCalculated(boolean isCalculated) {
			pivotPoints.isCalculated = isCalculated;
		}

		public void setDateTime(Date dateTime) {
			pivotPoints.dateTime = dateTime;
		}

		public void setCalculatedOnDateTime(Date calculatedOnDateTime) {
			pivotPoints.calculatedOnDateTime = calculatedOnDateTime;
		}

		public void setPivotPoint(double pivotPoint) {
			pivotPoints.pivotPoint = pivotPoint;
		}

		public void setResistance1(double resistance1) {
			pivotPoints.resistance1 = resistance1;
		}

		public void setResistance2(double resistance2) {
			pivotPoints.resistance2 = resistance2;
		}

		public void setResistance3(double resistance3) {
			pivotPoints.resistance3 = resistance3;
		}

		public void setSupport1(double support1) {
			pivotPoints.support1 = support1;
		}

		public void setSupport2(double support2) {
			pivotPoints.support2 = support2;
		}

		public void setSupport3(double support3) {
			pivotPoints.support3 = support3;
		}

		public PivotPoints build() {
			PivotPoints pivotPoints = new PivotPoints();
			pivotPoints.barSize = this.pivotPoints.barSize;
			pivotPoints.calculatedOnDateTime = this.pivotPoints.calculatedOnDateTime;
			pivotPoints.dateTime = this.pivotPoints.dateTime;
			pivotPoints.isCalculated = this.pivotPoints.isCalculated;
			pivotPoints.securityKey = this.pivotPoints.securityKey;
			pivotPoints.pivotPoint = this.pivotPoints.pivotPoint;
			pivotPoints.resistance1 = this.pivotPoints.resistance1;
			pivotPoints.resistance2 = this.pivotPoints.resistance2;
			pivotPoints.resistance3 = this.pivotPoints.resistance3;
			pivotPoints.support1 = this.pivotPoints.support1;
			pivotPoints.support2 = this.pivotPoints.support2;
			pivotPoints.support3 = this.pivotPoints.support3;

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(pivotPoints.variableName).append(": ");
			stringBuilder.append("dateTime=").append(pivotPoints.dateTime.toString()).append(",");
			stringBuilder.append("barSize=").append(pivotPoints.barSize.toString()).append(",");
			stringBuilder.append("pivotPoint").append(pivotPoints.pivotPoint).append(",");
			stringBuilder.append("resistance1").append(pivotPoints.resistance1).append(",");
			stringBuilder.append("resistance2").append(pivotPoints.resistance2).append(",");
			stringBuilder.append("resistance3").append(pivotPoints.resistance3).append(",");
			stringBuilder.append("support1").append(pivotPoints.support1).append(",");
			stringBuilder.append("support2").append(pivotPoints.support2).append(",");
			stringBuilder.append("support3").append(pivotPoints.support3).append(",");
			stringBuilder.append(pivotPoints.securityKey.toString());
			pivotPoints.toString = stringBuilder.toString();

			return pivotPoints;
		}
	}
}
