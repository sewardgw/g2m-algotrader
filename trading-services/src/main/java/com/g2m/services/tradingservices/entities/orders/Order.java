package com.g2m.services.tradingservices.entities.orders;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import com.g2m.services.tradingservices.annotations.OrderEntity;
import com.g2m.services.tradingservices.entities.Security;
import com.g2m.services.tradingservices.entities.Security.SecurityKey;
import com.g2m.services.tradingservices.enums.OrderAction;
import com.g2m.services.tradingservices.enums.OrderType;

/**
 * Added 5/7/2015.
 * 
 * @author Michael Borromeo
 */
@OrderEntity
public abstract class Order {
	@Id
	ObjectId id;
	protected OrderKey orderKey;
	protected int quantity;
	protected OrderAction orderAction;
	protected OrderType orderType;
	protected OrderStates orderStates;
	protected String reasonForOrder;
	protected Security security;
	protected double orderPrice;

	public OrderKey getKey() {
		return orderKey;
	}
	
	public Security getSecurity(){
		return security;
	}

	public SecurityKey getSecurityKey() {
		return security.getKey();
	}

	public int getQuantity() {
		return quantity;
	}

	public OrderAction getOrderAction() {
		return orderAction;
	}

	public OrderType getOrderType() {
		return orderType;
	}

	public OrderStates getOrderStates() {
		return orderStates;
	}

	public void setOrderStates(OrderStates orderStates) {
		this.orderStates = orderStates;
	}
	
	public void setOrderPrice(double orderPrice){
		this.orderPrice = orderPrice;
	}

	public double getOrderPrice() { return this.orderPrice;}
	
	public OrderState getLatestOrderState() {
		if (null == orderStates) {
			return null;
		}
		return orderStates.getLatestOrderState();
	}

	public void addOrderState(OrderState orderState) {
		orderStates.addOrderState(orderState);
	}

	public double getCommission() {
		if (null == getLatestOrderState()) {
			return 0;
		} else {
			return getLatestOrderState().getCommission();
		}
	}

	public String getReasonForOrder() {
		return reasonForOrder;
	}

	public void setReasonForOrder(String reasonForOrder) {
		this.reasonForOrder = reasonForOrder;
	}

	public abstract Order copyOrder();

	public abstract boolean isReadyToFill(double price);
}
