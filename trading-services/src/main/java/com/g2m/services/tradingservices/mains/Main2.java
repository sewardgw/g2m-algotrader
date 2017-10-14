package com.g2m.services.tradingservices.mains;

import java.util.Date;

import com.g2m.services.tradingservices.brokerage.BrokerageConnection;
import com.ib.controller.ApiController.ITopMktDataHandler;
import com.ib.controller.NewContract;
import com.ib.controller.NewTickType;
import com.ib.controller.Types.MktDataType;
import com.ib.controller.Types.Right;
import com.ib.controller.Types.SecType;

/**
 * @author Michael Borromeo
 */
public class Main2 implements ITopMktDataHandler {
	private int count = 0;

	public static void main(String[] args) {

		BrokerageConnection conn = new BrokerageConnection();
		conn.connect("localhost", 4001, 2);

		NewContract contract = new NewContract();
		contract.currency("USD");
		contract.exchange("GLOBEX");
		contract.expiry("201506");
		contract.secType(SecType.FUT);
		contract.symbol("ES");
		contract.right(Right.None);
		System.out.println(contract.toString());
		conn.getApiController().reqMktDataType(MktDataType.Realtime);
		conn.getApiController().reqTopMktData(contract, "", false, new Main2());
	}

	@Override
	public void tickPrice(NewTickType tickType, double price, int canAutoExecute) {
		System.out.println(count++);
		System.out.println("tickPrice");
		System.out.println("tickType: " + tickType);
		System.out.println("price: " + price);
		System.out.println("canAutoExecute: " + canAutoExecute);
		System.out.println("");
	}

	@Override
	public void tickSize(NewTickType tickType, int size) {
		System.out.println(count++);
		System.out.println("tickSize");
		System.out.println("tickType: " + tickType);
		System.out.println("size: " + size);
		System.out.println("");
	}

	@Override
	public void tickString(NewTickType tickType, String value) {
		if (NewTickType.LAST_TIMESTAMP.equals(tickType)) {
			System.out.println((new Date(1000 * Long.parseLong(value))).toString());
			System.out.println("");
		}
	}

	@Override
	public void tickSnapshotEnd() {
		System.out.println(count++);
		System.out.println("tickSnapshotEnd");
		System.out.println("");
	}

	@Override
	public void marketDataType(MktDataType marketDataType) {
		System.out.println(count++);
		System.out.println("marketDataType");
		System.out.println("MktDataType: " + marketDataType);
		System.out.println("");
	}
}
