package com.g2m.services.variables.pmml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;

import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Target;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PmmlEvaluator {

	Evaluator evaluator;
	PMML pmml = null;

	public PmmlEvaluator(URL newPmmlUrl) {
		setPmmlFromUrl(newPmmlUrl);
		if(pmml != null){
			ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
			evaluator = modelEvaluatorFactory.newModelManager(pmml);
		}
	}

	private void setPmmlFromUrl(URL newPmmlUrl) {
		File pmmlFile = null;
		InputStream is = null;
		Source transformedSource;

		try {
			pmmlFile = new File(newPmmlUrl.getFile());
			is = new FileInputStream(pmmlFile);
			transformedSource = ImportFilter.apply(new InputSource(is));
			pmml = JAXBUtil.unmarshalPMML(transformedSource);
		} catch (FileNotFoundException | SAXException | JAXBException e) {
			e.printStackTrace();
		}

		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public Evaluator getPmmlEvaluator(){
		return evaluator;
	}
	
	public PMML getPmml(){
		return pmml;
	}

}
