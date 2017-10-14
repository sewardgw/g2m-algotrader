package com.g2m.services.variables.utilities;

import java.util.Date;

import com.g2m.services.tradingservices.entities.Bar;
import com.g2m.services.tradingservices.entities.Bar.BarBuilder;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityBuilder;
import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.tradingservices.enums.Right;
import com.g2m.services.tradingservices.enums.SecurityType;
import com.mongodb.DBObject;

public class DBObjectConverter {

	public DBObjectConverter() {
		
	}

	public static Bar convertFromObjectToBar(DBObject object){

		BarBuilder bar = new BarBuilder();

		bar.setDateTime((Date) object.get("dateTime"));
		if(object.get("high") instanceof Double)
			bar.setHigh((Double) object.get("high")); 
		if(object.get("low") instanceof Double)
			bar.setLow((Double) object.get("low"));
		if(object.get("open") instanceof Double)
			bar.setOpen((Double) object.get("open"));
		if(object.get("close") instanceof Double)
			bar.setClose((Double) object.get("close"));
		if(object.get("volume") instanceof Integer)
			bar.setVolume((Integer) object.get("volume"));
		bar.setSecurity(convertFromBarToSecurity((DBObject) object.get("security")));
		bar.setBarSize(BarSize.valueOf((String) object.get("barSize")));

		return bar.build();
	}

	public static Security convertFromBarToSecurity(DBObject object){

		SecurityBuilder security = new SecurityBuilder();

		security.setSymbol((String) object.get("symbol"));
		security.setExchange((String) object.get("exchange"));
		security.setCurrency((String) object.get("currency"));
		security.setSecurityType(SecurityType.valueOf((String) object.get("securityType")));
		security.setRight(Right.valueOf((String) object.get("right")));
		if(object.get("expiry") != null)
			security.setExpiry((String) object.get("expiry"));
		if(object.get("strike") != null)
			security.setStrike((Double) object.get("strike"));
		
		return security.build();
	}

}
