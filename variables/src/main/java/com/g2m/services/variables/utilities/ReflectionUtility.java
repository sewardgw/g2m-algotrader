package com.g2m.services.variables.utilities;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.stereotype.Component;

import com.g2m.services.variables.annotations.VariableDelegates;
import com.g2m.services.variables.annotations.VariableEntity;
import com.g2m.services.variables.annotations.VariableParameter;
import com.g2m.services.variables.annotations.VariableValue;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class ReflectionUtility {
	private Map<Class<?>, List<Field>> allFieldMap;
	private Map<Class<?>, List<Field>> variableParameterFieldMap;
	private Map<Class<?>, List<Field>> variableValueFieldMap;
	private final String delegatePackage = "com.g2m.services.variables.delegates";
	private final String variableEntityPackage = "com.g2m.services.variables.entities";

	public ReflectionUtility() {
		allFieldMap = new HashMap<Class<?>, List<Field>>();
		variableParameterFieldMap = new HashMap<Class<?>, List<Field>>();
		variableValueFieldMap = new HashMap<Class<?>, List<Field>>();
	}

	/**
	 * Used when setting up entity->delegate maps in VariableService.
	 */
	public Map<Class<?>, Class<?>> createEntityDelegateClassMap() {
		Map<Class<?>, Class<?>> map = new HashMap<Class<?>, Class<?>>();
		for (Class<?> delegateClass : getVariableDelegateClasses()) {
			map.put(getEntityClassForDelegate(delegateClass), delegateClass);
		}
		return map;
	}

	public Class<?> getEntityClassForDelegate(Class<?> delegateClass) {
		return delegateClass.getAnnotation(VariableDelegates.class).value();
	}

	public Set<Class<?>> getVariableDelegateClasses() {
		Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(
				ClasspathHelper.forPackage(delegatePackage)).setScanners(new TypeAnnotationsScanner()));
		return reflections.getTypesAnnotatedWith(VariableDelegates.class);
	}

	public Set<Class<?>> getVariableEntityClasses() {
		Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(
				ClasspathHelper.forPackage(variableEntityPackage)).setScanners(new TypeAnnotationsScanner()));
		return reflections.getTypesAnnotatedWith(VariableEntity.class);
	}

	/**
	 * Used when creating the hash strings for setting up cache values.
	 */
	public List<Field> getVariableParameterFields(Class<?> type) {
		if (!variableParameterFieldMap.containsKey(type)) {
			List<Field> cacheSearchParameterFields = new LinkedList<Field>();
			for (Field field : getAllFields(type)) {
				field.setAccessible(true);
				if (field.isAnnotationPresent(VariableParameter.class)) {
					cacheSearchParameterFields.add(field);
				}
			}
			variableParameterFieldMap.put(type, cacheSearchParameterFields);
		}
		return variableParameterFieldMap.get(type);
	}

	public List<Field> getVariableValueFields(Class<?> type) {
		if (!variableValueFieldMap.containsKey(type)) {
			List<Field> variableValueFields = new LinkedList<Field>();
			for (Field field : getAllFields(type)) {
				field.setAccessible(true);
				if (field.isAnnotationPresent(VariableValue.class)) {
					variableValueFields.add(field);
				}
			}
			variableValueFieldMap.put(type, variableValueFields);
		}
		return variableValueFieldMap.get(type);
	}

	public List<Field> getAllFields(Class<?> type) {
		if (!allFieldMap.containsKey(type)) {
			allFieldMap.put(type, getAllFields(new LinkedList<Field>(), type));
		}
		return allFieldMap.get(type);
	}

	public List<Field> getAllFields(List<Field> fields, Class<?> type) {
		fields.addAll(Arrays.asList(type.getDeclaredFields()));
		if (type.getSuperclass() != null) {
			fields = getAllFields(fields, type.getSuperclass());
		}
		return fields;
	}

	public Object getQueryParameterValue(Object object) {
		if (object.getClass().isEnum()) {
			return object.toString();
		} else {
			return object;
		}
	}
}
