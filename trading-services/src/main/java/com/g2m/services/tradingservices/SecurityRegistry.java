package com.g2m.services.tradingservices;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;

/**
 * Added 4/4/2015.
 * 
 * @author Michael Borromeo
 */
public class SecurityRegistry {
	private static Map<SecurityKey, Security> securities;

	static {
		securities = new HashMap<SecurityKey, Security>();
	}

	public static Security get(SecurityKey key) {
		return securities.get(key);
	}

	public static void add(Security security) {
		securities.put(security.getKey(), security);
	}

	public static void addIfNotPresent(Security security) {
		if (!contains(security)) {
			add(security);
		}
	}

	public static boolean contains(Security security) {
		return securities.containsKey(security.getKey());
	}

	public static Security matchExisting(Security originalSecurity) {
		for (Security security : securities.values()) {
			if (originalSecurity.equals(security)) {
//				System.out.println("MATCHED -- SECURITY FROM IB: " + originalSecurity.getSymbol() + "." + originalSecurity.getCurrency() + "  -  SECURITY FROM FILE " + 
//						security.getSymbol() + "." + security.getCurrency());
				return security;
			}
		}
		return null;
	}
	
	public static Set<SecurityKey> getSecurities(){
		return securities.keySet();
	}

	public static void clear() {
		securities.clear();
	}
}
