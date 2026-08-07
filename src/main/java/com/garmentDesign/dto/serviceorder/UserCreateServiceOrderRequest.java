package com.garmentDesign.dto.serviceorder;

import java.math.BigDecimal;

public class UserCreateServiceOrderRequest {

    private Long serviceId;
    private Long addressId;
    private String productName;
    private String customerRequest;
    private BigDecimal quantity;

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
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

    public void setCustomerRequest(
            String customerRequest
    ) {
        this.customerRequest = customerRequest;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}