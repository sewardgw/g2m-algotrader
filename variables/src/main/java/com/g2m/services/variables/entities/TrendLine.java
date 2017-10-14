package com.g2m.services.variables.entities;

import java.util.Date;

import org.bson.types.ObjectId;

import com.g2m.services.tradingservices.entities.Bar;
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
public class TrendLine extends Variable {

	// The value of the support / resistance line
	@VariableValue
	private double value;

	// The value of the support / resistance line
	@VariableValue
	private int barsSinceCreated;

	// The value of the support / resistance line
	@VariableValue
	private int barsSinceSet;

	// The value of the support / resistance line
	@VariableValue
	private long startTime;

	// These are used to calculate the trend line
	@VariableValue
	private double intercept;

	@VariableValue
	private double slope;

	// This is used to weed out very short lived support and 
	// resistance lines. The value will be between 0-100
	// with 100 being the highest weight meaning that there
	// is a stronger support / resistance at the value
	@VariableValue
	private double weight;

	// This variable is true if the line is a support line,
	// it returns false if it is a resistance line
	@VariableValue
	private boolean isSupport;

	// Tells whether or not the trend has been confirmed or if the
	// computer is still trying to adjust it
	@VariableValue
	private boolean isSet;

	// This variable is whether or not the resistance line is active
	// If the trend line has been broken it is likely that the
	// band is no longer active
	@VariableValue
	private boolean activeStatus;

	// This variable says whether or not there is a parallel
	// line that is resistance or support
	@VariableValue
	private boolean hasParallel;

	// This is the original bar that created the start of the trend line
	@VariableValue
	private Bar bar;

	@VariableParameter
	private int period;


	public TrendLine() {
		variableName = "TREND_LINE";
	}

	public void setDateTime(Date date) {
		dateTime = date;
	}

	public int getPeriod() {
		return period;
	}

	public Bar getBar() {
		return bar;
	}
	
	public double getIntercept() {
		return intercept;
	}

	public int getBarsSinceCreated() {
		return barsSinceCreated;
	}

	public int getBarsSinceSet() {
		return barsSinceSet;
	}

	public double getSlope() {
		return slope;
	}

	public double getValue() {
		return value;
	}

	public long getStartTime() {
		return startTime;
	}


	public double getWeight() {
		return weight;
	}

	public boolean isSupport() {
		return isSupport;
	}

	public boolean isSet() {
		return isSet;
	}

	public boolean isActive () {
		return activeStatus;
	}

	public boolean getHasParallel () {
		return hasParallel;
	}

	public void updateBarsSinceCreated(){
		barsSinceCreated++;
	}
	
	public void updateBarsSinceSet(int newVal){

		barsSinceSet = newVal;
	}

	public void updateBarsSinceSet(){
		barsSinceSet++;
	}

	public void setSlope(double slopeVal){
		slope = slopeVal;
	}
	
	
	public void setIntercept(double newVal){
		intercept = newVal;
	}
	
	public void setWeight (double newVal){
		weight = newVal;
	}
	
	public void setHasParallel (Boolean newVal){
		hasParallel = newVal;
	}
	
	public void setBar (Bar newBar){
		bar = newBar;
	}
	
	public void setCalculatedOnDateTime(Date newVal){
		calculatedOnDateTime = newVal;
	}
	
	public void makeLineSet(boolean setValue) {
		isSet = setValue;
	}
	
	public void makeLineActive(boolean activeValue) {
		activeStatus = activeValue;
	}
	
	public static TrendLine createParameters(SecurityKey securityKey, BarSize barSize) {
		TrendLine parameters = new TrendLine();
		parameters.securityKey = securityKey;
		parameters.barSize = barSize;
		return parameters;
	}

	public double getTrendLineValue(long currentTime){
		return this.getIntercept() + ((currentTime - this.getStartTime()) *this.getSlope());
	}
	
	public static class TrendLineBuilder {
		private TrendLine trendLine;

		public TrendLineBuilder() {
			trendLine = new TrendLine();
		}
		
		public void setObjectId (ObjectId newId){
			trendLine.id = newId;
		}
		
		public void setToString (String newVal){
			trendLine.toString = newVal;
		}
		
		public void makeLineSet(boolean setValue) {
			trendLine.isSet = setValue;
		}
		
		public void makeLineActive(boolean activeValue) {
			trendLine.activeStatus = activeValue;
		}
		
		public void setIsCalculated (Boolean newVal){
			trendLine.isCalculated = newVal;
		}
		
		public void setSupport (Boolean newVal){
			trendLine.isSupport = newVal;
		}
		
		public void setValue(double newVal){
			trendLine.value = newVal;
		}
		public void setBarsSinceCreated (int newVal){
			trendLine.barsSinceCreated = newVal;
		}
		
		public void setBarsSinceSet (int newVal){
			trendLine.barsSinceSet = newVal;
		}
		
		public void setStartTime (Long newVal){
			trendLine.startTime = newVal;
		}

		public void setSecurityKey(SecurityKey securityKey) {
			trendLine.securityKey = securityKey;
		}

		public void setBarSize(BarSize barSize) {
			trendLine.barSize = barSize;
		}

		public void setCalculated(boolean isCalculated) {
			trendLine.isCalculated = isCalculated;
		}

		public void setDateTime(Date dateTime) {
			trendLine.dateTime = dateTime;
		}

		public void setCalculatedOnDateTime(Date calculatedOnDateTime) {
			trendLine.calculatedOnDateTime = calculatedOnDateTime;
		}

		public void setPeriod(int period) {
			trendLine.period = period;
		}
		
		public void setBar(Bar bar){
			trendLine.bar = bar;
		}

		public void updateBarsSinceCreated(){

			trendLine.barsSinceCreated = trendLine.barsSinceCreated++;
		}

		public void updateBarsSinceSet(){

			trendLine.barsSinceSet = trendLine.barsSinceSet++;
		}
		

		public void setStartTime(long startTime){

			trendLine.startTime= startTime;
		}

		public void setIntercept(double intercept){

			trendLine.intercept = intercept;
		}

		public void setSlope(double slope){

			trendLine.slope = slope;
		}

		public void setWeight(double weight) {
			if (weight <= 0)
				trendLine.weight = 0;
			else if (weight >= 100)
				trendLine.weight = 100;
			else
				trendLine.weight = weight;
		}

		public void setIsSupport(boolean isSupport) {
			trendLine.isSupport = isSupport;
		}

		public void setIsSet(boolean isSet) {
			trendLine.isSet = isSet;
		}

		public void setActiveStatus(boolean activeStatus) {
			trendLine.activeStatus = activeStatus;
		}

		public void setHasParallel(boolean hasParallel) {
			trendLine.hasParallel = hasParallel;
		}

		public TrendLine build() {
			TrendLine trendLine = new TrendLine();
			trendLine.barSize = this.trendLine.barSize;
			trendLine.calculatedOnDateTime = this.trendLine.calculatedOnDateTime;
			trendLine.dateTime = this.trendLine.dateTime;
			trendLine.isCalculated = this.trendLine.isCalculated;
			trendLine.securityKey = this.trendLine.securityKey;
			trendLine.startTime = this.trendLine.startTime;
			trendLine.intercept = this.trendLine.intercept;
			trendLine.slope = this.trendLine.slope;
			trendLine.activeStatus = this.trendLine.activeStatus;
			trendLine.barsSinceCreated = this.trendLine.barsSinceCreated;
			trendLine.barsSinceSet = this.trendLine.barsSinceSet;
			trendLine.isSupport = this.trendLine.isSupport;
			trendLine.isSet = this.trendLine.isSet;
			trendLine.weight = this.trendLine.weight;
			trendLine.bar = this.trendLine.bar;

			// **************** need to Update string builder w/ all of the values
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(trendLine.variableName).append(": ");
			stringBuilder.append("dateTime=").append(trendLine.dateTime.toString()).append(",");
			stringBuilder.append("barSize=").append(trendLine.barSize.toString()).append(",");
			stringBuilder.append("isSupport=").append(trendLine.isSupport).append(",");
			stringBuilder.append("isActive=").append(trendLine.activeStatus).append(",");
			stringBuilder.append("isSet=").append(trendLine.isSet).append(",");
			stringBuilder.append("intercept=").append(trendLine.intercept).append(",");
			stringBuilder.append("slope=").append(trendLine.slope).append(",");
			stringBuilder.append("startTime=").append(trendLine.startTime).append(",");
			stringBuilder.append(trendLine.securityKey.toString());
			
			trendLine.toString = stringBuilder.toString();

			return trendLine;
		}
	}
}
