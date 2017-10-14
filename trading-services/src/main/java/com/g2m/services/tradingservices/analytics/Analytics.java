package com.g2m.services.tradingservices.analytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.g2m.services.tradingservices.Account;
import com.g2m.services.tradingservices.analytics.AggregateAnalytics.AggregateAnalyticsBuilder;
import com.g2m.services.tradingservices.analytics.PositionAnalytics.PositionAnalyticsBuilder;
import com.g2m.services.tradingservices.caches.TickCache;
import com.g2m.services.tradingservices.entities.Position;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.entities.Tick;

/**
 * Added 6/17/2015.
 * 
 * @author michaelborromeo
 */
@Component
public class Analytics {
	private Map<SecurityKey, List<PositionAnalyticsBuilder>> positionAnalyticsMap;
	private AggregateAnalyticsBuilder aggregateAnalyticsBuilder;

	public Analytics() {
		positionAnalyticsMap = new HashMap<SecurityKey, List<PositionAnalyticsBuilder>>();
		aggregateAnalyticsBuilder = new AggregateAnalyticsBuilder();
	}

	public AggregateAnalytics getAggregateAnalytics() {
		List<PositionAnalytics> positionAnalyticsList = new ArrayList<PositionAnalytics>();
		for (SecurityKey securityKey : positionAnalyticsMap.keySet()) {
			for (PositionAnalyticsBuilder builder : positionAnalyticsMap.get(securityKey)) {
				positionAnalyticsList.add(builder.build());
			}
		}
		aggregateAnalyticsBuilder.updateFromPositionAnalytics(positionAnalyticsList);
		return aggregateAnalyticsBuilder.build();
	}

	public List<PositionAnalytics> getAllPositionAnalytics(SecurityKey securityKey) {
		List<PositionAnalytics> positionAnalyticsList = new ArrayList<PositionAnalytics>();
		for (PositionAnalyticsBuilder builder : positionAnalyticsMap.get(securityKey)) {
			positionAnalyticsList.add(builder.build());
		}
		return positionAnalyticsList;
	}

	public PositionAnalytics getPositionAnalytics(SecurityKey securityKey) {
		PositionAnalyticsBuilder builder = getLatestPositionAnalyticsBuilder(securityKey);
		if (null == builder) {
			return null;
		}
		return builder.build();
	}

	private PositionAnalyticsBuilder getLatestPositionAnalyticsBuilder(SecurityKey securityKey) {
		if (!positionAnalyticsMap.containsKey(securityKey)) {
			return null;
		} else if (positionAnalyticsMap.get(securityKey).isEmpty()) {
			return null;
		}
		return positionAnalyticsMap.get(securityKey).get(positionAnalyticsMap.get(securityKey).size() - 1);
	}

	public void updateFromTick(Tick tick) {
		SecurityKey securityKey = tick.getSecurity().getKey();
		if (positionAnalyticsMap.containsKey(securityKey)) {
			getLatestPositionAnalyticsBuilder(securityKey).updateFromTick(tick);
		}
	}

	
	// if the order being filled matches the quantity of that position
	// that already exists, then create a new position analytics
	public void updateFromPosition(Position position) {
		SecurityKey securityKey = position.getSecurity().getKey();
		if (positionAnalyticsMap.containsKey(securityKey)) {
			if (isNewPositionBeingOpened(position)) {
				createPositionAnalytics(position);
			}
			getLatestPositionAnalyticsBuilder(securityKey).updateFromPosition(position);
		} else {
			createPositionAnalytics(position);
			getLatestPositionAnalyticsBuilder(securityKey).updateFromPosition(position);
		}
	}

	private boolean isNewPositionBeingOpened(Position position) {
		return 0 != position.getQuantity() && getLatestPositionAnalyticsBuilder(position.getSecurity().getKey()).isClosed();
	}

	private void createPositionAnalytics(Position position) {
		SecurityKey securityKey = position.getSecurity().getKey();
		if (!positionAnalyticsMap.containsKey(securityKey)) {
			positionAnalyticsMap.put(securityKey, new ArrayList<PositionAnalyticsBuilder>());
		}
		positionAnalyticsMap.get(securityKey).add(new PositionAnalyticsBuilder(position));
	}	
	
	public boolean positionOpen(SecurityKey securityKey){
		if(getLatestPositionAnalyticsBuilder(securityKey) == null)
			return false;
		else
			return !getLatestPositionAnalyticsBuilder(securityKey).isClosed();
		//else if(positionAnalyticsMap.get(securityKey).)
	}
}
