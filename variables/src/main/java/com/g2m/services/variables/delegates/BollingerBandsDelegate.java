package com.g2m.services.variables.delegates;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.variables.annotations.VariableDelegates;
import com.g2m.services.variables.entities.BollingerBands;
import com.g2m.services.variables.entities.BollingerBands.BollingerBandsBuilder;
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
@VariableDelegates(BollingerBands.class)
public class BollingerBandsDelegate extends VariableDelegate {
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
		if (!(variableParameters instanceof BollingerBands)) {
			throw new IllegalArgumentException("variableParameters must be an instance of BollingerBands");
		}

		double[] outputUpperBand = new double[bars.size()];
		double[] outputMiddleBand = new double[bars.size()];
		double[] outputLowerBand = new double[bars.size()];
		MInteger outputBegin = new MInteger();
		MInteger outputLength = new MInteger();

		BollingerBands castedVariableParameters = (BollingerBands) variableParameters;

		RetCode retCode = this.core.bbands(0, bars.size() - 1, getClosePricesDoubleArray(bars),
				castedVariableParameters.getPeriod(), castedVariableParameters.getDeviationsUp(),
				castedVariableParameters.getDeviationsDown(), this.maType, outputBegin, outputLength, outputUpperBand,
				outputMiddleBand, outputLowerBand);

		if (retCode != RetCode.Success) {
			throw new Exception("Error calling TALib.BBands: Call not successful (" + retCode + ").");
		}

		if (bars.size() - count != outputBegin.value || count != outputLength.value) {
			throw new Exception("Error calling TALib.BBands: Bad values returned (" + outputBegin.value + ", "
					+ outputLength.value + ").");
		}

		List<BollingerBands> outputValues = new ArrayList<BollingerBands>();
		BollingerBandsBuilder bollingerBandsBuilder = new BollingerBandsBuilder();
		bollingerBandsBuilder.setBarSize(variableParameters.getBarSize());
		bollingerBandsBuilder.setCalculatedOnDateTime(new Date());
		bollingerBandsBuilder.setCalculated(true);
		bollingerBandsBuilder.setDeviationsUp(((BollingerBands) variableParameters).getDeviationsUp());
		bollingerBandsBuilder.setDeviationsDown(((BollingerBands) variableParameters).getDeviationsDown());
		bollingerBandsBuilder.setPeriod(((BollingerBands) variableParameters).getPeriod());
		bollingerBandsBuilder.setSecurityKey(variableParameters.getSecurityKey());
		for (int i = 0; i < count; i++) {
			bollingerBandsBuilder.setDateTime(bars.get(bars.size() - count + i).getDateTime());
			bollingerBandsBuilder.setUpperBand(outputUpperBand[i]);
			bollingerBandsBuilder.setMiddleBand(outputMiddleBand[i]);
			bollingerBandsBuilder.setLowerBand(outputLowerBand[i]);
			outputValues.add(bollingerBandsBuilder.build());
		}

		return outputValues;
	}

	@Override
	protected int getLookBackPeriod(Variable variableParameters) {
		if (!(variableParameters instanceof BollingerBands)) {
			throw new IllegalArgumentException("variableParameters must be an instance of BollingerBands");
		}

		BollingerBands castedVariableParameters = (BollingerBands) variableParameters;

		return this.core.bbandsLookback(castedVariableParameters.getPeriod(), castedVariableParameters.getDeviationsUp(),
				castedVariableParameters.getDeviationsDown(), this.maType);
	}
}
