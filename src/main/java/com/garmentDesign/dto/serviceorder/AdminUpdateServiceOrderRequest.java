package com.garmentDesign.dto.serviceorder;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AdminUpdateServiceOrderRequest {

	private String status;
	private String productName;
	private String customerRequest;
	private String unitType;
	private BigDecimal quantity;
	private BigDecimal meterCount;
	private BigDecimal unitPrice;
	private BigDecimal discountAmount;
	private BigDecimal totalPrice;
	private LocalDate receivedDate;
	private LocalDate completedDate;

	public AdminUpdateServiceOrderRequest() {
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getCustomerRequest() {
		return customerRequest;
	}

	public void setCustomerRequest(String customerRequest) {
		this.customerRequest = customerRequest;
	}

	public String getUnitType() {
		return unitType;
	}

	public void setUnitType(String unitType) {
		this.unitType = unitType;
	}

	public BigDecimal getMeterCount() {
		return meterCount;
	}

	public void setMeterCount(BigDecimal meterCount) {
		this.meterCount = meterCount;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
	}

	public BigDecimal getDiscountAmount() {
		return discountAmount;
	}

	public void setDiscountAmount(BigDecimal discountAmount) {
		this.discountAmount = discountAmount;
	}

	public BigDecimal getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(BigDecimal totalPrice) {
		this.totalPrice = totalPrice;
	}

	public LocalDate getReceivedDate() {
		return receivedDate;
	}

	public void setReceivedDate(LocalDate receivedDate) {
		this.receivedDate = receivedDate;
	}

	public LocalDate getCompletedDate() {
		return completedDate;
	}

	public void setCompletedDate(LocalDate completedDate) {
		this.completedDate = completedDate;
	}
}
