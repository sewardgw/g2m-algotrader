package com.g2m.services.tradingservices.brokerage;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.ib.controller.ApiConnection.ILogger;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IConnectionHandler;

/**
 * Added 3/16/2015.
 * 
 * @author Michael Borromeo
 */
@Component
public class BrokerageConnection {
	final static Logger LOGGER = Logger.getLogger(BrokerageConnection.class);
	private ApiController apiController;
	private boolean connected;
	private List<String> accountCodes;

	public BrokerageConnection() {
		accountCodes = new ArrayList<String>();
		ConnectionHandler connectionHandler = new ConnectionHandler();
		apiController = new ApiController(connectionHandler, connectionHandler, connectionHandler);
	}

	public void connect(String server, int port, int clientId) {
		apiController.connect(server, port, clientId);
		synchronized (this) {
			try {
				wait();
			} catch (Exception e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}
	}

	public void disconnect() {
		apiController.disconnect();
	}

	public ApiController getApiController() {
		return apiController;
	}

	public boolean isConnected() {
		return connected;
	}

	public List<String> getAccountCodes() {
		return accountCodes;
	}

	public class ConnectionHandler implements ILogger, IConnectionHandler {
		@Override
		public void connected() {
			synchronized (BrokerageConnection.this) {
				connected = true;
				BrokerageConnection.this.notify();
			}
		}

		@Override
		public void disconnected() {
			connected = false;
		}

		@Override
		public void accountList(ArrayList<String> list) {
			accountCodes = new ArrayList<String>(list);
		}

		@Override
		public void error(Exception e) {
			LOGGER.debug(e.getMessage(), e);
		}

		@Override
		public void message(int id, int errorCode, String errorMsg) {
			LOGGER.debug("message" + errorMsg.toString());
		}

		@Override
		public void show(String string) {
			// intentionally blank
		}

		@Override
		public void log(String valueOf) {
			// intentionally blank
		}
	}
}