package com.g2m.services.tradingservices.brokerage.mappers;

import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityBuilder;
import com.ib.controller.NewContract;

/**
 * Added 3/15/2015.
 * 
 * TODO Cache the created contracts and positions so the same ones don't get created multiple times.
 *
 * @author Michael Borromeo
 */
public class BrokerageSecurityMapper {
	public static NewContract createContract(Security security) {
		NewContract contract = new NewContract();
		contract.symbol(security.getSymbol());
		contract.secType(BrokerageEnumMapper.getSecurityType(security.getSecurityType()));
		contract.expiry(security.getExpiry());
		contract.strike(security.getStrike());
		contract.right(BrokerageEnumMapper.getRight(security.getRight()));
		contract.multiplier(String.valueOf(security.getMultiplier()));
		contract.exchange(security.getExchange());
		contract.primaryExch(security.getListingExchange());
		contract.currency(security.getCurrency());
		contract.localSymbol(security.getLocalSymbol());
		contract.tradingClass(security.getTradingClass());
		return contract;
	}

	public static Security createSecurity(NewContract contract) {
		
		SecurityBuilder builder = new SecurityBuilder();
		builder.setCurrency(contract.currency());
		
		if (contract.expiry().length() > 0)
			builder.setExpiry(contract.expiry());
		
		builder.setListingExchange(contract.primaryExch());
		builder.setLocalSymbol(contract.localSymbol());
		
		if(contract.multiplier() != null)
			builder.setMultiplier(Double.valueOf(contract.multiplier()));
		
		builder.setExchange(contract.exchange());
		builder.setRight(BrokerageEnumMapper.getRight(contract.right()));
		builder.setSecurityType(BrokerageEnumMapper.getSecurityType(contract.secType()));
		builder.setStrike(contract.strike());
		builder.setSymbol(contract.symbol());
		builder.setTradingClass(contract.tradingClass());
		
		return builder.build();
	}
}
