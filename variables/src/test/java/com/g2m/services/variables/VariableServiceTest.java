package com.g2m.services.variables;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.internal.WhiteboxImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.g2m.services.tradingservices.caches.BarCache;
import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityBuilder;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.tradingservices.enums.SecurityType;
import com.g2m.services.variables.caches.VariableCache;
import com.g2m.services.variables.entities.BollingerBands;
import com.g2m.services.variables.entities.Macd;
import com.g2m.services.variables.entities.MovingAverage;
import com.g2m.services.variables.entities.PivotPoints;
import com.g2m.services.variables.entities.Rsi;
import com.g2m.services.variables.entities.Variable;
import com.g2m.services.variables.utilities.ReflectionUtility;
import com.g2m.services.variables.utilities.TestContextConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Added 6/8/2015.
 * 
 * @author michaelborromeo
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestContextConfiguration.class)
public class VariableServiceTest {
	@Autowired
	private BarCache barCache;
	@Autowired
	private VariableCache variableCache;
	@Autowired
	private VariableService variableService;
	@Autowired
	private ReflectionUtility reflectionUtility;
	private Security security;
	private static final String TEST_DATA_FILE_FORMAT = "csv";

	private List<TestDataLine> createTestDataLines(Class<?> clazz) throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
		if (null != classLoader.getResource(clazz.getSimpleName() + "." + TEST_DATA_FILE_FORMAT)) {
			return readTestDataLines(clazz, classLoader.getResource(clazz.getSimpleName() + "." + TEST_DATA_FILE_FORMAT)
					.getFile());
		} else {
			return Collections.emptyList();
		}
	}

	private List<TestDataLine> readTestDataLines(Class<?> clazz, String filePath) throws Exception {
		List<TestDataLine> lines = new ArrayList<TestDataLine>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(filePath));
			String line = reader.readLine();
			Map<String, Integer> fieldIndexMap = createFieldIndexMap(clazz, line);
			Calendar dateTime = Calendar.getInstance();
			dateTime.setTime(new Date());
			while (null != (line = reader.readLine())) {
				String[] columns = line.split(",");
				BarSize barSize = BarSize.valueOf(columns[0]);
				TestDataLine testDataLine = new TestDataLine(clazz, barSize, dateTime.getTime(), columns, fieldIndexMap);
				lines.add(testDataLine);
				dateTime.add(Calendar.SECOND, barSize.getSecondsInBar() + Integer.parseInt(columns[1].trim()));
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (null != reader) {
				reader.close();
			}
		}
		return lines;
	}

	/**
	 * Line format: BarSize, Timestamp Offset, High, Low, Open, Close, [Variable Parameters],
	 * [Variable Values]
	 */
	private Map<String, Integer> createFieldIndexMap(Class<?> clazz, String line) {
		if (null == line) {
			throw new RuntimeException("Header line cannot be null.");
		}
		Map<String, Integer> fieldIndexMap = new HashMap<String, Integer>();
		String[] columnHeaders = line.split(",");
		if (7 > columnHeaders.length) {
			throw new RuntimeException("Insufficient number of column headers.");
		}
		List<Field> variableFields = reflectionUtility.getVariableParameterFields(clazz);
		variableFields.addAll(reflectionUtility.getVariableValueFields(clazz));
		for (Field field : variableFields) {
			for (int i = 6; i < columnHeaders.length; i++) {
				if (field.getName().equals(columnHeaders[i].trim())) {
					fieldIndexMap.put(field.getName(), i);
					break;
				}
			}
		}
		if (fieldIndexMap.size() != variableFields.size()) {
			throw new RuntimeException("Column headers don't match variable field parameters/values.");
		}
		return fieldIndexMap;
	}

	private class TestDataLine {
		private Bar bar;
		private Variable variable;

		public TestDataLine(Class<?> clazz, BarSize barSize, Date dateTime, String[] columns,
				Map<String, Integer> fieldIndexMap) throws Exception {
			bar = new Bar();
			WhiteboxImpl.setInternalState(bar, "security", security);
			WhiteboxImpl.setInternalState(bar, "barSize", barSize);
			WhiteboxImpl.setInternalState(bar, "dateTime", dateTime);
			WhiteboxImpl.setInternalState(bar, "high", Double.parseDouble(columns[2]));
			WhiteboxImpl.setInternalState(bar, "low", Double.parseDouble(columns[3]));
			WhiteboxImpl.setInternalState(bar, "open", Double.parseDouble(columns[4]));
			WhiteboxImpl.setInternalState(bar, "close", Double.parseDouble(columns[5]));

			if (hasVariableData(columns, fieldIndexMap)) {
				variable = (Variable) clazz.newInstance();
				WhiteboxImpl.setInternalState(variable, "securityKey", security.getKey());
				WhiteboxImpl.setInternalState(variable, "barSize", barSize);
				WhiteboxImpl.setInternalState(variable, "dateTime", dateTime);
				List<Field> variableFields = reflectionUtility.getVariableParameterFields(clazz);
				variableFields.addAll(reflectionUtility.getVariableValueFields(clazz));
				for (Field field : variableFields) {
					if (field.getType().equals(int.class)) {
						int intValue = Integer.parseInt(columns[fieldIndexMap.get(field.getName())].trim());
						WhiteboxImpl.setInternalState(variable, field.getName(), intValue);
					} else if (field.getType().equals(double.class)) {
						double doubleValue = Double.parseDouble(columns[fieldIndexMap.get(field.getName())].trim());
						WhiteboxImpl.setInternalState(variable, field.getName(), doubleValue);
					}
				}
			} else {
				variable = (Variable) clazz.newInstance();
			}
		}

		private boolean hasVariableData(String[] columns, Map<String, Integer> fieldIndexMap) {
			return (columns.length - 6 - fieldIndexMap.size() >= 0);
		}

		public Bar getBar() {
			return bar;
		}

		public Variable getVariable() {
			return variable;
		}
	}

	@Before
	public void setup() {
		barCache.clear();
		variableCache.clear();
		SecurityBuilder securityBuilder = new SecurityBuilder();
		securityBuilder.setSymbol("ES");
		securityBuilder.setExchange("GLOBEX");
		securityBuilder.setExpiry(2015, Calendar.JUNE);
		securityBuilder.setSecurityType(SecurityType.FUTURE);
		security = securityBuilder.build();
	}

	@Test
	public void bollingerBandsTest() throws Exception {
		List<TestDataLine> testDataLines = createTestDataLines(BollingerBands.class);
		for (TestDataLine line : testDataLines) {
			barCache.save(line.getBar());
			if (!line.getVariable().isEmpty()) {
				BollingerBands expected = (BollingerBands) line.getVariable();
				BollingerBands parameters = BollingerBands.createParameters(security.getKey(), expected.getBarSize(),
						expected.getPeriod(), expected.getDeviationsUp(), expected.getDeviationsDown());
				BollingerBands actual = variableService.get(parameters, expected.getDateTime());
				assertFalse(actual.isEmpty());
				assertVariableEquals(expected, actual);
			}
		}
	}

	@Test
	public void macdTest() throws Exception {
		List<TestDataLine> testDataLines = createTestDataLines(Macd.class);
		for (TestDataLine line : testDataLines) {
			barCache.save(line.getBar());
			if (!line.getVariable().isEmpty()) {
				Macd expected = (Macd) line.getVariable();
				Macd parameters = Macd.createParameters(security.getKey(), expected.getBarSize(), expected.getSlowPeriod(),
						expected.getFastPeriod(), expected.getSignalPeriod());
				Macd actual = variableService.get(parameters, expected.getDateTime());
				assertFalse(actual.isEmpty());
				assertVariableEquals(expected, actual);
			}
		}
	}

	@Test
	public void movingAverageTest() throws Exception {
		List<TestDataLine> testDataLines = createTestDataLines(MovingAverage.class);
		for (TestDataLine line : testDataLines) {
			barCache.save(line.getBar());
			if (!line.getVariable().isEmpty()) {
				MovingAverage expected = (MovingAverage) line.getVariable();
				MovingAverage parameters = MovingAverage.createParameters(security.getKey(), expected.getBarSize(),
						expected.getPeriod());
				MovingAverage actual = variableService.get(parameters, expected.getDateTime());
				assertFalse(actual.isEmpty());
				assertVariableEquals(expected, actual);
			}
		}
	}

	@Test
	public void pivotPointsTest() throws Exception {
		List<TestDataLine> testDataLines = createTestDataLines(PivotPoints.class);
		for (TestDataLine line : testDataLines) {
			barCache.save(line.getBar());
			if (!line.getVariable().isEmpty()) {
				PivotPoints expected = (PivotPoints) line.getVariable();
				PivotPoints parameters = PivotPoints.createParameters(security.getKey());
				PivotPoints actual = variableService.get(parameters, expected.getDateTime());
				assertFalse(actual.isEmpty());
				assertVariableEquals(expected, actual);
			}
		}
	}

	@Test
	public void rsiTest() throws Exception {
		List<TestDataLine> testDataLines = createTestDataLines(Rsi.class);
		for (TestDataLine line : testDataLines) {
			barCache.save(line.getBar());
			if (!line.getVariable().isEmpty()) {
				Rsi expected = (Rsi) line.getVariable();
				Rsi parameters = Rsi.createParameters(security.getKey(), expected.getBarSize(), expected.getPeriod());
				Rsi actual = variableService.get(parameters, expected.getDateTime());
				assertFalse(actual.isEmpty());
				assertVariableEquals(expected, actual);
			}
		}
	}

	private void assertVariableEquals(Variable expected, Variable actual) {
		List<Field> valueFields = reflectionUtility.getVariableValueFields(expected.getClass());
		for (Field field : valueFields) {
			if (field.getType().equals(int.class)) {
				int expectedValue = WhiteboxImpl.getInternalState(expected, field.getName());
				int actualValue = WhiteboxImpl.getInternalState(actual, field.getName());
				assertEquals(expectedValue, actualValue);
			} else if (field.getType().equals(double.class)) {
				double expectedValue = WhiteboxImpl.getInternalState(expected, field.getName());
				double actualValue = WhiteboxImpl.getInternalState(actual, field.getName());
				assertEquals(expectedValue, actualValue, 0.01);
			} else {
				Assert.fail("Unknown VariableValue field type: " + field.getType());
			}
		}
	}
}
