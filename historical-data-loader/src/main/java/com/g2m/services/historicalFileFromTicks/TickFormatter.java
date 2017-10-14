package com.g2m.services.historicalFileFromTicks;

import java.util.concurrent.Exchanger;

import org.joda.time.DateTime;

public class TickFormatter {

	long dateTime;
	double lastPrice;
	long lastDateTime;
	int volumeBid;
	int volumeAsk;
	double bidPrice;
	double askPrice;
	int tradeVolume;
	int openInterest;
	double settlement;
	String symbol;
	String currency;
	String securityType; 
	String exchange; 
	String expiry; 
	String formattedTick;
	String tickKey;

	public TickFormatter(String s) {
		setTickValues(s);
	}

	public String getSymbol(){
		return symbol;
	}

	public String getCurrency(){
		return currency;
	}

	public String getSecurityType(){
		return securityType;
	}

	public String getExpiry(){
		return expiry;
	}

	public String getExchange(){
		return expiry;
	}

	public String getFormattedTick(){
		return formattedTick;
	}

	public String getTickKey(){
		return tickKey;
	}

	private void setTickValues(String s) {
		
		String [] splitString = s.split(",");
		
		try{

			dateTime = new DateTime(splitString[0]).getMillis();

			if(Double.valueOf(splitString[1]) < 0)
				lastPrice = 0.0;
			else
				lastPrice = Double.valueOf(splitString[1]);

			lastDateTime =  new DateTime(splitString[2]).getMillis();

			if(Integer.valueOf(splitString[3]) < 0)
				volumeBid = 0;
			else
				volumeBid = Integer.valueOf(splitString[3]);

			if(Integer.valueOf(splitString[4]) < 0)
				volumeAsk = 0;
			else
				volumeAsk = Integer.valueOf(splitString[4]);

			if (Double.valueOf(splitString[5]) < 0)
				bidPrice = 0.0;
			else
				bidPrice = Double.valueOf(splitString[5]);

			if (Double.valueOf(splitString[6]) < 0)
				askPrice = 0.0;
			else
				askPrice = Double.valueOf(splitString[6]);

			if (Integer.valueOf(splitString[7]) < 0)
				tradeVolume = 0;
			else
				tradeVolume = Integer.valueOf(splitString[7]);

			if (Integer.valueOf(splitString[8]) < 0)
				openInterest = 0;
			else 
				openInterest = Integer.valueOf(splitString[8]);

			if (Double.valueOf(splitString[9]) < 0)
				settlement = 0.0;
			else
				settlement = Double.valueOf(splitString[9]);

			symbol = splitString[10];
			currency = splitString[11];
			securityType = splitString[12];
			exchange = splitString[13];
			if(splitString.length > 14 && !splitString[14].equals('"'+'"') && splitString[14].length() > 3)
				expiry = splitString[14];
			else
				expiry = "";


			formattedTick = createFormattedTick();
			tickKey = createTickKey();
		} catch (Exception e){
			System.out.println(s);
			for(int i = 0; i < splitString.length; i++)
				System.out.println(splitString[i]);
			System.out.println(e);
		}
	}


	private String createTickKey() {
		StringBuilder sb = new StringBuilder();
		sb.append(symbol);
		sb.append("-");
		sb.append(expiry);
		sb.append("-");
		sb.append(securityType);
		sb.append("-");
		sb.append(currency);
		sb.append("-");
		sb.append(exchange);
		return sb.toString();
	}

	public String createFormattedTick(){
		StringBuilder sb = new StringBuilder();
		sb.append(dateTime);
		sb.append(",");
		sb.append(lastPrice);
		sb.append(",");
		sb.append(lastDateTime);
		sb.append(",");
		sb.append(volumeBid);
		sb.append(",");
		sb.append(volumeAsk);
		sb.append(",");
		sb.append(bidPrice);
		sb.append(",");
		sb.append(askPrice);
		sb.append(",");
		sb.append(tradeVolume);
		sb.append(",");
		sb.append(openInterest);
		sb.append(",");
		sb.append(settlement);
		sb.append(System.lineSeparator());
		return sb.toString();
	}
}
