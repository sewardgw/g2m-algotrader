package com.g2m.strategies.examples;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import com.g2m.services.strategybuilder.HistoricalBarCacheLoader;
import com.g2m.services.strategybuilder.Strategy;
import com.g2m.services.strategybuilder.StrategyComponent;
import com.g2m.services.strategybuilder.enums.EntityPersistMode;
import com.g2m.services.tradingservices.SecurityRegistry;
import com.g2m.services.tradingservices.caches.BarCache;
import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityBuilder;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.orders.MarketOrder;
import com.g2m.services.tradingservices.entities.orders.Order;
import com.g2m.services.tradingservices.entities.Tick;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.tradingservices.enums.OrderAction;
import com.g2m.services.tradingservices.enums.SecurityType;
import com.g2m.services.variables.entities.Macd;
import com.g2m.services.variables.entities.MovingAverage;
import com.g2m.services.variables.entities.TrendLine;
import com.g2m.services.variables.entities.Variable;




/**
 * Added 6/28/2015.
 * 
 * @author Grant Seward
 */
@StrategyComponent
public class EntityPersistStrategy extends Strategy {

	int tickCnt = 0;

	public static void main(String... args) {
		Strategy.initialize(EntityPersistStrategy.class);
	}

	List<MovingAverage> variableList = new ArrayList<MovingAverage>();

	@Override
	protected void run() {
		//preloadBarsIntoCache();
		List<BarSize> allBarSizes = new ArrayList<BarSize>();
        String usrHome = System.getProperty("user.home");
        String securityLocs =  usrHome + "/Github/g2m-algotrader/resources/ticks/securities/securities_full.csv";
		registerSecuritiesFromFile(allBarSizes, securityLocs);
		
		setEntityPersistMode(EntityPersistMode.ALL);
		start("localhost", 4001,0);
		//start("107.170.57.96", 4001, 0);
	}

	@Override
	protected void tickReceived(Tick tick) {
		tickCnt ++;
		//System.out.println(tick);
		if(tickCnt == 1 || tickCnt % 10000 == 0){
			StringBuilder sb = new StringBuilder();
			sb.append("----------------------------------");
			sb.append(System.lineSeparator());
			sb.append(tickCnt + " - " + tick);
			sb.append(System.lineSeparator());
			sb.append("AVAILABLE FUNDS: " + getAccount().getAvailableFunds());
			sb.append(System.lineSeparator());
			sb.append("FULL AVAILABLE FUNDS: " + getAccount().getFullAvailableFunds());
			sb.append(System.lineSeparator());
			sb.append("BUYING POWER: " + getAccount().getBuyingPower());
			sb.append(System.lineSeparator());
			sb.append("CASH BALANCE: " + getAccount().getCashBalance());
			sb.append(System.lineSeparator());
			sb.append("INITIAL MARGIN REQUIREMENT: " + getAccount().getInitialMarginRequirement());
			sb.append(System.lineSeparator());
			sb.append("FULL INITIAL MARGIN REQUIREMENT: " + getAccount().getFullInitialMarginRequirement());
			sb.append(System.lineSeparator());
			sb.append("MAINTENANCE MARGIN REQUIREMENT: " + getAccount().getFullMaintenanceMarginRequirement());
			sb.append(System.lineSeparator());
			sb.append("REG T MARGIN REQUIREMENT: " + getAccount().getRegulationTMargin());
			sb.append(System.lineSeparator());
			sb.append("SMA: " + getAccount().getSma());
			sb.append(System.lineSeparator());
			sb.append("ACCRUED CASH: " + getAccount().getAccruedCash());
			sb.append(System.lineSeparator());
			sb.append("EQUITY WITH LOAN VALUE: " + getAccount().getEquityWithLoanValue());
			sb.append(System.lineSeparator());
			sb.append("EXCESS LIQUIDITY: " + getAccount().getExcessLiquidity());
			sb.append(System.lineSeparator());
			sb.append("DAY TRADES REMAINING: " + getAccount().getDayTradesRemaining());
			sb.append(System.lineSeparator());
			
			System.out.println(sb.toString());
		}
	}


	@Override
	protected void barCreated(Bar bar) {
		
	}



}

