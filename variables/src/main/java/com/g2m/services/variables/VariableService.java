package com.g2m.services.variables;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.g2m.services.variables.delegates.VariableDelegate;
import com.g2m.services.variables.entities.Variable;
import com.g2m.services.variables.utilities.ReflectionUtility;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class VariableService implements ApplicationContextAware {
	final static Logger LOGGER = Logger.getLogger(VariableService.class);
	/**
	 * Create a list of variable delegates that can be mapped by variable entities. This allows the
	 * get() function to accept any Variable and map it to the appropriate delegate to calculate the
	 * values. The delegate objects are created using the applicationContext.
	 */
	private Map<Class<?>, VariableDelegate> delegateForEntityMap;
	private ApplicationContext applicationContext;
	@Autowired
	ReflectionUtility reflectionUtility;

	@SuppressWarnings("unchecked")
	public <T extends Variable> T get(T parameters, Date currentDateTime) {
		VariableDelegate delegate = this.getVariableDelegate(parameters.getClass());
		try {
			return (T) delegate.getValues(parameters, currentDateTime);
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
			try {
				return (T) parameters.getClass().newInstance();
			} catch (Exception e1) {
				LOGGER.debug(e1.getMessage(), e1);
				return null;
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends Variable> List<T> get(T parameters, Date currentDateTime, int count) {
		VariableDelegate delegate = getVariableDelegate(parameters.getClass());
		try {
			return (List<T>) delegate.getValues(parameters, currentDateTime, count);
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
			return Collections.emptyList();
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Variable> List<T> getValuesUnknownLength(T parameters, Date currentDateTime) {
		VariableDelegate delegate = getVariableDelegate(parameters.getClass());
		try {
			return (List<T>) delegate.getValuesUnknownListLength(parameters, currentDateTime);
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@PostConstruct
	public void initialize() {
		loadDelegateForEntityMap();
	}

	private void loadDelegateForEntityMap() {
		Map<Class<?>, Class<?>> entityDelegateClassMap = reflectionUtility.createEntityDelegateClassMap();
		delegateForEntityMap = new HashMap<Class<?>, VariableDelegate>();
		for (Class<?> entityClass : entityDelegateClassMap.keySet()) {
			createDelegateForEntity(entityDelegateClassMap, entityClass);
		}
	}

	private void createDelegateForEntity(Map<Class<?>, Class<?>> entityDelegateClassMap, Class<?> entityClass) {
		try {
			VariableDelegate delegate = (VariableDelegate) applicationContext.getBean(entityDelegateClassMap
					.get(entityClass));
			delegateForEntityMap.put(entityClass, delegate);
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
		}
	}

	private VariableDelegate getVariableDelegate(Class<?> variableClass) {
		if (!delegateForEntityMap.containsKey(variableClass)) {
			throw new IllegalArgumentException("Variable class '" + variableClass.getSimpleName()
					+ "' doesn't have a delegate associated with it");
		}

		return this.delegateForEntityMap.get(variableClass);
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void getExistingVariablesFromDB(Variable parameters) {
		VariableDelegate delegate = this.getVariableDelegate(parameters.getClass());
		try {
			delegate.getExistingVariablesFromDB(parameters);
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
			try {
				parameters.getClass().newInstance();
			} catch (Exception e1) {
				LOGGER.debug(e1.getMessage(), e1);
				
			}
		}
		
	}
}
