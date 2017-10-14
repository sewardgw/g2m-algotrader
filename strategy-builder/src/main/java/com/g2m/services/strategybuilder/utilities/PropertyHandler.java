package com.g2m.services.strategybuilder.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.springframework.stereotype.Component;

import com.g2m.services.strategybuilder.Strategy;

@Component
public class PropertyHandler {

	private String propertyFileOutputLocation;
	private String propertyFileInitialLocation;
	private String stratName;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd'-'HH.mm.ss");
	private Properties currentProps;

	public PropertyHandler() {

	}

	public Properties getProperties(String propsLocation){
		File file = getPropertyFileLocation(propsLocation);
		
		Properties props = new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream(file.getAbsolutePath());

			// load a properties file
			props.load(input);

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return props;
	}

	public Properties getInitialProperties(){
		return getProperties(propertyFileInitialLocation);
	}

	public Properties getInitialProperties(String location){
		return getProperties(location);
	}

	public Properties getCurrentProperties(){
		if(propertyFileOutputLocation == null)
			return getInitialProperties();
		else 
			return getProperties(propertyFileOutputLocation);
	}

	private File getPropertyFileInitialLocation(){
		propertyFileInitialLocation = ClassLoader.getSystemResource(stratName + ".props").getPath();
		return getPropertyFileLocation(propertyFileInitialLocation);		
	}

	private File getPropertyFileLocation(String propsLocation){
		File file = new File(propsLocation);
		return file;
	}

	private File getPropertyFileOutputLocation(){
		return getPropertyFileOutputLocation(propertyFileOutputLocation);		
	}

	private File getPropertyFileOutputLocation(String propsLocation){
		File file = new File(propsLocation);
		return file;
	}

	public void setProperties(Properties props){
		File file = getPropertyFileOutputLocation();
		OutputStream output = null;

		try {
			output = new FileOutputStream(file, false);
			// save properties to project root folder
			// If the strategy doesn't have any properties then don't do this step
			if(props != null)
				props.store(output, null);

		} catch (IOException io) {
			io.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}

	public void setPropertyFileOutputLocation(String location){
		propertyFileOutputLocation = location + "props";
		setProperties(currentProps);
	}

	public void writeStrategyPropertiesToFile(Strategy strat){
		writeStrategyPropertiesToFile(strat, propertyFileOutputLocation);
	}

	public void writeStrategyPropertiesToFile(Strategy strat, String propsFileLocation){
		setPropertyFileOutputLocation(propsFileLocation);
		Properties props = new Properties();
		for (Field f : strat.getClass().getDeclaredFields()){
			String fieldName = f.getName();
			String methodName = Character.toString(fieldName.charAt(0)).toUpperCase()+fieldName.substring(1);
			if (f.getType().equals(boolean.class))
				methodName = "is" + methodName;
			else 
				methodName = "get" + methodName;
			Object result = invokeMethod(methodName, strat.getClass(), null, f);

			// If there is a result, add the field properties to the properties file;
			if (result != null){
				String castedProp = "";
				if(f.getType().equals(boolean.class))
					castedProp = Boolean.toString((boolean) result);
				else if(f.getType().equals(double.class))
					castedProp = Double.toString((double) result);
				else if(f.getType().equals(int.class))
					castedProp = Integer.toString((int) result);
				else if(f.getType().equals(long.class))
					castedProp = Long.toString((long) result);
				else if(f.getType().equals(String.class))
					castedProp = (String) result;
				else
					System.out.println(fieldName 
							+ " NOT ADDED TO PROPERTIES, HAS VALUE " 
							+ result.toString() 
							+ " AND CLASS "
							+ result.getClass()
							+ " BUT NEEDS "
							+ f.getType());
				if(!castedProp.equals(""))
					props.put(fieldName, castedProp);
			}
		}
		currentProps = props;
		setProperties(currentProps);
	}

	public void setStrategyPropertiesFromFile(Strategy strat){
		stratName = strat.getClass().getSimpleName();
		setStrategyPropertiesFromFile(strat, getPropertyFileInitialLocation().getPath());
	}

	public void setStrategyPropertiesFromFile(Strategy strat, String propsFileLocation){
		//setPropertyFileLocation(propsFileLocation);
		if (stratName == null)
			stratName = strat.getClass().getSimpleName();
		currentProps = getProperties(propsFileLocation);
		
		for (Field f : strat.getClass().getDeclaredFields()){
			if (currentProps.containsKey(f.getName())){
				String fieldName = f.getName();
				String methodName = Character.toString(fieldName.charAt(0)).toUpperCase()+fieldName.substring(1);
				methodName = "set" + methodName;
				Object result = invokeMethod(methodName, strat.getClass(), currentProps.get(fieldName), f);
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object invokeMethod(String methodName, Class c, Object arguments, Field f){

		try{
			Object obj = c.newInstance();
			Object result = null; 

			Method m = null;
			if(arguments == null){
				m = c.getMethod(methodName, null);
				result =  m.invoke(obj, null); 
			}
			else {
				m = c.getMethod(methodName, f.getType());

				if(f.getType().equals(boolean.class))
					result = m.invoke(obj, Boolean.parseBoolean((String) arguments));
				else if(f.getType().equals(double.class))
					result = m.invoke(obj, Double.parseDouble((String) arguments));
				else if(f.getType().equals(int.class))
					result = m.invoke(obj, Integer.parseInt((String) arguments));
				else if(f.getType().equals(long.class))
					result = m.invoke(obj, Long.parseLong((String) arguments));
				else if(f.getType().equals(String.class))
					result = m.invoke(obj, (String) arguments);
			}

			return result;
		} catch (NoSuchMethodException nsme) {
			nsme.printStackTrace();
		} catch (InstantiationException e1) {
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} 
		return null;
	}

	public void setStrategyName(String stratName){
		this.stratName = stratName;
	}

}
