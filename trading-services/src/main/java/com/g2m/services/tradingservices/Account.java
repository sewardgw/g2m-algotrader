package com.g2m.services.tradingservices;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.g2m.services.tradingservices.brokerage.BrokerageAccount;
import com.g2m.services.tradingservices.entities.Position;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.persistence.PositionPersistThread;

/**
 * Added 3/18/2015.
 * 
 * @author Michael Borromeo
 */
public abstract class Account {
	protected double accruedCash;
	protected double availableFunds;
	protected double buyingPower;
	protected double cashBalance;
	protected int dayTradesRemaining;
	protected double excessLiquidity;
	protected double fullAvailableFunds;
	protected double fullInitialMarginRequirement;
	protected double fullMaintenanceMarginRequirement;
	protected double initialMarginRequirement;
	protected double regulationTMargin;
	protected double unrealizedProfitAndLoss;
	protected double equityWithLoanValue;
	protected double sma;
	protected Map<SecurityKey, Position> positions;
	protected boolean accountInitialized;

	public Account() {
		this.positions = new HashMap<SecurityKey, Position>();
	}

	public double getAccruedCash() {
		return accruedCash;
	}

	public void setAccruedCash(double accruedCash) {
		this.accruedCash = accruedCash;
	}

	public double getAvailableFunds() {
		return availableFunds;
	}

	public void setAvailableFunds(double availableFunds) {
		this.availableFunds = availableFunds;
	}

	public double getBuyingPower() {
		return buyingPower;
	}

	public void setBuyingPower(double buyingPower) {
		this.buyingPower = buyingPower;
	}

	public double getCashBalance() {
		return cashBalance;
	}

	public void setCashBalance(double cashBalance) {
		this.cashBalance = cashBalance;
	}

	public int getDayTradesRemaining() {
		return dayTradesRemaining;
	}

	public void setDayTradesRemaining(int dayTradesRemaining) {
		this.dayTradesRemaining = dayTradesRemaining;
	}

	public double getExcessLiquidity() {
		return excessLiquidity;
	}

	public void setExcessLiquidity(double excessLiquidity) {
		this.excessLiquidity = excessLiquidity;
	}

	public double getFullAvailableFunds() {
		return fullAvailableFunds;
	}

	public void setFullAvailableFunds(double fullAvailableFunds) {
		this.fullAvailableFunds = fullAvailableFunds;
	}

	public double getFullInitialMarginRequirement() {
		return fullInitialMarginRequirement;
	}

	public void setFullInitialMarginRequirement(double fullInitialMarginRequirement) {
		this.fullInitialMarginRequirement = fullInitialMarginRequirement;
	}

	public double getFullMaintenanceMarginRequirement() {
		return fullMaintenanceMarginRequirement;
	}

	public void setFullMaintenanceMarginRequirement(double fullMaintenanceMarginRequirement) {
		this.fullMaintenanceMarginRequirement = fullMaintenanceMarginRequirement;
	}

	public double getInitialMarginRequirement() {
		return initialMarginRequirement;
	}

	public void setInitialMarginRequirement(double initialMarginRequirement) {
		this.initialMarginRequirement = initialMarginRequirement;
	}

	public double getRegulationTMargin() {
		return regulationTMargin;
	}

	public void setRegulationTMargin(double regulationTMargin) {
		this.regulationTMargin = regulationTMargin;
	}

	public double getUnrealizedProfitAndLoss() {
		return unrealizedProfitAndLoss;
	}

	public void setUnrealizedProfitAndLoss(double unrealizedProfitAndLoss) {
		this.unrealizedProfitAndLoss = unrealizedProfitAndLoss;
	}

	public double getEquityWithLoanValue() {
		return equityWithLoanValue;
	}

	public void setEquityWithLoanValue(double equityWithLoanValue) {
		this.equityWithLoanValue = equityWithLoanValue;
	}

	public double getSma() {
		return sma;
	}

	public void setSma(double sma) {
		this.sma = sma;
	}

	public Map<SecurityKey, Position> getPositions() {
		return positions;
	}

	public void setPositions(Map<SecurityKey, Position> positions) {
		this.positions = positions;
	}

	public boolean isAccountInitialized() {
		return accountInitialized;
	}

	public void setAccountInitialized(boolean accountInitialized) {
		this.accountInitialized = accountInitialized;
	}
	
}
