package com.g2m.services.tradingservices.mains;

import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import com.g2m.services.tradingservices.SecurityRegistry;
import com.g2m.services.tradingservices.brokerage.BrokerageAccount;
import com.g2m.services.tradingservices.brokerage.BrokerageConnection;
import com.g2m.services.tradingservices.brokerage.BrokerageTrader;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityBuilder;
import com.g2m.services.tradingservices.entities.orders.MarketOrder;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.entities.orders.OrderState;
import com.g2m.services.tradingservices.enums.OrderAction;
import com.g2m.services.tradingservices.enums.SecurityType;

/**
 * @author Michael Borromeo
 */
@EnableAutoConfiguration
@ComponentScan("com.g2m.services.tradingservices")
public class Main1 {
	@Autowired
	private BrokerageAccount account;
	@Autowired
	private BrokerageConnection connection;
	@Autowired
	private BrokerageTrader trader;

	public static void main(String[] args) throws InterruptedException {
		SpringApplication application = new SpringApplication(Main1.class);
		ApplicationContext context = application.run();
		Main1 main = context.getBean(Main1.class);
		main.run();
	}

	private void run() throws InterruptedException {
		connection.connect("localhost", 4001, 1);
		account.subscribeToUpdates();

		SecurityBuilder securityBuilder = new SecurityBuilder();
		securityBuilder.setSymbol("ES");
		securityBuilder.setExchange("GLOBEX");
		securityBuilder.setExpiry(2015, Calendar.JUNE);
		securityBuilder.setSecurityType(SecurityType.FUTURE);
		Security security = securityBuilder.build();
		SecurityRegistry.add(security);

		Order order = new MarketOrder(OrderAction.BUY, 1, security);

		trader.submitOrder(order);

		Thread.sleep(2000);

		List<Order> orders = trader.getOrders(security.getKey());

		for (Order o : orders) {
			System.out.println(o.getQuantity());
			System.out.println(o.getOrderAction());
			for (OrderState state : o.getOrderStates().getOrderStates()) {
				System.out.println(state.getOrderStatus());
				System.out.println(state.getLastFillPrice());
			}
		}
	}
}
