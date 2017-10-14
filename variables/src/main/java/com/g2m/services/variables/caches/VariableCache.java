package com.g2m.services.variables.caches;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.shared.utilities.DateTimeUtility;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.variables.entities.Variable;
import com.g2m.services.variables.utilities.ReflectionUtility;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class VariableCache {
	final static Logger LOGGER = Logger.getLogger(VariableCache.class);
	@Autowired
	private ReflectionUtility reflectionUtility;
	private Map<String, VariableGroup> variableGroupMap;
	private int variableGroupMaxSize = 1000;

	public VariableCache() {
		variableGroupMap = new HashMap<String, VariableGroup>();
	}

	public int getVariableGroupCount(Variable variableParameters, int count) {
		String variableHash = createVariableHash(variableParameters, count);
		if (!variableGroupMap.containsKey(variableHash)) {
			return 0;
		}
		return variableGroupMap.get(variableHash).getVariableValueCount();
	}

	public List<? extends Variable> getValues(Variable variableParameters, Date beforeDateTime, int count) throws Exception {
		String variableHash = createVariableHash(variableParameters, count);
		if (!variableGroupMap.containsKey(variableHash)) {
			return Collections.emptyList();
		}
		return variableGroupMap.get(variableHash).getValues(variableParameters, beforeDateTime);
	}
	
	public List<? extends Variable> getValuesUnknownListLength(Variable variableParameters, Date beforeDateTime, boolean requestFromVariableClass) throws Exception {
		String variableHash = createVariableHashUnknownListLength(variableParameters);
		if (!variableGroupMap.containsKey(variableHash)) {
			return Collections.emptyList();
		}
		return variableGroupMap.get(variableHash).getUnexpireableValues(variableParameters, beforeDateTime, requestFromVariableClass);
	}

	public void save(List<? extends Variable> values, Variable variableParameters, Date beforeDateTime) throws Exception {
		String variableHash = createVariableHash(variableParameters, values.size());
		if (!variableGroupMap.containsKey(variableHash)) {
			variableGroupMap.put(variableHash, new VariableGroup());
		}
		variableGroupMap.get(variableHash).save(values, variableParameters, beforeDateTime);
	}
	
	// Allows saving a list that doesn't have a known size for the number of variables in the list
	public void saveUnknownListLength(List<? extends Variable> values, Variable variableParameters, Date beforeDateTime) throws Exception {
		String variableHash = createVariableHashUnknownListLength(variableParameters);
		if (!variableGroupMap.containsKey(variableHash)) {
			variableGroupMap.put(variableHash, new VariableGroup());
		}
		variableGroupMap.get(variableHash).save(values, variableParameters, beforeDateTime);
	}
	
	// Added ability for VariableCache to remove a specific list from the hash, 
	// this is needed because the SupportResistance class can only have one valid list at a time, 
	//unlike MovingAverage or RSI where the prior list of values could still be used to calculate rate of change
	public void removeList(int count, Variable variableParameters) throws Exception {
		String variableHash = createVariableHash(variableParameters, count);
		if (variableGroupMap.containsKey(variableHash)) {
			variableGroupMap.remove(variableHash);
		}	
	}

	public void clear() {
		variableGroupMap.clear();
	}

	public int getVariableGroupMaxSize() {
		return variableGroupMaxSize;
	}

	public void setVariableGroupMaxSize(int variableGroupMaxSize) {
		this.variableGroupMaxSize = variableGroupMaxSize;
	}

	/**
	 * The hash combines the name=value pairs for all variable fields annotated with @CacheSearchParameter
	 */
	private String createVariableHash(Variable variableParameters, int count) {
		Class<? extends Variable> clazz = variableParameters.getClass();
		StringBuilder hash = new StringBuilder(clazz.getSimpleName()).append("&");
		for (Field field : reflectionUtility.getVariableParameterFields(clazz)) {
			try {
				hash.append(field.getName()).append("=").append(field.get(variableParameters).toString()).append("&");
			} catch (Exception e) {
				LOGGER.debug(e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}
		hash.append("count=").append(count);
		return hash.toString();
	}
	
	/**
	 * The hash combines the name=value pairs for all variable fields annotated with @CacheSearchParameter
	 */
	private String createVariableHashUnknownListLength(Variable variableParameters) {
		Class<? extends Variable> clazz = variableParameters.getClass();
		StringBuilder hash = new StringBuilder(clazz.getSimpleName()).append("&");
		for (Field field : reflectionUtility.getVariableParameterFields(clazz)) {
			try {
				hash.append(field.getName()).append("=").append(field.get(variableParameters).toString()).append("&");
			} catch (Exception e) {
				LOGGER.debug(e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}
		return hash.toString();
	}

	/**
	 * VariableValues will hold all the values for e.g.
	 * MOVING_AVERAGE,PERIOD=10,BAR_SIZE=10_MINS,COUNT=5. As calls are made to get the moving
	 * average values they are cached in valueListsByDateTimeMap. The key for
	 * valueListsByDateTimeMap is the dateTime which is used to track both the timestamp of the
	 * value list and how long the cahced value list is good for.
	 */
	private class VariableGroup {
		private Map<Date, List<? extends Variable>> valueListsByDateTimeMap;
		private Queue<Date> dateTimeQueue;
		private static final int MAX_VALUE_LISTS = 1000;

		public VariableGroup() {
			valueListsByDateTimeMap = new TreeMap<Date, List<? extends Variable>>(Collections.reverseOrder());
			dateTimeQueue = new LinkedList<Date>();
		}

		public List<? extends Variable> getValues(Variable variableParameters, Date beforeDateTime) {
			Date cachedDateTime = findNearestNonExpiredCachedDateTime(variableParameters, beforeDateTime);
			if (null == cachedDateTime) {
				return Collections.emptyList();
			}
			return valueListsByDateTimeMap.get(cachedDateTime);
		}
		
		// This is added to be able to retrieve the list of values that do not expire via time 
		// These values are managed by their parent class which will create / destroy the values
		// This will support the flow as to when the parent class will be contacted
		// The callFromParent input was added to allow the parent class to call this function
		// And to return the actual values in the list but to not have this impact the normal
		// Data flow for the cache 
		public List<? extends Variable> getUnexpireableValues(Variable variableParameters, Date beforeDateTime , boolean callFromParent) {
			UnexpirableVariable uev = findNearestCachedDateTimeForNonExpirables(variableParameters, beforeDateTime);
			if (null == uev || (uev.getSendToParentFlow() && !callFromParent)) {
				return Collections.emptyList();
			} else if (uev.getSendToParentFlow() && callFromParent) 
				return valueListsByDateTimeMap.get(uev.getNearestCachedDateTime());
			else {
				return valueListsByDateTimeMap.get(uev.getNearestCachedDateTime());
			}
			
		}
		
		public int getVariableValueCount() {
			return dateTimeQueue.size();
		}

		private Date findNearestNonExpiredCachedDateTime(Variable variableParameters, Date beforeDateTime) {
			Date nearestCachedDateTime = findNearestCachedDateTime(beforeDateTime);
			if (null == nearestCachedDateTime) {
				return null;
			} else if (isCachedDateTimeExpired(variableParameters.getBarSize(), beforeDateTime, nearestCachedDateTime)) {
				return null;
			}
			return nearestCachedDateTime;
		}
		
		// Added to allow the nearest cached values to always be retrieved which will allow the parent class of a 
		// non-expirable variable to manage when an item should be removed
		private UnexpirableVariable findNearestCachedDateTimeForNonExpirables(Variable variableParameters, Date beforeDateTime) {
			Date nearestCachedDateTime = findNearestCachedDateTime(beforeDateTime);
			
			UnexpirableVariable uev = new UnexpirableVariable(nearestCachedDateTime);
			
			if (null == nearestCachedDateTime) {
				return null;
			} else if (isCachedDateTimeExpired(variableParameters.getBarSize(), beforeDateTime, nearestCachedDateTime)) {
				uev.setSendToParentVariableClass(true);
				return uev;
			}
			return uev;
		}

		private Date findNearestCachedDateTime(Date beforeDateTime) {
			for (Date date : valueListsByDateTimeMap.keySet()) {
				if (beforeDateTime.after(date) || beforeDateTime.equals(date)) {
					return date;
				}
			}
			return null;
		}

		private boolean isCachedDateTimeExpired(BarSize barSize, Date beforeDateTime, Date nearestCachedDateTime) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(nearestCachedDateTime);
			calendar.add(Calendar.SECOND, barSize.getSecondsInBar());
			Date expirationDateTime = calendar.getTime();
			return beforeDateTime.after(expirationDateTime) || beforeDateTime.equals(expirationDateTime);
		}

		public void save(List<? extends Variable> values, Variable variableParameters, Date beforeDateTime) {
			Date cacheDateTimeForValues = getCacheDateTimeForValues(values, variableParameters, beforeDateTime);
			valueListsByDateTimeMap.put(cacheDateTimeForValues, values);
			dateTimeQueue.add(cacheDateTimeForValues);
			pruneOldValues();
		}

		private Date getCacheDateTimeForValues(List<? extends Variable> values, Variable variableParameters,
				Date beforeDateTime) {
			/*
			 * This will happen when there's a gap between now (beforeDateTime) and the value
			 * timestamps, which in turns happens because the bars used to calculate variables are
			 * returned even if there is a gap between when they were created and when they were
			 * requested (e.g start of the day, after holidays). So, the variables get timestamped
			 * with the bar's timestamp which will be 'old'. The solution is to ignore the 'old'
			 * timestamp and use the now (beforeDateTime) timestamp. This will prevent the situation
			 * where the values have an 'old' timestamp and are not retrievable since the
			 * getValues() method won't know about the 'old' timestamp and will continually look for
			 * current timestamps.
			 */
			if (beforeDateTime.after(getEndOfWindowDateTimeForLastValue(values, variableParameters))) {
				if (BarSize._1_DAY == variableParameters.getBarSize()) {
					return DateTimeUtility.getMidnightEST(beforeDateTime);
				} else {
					return beforeDateTime;
				}
			}
			return values.get(values.size() - 1).getDateTime();
		}

		private Date getEndOfWindowDateTimeForLastValue(List<? extends Variable> values, Variable variableParameters) {
			Calendar lastValueDateTime = Calendar.getInstance();
			lastValueDateTime.setTime(values.get(values.size() - 1).getDateTime());
			lastValueDateTime.add(Calendar.SECOND, variableParameters.getBarSize().getSecondsInBar());
			return lastValueDateTime.getTime();
		}

		private void pruneOldValues() {
			while (MAX_VALUE_LISTS < dateTimeQueue.size()) {
				valueListsByDateTimeMap.remove(dateTimeQueue.poll());
			}
		}
		
		private class UnexpirableVariable{
			
			Boolean sendFlowToParentVariableClass = false;
			Date nearestCachedDateTime;
			
			public UnexpirableVariable(Date lastCahcedDate){
				nearestCachedDateTime = lastCahcedDate;
			}
			
			public void setNearestCachedDateTime(Date lastCahcedDate){
				nearestCachedDateTime = lastCahcedDate;
			}
			
			public void setSendToParentVariableClass(boolean sendToParent){
				sendFlowToParentVariableClass = sendToParent;
			}
			
			public boolean getSendToParentFlow(){
				return sendFlowToParentVariableClass;
			}
			
			public Date getNearestCachedDateTime(){
				return nearestCachedDateTime;
			}
		}
	}
}
