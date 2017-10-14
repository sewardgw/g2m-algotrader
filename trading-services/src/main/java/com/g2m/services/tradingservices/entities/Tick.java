package com.g2m.services.tradingservices.entities;

import java.util.Date;
import java.util.HashMap;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.g2m.services.tradingservices.entities.Security.SecurityKey;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
@Document(collection = "ticks")
public class Tick {
	@Id
	private ObjectId id;
	private Security security;
	private double lastPrice;
	private Date dateTime;
	private int volume;
	private int volumeBid;
	private int volumeAsk;
	private double bidPrice;
	private double askPrice;
	private int openInterest;
	private double settlement;
	private double change;
	@Transient
	private String toString;
	private static HashMap<SecurityKey, Long> lastVolume;

	public Tick() {
	}

	public Security getSecurity() {
		return security;
	}

	public double getLastPrice() {
		return lastPrice;
	}

	public Date getDateTime() {
		return dateTime;
	}

	public int getVolume() {
		return volume;
	}

	public int getVolumeBid() {
		return volumeBid;
	}

	public int getVolumeAsk() {
		return volumeAsk;
	}

	public double getBidPrice() {
		return bidPrice;
	}

	public double getAskPrice() {
		return askPrice;
	}

	public int getOpenInterest() {
		return openInterest;
	}

	public double getSettlement() {
		return settlement;
	}

	public double getChange() {
		return change;
	}

	@Override
	public String toString() {
		return toString;
	}

	public static class TickBuilder {
		private Tick tick;

		public TickBuilder() {
			tick = new Tick();
		}

		public TickBuilder setSecurity(Security security) {
			tick.security = security;
			return this;
		}

		public TickBuilder setLastPrice(double lastPrice) {
			tick.lastPrice = lastPrice;
			return this;
		}

		public TickBuilder setDateTime(Date dateTime) {
			tick.dateTime = dateTime;
			return this;
		}

		public TickBuilder setVolume(int volume) {
			tick.volume = volume;
			return this;
		}

		public TickBuilder setVolumeBid(int volumeBid) {
			tick.volumeBid = volumeBid;
			return this;
		}

		public TickBuilder setVolumeAsk(int volumeAsk) {
			tick.volumeAsk = volumeAsk;
			return this;
		}

		public TickBuilder setBidPrice(double bidPrice) {
			tick.bidPrice = bidPrice;
			return this;
		}

		public TickBuilder setAskPrice(double askPrice) {
			tick.askPrice = askPrice;
			return this;
		}

		public TickBuilder setOpenInterest(int openInterest) {
			tick.openInterest = openInterest;
			return this;
		}

		public TickBuilder setSettlement(double settlement) {
			tick.settlement = settlement;
			return this;
		}

		public TickBuilder setChange(double change) {
			tick.change = change;
			return this;
		}

		public Tick build() {
			Tick tick = new Tick();
			tick.askPrice = this.tick.askPrice;
			tick.bidPrice = this.tick.bidPrice;
			tick.change = this.tick.change;
			tick.dateTime = this.tick.dateTime;
			tick.openInterest = this.tick.openInterest;
			tick.security = this.tick.security;
			tick.settlement = this.tick.settlement;
			tick.volume = this.tick.volume;
			tick.volumeAsk = this.tick.volumeAsk;
			tick.volumeBid = this.tick.volumeBid;
			
			// If this tick doesnt have a last traded price
			// Then create it with the average of the ask
			// And the bid
			if(this.tick.lastPrice != 0)
				tick.lastPrice = this.tick.lastPrice;
			else
				tick.lastPrice = (this.tick.askPrice + this.tick.bidPrice) / 2;

			StringBuilder builder = new StringBuilder();
			builder.append(tick.getSecurity().getSymbol());
			builder.append(" dateTime=").append(tick.getDateTime());
			builder.append(" lastPrice=").append(tick.getLastPrice());
			builder.append(" volume=").append(tick.getVolume());
			builder.append(" bidPrice=").append(tick.getBidPrice());
			builder.append(" volumeBid=").append(tick.getVolumeBid());
			builder.append(" askPrice=").append(tick.getAskPrice());
			builder.append(" volumeAsk=").append(tick.getVolumeAsk());
			builder.append(" openInterest=").append(tick.getOpenInterest());
			builder.append(" settlement=").append(tick.getSettlement());
			tick.toString = builder.toString();

			return tick;
		}
	}
}
