package com.g2m.services.historicaldataloader;

import java.util.concurrent.TimeUnit;

import com.ib.controller.Types.DurationUnit;

/**
 * @author Michael Borromeo
 */
public class DurationWithUnit implements Comparable<DurationWithUnit> {
	public static final DurationWithUnit _1_YEAR = new DurationWithUnit(DurationUnit.YEAR, 1);
	public static final DurationWithUnit _1_MONTH = new DurationWithUnit(DurationUnit.MONTH, 1);
	public static final DurationWithUnit _1_WEEK = new DurationWithUnit(DurationUnit.WEEK, 1);
	public static final DurationWithUnit _2_DAYS = new DurationWithUnit(DurationUnit.DAY, 2);
	public static final DurationWithUnit _1_DAY = new DurationWithUnit(DurationUnit.DAY, 1);
	public static final DurationWithUnit _4_HOURS = new DurationWithUnit(DurationUnit.SECOND, 14400);
	public static final DurationWithUnit _2_HOURS = new DurationWithUnit(DurationUnit.SECOND, 7200);
	public static final DurationWithUnit _1_HOUR = new DurationWithUnit(DurationUnit.SECOND, 3600);
	public static final DurationWithUnit _30_MINUTES = new DurationWithUnit(DurationUnit.SECOND, 1800);
	public static final DurationWithUnit _15_MINUTES = new DurationWithUnit(DurationUnit.SECOND, 900);
	public static final DurationWithUnit _5_MINUTES = new DurationWithUnit(DurationUnit.SECOND, 300);
	public static final DurationWithUnit _1_MINUTE = new DurationWithUnit(DurationUnit.SECOND, 60);

	private final DurationUnit durationUnit;
	private final long duration;
	private final long durationInSeconds;

	public DurationWithUnit(DurationUnit durationUnit, long duration) {
		this.durationUnit = durationUnit;
		this.duration = duration;
		this.durationInSeconds = DurationWithUnit.convertDurationToSeconds(durationUnit, duration);
	}

	public DurationUnit getDurationUnit() {
		return this.durationUnit;
	}

	public long getDuration() {
		return this.duration;
	}

	public long getDurationInSeconds() {
		return this.durationInSeconds;
	}

	/*
	 * These conversions are used to avoid repetition in setting up minimum bar
	 * sizes for a given duration. E.g. 1 year, 365 days, ..., 30585600 seconds
	 * should all have the same minimum bar sizes. Even though a year doesn't
	 * have exactly 365 days it won't matter since the conversions are only used
	 * for mapping purposes. Same goes for months.
	 */
	public static long convertDurationToSeconds(DurationUnit durationUnit, long duration) {
		if (DurationUnit.YEAR == durationUnit) {
			return duration * ValidBarSizesForDurations.DAYS_IN_YEAR * TimeUnit.DAYS.toSeconds(1);
		} else if (DurationUnit.MONTH == durationUnit) {
			return duration * ValidBarSizesForDurations.DAYS_IN_MONTH * TimeUnit.DAYS.toSeconds(1);
		} else if (DurationUnit.WEEK == durationUnit) {
			return duration * ValidBarSizesForDurations.DAYS_IN_WEEK * TimeUnit.DAYS.toSeconds(1);
		} else if (DurationUnit.DAY == durationUnit) {
			return duration * TimeUnit.DAYS.toSeconds(1);
		} else if (DurationUnit.SECOND == durationUnit) {
			return duration;
		} else {
			throw new IllegalArgumentException("Unknown DurationUnit argument: " + durationUnit.toString());
		}
	}

	public static DurationWithUnit convertSecondsToLargestUnit(long seconds) {
		DurationWithUnit durations[] = { DurationWithUnit._1_YEAR, DurationWithUnit._1_MONTH, DurationWithUnit._1_WEEK, DurationWithUnit._2_DAYS,
				DurationWithUnit._1_DAY, DurationWithUnit._4_HOURS, DurationWithUnit._2_HOURS, DurationWithUnit._1_HOUR,
				DurationWithUnit._30_MINUTES, DurationWithUnit._15_MINUTES, DurationWithUnit._5_MINUTES, DurationWithUnit._1_MINUTE };

		DurationWithUnit lastDuration = null;
		for (DurationWithUnit duration : durations) {
			lastDuration = duration;

			if (duration.getDurationInSeconds() <= seconds) {
				break;
			}
		}

		long multiplier = 1;
		while (true) {
			if (lastDuration.getDurationInSeconds() * multiplier >= seconds) {
				break;
			}

			multiplier++;
		}

		return new DurationWithUnit(lastDuration.getDurationUnit(), multiplier * lastDuration.getDuration());
	}

	@Override
	public int compareTo(DurationWithUnit durationToCompare) {
		if (durationToCompare.getDurationInSeconds() == this.getDurationInSeconds())
			return 0;
		else
			return (durationToCompare.getDurationInSeconds() > this.getDurationInSeconds() ? -1 : 1);
	}

	@Override
	public String toString() {
		return this.duration + " " + this.durationUnit.toString();
	}
}