package com.g2m.services.variables.delegates;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.g2m.services.tradingservices.caches.BarCache;
import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.variables.caches.VariableCache;
import com.g2m.services.variables.entities.Variable;
import com.g2m.services.variables.persistence.DBRetrieval;
import com.mongodb.DBObject;
import com.tictactec.ta.lib.Core;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
public abstract class VariableDelegate {
	@Autowired
	private VariableCache variableCache;
	@Autowired
	private BarCache barCache;
	@Autowired
	private DBRetrieval variableRetrieval;
	protected Core core;

	public VariableDelegate() {
		core = new Core();
	}

	public Variable getValues(Variable searchParameters, Date beforeDateTime) throws Exception {
		List<? extends Variable> values = this.getValues(searchParameters, beforeDateTime, 1);
		if (0 < values.size()) {
			return values.get(0);
		} else {
			return (Variable) searchParameters.getClass().newInstance();
		}
	}

	public List<? extends Variable> getValues(Variable searchParameters, Date beforeDateTime, int count) throws Exception {
		List<? extends Variable> values;
		values = variableCache.getValues(searchParameters, beforeDateTime, count);	
		if (values.isEmpty()) {
			return fetchBarsAndCalculateVariable(searchParameters, beforeDateTime, count);
		} else {
			return values;
		}
	}

	// Added to be able to return a list of unknown length, such as the TrendLines
	@SuppressWarnings("unchecked")
	public List<? extends Variable> getValuesUnknownListLength(Variable searchParameters, Date currentDateTime) throws Exception {
		List<? extends Variable> values;
		// The normal request flow from the ticks will always have false for the requestFromParent input 
		values = variableCache.getValuesUnknownListLength(searchParameters, currentDateTime, false);
		if (values.isEmpty()) {
			return fetchBarsAndCalculateVariableUnknownListLength(searchParameters, currentDateTime);
		} else {
			return values;
		}
	}


	private List<? extends Variable> fetchBarsAndCalculateVariable(Variable searchParameters, Date beforeDateTime, int count)
			throws Exception {
		int numberOfBarsNeeded = count + getLookBackPeriod(searchParameters);
		List<Bar> bars = barCache.getBars(searchParameters.getSecurityKey(), searchParameters.getBarSize(),
				beforeDateTime, numberOfBarsNeeded);
		if (numberOfBarsNeeded > bars.size()) {
			return Collections.emptyList();
		}
		return calculateVariableAndSaveToCache(searchParameters, beforeDateTime, count, bars);
	}

	// Added to be able to request the bars needed without having the count of variables to be returned
	private List<? extends Variable> fetchBarsAndCalculateVariableUnknownListLength(Variable searchParameters, Date beforeDateTime)
			throws Exception {
		int numberOfBarsNeeded = getLookBackPeriod(searchParameters);
		List<Bar> bars = barCache.getBars(searchParameters.getSecurityKey(), searchParameters.getBarSize(),
				beforeDateTime, numberOfBarsNeeded);
		if (numberOfBarsNeeded > bars.size()) {
			return Collections.emptyList();
		}
		return calculateVariableAndSaveToCacheUnknownListLength(searchParameters, beforeDateTime, bars);
	}


	private List<? extends Variable> calculateVariableAndSaveToCache(Variable searchParameters, Date beforeDateTime,
			int count, List<Bar> bars) throws Exception {
		List<? extends Variable> calculatedValues = calculateVariable(bars, searchParameters, count);
		if(!calculatedValues.isEmpty()){
			variableCache.save(calculatedValues, searchParameters, beforeDateTime);
			return calculatedValues;
		} else {
			return Collections.EMPTY_LIST;
		}
	}

	private List<? extends Variable> calculateVariableAndSaveToCacheUnknownListLength(Variable searchParameters, Date beforeDateTime,
			List<Bar> bars) throws Exception {
		List<? extends Variable> calculatedValues = calculateVariable(bars, searchParameters);
		if (!calculatedValues.isEmpty()){
			variableCache.saveUnknownListLength(calculatedValues, searchParameters, beforeDateTime);
			return calculatedValues;
		} else {
			return Collections.EMPTY_LIST;
		}
	}

	protected double[] getClosePricesDoubleArray(List<Bar> barList) {
		double barArray[] = new double[barList.size()];
		for (int i = 0; i < barList.size(); i++) {
			barArray[i] = barList.get(i).getClose();
		}
		return barArray;
	}

	protected abstract List<? extends Variable> calculateVariable(List<Bar> bars, Variable searchParameters, int count)
			throws Exception;

	protected abstract List<? extends Variable> calculateVariable(List<Bar> bars, Variable searchParameters)
			throws Exception;

	protected abstract int getLookBackPeriod(Variable searchParameters);

	public void getExistingVariablesFromDB(Variable parameters) throws Exception{
		List<? extends Variable> calculatedValues = convertExistingVariablesFromDBObject(
				variableRetrieval.getVariablesFromMongo(parameters), parameters);
		variableCache.saveUnknownListLength(calculatedValues, parameters, new Date());
	}

	protected abstract List<? extends Variable> convertExistingVariablesFromDBObject(List<DBObject> existingValues,
			Variable parameters);
}
