package com.g2m.services.tradingservices.entities;

import java.util.Date;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.g2m.services.tradingservices.enums.BarSize;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
@Document(collection = "bars")
public class Bar {
	@Id
	private ObjectId id;
	private Security security;
	private Date dateTime;
	private Date startTime;
	private double high;
	private double low;
	private double open;
	private double close;
	private long volume;
	private BarSize barSize;
	@Transient
	private String toString;

	public Security getSecurity() {
		return security;
	}

	public Date getDateTime() {
		return dateTime;
	}
	
	public Date getStartDateTime() {
		return startTime;
	}

	public double getHigh() {
		return high;
	}

	public double getLow() {
		return low;
	}

	public double getOpen() {
		return open;
	}

	public double getClose() {
		return close;
	}

	public long getVolume() {
		return volume;
	}

	public BarSize getBarSize() {
		return barSize;
	}

	@Override
	public String toString() {
		return toString;
	}

	public static class BarBuilder {
		private Bar bar;

		public BarBuilder() {
			bar = new Bar();
		}

		public void setSecurity(Security security) {
			bar.security = security;
		}

		public void setDateTime(Date dateTime) {
			bar.dateTime = dateTime;
		}
		
		public void setStartDateTime(Date startDateTime){
			bar.startTime = startDateTime;
		}

		public void setHigh(double high) {
			bar.high = high;
		}

		public void setLow(double low) {
			bar.low = low;
		}

		public void setOpen(double open) {
			bar.open = open;
		}

		public void setClose(double close) {
			bar.close = close;
		}

		public void setVolume(long volume) {
			bar.volume = volume;
		}

		public void setBarSize(BarSize barSize) {
			bar.barSize = barSize;
		}

		public Bar build() {
			Bar bar = new Bar();
			bar.barSize = this.bar.barSize;
			bar.close = this.bar.close;
			bar.dateTime = this.bar.dateTime;
			bar.high = this.bar.high;
			bar.low = this.bar.low;
			bar.open = this.bar.open;
			bar.security = this.bar.security;
			bar.volume = this.bar.volume;
			bar.startTime = this.bar.startTime;

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("Bar ").append(bar.barSize).append(" ").append(bar.security.getSymbol()).append(": ")
					.append(bar.dateTime);
			stringBuilder.append(" High=").append(bar.high);
			stringBuilder.append(" Low=").append(bar.low);
			stringBuilder.append(" Open=").append(bar.open);
			stringBuilder.append(" Close=").append(bar.close);
			bar.toString = stringBuilder.toString();

			return bar;
		}
	}
}
