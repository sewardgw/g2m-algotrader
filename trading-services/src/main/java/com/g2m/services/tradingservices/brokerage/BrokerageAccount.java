package com.g2m.services.tradingservices.brokerage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.Account;
import com.g2m.services.tradingservices.SecurityRegistry;
import com.g2m.services.tradingservices.analytics.Analytics;
import com.g2m.services.tradingservices.brokerage.mappers.BrokeragePositionMapper;
import com.g2m.services.tradingservices.brokerage.mappers.BrokerageSecurityMapper;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.persistence.PositionPersistThread;
import com.ib.controller.ApiController.IAccountHandler;
import com.ib.controller.Position;

/**
 * Added 3/18/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class BrokerageAccount extends Account {
	@Autowired
	private BrokerageConnection connection;
	@Autowired
	private Analytics analytics;
	private static PositionPersistThread positionPersistThread;
	private Map<String, Map<String, String>> accountValues;
	private String accountCode;
	private String lastUpdated;
	private AccountHandler accountHandler;
	private static boolean persist = false;
	

	public BrokerageAccount() {
		super();
		accountValues = new HashMap<String, Map<String, String>>();
	}
	
	public static void startPositionPersistThread(){
		positionPersistThread = new PositionPersistThread();
		//positionPersistThread.start();
		//setEntityPersistMode(true);
	}

	public void subscribeToUpdates() {
		accountCode = connection.getAccountCodes().get(0);
		accountHandler = new AccountHandler();
		connection.getApiController().reqAccountUpdates(true, getAccountCode(), accountHandler);
	}

	public String getAccountCode() {
		return accountCode;
	}

	public String getLastUpdated() {
		return lastUpdated;
	}

	public static void setEntityPersistMode(boolean persistVal){
		persist = persistVal;
	}
	
	public static void stopPersistThread() {
		positionPersistThread.stopRunning();
		
	}
	
	protected class AccountHandler implements IAccountHandler {
		@Override
		public void accountValue(String account, String key, String value, String currency) {
			if (BrokerageAccount.this.getAccountCode().equalsIgnoreCase(account)) {
				if (!BrokerageAccount.this.accountValues.containsKey(key)) {
					BrokerageAccount.this.accountValues.put(key, new HashMap<String, String>());
				}

				BrokerageAccount.this.accountValues.get(key).put(currency, value);

				if ("AccruedCash".equalsIgnoreCase(key)) {
					if ("USD".equalsIgnoreCase(currency)) {
						BrokerageAccount.this.setAccruedCash(Double.valueOf(value));
					}
				} else if ("AvailableFunds".equalsIgnoreCase(key)) {
					if ("USD".equalsIgnoreCase(currency)) {
						BrokerageAccount.this.setAvailableFunds(Double.valueOf(value));
					}
				} else if ("BuyingPower".equalsIgnoreCase(key)) {
					if ("USD".equalsIgnoreCase(currency)) {
						BrokerageAccount.this.setBuyingPower(Double.valueOf(value));
					}
				} else if ("CashBalance".equalsIgnoreCase(key)) {
					if ("USD".equalsIgnoreCase(currency)) {
						BrokerageAccount.this.setCashBalance(Double.valueOf(value));
					}
				} else if ("DayTradesRemaining".equalsIgnoreCase(key)) {
					BrokerageAccount.this.setDayTradesRemaining(Integer.valueOf(value));
				} else if ("ExcessLiquidity".equalsIgnoreCase(key)) {
					if ("USD".equalsIgnoreCase(currency)) {
						BrokerageAccount.this.setExcessLiquidity(Double.valueOf(value));
					}
				} else if ("FullAvailableFunds".equalsIgnoreCase(key)) {
					if ("USD".equalsIgnoreCase(currency)) {
						BrokerageAccount.this.setFullAvailableFunds(Double.valueOf(value));
					}
				} else if ("FullInitMarginReq".equalsIgnoreCase(key)) {
					if ("USD".equalsIgnoreCase(currency)) {
						BrokerageAccount.this.setFullInitialMarginRequirement(Double.valueOf(value));
					}
				} else if ("FullMaintMarginReq".equalsIgnoreCase(key)) {
					if ("USD".equalsIgnoreCase(currency)) {
						BrokerageAccount.this.setFullMaintenanceMarginRequirement(Double.valueOf(value));
					}
				} else if ("InitMarginReq".equalsIgnoreCase(key)) {
					if ("USD".equalsIgnoreCase(currency)) {
						BrokerageAccount.this.setInitialMarginRequirement(Double.valueOf(value));
					}
				} else if ("RegTMargin".equalsIgnoreCase(key)) {
					if ("USD".equalsIgnoreCase(currency)) {
						BrokerageAccount.this.setRegulationTMargin(Double.valueOf(value));
					}
				} else if ("UnrealizedPnL".equalsIgnoreCase(key)) {
					if ("USD".equalsIgnoreCase(currency)) {
						BrokerageAccount.this.setUnrealizedProfitAndLoss(Double.valueOf(value));
					}
				} else if ("EquityWithLoanValue".equalsIgnoreCase(key)) {
					if ("USD".equalsIgnoreCase(currency)) {
						BrokerageAccount.this.setEquityWithLoanValue(Double.valueOf(value));
					}
				} else if ("SMA".equalsIgnoreCase(key)) {
					if ("USD".equalsIgnoreCase(currency)) {
						BrokerageAccount.this.setSma(Double.valueOf(value));
					}
				}
			}
		}

		@Override
		public void accountTime(String timeStamp) {
			BrokerageAccount.this.lastUpdated = timeStamp;
		}

		@Override
		public void accountDownloadEnd(String account) {
			BrokerageAccount.this.setAccountInitialized(true);
		}

		@Override
		public void updatePortfolio(Position position) {
			Security mappedSecurity = BrokerageSecurityMapper.createSecurity(position.contract());
			Security registeredSecurity = SecurityRegistry.matchExisting(mappedSecurity);
			Security security = (null != registeredSecurity ? registeredSecurity : mappedSecurity);
			com.g2m.services.tradingservices.entities.Position newPosition = BrokeragePositionMapper.createPosition(position,
					security);

			// If a position has been closed in the same business day IB will still send an empty
			// position to the call back method. We do not want to add these to our system 
			// that keeps track of the positions and updates analytics based off of the closed
			// positions
			
			BrokerageAccount.this.positions.put(security.getKey(), newPosition);
			analytics.updateFromPosition(newPosition);
			
			
		}
		
	}
}