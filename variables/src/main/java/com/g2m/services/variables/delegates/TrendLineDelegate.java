package com.g2m.services.variables.delegates;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Transient;
import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.caches.BarCache;
import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.variables.annotations.VariableDelegates;
import com.g2m.services.variables.annotations.VariableParameter;
import com.g2m.services.variables.annotations.VariableValue;
import com.g2m.services.variables.caches.VariableCache;
import com.g2m.services.variables.entities.TrendLine;
import com.g2m.services.variables.entities.TrendLine.TrendLineBuilder;
import com.g2m.services.variables.entities.Variable;
import com.g2m.services.variables.utilities.DBObjectConverter;
import com.mongodb.DBObject;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
@Component
@VariableDelegates(TrendLine.class)
public class TrendLineDelegate extends NonExpirableVariableDelegate {


	@Autowired
	private VariableCache variableCache;

	// this is the total number of bars to return from the bar cache when looking
	// to see if there was a new high or a new low
	int barsInRecentWindow = 20;
	int barsUntilSet = 10;
	int barsUntilActive = 4;
	static int i = 0;


	@SuppressWarnings("unchecked")
	@Override
	protected List<? extends Variable> calculateVariable(List<Bar> bars, Variable variableParameters)
			throws Exception {

		if (!(variableParameters instanceof TrendLine)) {
			throw new IllegalArgumentException("variableParameters must be an instance of MovingAverage");
		}

		// Get the existing TrendLines that were already created
		List<TrendLine> existingTrendLines = (List<TrendLine>) variableCache
				.getValuesUnknownListLength(variableParameters,bars.get(bars.size()-1).getDateTime(), true);

		// This will: 1) check to see if a new TrendLine should be added
		// 2) Remove TrendLines that haven't been set as Active if needed
		// 3) Update the exiting TrendLines
		List<TrendLine> outputValues = updateTrendLines(bars, existingTrendLines);

		return outputValues;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<? extends Variable> calculateVariable(List<Bar> bars, Variable variableParameters, int count)
			throws Exception {

		// Not currently used, only a place holder since the 
		// Variables that do expire will use this function

		return null;
	}



	@Override
	protected int getLookBackPeriod(Variable variableParameters) {

		if (!(variableParameters instanceof TrendLine)) {
			throw new IllegalArgumentException("variableParameters must be an instance of TrendLine");
		}

		return barsInRecentWindow;
	}



	public List<TrendLine> updateTrendLines(List<Bar> bars, List<TrendLine> existingLines) {

		List<TrendLine> newTrendLines = null;

		// check to see if the high or low of this bar is greater / less than each of the prior 20 bars. 
		// Return of 1 = new high, return 2 = new low, return 3 = both, return 0 = neither
		switch(isNewHighOrLowInRecentWindow(bars)){

		case 1: newTrendLines = createResistanceLineWithNewLowestBarInWindow(bars.get(bars.size()-1), existingLines); 
		break;

		case 2: newTrendLines = createSupportLineWithNewHighestBarInWindow(bars.get(bars.size()-1), existingLines);
		break;

		case 3: newTrendLines = newResistanceAndSupportFromOneBar(bars.get(bars.size()-1), existingLines);
		break;

		default: newTrendLines = existingLines;
		break;
		}

		if (newTrendLines != null){
			newTrendLines = updateSetLines(bars.get(bars.size()-1), newTrendLines);
			newTrendLines = updateNonSetLines(bars, newTrendLines);
		}
		return newTrendLines;

		//		on start - 
		//		
		//			- a new trend line can only form from the highest high or the lowest low 
		//		of the prior 20 bars
		//			- set low point to lowest on the bar
		//		when the next bar is created, if it is lower, make it the new low, 
		//		if it is higher, wait until bar #10 to set slope so that the
		//		trend line runs through the low points for both bars
		//			-  The slope of the first bar must NOT cross over the lowest point for bars 2-n or 
		//		any other bars tails (i.e. If the bar that generates the trend line is bar #1 in the sequence,
		//		then the first confirmation must be at least #10 (should apply to all bar sizes.. TBD))
		// 			- Set the support / resistance indicators


		//  	For confirmation - 
		//			- For confirmation, a trend line will not become active unless 
		//			A) A major drop occurs in the next bar (i.e. some dynamic value depending on recent volatility)
		//		***** need to implement*****	B) No bar crosses above in the next 4 bars (if one does cross above, reset the line and start from #3

		//		After confirmation -
		//			- Set the weight of the bar (determine formula based off of bar width)
		//			- Change the isSet indicator
		//			- 

		//		On tries, touches and crosses
		//			- NOT MVP - determine how to re-adjust existing trendlines for best fit
		//			- Increase weight for each time a bar crosses but doesn't close above or comes 
		//		close without touching
		//			- Set trendline to inactive after 3 consecutive bars close above -
		//		** will need to use weight, support / resistance indicator, bar width, etc to determine whether
		//		or not trades should be placed after support is broken **
		//		

		//		9) For each unique combination of ticker and bar size, there can be N number of both support
		//		and resistance trend lines
		//		
		//		Determine if new trend line, set start time = tick millisecond -	
		//		if lower than low, set low point for trend line	
		//		
		//		What types of points should be kept?
		//		- The lowest point in the past 30 days? 60 days? 
		//		- High / low points for tend lines that reach high weights?
		//		
		//		
	}


	private List<TrendLine> updateSetLines(Bar bar, List<TrendLine> existingLines) {

		// If a line is set that means that the intercept and slope have been created
		// If a line is active that means that there has been a confirmation signal that means the
		//	trend line is valid and can be traded against

		// For each line that is not already set, we're going to update the # of bars since its creation
		// and then check to see if it can be confirmed. If it can be confirmed the method will set the values
		for (TrendLine line : existingLines){

			// Update the number of bars since it was created for all lines,
			// This will eventually be used for setting the weight
			if (bar != line.getBar()){
				line.updateBarsSinceCreated();

				// Check to see if the current line should be removed
				if (line.isActive() && removeActiveLine(line, bar)){
					existingLines.remove(line);
				} 
				// If the line is not active, but it has been set, see if we can make it active
				else {
					if(!line.isActive() && line.isSet()){
						line.updateBarsSinceSet();
						if(checkIfLineCanBeActive(line, bar))
							line.makeLineActive(true);
					}
				}
			}
		}
		return existingLines;
	}

	private boolean removeActiveLine(TrendLine line, Bar bar) {
		double intercept = line.getIntercept();
		long totalTime = bar.getDateTime().getTime() - line.getStartTime();
		double slope = line.getSlope();

		// If the bar's low / high has crossed over the respective resistance / support then
		// return true because we will remove the trend line
		if(line.isSupport() && bar.getLow() < intercept + (totalTime*slope)){
			return true;
		} else if (!line.isSupport() && bar.getHigh() > intercept + (totalTime*slope)) {	
			return true;
		} else{	
			return false;				
		}
	}


	private boolean checkIfLineCanBeActive(TrendLine line, Bar bar) {

		double intercept = line.getIntercept();
		long totalTime = bar.getDateTime().getTime() - line.getStartTime();
		double slope = line.getSlope();

		if(line.isSupport() && bar.getLow() < intercept + (totalTime*slope)){
			line.makeLineSet(false);
			line.updateBarsSinceSet(0);
			return false;
		} else if (!line.isSupport() && bar.getHigh() > intercept + (totalTime*slope)) {	
			line.makeLineSet(false);
			line.updateBarsSinceSet(0);
			return false;
		} else{
			line.updateBarsSinceSet();	
			if (line.getBarsSinceSet() > barsUntilActive)
				return true;
			else 
				return false;				
		}
	}

	private List<TrendLine> updateNonSetLines(List<Bar> bars, List<TrendLine> existingTrendLines) {

		Bar currBar = bars.get(bars.size()-1);

		// For each line that is not already set, we're going to update the # of bars since its creation
		// and then check to see if it can be set. If it can be set the method will set the values
		for (TrendLine line : existingTrendLines){
			if(!line.isSet() && line.getBarsSinceCreated() >= barsUntilSet && line.getBar() != currBar){
				line = checkIfLineCanBeSetAndGetSlope(line, bars, currBar);
			}
		}
		return existingTrendLines;
	}

	private TrendLine checkIfLineCanBeSetAndGetSlope(TrendLine line, List<Bar> bars, Bar currBar) {

		double slope = 0.0;
		boolean set = true;

		// Generate the slope for each line and then iterate through all of the bars to make sure that the
		// Trend line does not pass through any of them
		if (line.isSupport() && !line.isSet()){
			slope = (currBar.getLow() - line.getIntercept()) / (currBar.getDateTime().getTime() - line.getStartTime());
			for (Bar b : bars){
				//				System.out.println("If posive then set support " + (b.getLow() - (line.getIntercept() + ((b.getDateTime().getTime() - line.getStartTime()) * slope))));
				// if any bar is < intercept + time*slope then don't confirm
				if (b.getLow() < line.getIntercept() + ((b.getDateTime().getTime() - line.getStartTime()) * slope)){
					set = false;
					break;
				}
			}
		} else if (!line.isSupport() && !line.isSet()){
			slope = (currBar.getHigh() - line.getIntercept()) / (currBar.getDateTime().getTime() - line.getStartTime());
			for (Bar b : bars){
				// if any bar is < intercept + time*slope then don't confirm
				//				System.out.println("If negative then set resistance: " + (b.getHigh() - (line.getIntercept() + ((b.getDateTime().getTime() - line.getStartTime()) * slope))));
				if (b.getHigh() > line.getIntercept() + ((b.getDateTime().getTime() - line.getStartTime()) * slope)){
					set = false;
					break;
				}
			}
		}	

		if (set){
			line.setSlope(slope);
			line.makeLineSet(true);
			return line;
		}
		return line;

	}

	private List<TrendLine> newResistanceAndSupportFromOneBar(Bar bar, List<TrendLine> existingTrendLines) {

		// Look at the most recent line's start time to see if it is within the last 20 bars
		// and if it is the same type of line, in this case support. If so, remove it from the list 
		List<TrendLine> newLines = new ArrayList<TrendLine>();
		for (TrendLine line : existingTrendLines){
			if (line.isActive()){  
				newLines.add(line);
			}		
		}

		// Create a new bar and add it to the trendLines 
		TrendLine supportLine = createNewLine(bar, true, bar.getLow());
		TrendLine resistanceLine = createNewLine(bar, false, bar.getHigh());
		newLines.add(supportLine);
		newLines.add(resistanceLine);

		return newLines;
	}

	private List<TrendLine> createSupportLineWithNewHighestBarInWindow(Bar bar, List<TrendLine> existingTrendLines) {

		// Look at the most recent line's start time to see if it is within the last 20 bars
		// and if it is the same type of line, in this case support. If so, remove it from the list 
		List<TrendLine> newLines = new ArrayList<TrendLine>();
		for (TrendLine line : existingTrendLines){
			if (line.isActive() || !line.isSupport()){  
				newLines.add(line);
			}		
		}

		// Create a new bar and add it to the trendLines 
		TrendLine trendLine = createNewLine(bar, true, bar.getLow());
		newLines.add(trendLine);

		return newLines;
	}

	private List<TrendLine> createResistanceLineWithNewLowestBarInWindow(Bar bar, List<TrendLine> existingTrendLines) {

		// Look at the most recent line's start time to see if it is within the last 20 bars
		// and if it is the same type of line, in this case resistance. If so, remove it from the list 
		List<TrendLine> newLines = new ArrayList<TrendLine>();
		for (TrendLine line : existingTrendLines){
			if (line.isActive() || line.isSupport()){  
				newLines.add(line);
			}		
		}

		// Create a new bar and add it to the trendLines 
		TrendLine trendLine = createNewLine(bar, false, bar.getHigh());
		newLines.add(trendLine);

		return newLines;
	}

	private TrendLine createNewLine(Bar bar, boolean isSupport, double intercept) {

		TrendLineBuilder trendLine = new TrendLineBuilder();
		trendLine.setIsSupport(isSupport);
		trendLine.setIntercept(intercept);
		trendLine.setStartTime(bar.getDateTime().getTime());
		trendLine.setDateTime(bar.getDateTime());
		trendLine.setBarSize(bar.getBarSize());
		trendLine.setCalculated(false);
		trendLine.setCalculatedOnDateTime(new Date());
		trendLine.setIsSet(false);
		trendLine.setSecurityKey(bar.getSecurity().getKey());
		trendLine.setBar(bar);

		return trendLine.build();
	}


	// Returns 1 if the bar is the highest in the recent window
	// Returns 2 if the bar is the lowest in the recent window
	// Returns 0 if the bar is neither the highest or the lowest
	private int isNewHighOrLowInRecentWindow(List<Bar> bars) {

		boolean isHighest = true;
		boolean isLowest = true;
		Bar currBar = bars.get(bars.size()-1);

		for (Bar b : bars){
			if(b.getHigh() > currBar.getHigh() && isHighest && b != currBar)
				isHighest = false;
			if(b.getLow() < currBar.getLow() && isLowest && b != currBar)
				isLowest = false;
		}

		if(isHighest && isLowest)
			return 3;
		else if(isHighest)
			return 1;
		else if (isLowest)
			return 2;
		else 
			return 0;

	}


	@SuppressWarnings("unchecked")
	@Override
	protected List<? extends Variable> convertExistingVariablesFromDBObject(List<DBObject> existingValues, Variable parameters){

		double value = 0.0;
		double intercept = 0.0;
		double slope  = 0.0;
		double weight  = 0.0;
		int barsSinceCreated = 0;
		int barsSinceSet = 0;
		int period = 0;
		long startTime = 0;
		boolean isSupport = false;
		boolean isSet = false;
		boolean activeStatus = false;
		boolean hasParallel = false;
		boolean isCalculated = false;
		BarSize barSize = null;
		Bar bar = null;
		SecurityKey securityKey = null;
		Date dateTime = null;
		Date calculatedOnDateTime = null;

		List<TrendLine> returnList = new ArrayList<TrendLine>();

		for (DBObject dbObject : existingValues){
			if(dbObject.get("value") instanceof Double)
				value = (double) dbObject.get("value");
			if(dbObject.get("intercept") instanceof Double)
				intercept = (double) dbObject.get("intercept");
			if(dbObject.get("slope") instanceof Double)
				slope = (double) dbObject.get("slope");
			if(dbObject.get("weight") instanceof Double)
				weight = (double) dbObject.get("weight");
			if(dbObject.get("barsSinceCreated") instanceof Integer)
				barsSinceCreated = (Integer) dbObject.get("barsSinceCreated");
			if(dbObject.get("barsSinceSet") instanceof Integer)
				barsSinceSet = (Integer) dbObject.get("barsSinceSet");
			if(dbObject.get("period") instanceof Integer)
				period = (Integer) dbObject.get("period");
			if(dbObject.get("startTime") instanceof Long)
				startTime = (Long) dbObject.get("startTime");
			if(dbObject.get("isSet") != null)
				isSet = (Boolean) dbObject.get("isSet");
			if(dbObject.get("isSupport") != null)
				isSupport = (Boolean) dbObject.get("isSupport");
			if(dbObject.get("activeStatus") != null)
				activeStatus = (Boolean) dbObject.get("activeStatus");
			if(dbObject.get("hasParallel") != null)
				hasParallel = (Boolean) dbObject.get("hasParallel");
			if(DBObjectConverter.convertFromObjectToBar((DBObject) dbObject.get("bar")) instanceof Bar) // DBObject object = (DBObject) origObject.get("bar");
				bar = DBObjectConverter.convertFromObjectToBar((DBObject) dbObject.get("bar"));
			if(dbObject.get("barSize") instanceof BarSize)
				barSize = BarSize._1_MIN;  //BarSize doesnt matter, the hash is set from the paramater's barSize
			if(dbObject.get("dateTime") instanceof Date)
				dateTime = (Date) dbObject.get("dateTime");
			if(dbObject.get("calculatedOnDateTime") instanceof Date)
				calculatedOnDateTime = (Date) dbObject.get("calculatedOnDateTime");

			TrendLineBuilder newTrendLine = new TrendLineBuilder();
			newTrendLine.setValue(value);
			newTrendLine.setIntercept(intercept);//
			newTrendLine.setSlope(slope);//
			newTrendLine.setWeight(weight);//
			newTrendLine.setBarsSinceCreated(barsSinceCreated);//
			newTrendLine.setBarsSinceSet(barsSinceSet);//
			newTrendLine.setPeriod(period);
			newTrendLine.setStartTime(startTime);//
			newTrendLine.makeLineSet(isSet);//
			newTrendLine.setSupport(isSupport);//
			newTrendLine.makeLineActive(activeStatus);//
			newTrendLine.setHasParallel(hasParallel);
			newTrendLine.setBar(bar);//
			newTrendLine.setSecurityKey(parameters.getSecurityKey());//
			newTrendLine.setBarSize(parameters.getBarSize());//
			newTrendLine.setDateTime(dateTime);//
			newTrendLine.setCalculatedOnDateTime(calculatedOnDateTime);//
			
			returnList.add(newTrendLine.build());
		}
		return returnList;
	}



}
