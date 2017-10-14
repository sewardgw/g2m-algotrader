package com.g2m.services.variables.entities;

import java.util.Date;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.variables.annotations.VariableParameter;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
public abstract class Variable {
	@Id
	protected ObjectId id;
	@VariableParameter
	protected SecurityKey securityKey;
	@VariableParameter
	protected BarSize barSize;
	@Transient
	protected boolean isCalculated;
	@Transient
	protected String toString;
	protected String variableName;
	protected Date dateTime;
	protected Date calculatedOnDateTime;

	public SecurityKey getSecurityKey() {
		return securityKey;
	}

	public Date getDateTime() {
		return dateTime;
	}

	public BarSize getBarSize() {
		return barSize;
	}

	public Date getCalculatedOnDateTime() {
		return calculatedOnDateTime;
	}

	public boolean isCalculated() {
		return isCalculated;
	}

	public boolean isEmpty() {
		return (null == getSecurityKey() && null == getDateTime());
	}

	@Override
	public String toString() {
		return toString;
	}
	
	public String getVariableName() {
		return variableName;
	}
	
}
