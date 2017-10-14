package com.g2m.services.variables.caches;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.g2m.services.tradingservices.caches.BarCache;
import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityBuilder;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.tradingservices.enums.SecurityType;
import com.g2m.services.variables.VariableService;
import com.g2m.services.variables.entities.MovingAverage;
import com.g2m.services.variables.testdata.BarTestData;
import com.g2m.services.variables.utilities.TestContextConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Added 5/1/2015.
 * 
 * @author Michael Borromeo
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestContextConfiguration.class)
public class VariableCacheTest {
	@Autowired
	private VariableCache variableCache;
	@Autowired
	private BarCache barCache;
	@Autowired
	private VariableService variableService;
	List<BarSize> barSizes;
	List<Security> securities;

	@Before
	public void setup() {
		clearCaches();
		setupSecuritiesAndBarSizes();
	}

	private void clearCaches() {
		variableCache.clear();
		barCache.clear();
	}

	private void setupSecuritiesAndBarSizes() {
		barSizes = new ArrayList<BarSize>();
		barSizes.add(BarSize._1_SEC);
		barSizes.add(BarSize._10_MINS);
		barSizes.add(BarSize._1_DAY);

		securities = new ArrayList<Security>();
		SecurityBuilder securityBuilder = new SecurityBuilder();
		securityBuilder.setSymbol("ES");
		securityBuilder.setExchange("GLOBEX");
		securityBuilder.setExpiry(2015, Calendar.JUNE);
		securityBuilder.setSecurityType(SecurityType.FUTURE);
		securities.add(securityBuilder.build());
		securityBuilder.setExpiry(2015, Calendar.SEPTEMBER);
		securities.add(securityBuilder.build());
		securityBuilder.setExpiry(2015, Calendar.DECEMBER);
		securities.add(securityBuilder.build());
	}

	@Test
	public void getValueExpirationTest() throws Exception {
		List<Bar> bars = BarTestData.createTestBars(securities.get(0), BarSize._5_MINS, 2);
		for (Bar bar : bars) {
			barCache.save(bar);
		}

		MovingAverage parameters = MovingAverage.createParameters(securities.get(0).getKey(), BarSize._5_MINS, 1);

		// get variable created from first bar, since period=1 the MA should equal the bar close
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(bars.get(0).getDateTime());
		MovingAverage ma = variableService.get(parameters, calendar.getTime());
		assertFalse(ma.isEmpty());
		assertEquals(bars.get(0).getClose(), ma.getMovingAverage(), 0.01);
		
		// get variable 1s before second bar is created, should return first cached variable
		calendar.setTime(bars.get(1).getDateTime());
		calendar.add(Calendar.SECOND, -1);
		ma = variableService.get(parameters, calendar.getTime());
		assertFalse(ma.isEmpty());
		assertEquals(bars.get(0).getClose(), ma.getMovingAverage(), 0.01);
		
		// get variable when second bar is created, should be created with new bar
		calendar.setTime(bars.get(1).getDateTime());
		ma = variableService.get(parameters, calendar.getTime());
		assertFalse(ma.isEmpty());
		assertEquals(bars.get(1).getClose(), ma.getMovingAverage(), 0.01);
		
		// get variable 1s after second bar is created, should be created with new bar
		calendar.setTime(bars.get(1).getDateTime());
		calendar.add(Calendar.SECOND, 1);
		ma = variableService.get(parameters, calendar.getTime());
		assertFalse(ma.isEmpty());
		assertEquals(bars.get(1).getClose(), ma.getMovingAverage(), 0.01);
	}

	@Test
	public void getValuesTest_NotEnoughBars() throws Exception {
		int period = 5;
		for (int securityIndex = 0; securityIndex < securities.size(); securityIndex++) {
			for (int barSizeIndex = 0; barSizeIndex < barSizes.size(); barSizeIndex++) {
				MovingAverage variableParameters = MovingAverage.createParameters(securities.get(securityIndex).getKey(),
						barSizes.get(barSizeIndex), period);
				List<Bar> bars = BarTestData.createTestBars(securities.get(securityIndex), barSizes.get(barSizeIndex),
						period);
				for (int j = 0; j < bars.size(); j++) {
					// there shouldn't be enough bars in barCache to create any variable values
					assertTrue(variableService.get(variableParameters, bars.get(j).getDateTime(), 1).isEmpty());
					assertTrue(variableCache.getValues(variableParameters, bars.get(j).getDateTime(), 1).isEmpty());
					barCache.save(bars.get(j));
				}
			}
		}
	}

	@Test
	public void getValuesTest() throws Exception {
		getValuesTest_MultipleValues(1);
		clearCaches();
		getValuesTest_MultipleValues(2);
		clearCaches();
		getValuesTest_MultipleValues(5);
		clearCaches();
		getValuesTest_MultipleValues(10);
		clearCaches();
		getValuesTest_MultipleValues(50);
	}

	public void getValuesTest_MultipleValues(int requestValueCount) throws Exception {
		int period = 5;
		for (int securityIndex = 0; securityIndex < securities.size(); securityIndex++) {
			for (int barSizeIndex = 0; barSizeIndex < barSizes.size(); barSizeIndex++) {
				MovingAverage variableParameters = MovingAverage.createParameters(securities.get(securityIndex).getKey(),
						barSizes.get(barSizeIndex), period);
				List<Bar> bars = BarTestData.createTestBars(securities.get(securityIndex), barSizes.get(barSizeIndex),
						period + requestValueCount - 1);
				for (int j = 0; j < bars.size(); j++) {
					barCache.save(bars.get(j));
				}

				/*
				 * Try getting just one value
				 */
				assertFalse(variableService.get(variableParameters, bars.get(bars.size() - 1).getDateTime(), 1).isEmpty());
				assertFalse(variableCache.getValues(variableParameters, bars.get(bars.size() - 1).getDateTime(), 1)
						.isEmpty());

				/*
				 * There should be enough bars in barCache to create [requestValueCount] variable
				 * values
				 */
				assertFalse(variableService.get(variableParameters, bars.get(bars.size() - 1).getDateTime(),
						requestValueCount).isEmpty());
				assertFalse(variableCache.getValues(variableParameters, bars.get(bars.size() - 1).getDateTime(),
						requestValueCount).isEmpty());

				/*
				 * Try requesting too many [requestValueCount + 1]
				 */
				assertTrue(variableService.get(variableParameters, bars.get(bars.size() - 1).getDateTime(),
						requestValueCount + 1).isEmpty());
				assertTrue(variableCache.getValues(variableParameters, bars.get(bars.size() - 1).getDateTime(),
						requestValueCount + 1).isEmpty());

				/*
				 * Check the dateTimes on the variables and make sure they line up with the
				 * dateTimes on the bars
				 */
				for (int i = 0; i < requestValueCount; i++) {
					assertEquals(
							bars.get(period + i - 1).getDateTime(),
							variableCache
									.getValues(variableParameters, bars.get(bars.size() - 1).getDateTime(),
											requestValueCount).get(i).getDateTime());
				}
			}
		}
	}

	@Test
	public void getValuesTest_StartOfDay() throws Exception {
		int period = 5;
		for (int securityIndex = 0; securityIndex < securities.size(); securityIndex++) {
			for (int barSizeIndex = 0; barSizeIndex < barSizes.size(); barSizeIndex++) {
				MovingAverage variableParameters = MovingAverage.createParameters(securities.get(securityIndex).getKey(),
						barSizes.get(barSizeIndex), period);
				List<Bar> bars = BarTestData.createTestBars(securities.get(securityIndex), barSizes.get(barSizeIndex),
						period);
				for (int j = 0; j < bars.size(); j++) {
					barCache.save(bars.get(j));
				}

				Calendar now = Calendar.getInstance();
				now.setTime(bars.get(bars.size() - 1).getDateTime());
				now.add(Calendar.HOUR, 10);

				/*
				 * Try getting just a value
				 */
				assertFalse(variableService.get(variableParameters, now.getTime(), 1).isEmpty());
				assertFalse(variableCache.getValues(variableParameters, now.getTime(), 1).isEmpty());

				/*
				 * Try getting just a value a minute after the initial request
				 */
				now.add(Calendar.MINUTE, 1);
				assertFalse(variableService.get(variableParameters, now.getTime(), 1).isEmpty());
				assertFalse(variableCache.getValues(variableParameters, now.getTime(), 1).isEmpty());

				/*
				 * Check the dateTimes on the variables and make sure they line up with the
				 * dateTimes on the bars
				 */
				if (BarSize._1_DAY.equals(barSizes.get(barSizeIndex))) {
					assertEquals(bars.get(bars.size() - 1).getDateTime(),
							variableCache.getValues(variableParameters, bars.get(bars.size() - 1).getDateTime(), 1).get(0)
									.getDateTime());
				} else {

				}
			}
		}
	}

	@Test
	public void pruneOldValuesTest() {
		int period = 5;
		for (int securityIndex = 0; securityIndex < securities.size(); securityIndex++) {
			for (int barSizeIndex = 0; barSizeIndex < barSizes.size(); barSizeIndex++) {
				MovingAverage variableParameters = MovingAverage.createParameters(securities.get(securityIndex).getKey(),
						barSizes.get(barSizeIndex), period);
				// create enough bars to be able to exceed the variableCache groupMaxSize limit
				List<Bar> bars = BarTestData.createTestBars(securities.get(securityIndex), barSizes.get(barSizeIndex),
						period + (variableCache.getVariableGroupMaxSize() * 2));
				for (int j = 0; j < bars.size(); j++) {
					barCache.save(bars.get(j));
					// this will try to create a variable value and store it in the cache if created
					variableService.get(variableParameters, bars.get(j).getDateTime(), 1);
				}

				// make sure the variable values are getting pruned
				assertEquals(variableCache.getVariableGroupMaxSize(),
						variableCache.getVariableGroupCount(variableParameters, 1));
			}
		}
	}
}
