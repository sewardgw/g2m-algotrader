package com.g2m.services.variables.delegates;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.variables.annotations.VariableDelegates;
import com.g2m.services.variables.entities.PivotPoints;
import com.g2m.services.variables.entities.PivotPoints.PivotPointsBuilder;
import com.g2m.services.variables.entities.Variable;
import com.mongodb.DBObject;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
@Component
@VariableDelegates(PivotPoints.class)
public class PivotPointsDelegate extends VariableDelegate {

	/**
	 * <pre>
	 * Pivot Point = (Previous High + Previous Low + Previous Close) / 3
	 * 
	 * Resistance Level 1 = (2 * Pivot Point) - Previous Low Support Level 1 =
	 * (2 * Pivot Point) - Previous High
	 * 
	 * Resistance Level 2 = (Pivot Point - Support Level 1) + Resistance Level 1
	 * Support Level 2 = Pivot Point - (Resistance Level 1 - Support Level 1)
	 * 
	 * Resistance Level 3 = (Pivot Point - Support Level 2) + Resistance Level 2
	 * Support Level 3 = Pivot Point - (Resistance Level 2 - Support Level 2)
	 * </pre>
	 */
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
	protected List<? extends Variable> calculateVariable(List<Bar> bars, Variable variableParameters, int count)
			throws Exception {
		if (!(variableParameters instanceof PivotPoints)) {
			throw new IllegalArgumentException("variableParameters must be an instance of PivotPoints");
		}

		List<PivotPoints> outputValues = new ArrayList<PivotPoints>();
		PivotPointsBuilder pivotPointsBuilder = new PivotPointsBuilder();
		pivotPointsBuilder.setCalculatedOnDateTime(bars.get(bars.size()-1).getDateTime());
		pivotPointsBuilder.setCalculated(true);
		pivotPointsBuilder.setSecurityKey(variableParameters.getSecurityKey());
		for (Bar bar : bars) {
			double pivotPoint = (bar.getHigh() + bar.getLow() + bar.getClose()) / 3;
			double resistance1 = (2 * pivotPoint) - bar.getLow();
			double support1 = (2 * pivotPoint) - bar.getHigh();
			double resistance2 = (pivotPoint - support1 + resistance1);
			double support2 = pivotPoint - (resistance1 - support1);
			double resistance3 = (pivotPoint - support2) + resistance2;
			double support3 = pivotPoint - (resistance2 - support2);

			pivotPointsBuilder.setPivotPoint(pivotPoint);
			pivotPointsBuilder.setResistance1(resistance1);
			pivotPointsBuilder.setResistance2(resistance2);
			pivotPointsBuilder.setResistance3(resistance3);
			pivotPointsBuilder.setSupport1(support1);
			pivotPointsBuilder.setSupport2(support2);
			pivotPointsBuilder.setSupport3(support3);
			pivotPointsBuilder.setDateTime(bar.getDateTime());
			outputValues.add(pivotPointsBuilder.build());
		}

		return outputValues;
	}

	@Override
	protected int getLookBackPeriod(Variable variableParameters) {
		return 0;
	}
}
