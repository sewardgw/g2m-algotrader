package com.g2m.services.variables.pmml;

import java.io.IOException;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OutputField;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;


public class PmmlResults {

	Map<FieldName, ?> arguments;

	PmmlEvaluator pmmlEval;
	Map<String, Double> resultProbabilities = new HashMap<String, Double>();
	int classificationTarget;

	// All fields must be provided in order that they are expected
	// From the evaluator.getActiveFields()
	public PmmlResults(PmmlEvaluator evaluator, String valsAsString) {

		pmmlEval = evaluator;

		String[] valueArray = valsAsString.split(",");
		ArrayList<String> values = new ArrayList<String>();
		for (String s : valueArray){	
			values.add(checkForNonNumericValues(s));
			//values.add(s);
		}
		pmmlResults(values);
	}

	private void pmmlResults(ArrayList<String> values){
		Map<FieldName, ?> arguments = null;
		try {
			arguments = readArguments(pmmlEval.getPmmlEvaluator(), values);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<FieldName, ?> result = pmmlEval.getPmmlEvaluator().evaluate(arguments);
		createResult(result);
	}

	private void createResult(Map<FieldName, ?> result){
		//				int line = 1;

		//		System.out.println("Writing " + result.size() + " result(s):");

		List<FieldName> predictedFields = pmmlEval.getPmmlEvaluator().getOutputFields();
		for(FieldName predictedField : predictedFields){
			//						DataField dataField = pmmlEval.getPmmlEvaluator().getDataField(predictedField);

			Object predictedValue = result.get(predictedField);

			//						System.out.println(line 
			//								+ ") displayName=" + getDisplayName(dataField, predictedField) 
			//								+ ": " + EvaluatorUtil.decode(predictedValue));

			addResultToSet(predictedField.getValue(), EvaluatorUtil.decode(predictedValue));
			//			line++;
		}
	}

	private void addResultToSet(String fieldName, Object obj) {
		String mapFieldName = fieldName.substring(fieldName.indexOf("_")+1);

		if(obj instanceof String){
			if(fieldName.equals("Predicted_target"))
				classificationTarget = Integer.valueOf((String) obj);
			else
				resultProbabilities.put(mapFieldName, Double.valueOf((String) obj));

		} else if(obj instanceof Double){
			if(fieldName.equals("Predicted_target"))
				classificationTarget = (Integer) obj;
			else 
				resultProbabilities.put(mapFieldName, (Double) obj);

		} else if(obj instanceof Integer){
			if(fieldName.equals("Predicted_target"))
				classificationTarget = (Integer) obj;
			else
				resultProbabilities.put(mapFieldName, (Double) obj);
		} 
	}

	private Map<FieldName, ?> readArguments(Evaluator evaluator, ArrayList<String> values) throws IOException {
		Map<FieldName, Object> arguments = new LinkedHashMap<FieldName, Object>();

		// Get the set of fields that will need to be evaluated, 
		// these are provided by the PMML file
		List<FieldName> activeFields = evaluator.getActiveFields();
		//		System.out.println("Reading " + activeFields.size() + " argument(s):");

		int i = 4;

		for(FieldName activeField : activeFields){
			DataField dataField = evaluator.getDataField(activeField);

			// The input into a given field is provided by the values that we originally
			// provided to the class. In this case the validation that the values are
			// provided in the correct order is completed upstream to this method
			String input = values.get(i);
			arguments.put(activeField, evaluator.prepare(activeField, input));
			i++;
		}
		return arguments;
	}

	private String checkForNonNumericValues(String s){
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i)) && !s.contains(".")) {
				return Integer.toString(0);
			}
		}
		return s;
	}

	private String getDataType(Field field){
		DataType dataType = field.getDataType();
		return dataType.name();
	}


	private String getDisplayName(Field field, FieldName fieldName){
		return fieldName.getValue();
	}

	public int getClassificationTarget(){
		return classificationTarget;
	}

	public double getPredictedProbability(int i){
		return resultProbabilities.get(String.valueOf(i));
	}


}
