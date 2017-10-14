package com.g2m.services.historicaldataloader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ib.controller.Types.BarSize;
import com.ib.controller.Types.DurationUnit;

/**
 * @author Michael Borromeo
 */
public class ValidBarSizesForDurations {
	private static Map<DurationWithUnit, List<BarSize>> valueMap;

	final static int DAYS_IN_MONTH = 30;
	final static int DAYS_IN_YEAR = 365;
	final static int DAYS_IN_WEEK = 7;

	static {
		ValidBarSizesForDurations.setupValidBarSizesforDurations();
	}

	private static void setupValidBarSizesforDurations() {
		ValidBarSizesForDurations.valueMap = new TreeMap<DurationWithUnit, List<BarSize>>();

		// duration < 60 seconds -> bar size = 30 secs, 15 secs, 5 secs, 1 secs
		List<BarSize> barSizes = new ArrayList<BarSize>();
		barSizes.add(BarSize._30_secs);
		barSizes.add(BarSize._15_secs);
		barSizes.add(BarSize._10_secs);
		barSizes.add(BarSize._5_secs);
		barSizes.add(BarSize._1_secs);
		ValidBarSizesForDurations.valueMap.put(DurationWithUnit._1_MINUTE, barSizes);

		// duration < 300 seconds -> bar size = 3 mins, 2 mins, 1 min, 30 secs,
		// 15 secs, 5 secs, 1 secs
		barSizes = new ArrayList<BarSize>();
		barSizes.add(BarSize._3_mins);
		barSizes.add(BarSize._2_mins);
		barSizes.add(BarSize._1_min);
		barSizes.add(BarSize._30_secs);
		barSizes.add(BarSize._15_secs);
		barSizes.add(BarSize._10_secs);
		barSizes.add(BarSize._5_secs);
		barSizes.add(BarSize._1_secs);
		ValidBarSizesForDurations.valueMap.put(DurationWithUnit._5_MINUTES, barSizes);

		// duration < 900 seconds -> bar size = 5 mins, 3 mins, 2 mins, 1 min,
		// 30 secs, 15 secs, 5 secs, 1 secs
		barSizes = new ArrayList<BarSize>();
		barSizes.add(BarSize._5_mins);
		barSizes.add(BarSize._3_mins);
		barSizes.add(BarSize._2_mins);
		barSizes.add(BarSize._1_min);
		barSizes.add(BarSize._30_secs);
		barSizes.add(BarSize._15_secs);
		barSizes.add(BarSize._10_secs);
		barSizes.add(BarSize._5_secs);
		barSizes.add(BarSize._1_secs);
		ValidBarSizesForDurations.valueMap.put(DurationWithUnit._15_MINUTES, barSizes);

		// duration < 1800 seconds -> bar size = 15 mins, 5 mins, 3 mins, 2
		// mins, 1 min, 30 secs, 15 secs, 5 secs, 1 secs
		barSizes = new ArrayList<BarSize>();
		barSizes.add(BarSize._15_mins);
		barSizes.add(BarSize._5_mins);
		barSizes.add(BarSize._3_mins);
		barSizes.add(BarSize._2_mins);
		barSizes.add(BarSize._1_min);
		barSizes.add(BarSize._30_secs);
		barSizes.add(BarSize._15_secs);
		barSizes.add(BarSize._5_secs);
		barSizes.add(BarSize._10_secs);
		barSizes.add(BarSize._1_secs);
		ValidBarSizesForDurations.valueMap.put(DurationWithUnit._30_MINUTES, barSizes);

		// duration < 3600 seconds -> bar size = 15 mins, 5 mins, 3 mins, 2
		// mins, 1 min, 30 secs, 15 secs, 5 secs
		barSizes = new ArrayList<BarSize>();
		barSizes.add(BarSize._15_mins);
		barSizes.add(BarSize._5_mins);
		barSizes.add(BarSize._3_mins);
		barSizes.add(BarSize._2_mins);
		barSizes.add(BarSize._1_min);
		barSizes.add(BarSize._30_secs);
		barSizes.add(BarSize._15_secs);
		barSizes.add(BarSize._10_secs);
		barSizes.add(BarSize._5_secs);
		ValidBarSizesForDurations.valueMap.put(DurationWithUnit._1_HOUR, barSizes);

		// duration < 7200 seconds -> bar size = 1 hour, 30 mins, 15 mins,
		// 5mins, 3 mins, 2 mins, 1 min, 30 secs, 15 secs, 5 secs
		barSizes = new ArrayList<BarSize>();
		barSizes.add(BarSize._1_hour);
		barSizes.add(BarSize._30_mins);
		barSizes.add(BarSize._15_mins);
		barSizes.add(BarSize._5_mins);
		barSizes.add(BarSize._3_mins);
		barSizes.add(BarSize._2_mins);
		barSizes.add(BarSize._1_min);
		barSizes.add(BarSize._30_secs);
		barSizes.add(BarSize._15_secs);
		barSizes.add(BarSize._10_secs);
		barSizes.add(BarSize._5_secs);
		ValidBarSizesForDurations.valueMap.put(DurationWithUnit._2_HOURS, barSizes);

		// duration < 14400 seconds -> bar size = 1 hour, 30 mins, 15 mins, 5
		// mins, 3 mins, 2 mins, 1 min, 30 secs, 15 secs
		barSizes = new ArrayList<BarSize>();
		barSizes.add(BarSize._1_hour);
		barSizes.add(BarSize._30_mins);
		barSizes.add(BarSize._15_mins);
		barSizes.add(BarSize._5_mins);
		barSizes.add(BarSize._3_mins);
		barSizes.add(BarSize._2_mins);
		barSizes.add(BarSize._1_min);
		barSizes.add(BarSize._30_secs);
		barSizes.add(BarSize._15_secs);
		barSizes.add(BarSize._10_secs);
		ValidBarSizesForDurations.valueMap.put(DurationWithUnit._4_HOURS, barSizes);

		// duration < 1 day -> bar size = 1 hour, 30 mins, 15 mins, 5 mins, 3
		// mins, 2 mins, 1 min, 30 secs
		barSizes = new ArrayList<BarSize>();
		barSizes.add(BarSize._4_hours);
		barSizes.add(BarSize._1_hour);
		barSizes.add(BarSize._30_mins);
		barSizes.add(BarSize._15_mins);
		barSizes.add(BarSize._5_mins);
		barSizes.add(BarSize._3_mins);
		barSizes.add(BarSize._2_mins);
		barSizes.add(BarSize._1_min);
		barSizes.add(BarSize._30_secs);
		ValidBarSizesForDurations.valueMap.put(DurationWithUnit._1_DAY, barSizes);

		// duration < 2 days -> bar size = 1 hour, 30 mins, 15 mins, 3 mins, 2
		// mins, 1 min
		barSizes = new ArrayList<BarSize>();
		barSizes.add(BarSize._1_day);
		barSizes.add(BarSize._4_hours);
		barSizes.add(BarSize._1_hour);
		barSizes.add(BarSize._30_mins);
		barSizes.add(BarSize._15_mins);
		barSizes.add(BarSize._5_mins);
		barSizes.add(BarSize._3_mins);
		barSizes.add(BarSize._2_mins);
		barSizes.add(BarSize._1_min);
		ValidBarSizesForDurations.valueMap.put(DurationWithUnit._2_DAYS, barSizes);

		// duration < 1 week
		barSizes = new ArrayList<BarSize>();
		barSizes.add(BarSize._1_day);
		barSizes.add(BarSize._4_hours);
		barSizes.add(BarSize._1_hour);
		barSizes.add(BarSize._30_mins);
		barSizes.add(BarSize._15_mins);
		barSizes.add(BarSize._5_mins);
		barSizes.add(BarSize._3_mins);
		barSizes.add(BarSize._2_mins);
		barSizes.add(BarSize._1_min);
		ValidBarSizesForDurations.valueMap.put(DurationWithUnit._1_WEEK, barSizes);

		// duration < 1 month
		barSizes = new ArrayList<BarSize>();
		barSizes.add(BarSize._1_day);
		barSizes.add(BarSize._4_hours);
		barSizes.add(BarSize._1_hour);
		ValidBarSizesForDurations.valueMap.put(DurationWithUnit._1_MONTH, barSizes);

		// duration < 1 year
		barSizes = new ArrayList<BarSize>();
		barSizes.add(BarSize._1_day);
		ValidBarSizesForDurations.valueMap.put(DurationWithUnit._1_YEAR, barSizes);
	}

	public static List<BarSize> getFromDuration(DurationUnit durationUnit, int duration) {
		Long durationInSeconds = DurationWithUnit.convertDurationToSeconds(durationUnit, duration);
		for (DurationWithUnit minimumDuration : ValidBarSizesForDurations.valueMap.keySet()) {
			if (durationInSeconds <= minimumDuration.getDurationInSeconds())
				return ValidBarSizesForDurations.valueMap.get(minimumDuration);
		}

		return Collections.<BarSize> emptyList();
	}

	public static DurationWithUnit getLargestDurationFromBarSize(BarSize barSize) {
		Object[] keys = ValidBarSizesForDurations.valueMap.keySet().toArray();
		for (int i = keys.length - 1; i >= 0; i--) {
			DurationWithUnit key = (DurationWithUnit) keys[i];
			List<BarSize> barSizes = ValidBarSizesForDurations.valueMap.get(key);

			if (barSizes.contains(barSize)) {
				return key;
			}
		}

		throw new IllegalArgumentException("Unhandled BarSize argument: " + barSize.toString());
	}
}
