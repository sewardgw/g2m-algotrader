package com.g2m.services.tradingservices.brokerage.mappers;

import com.g2m.services.tradingservices.enums.BarSize;
import com.g2m.services.tradingservices.enums.OrderAction;
import com.ib.controller.Types.Action;

/**
 * Added 3/29/2015.
 * 
 * @author Michael Borromeo
 * 
 *         TODO Refactor the if's to switch's.
 */
public class BrokerageEnumMapper {
	public static com.ib.controller.Types.Right getRight(com.g2m.services.tradingservices.enums.Right right) {
		if (com.g2m.services.tradingservices.enums.Right.CALL.equals(right)) {
			return com.ib.controller.Types.Right.Call;
		} else if (com.g2m.services.tradingservices.enums.Right.PUT.equals(right)) {
			return com.ib.controller.Types.Right.Put;
		}

		return com.ib.controller.Types.Right.None;
	}

	public static com.g2m.services.tradingservices.enums.Right getRight(com.ib.controller.Types.Right right) {
		if (com.ib.controller.Types.Right.Call.equals(right)) {
			return com.g2m.services.tradingservices.enums.Right.CALL;
		} else if (com.ib.controller.Types.Right.Put.equals(right)) {
			return com.g2m.services.tradingservices.enums.Right.PUT;
		}

		return com.g2m.services.tradingservices.enums.Right.NONE;
	}

	public static com.g2m.services.tradingservices.enums.OrderStatus getOrderStatus(
			com.ib.controller.OrderStatus orderStatus) {
		switch (orderStatus) {
		case ApiPending:
			return com.g2m.services.tradingservices.enums.OrderStatus.API_PENDING;
		case ApiCancelled:
			return com.g2m.services.tradingservices.enums.OrderStatus.API_CANCELLED;
		case PreSubmitted:
			return com.g2m.services.tradingservices.enums.OrderStatus.PRE_SUBMITTED;
		case PendingCancel:
			return com.g2m.services.tradingservices.enums.OrderStatus.PENDING_CANCEL;
		case Cancelled:
			return com.g2m.services.tradingservices.enums.OrderStatus.CANCELLED;
		case Submitted:
			return com.g2m.services.tradingservices.enums.OrderStatus.SUBMITTED;
		case Filled:
			return com.g2m.services.tradingservices.enums.OrderStatus.FILLED;
		case Inactive:
			return com.g2m.services.tradingservices.enums.OrderStatus.INACTIVE;
		case PendingSubmit:
			return com.g2m.services.tradingservices.enums.OrderStatus.PENDING_SUBMIT;
		case Unknown:
		default:
			return com.g2m.services.tradingservices.enums.OrderStatus.UNKNOWN;
		}
	}

	public static com.ib.controller.OrderType getOrderType(com.g2m.services.tradingservices.enums.OrderType orderType) {
		switch (orderType) {
		case MARKET:
			return com.ib.controller.OrderType.MKT;
		case LIMIT:
			return com.ib.controller.OrderType.LMT;
		case STOP:
			return com.ib.controller.OrderType.STP;
		default:
			return com.ib.controller.OrderType.MKT;
		}
	}

	public static Action getOrderAction(OrderAction orderAction) {
		switch (orderAction) {
		case BUY:
			return Action.BUY;
		case SELL:
			return Action.SELL;
		default:
			return Action.BUY;
		}
	}

	public static com.ib.controller.Types.SecType getSecurityType(
			com.g2m.services.tradingservices.enums.SecurityType securityType) {
		if (com.g2m.services.tradingservices.enums.SecurityType.FUTURE.equals(securityType)) {
			return com.ib.controller.Types.SecType.FUT;
		} else if (com.g2m.services.tradingservices.enums.SecurityType.STOCK.equals(securityType)) {
			return com.ib.controller.Types.SecType.STK;
		} else if (com.g2m.services.tradingservices.enums.SecurityType.CASH.equals(securityType)) {
			return com.ib.controller.Types.SecType.CASH;
		}

		return com.ib.controller.Types.SecType.None;
	}

	public static com.g2m.services.tradingservices.enums.SecurityType getSecurityType(
			com.ib.controller.Types.SecType securityType) {
		if (com.ib.controller.Types.SecType.FUT.equals(securityType)) {
			return com.g2m.services.tradingservices.enums.SecurityType.FUTURE;
		} else if (com.ib.controller.Types.SecType.STK.equals(securityType)) {
			return com.g2m.services.tradingservices.enums.SecurityType.STOCK;
		}

		return com.g2m.services.tradingservices.enums.SecurityType.NONE;
	}

	public static BarSize getBarSize(com.ib.controller.Types.BarSize barSize) {
		switch (barSize) {
		case _1_secs:
			return BarSize._1_SEC;
		case _5_secs:
			return BarSize._5_SECS;
		case _10_secs:
			return BarSize._10_SECS;
		case _15_secs:
			return BarSize._15_SECS;
		case _30_secs:
			return BarSize._30_SECS;
		case _1_min:
			return BarSize._1_MIN;
		case _2_mins:
			return BarSize._2_MINS;
		case _3_mins:
			return BarSize._3_MINS;
		case _5_mins:
			return BarSize._5_MINS;
		case _10_mins:
			return BarSize._10_MINS;
		case _15_mins:
			return BarSize._15_MINS;
		case _20_mins:
			return BarSize._20_MINS;
		case _30_mins:
			return BarSize._30_MINS;
		case _1_hour:
			return BarSize._1_HOUR;
		case _4_hours:
			return BarSize._4_HOURS;
		case _1_day:
			return BarSize._1_DAY;
		case _1_week:
		default:
			throw new IllegalArgumentException("Cannot map " + barSize.toString());
		}
	}

	public static com.ib.controller.Types.BarSize getBarSize(BarSize barSize) {
		switch (barSize) {
		case _1_SEC:
			return com.ib.controller.Types.BarSize._1_secs;
		case _5_SECS:
			return com.ib.controller.Types.BarSize._5_secs;
		case _10_SECS:
			return com.ib.controller.Types.BarSize._10_secs;
		case _15_SECS:
			return com.ib.controller.Types.BarSize._15_secs;
		case _30_SECS:
			return com.ib.controller.Types.BarSize._30_secs;
		case _1_MIN:
			return com.ib.controller.Types.BarSize._1_min;
		case _2_MINS:
			return com.ib.controller.Types.BarSize._2_mins;
		case _3_MINS:
			return com.ib.controller.Types.BarSize._3_mins;
		case _5_MINS:
			return com.ib.controller.Types.BarSize._5_mins;
		case _10_MINS:
			return com.ib.controller.Types.BarSize._10_mins;
		case _15_MINS:
			return com.ib.controller.Types.BarSize._15_mins;
		case _20_MINS:
			return com.ib.controller.Types.BarSize._20_mins;
		case _30_MINS:
			return com.ib.controller.Types.BarSize._30_mins;
		case _1_HOUR:
			return com.ib.controller.Types.BarSize._1_hour;
		case _4_HOURS:
			return com.ib.controller.Types.BarSize._4_hours;
		case _1_DAY:
			return com.ib.controller.Types.BarSize._1_day;
		default:
			throw new IllegalArgumentException("Cannot map " + barSize.toString());
		}
	}
}
