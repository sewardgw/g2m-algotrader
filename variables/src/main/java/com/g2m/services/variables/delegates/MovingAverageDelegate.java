package com.g2m.services.variables.delegates;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.variables.annotations.VariableDelegates;
import com.g2m.services.variables.entities.MovingAverage;
import com.g2m.services.variables.entities.MovingAverage.MovingAverageBuilder;
import com.g2m.services.variables.entities.Variable;
import com.mongodb.DBObject;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
@Component
@VariableDelegates(MovingAverage.class)
public class MovingAverageDelegate extends VariableDelegate {
	private final MAType maType = MAType.Sma;

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
		if (!(variableParameters instanceof MovingAverage)) {
			throw new IllegalArgumentException("variableParameters must be an instance of MovingAverage");
		}

		double[] outputMovingAverage = new double[bars.size()];
		MInteger outputBegin = new MInteger();
		MInteger outputLength = new MInteger();

		MovingAverage castedVariableParameters = (MovingAverage) variableParameters;

		RetCode retCode = this.core.movingAverage(0, bars.size() - 1, getClosePricesDoubleArray(bars),
				castedVariableParameters.getPeriod(), this.maType, outputBegin, outputLength, outputMovingAverage);

		if (retCode != RetCode.Success) {
			throw new Exception("Error calling TALib.MovingAverage: Call not successful (" + retCode + ").");
		}

		if (bars.size() - count != outputBegin.value || count != outputLength.value) {
			throw new Exception("Error calling TALib.MovingAverage: Bad values returned (" + outputBegin.value + ", "
					+ outputLength.value + ").");
		}

		List<MovingAverage> outputValues = new ArrayList<MovingAverage>();
		MovingAverageBuilder movingAverageBuilder = new MovingAverageBuilder();
		movingAverageBuilder.setBarSize(variableParameters.getBarSize());
		movingAverageBuilder.setCalculatedOnDateTime(new Date());
		movingAverageBuilder.setCalculated(true);
		movingAverageBuilder.setPeriod(((MovingAverage) variableParameters).getPeriod());
		movingAverageBuilder.setSecurityKey(variableParameters.getSecurityKey());
		for (int i = 0; i < count; i++) {
			movingAverageBuilder.setDateTime(bars.get(bars.size() - count + i).getDateTime());
			movingAverageBuilder.setMovingAverage(outputMovingAverage[i]);
			outputValues.add(movingAverageBuilder.build());
		}

		return outputValues;
	}

	@Override
	protected int getLookBackPeriod(Variable variableParameters) {
		if (!(variableParameters instanceof MovingAverage)) {
			throw new IllegalArgumentException("variableParameters must be an instance of MovingAverage");
		}

		return this.core.movingAverageLookback(((MovingAverage) variableParameters).getPeriod(), this.maType);
	}
}
