package com.g2m.services.variables.delegates;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.variables.annotations.VariableDelegates;
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
@VariableDelegates(Rsi.class)
public class RsiDelegate extends VariableDelegate {

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
		if (!(variableParameters instanceof Rsi)) {
			throw new IllegalArgumentException("variableParameters must be an instance of Rsi");
		}

		double[] outputRsi = new double[bars.size()];
		MInteger outputBegin = new MInteger();
		MInteger outputLength = new MInteger();

		Rsi castedVariableParameters = (Rsi) variableParameters;

		RetCode retCode = this.core.rsi(0, bars.size() - 1, getClosePricesDoubleArray(bars),
				castedVariableParameters.getPeriod(), outputBegin, outputLength, outputRsi);

		if (retCode != RetCode.Success) {
			throw new Exception("Error calling TALib.Rsi: Call not successful (" + retCode + ").");
		}

		if (bars.size() - count != outputBegin.value || count != outputLength.value) {
			throw new Exception("Error calling TALib.Rsi: Bad values returned (" + outputBegin.value + ", "
					+ outputLength.value + ").");
		}

		List<Rsi> outputValues = new ArrayList<Rsi>();
		RsiBuilder rsiBuilder = new RsiBuilder();
		rsiBuilder.setBarSize(variableParameters.getBarSize());
		rsiBuilder.setCalculatedOnDateTime(new Date());
		rsiBuilder.setCalculated(true);
		rsiBuilder.setPeriod(((Rsi) variableParameters).getPeriod());
		rsiBuilder.setSecurityKey(variableParameters.getSecurityKey());
		for (int i = 0; i < count; i++) {
			rsiBuilder.setDateTime(bars.get(bars.size() - count + i).getDateTime());
			rsiBuilder.setRsi(outputRsi[i]);
			outputValues.add(rsiBuilder.build());
		}

		return outputValues;
	}

	@Override
	protected int getLookBackPeriod(Variable variableParameters) {
		if (!(variableParameters instanceof Rsi)) {
			throw new IllegalArgumentException("variableParameters must be an instance of Rsi");
		}
		return this.core.rsiLookback(((Rsi) variableParameters).getPeriod());
	}
}
