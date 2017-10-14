package com.g2m.services.variables.delegates;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.variables.annotations.VariableDelegates;
import com.g2m.services.variables.entities.Macd;
import com.g2m.services.variables.entities.Macd.MacdBuilder;
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
@VariableDelegates(Macd.class)
public class MacdDelegate extends VariableDelegate {

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
		if (!(variableParameters instanceof Macd)) {
			throw new IllegalArgumentException("variableParameters must be an instance of Macd");
		}

		double[] outputMacd = new double[bars.size()];
		double[] outputSignal = new double[bars.size()];
		double[] outputHistogram = new double[bars.size()];
		MInteger outputBegin = new MInteger();
		MInteger outputLength = new MInteger();

		Macd castedVariableParameters = (Macd) variableParameters;

		RetCode retCode = this.core.macd(0, bars.size() - 1, getClosePricesDoubleArray(bars),
				castedVariableParameters.getFastPeriod(), castedVariableParameters.getSlowPeriod(),
				castedVariableParameters.getSignalPeriod(), outputBegin, outputLength, outputMacd, outputSignal,
				outputHistogram);

		if (retCode != RetCode.Success) {
			throw new Exception("Error calling TALib.MACD: Call not successful (" + retCode + ").");
		}

		if (bars.size() - count != outputBegin.value || count != outputLength.value) {
			throw new Exception("Error calling TALib.MACD: Bad values returned (" + outputBegin.value + ", "
					+ outputLength.value + ").");
		}

		List<Macd> outputValues = new ArrayList<Macd>();
		MacdBuilder macdBuilder = new MacdBuilder();
		macdBuilder.setBarSize(variableParameters.getBarSize());
		macdBuilder.setCalculatedOnDateTime(new Date());
		macdBuilder.setCalculated(true);
		macdBuilder.setFastPeriod(((Macd) variableParameters).getFastPeriod());
		macdBuilder.setSlowPeriod(((Macd) variableParameters).getSlowPeriod());
		macdBuilder.setSignalPeriod(((Macd) variableParameters).getSignalPeriod());
		macdBuilder.setSecurityKey(variableParameters.getSecurityKey());
		for (int i = 0; i < count; i++) {
			macdBuilder.setDateTime(bars.get(bars.size() - count + i).getDateTime());
			macdBuilder.setMacd(outputMacd[i]);
			macdBuilder.setSignal(outputSignal[i]);
			macdBuilder.setHistogram(outputHistogram[i]);
			outputValues.add(macdBuilder.build());
		}

		return outputValues;
	}

	@Override
	protected int getLookBackPeriod(Variable variableParameters) {
		if (!(variableParameters instanceof Macd)) {
			throw new IllegalArgumentException("variableParameters must be an instance of Macd");
		}

		Macd castedVariableParameters = (Macd) variableParameters;

		return this.core.macdLookback(castedVariableParameters.getFastPeriod(), castedVariableParameters.getSlowPeriod(),
				castedVariableParameters.getSignalPeriod());
	}
}
