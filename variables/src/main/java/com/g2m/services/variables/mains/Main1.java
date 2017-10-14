package com.g2m.services.variables.mains;

import java.lang.reflect.Field;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.g2m.services.variables.entities.BollingerBands;
import com.g2m.services.variables.entities.Macd;
import com.g2m.services.variables.entities.MovingAverage;
import com.g2m.services.variables.entities.PivotPoints;
import com.g2m.services.variables.entities.Rsi;
import com.g2m.services.variables.utilities.ReflectionUtility;

/**
 * @author Michael Borromeo
 */
@EnableAutoConfiguration
@ComponentScan("com.g2m.services")
@EnableMongoRepositories({ "com.g2m.services.variables.persistance", "com.g2m.services.tradingservices.persistance" })
public class Main1 {
	@Autowired
	private ReflectionUtility ru;

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(Main1.class);
		ApplicationContext context = application.run();
		Main1 main = context.getBean(Main1.class);
		main.run();
	}

	private void run() {
		print(BollingerBands.class);
		print(MovingAverage.class);
		print(Macd.class);
		print(PivotPoints.class);
		print(Rsi.class);
	}

	private void print(Class<?> clazz) {
		List<Field> fields = ru.getVariableParameterFields(clazz);
		fields.addAll(ru.getVariableValueFields(clazz));
		// System.out.print("barSize, offset, high, low, open, close");
		for (Field f : fields) {
			if (f.getType().equals(int.class)) {
				System.out.println(f.getName());
			} else if (f.getType().equals(double.class)) {
				System.out.println(f.getName());
			}
		}
		System.out.println("");
	}
}
