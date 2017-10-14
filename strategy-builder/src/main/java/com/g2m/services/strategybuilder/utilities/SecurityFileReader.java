package com.g2m.services.strategybuilder.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.InputStreamResource;

import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityBuilder;
import com.g2m.services.tradingservices.enums.Right;
import com.g2m.services.tradingservices.enums.SecurityType;

public class SecurityFileReader {

	public SecurityFileReader() {

	}

	public static List<Security> createSecuritiesFromFileName(List<File> filePaths){
		
		List<Security> securities = new ArrayList<Security>();
		for (File f  : filePaths){
			String s = f.getAbsolutePath();
			String newFileName;
			if (s.contains("/"))
				newFileName = s.substring(s.lastIndexOf("/")+1);
			else 
				newFileName = s;
			SecurityBuilder sb = new SecurityBuilder();
			String[] fields = newFileName.split("-");
			for (String fie: fields){
				System.out.println(fie);
			}
			
			sb.setSymbol(fields[0]);
			
			if(fields[1].length() > 0)
				sb.setExpiry(fields[1]);
			
			sb.setSecurityType(getMappedSecurityType(fields[2]));
			sb.setCurrency(fields[3]);
			sb.setExchange(fields[4]);

			securities.add(sb.build());

		}
		return securities;
	}

	private static SecurityType getMappedSecurityType(String ibSecTypeString) {
		if(SecurityType.containsMappedValue(ibSecTypeString))
			return SecurityType.getMappedValue(ibSecTypeString);
		return SecurityType.NONE;
	}

	@SuppressWarnings("resource")
	public static List<Security> getSecuritiesFromFile(String fileName){

		URL url = ClassLoader.getSystemResource(fileName);
		File file;
		List<String> unParsedSecurities = new ArrayList<String>();

		try {
			file = new File(url.toURI());
			BufferedReader reader = new BufferedReader(new FileReader(file));	
			String line;
			while((line = reader.readLine()) != null)
				unParsedSecurities.add(line);

		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
			return null;
		}

		List<Security> securityList = getSecuritiesFromList(unParsedSecurities);


		return securityList;
	}

	private static List<Security> getSecuritiesFromList(List<String> unParsedSecurities) {
		int row = 0;
		int registeredCount = 0;
		List<Security> builtSecurities = new ArrayList();

		for (String str : unParsedSecurities){
			if (row > 0){

				if(!str.substring(0, 2).equals("//")){
					registeredCount ++;
					String[] rowValues = str.split(",",12);

					SecurityBuilder security = new SecurityBuilder();
					if(rowValues.length > 0 && !rowValues[0].equals(" "))
						security.setSymbol(rowValues[0]);
					if(rowValues.length > 1 && !rowValues[1].equals(" "))
						security.setExchange(rowValues[1]);
					if(rowValues.length > 2 && !rowValues[2].equals(" "))
						security.setSecurityType(SecurityType.valueOf(rowValues[2]));
					if(rowValues.length > 3 && !rowValues[3].equals(" "))	
						security.setCurrency(rowValues[3]);
					if(rowValues.length > 4 && !rowValues[4].equals(" "))
						security.setExpiry(rowValues[4]);
					if(rowValues.length > 5 && !rowValues[5].equals(" "))	
						security.setLocalSymbol(rowValues[5]);
					if(rowValues.length > 6 && !rowValues[6].equals(" "))
						security.setListingExchange(rowValues[6]);
					if(rowValues.length > 7 && !rowValues[7].equals(" "))	
						security.setMultiplier(Double.valueOf(rowValues[7]));
					if(rowValues.length > 8 && !rowValues[8].equals(" ")){
						if(rowValues[8].equals(" ") || rowValues[8].equals(""))
							security.setRight(Right.NONE);
						else
							security.setRight(Right.valueOf((rowValues[8])));
					if(rowValues.length > 9) 
						if(!rowValues[9].equals(" "))
							security.setStrike(Double.parseDouble(rowValues[9]));
						//else 
					}
						
					if(rowValues.length > 10 && !rowValues[10].equals(" "))
						security.setTradingClass(rowValues[10]);

					builtSecurities.add(security.build());
				}
			}
			row++;
		}
		System.out.println("--- " + registeredCount + " SECURITIES REGISTERED");
		return builtSecurities;
	}

}
