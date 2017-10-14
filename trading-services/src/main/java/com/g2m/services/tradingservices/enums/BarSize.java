package com.g2m.services.tradingservices.enums;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

/**
 * @author Michael Borromeo
 */
public enum BarSize {
	_1_SEC(1), _5_SECS(5), _10_SECS(10), _15_SECS(15), _30_SECS(30), _1_MIN(60), _2_MINS(120), _3_MINS(180), _5_MINS(300),
	_10_MINS(600), _15_MINS(900), _20_MINS(1200), _30_MINS(1800), _1_HOUR(3600), _4_HOURS(14400), _1_DAY(86400);

	private int secondsInBar;
	private static List<BarSize> registeredBarSizes = new ArrayList<BarSize>();
	private static HashMap<BarSize, List<BarSize>> returnHash = new HashMap<BarSize, List<BarSize>>();

	BarSize(int secondsInBar) {
		this.secondsInBar = secondsInBar;
	}

	@Override
	public String toString() {
		return super.toString();
	}

	public String toFormattedString() {
		return super.toString().substring(1).replace('_', ' ');
	}

	public int getSecondsInBar() {
		return this.secondsInBar;
	}

	/*
	 * We'll only register the specific bar sizes that are needed by 
	 * a strategy so that we don't have too much unnecessary data
	 * floating around the system.
	 */
	public static List<BarSize> registerSpecificBarSize(List<BarSize> barSizes){
		if(barSizes != null)
			for (BarSize bs : barSizes){
				registeredBarSizes.add(bs);
			}
		createNextBarSizeHasmap();
		
		return getRegisteredBarSizes();
	}

	private static void createNextBarSizeHasmap() {
		HashMap<BarSize, BarSize> barSizes = new HashMap<BarSize, BarSize>();
		for(BarSize bs : registeredBarSizes){
			returnHash.put(bs, new ArrayList<BarSize>());

			for (BarSize bs2 : registeredBarSizes){
				if(bs2.getSecondsInBar() > bs.getSecondsInBar() &&
						bs2.getSecondsInBar() % bs.getSecondsInBar() == 0){
					if(!barSizes.containsKey(bs2) || 
							bs.getSecondsInBar() > barSizes.get(bs2).getSecondsInBar()){
						barSizes.put(bs2, bs);
					}
				}
			}
		}

		for(BarSize bs : barSizes.keySet()){
			System.out.println("adding " + bs + " to be generated after " + barSizes.get(bs));
			returnHash.get(barSizes.get(bs)).add(bs);
		}

		checkIfNewBarSizeIsNeeded();
	}

	private static void checkIfNewBarSizeIsNeeded() {
		// If the two smallest barSizes that are registered are 3min and 5min bars then we
		// need to generate the 1 minute bar because the bar publisher uses the smallest bar
		// size to generate larger bars and a 3 min bar cannot create a 5 min bar
		List<BarSize> missedBarSizes = new ArrayList<BarSize>();
		for(BarSize bs : returnHash.keySet()){
			boolean alreadyRegistered = false;
			for(BarSize bs2 : returnHash.keySet())
				if(returnHash.get(bs2).contains(bs))
					alreadyRegistered = true;
			if(!alreadyRegistered)
				missedBarSizes.add(bs);		

		}

		if(missedBarSizes.size() > 1){
			generateMissingBarsForSmallestBarSize(missedBarSizes);
		}

	}

	private static void generateMissingBarsForSmallestBarSize(List<BarSize> smallestBars) {

		BarSize newBarSize = null;

		for(BarSize bs : getAllBarSizes()){
			boolean eligibleBar = true;
			for(BarSize bs2 : smallestBars){
				if(bs.getSecondsInBar() >= bs2.getSecondsInBar() 
						|| bs2.getSecondsInBar() % bs.getSecondsInBar() != 0){
					eligibleBar = false;
				} 
			}
			if(eligibleBar && (newBarSize == null || newBarSize.getSecondsInBar() < bs.getSecondsInBar())){
				newBarSize = bs;
			}
		}
		System.out.println("created new bar size " + newBarSize + " to generate bars: " + smallestBars);
		registeredBarSizes.add(newBarSize);
		returnHash.put(newBarSize, smallestBars);
	}

	public static List<BarSize> registerAllSecurities(boolean usingTicks){

		if(usingTicks){
			registeredBarSizes.add(BarSize._1_SEC);
			registeredBarSizes.add(BarSize._5_SECS);
			registeredBarSizes.add(BarSize._10_SECS);
			registeredBarSizes.add(BarSize._15_SECS);
			registeredBarSizes.add(BarSize._30_SECS);
		}
		registeredBarSizes.add(BarSize._1_MIN);
		registeredBarSizes.add(BarSize._2_MINS);
		registeredBarSizes.add(BarSize._3_MINS);
		registeredBarSizes.add(BarSize._5_MINS);
		registeredBarSizes.add(BarSize._10_MINS);
		registeredBarSizes.add(BarSize._15_MINS);
		registeredBarSizes.add(BarSize._20_MINS);
		registeredBarSizes.add(BarSize._30_MINS);
		registeredBarSizes.add(BarSize._1_HOUR);
		registeredBarSizes.add(BarSize._4_HOURS);
		registeredBarSizes.add(BarSize._1_DAY);

		createNextBarSizeHasmap();

		return getRegisteredBarSizes();
	}

	public static List<BarSize> getNextBarSizes(BarSize barSize){

		if(returnHash.containsKey(barSize))
			return returnHash.get(barSize);
		else
			return Collections.emptyList();

	}

	private static List<BarSize> getAllBarSizes(){
		List<BarSize> returnList = new ArrayList<BarSize>();
		returnList.add(BarSize._1_SEC);
		returnList.add(BarSize._5_SECS);
		returnList.add(BarSize._10_SECS);
		returnList.add(BarSize._15_SECS);
		returnList.add(BarSize._30_SECS);
		returnList.add(BarSize._1_MIN);
		returnList.add(BarSize._2_MINS);
		returnList.add(BarSize._3_MINS);
		returnList.add(BarSize._5_MINS);
		returnList.add(BarSize._10_MINS);
		returnList.add(BarSize._15_MINS);
		returnList.add(BarSize._20_MINS);
		returnList.add(BarSize._30_MINS);
		returnList.add(BarSize._1_HOUR);
		returnList.add(BarSize._4_HOURS);
		returnList.add(BarSize._1_DAY);

		return returnList;

	}
	
	private static List<BarSize> getRegisteredBarSizes(){
		return registeredBarSizes;
	}


}
