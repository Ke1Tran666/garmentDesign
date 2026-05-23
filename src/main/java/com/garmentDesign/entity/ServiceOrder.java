package com.garmentDesign.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Service_Orders")
public class ServiceOrder {
	
    @Id
    private Long serviceOrderId;
    @ManyToOne @JoinColumn(name = "id_User") private User user;
    @ManyToOne @JoinColumn(name = "service_id") private Service service;
    @ManyToOne @JoinColumn(name = "address_id") private UserAddress address;
    private String productName;
    private String productImage;
    private String customerRequest;
    private String unitType;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private String status;
    private LocalDate receivedDate;
    private LocalDate completedDate;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    
    public ServiceOrder() {}
    @PrePersist public void prePersist(){ createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate public void preUpdate(){ updatedAt = LocalDateTime.now(); }
    public Long getServiceOrderId(){return serviceOrderId;} 
    public void setServiceOrderId(Long serviceOrderId){this.serviceOrderId=serviceOrderId;}
    
    public User getUser(){return user;} 
    public void setUser(User user){this.user=user;}
    
    public Service getService(){return service;} 
    public void setService(Service service){this.service=service;}
    
    public UserAddress getAddress(){return address;} 
    public void setAddress(UserAddress address){this.address=address;}
    
    public String getProductName(){return productName;} 
    public void setProductName(String productName){this.productName=productName;}
    
    public String getProductImage(){return productImage;} 
    public void setProductImage(String productImage){this.productImage=productImage;}
    
    public String getCustomerRequest(){return customerRequest;} 
    public void setCustomerRequest(String customerRequest){this.customerRequest=customerRequest;}
    
    public String getUnitType(){return unitType;} 
    public void setUnitType(String unitType){this.unitType=unitType;}
    
    public BigDecimal getQuantity(){return quantity;} 
    public void setQuantity(BigDecimal quantity){this.quantity=quantity;}
    
    public BigDecimal getUnitPrice(){return unitPrice;} 
    public void setUnitPrice(BigDecimal unitPrice){this.unitPrice=unitPrice;}
    
    public BigDecimal getDiscountAmount(){return discountAmount;} 
    public void setDiscountAmount(BigDecimal discountAmount){this.discountAmount=discountAmount;}
    
    public BigDecimal getTotalPrice(){return totalPrice;} 
    public void setTotalPrice(BigDecimal totalPrice){this.totalPrice=totalPrice;}
    
    public String getStatus(){return status;} 
    public void setStatus(String status){this.status=status;}
    
    public LocalDate getReceivedDate(){return receivedDate;} 
    public void setReceivedDate(LocalDate receivedDate){this.receivedDate=receivedDate;}
    
    public LocalDate getCompletedDate(){return completedDate;} 
    public void setCompletedDate(LocalDate completedDate){this.completedDate=completedDate;}
    
    public String getCreatedBy(){return createdBy;} 
    public void setCreatedBy(String createdBy){this.createdBy=createdBy;}
    
    public String getUpdatedBy(){return updatedBy;} 
    public void setUpdatedBy(String updatedBy){this.updatedBy=updatedBy;}
    
    public LocalDateTime getCreatedAt(){return createdAt;} 
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
    
    public LocalDateTime getUpdatedAt(){return updatedAt;} 
    public void setUpdatedAt(LocalDateTime updatedAt){this.updatedAt=updatedAt;}
    
    public LocalDateTime getDeletedAt(){return deletedAt;} 
    public void setDeletedAt(LocalDateTime deletedAt){this.deletedAt=deletedAt;}
}
