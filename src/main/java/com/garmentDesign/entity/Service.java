package com.garmentDesign.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Services")
public class Service {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "service_id")
	private Long serviceId;
	
	@Column(name = "service_name")
	private String serviceName;

	@Column(name = "unit_type")
	private String unitType;
	
	@Column(name = "base_price")
	private BigDecimal basePrice;

    private String description;
    
    private String status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    public Service() {}
    @PrePersist public void prePersist(){ createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate public void preUpdate(){ updatedAt = LocalDateTime.now(); }
    public Long getServiceId(){return serviceId;} 
    public void setServiceId(Long serviceId){this.serviceId=serviceId;}
    
    public String getServiceName(){return serviceName;} 
    public void setServiceName(String serviceName){this.serviceName=serviceName;}
    
    public String getUnitType(){return unitType;} 
    public void setUnitType(String unitType){this.unitType=unitType;}
    
    public BigDecimal getBasePrice(){return basePrice;} 
    public void setBasePrice(BigDecimal basePrice){this.basePrice=basePrice;}
    
    public String getDescription(){return description;} 
    public void setDescription(String description){this.description=description;}
    
    public String getStatus(){return status;} 
    public void setStatus(String status){this.status=status;}
    
    public LocalDateTime getCreatedAt(){return createdAt;} 
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
    
    public LocalDateTime getUpdatedAt(){return updatedAt;} 
    public void setUpdatedAt(LocalDateTime updatedAt){this.updatedAt=updatedAt;}
    
    public LocalDateTime getDeletedAt(){return deletedAt;} 
    public void setDeletedAt(LocalDateTime deletedAt){this.deletedAt=deletedAt;}
}
