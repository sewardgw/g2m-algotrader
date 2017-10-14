package com.g2m.services.historicaldataloader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.ib.controller.NewContract;
import com.ib.controller.Types.BarSize;
import com.ib.controller.Types.DurationUnit;
import com.ib.controller.Types.WhatToShow;

/**
 * @author Michael Borromeo
 */
public class HistoricalRequest {
	private int id;
	private NewContract contract;
	private Date endDate;
	private boolean isRegularTradingHours;
	private DurationWithUnit duration;
	private BarSize barSize;
	private WhatToShow whatToShow;
	private boolean isRequestComplete;
	private boolean wasRequestSuccessful;
	private List<Date> attempts = new LinkedList<Date>();

	public HistoricalRequest() {
		this.isRequestComplete = false;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append(this.getContract().symbol());
		builder.append(" id=").append(this.getContract().conid());
		builder.append(" secType=").append(this.getContract().secType());
		builder.append(" expiry=").append(this.getContract().expiry());
		builder.append(" strike=").append(this.getContract().strike());
		builder.append(" right=").append(this.getContract().right());
		builder.append(" exchange=").append(this.getContract().exchange());
		builder.append(" currency=").append(this.getContract().currency());
		builder.append(" secIdType=").append(this.getContract().secIdType());

		builder.append(" barSize=").append(this.getBarSize());
		builder.append(" duration=").append(this.getDuration().toString());
		builder.append(" endDate=").append(this.getEndDate());
		builder.append(" rth=").append(this.isRegularTradingHours());
		builder.append(" whatToShow=").append(this.getWhatToShow());

		builder.append(" attempts=").append(this.getAttempts().size());

		if (this.isRequestComplete())
			builder.append(" success=").append(this.wasRequestSuccessful());

		return builder.toString();
	}

	public NewContract getContract() {
		return contract;
	}

	public void setContract(NewContract contract) {
		this.contract = contract;
	}

	public String getFormattedEndDate() {
		return (new SimpleDateFormat("yyyyMMdd HH:mm:ss"))
				.format(null == this.getEndDate() ? new Date() : this.getEndDate());
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public boolean isRegularTradingHours() {
		return isRegularTradingHours;
	}

	public void setRegularTradingHours(boolean isRegularTradingHours) {
		this.isRegularTradingHours = isRegularTradingHours;
	}

	public DurationWithUnit getDuration() {
		return duration;
	}

	public long getDurationValue() {
		return duration.getDuration();
	}

	public DurationUnit getDurationUnit() {
		return duration.getDurationUnit();
	}

	public void setDuration(DurationWithUnit duration) {
		this.duration = duration;
	}

	public BarSize getBarSize() {
		return barSize;
	}

	public void setBarSize(BarSize barSize) {
		this.barSize = barSize;
	}

	public WhatToShow getWhatToShow() {
		return whatToShow;
	}

	public void setWhatToShow(WhatToShow whatToShow) {
		this.whatToShow = whatToShow;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isRequestComplete() {
		return isRequestComplete;
	}

	public void setRequestComplete(boolean isRequestComplete) {
		this.isRequestComplete = isRequestComplete;
	}

	public boolean wasRequestSuccessful() {
		return this.wasRequestSuccessful;
	}

	public void setRequestSuccessful(boolean wasRequestSuccessful) {
		this.wasRequestSuccessful = wasRequestSuccessful;
	}

	public Date getRequestTimeStamp() {
		if (0 == this.attempts.size()) {
			return null;
		} else {
			return this.attempts.get(this.attempts.size() - 1);
		}
	}

	public List<Date> getAttempts() {
		return attempts;
	}

	public void attemptMade() {
		this.attempts.add(new Date());
	}

}
